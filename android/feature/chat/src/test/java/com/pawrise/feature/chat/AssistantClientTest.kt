package com.pawrise.feature.chat

import com.pawrise.feature.chat.assistant.AssistantClient
import com.pawrise.feature.chat.assistant.AssistantConfig
import com.pawrise.feature.chat.assistant.AssistantEvent
import com.pawrise.feature.chat.assistant.AssistantException
import com.pawrise.feature.chat.assistant.AssistantTurn
import com.pawrise.feature.chat.assistant.AssistantTurnRequest
import com.pawrise.feature.chat.assistant.SseParser
import com.pawrise.feature.chat.assistant.TurnRole
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AssistantClientTest {
    private val server = MockWebServer()

    @Before fun setUp() = server.start()

    @After fun tearDown() = server.close()

    private fun client() = AssistantClient(OkHttpClient(), AssistantConfig(server.url("/").toString(), null))

    private val request = AssistantTurnRequest(
        threadId = "t1",
        turnId = "u1",
        petRef = "pet_demo_nala",
        userMessage = "Et son sommeil ?",
        history = listOf(AssistantTurn(TurnRole.Owner, "Bonjour"), AssistantTurn(TurnRole.Assistant, "Salut")),
    )

    private fun sse(body: String) = MockResponse.Builder().addHeader("Content-Type", "text/event-stream").body(body).build()

    @Test
    fun `flux status puis response, requête en snake_case`() = runTest {
        server.enqueue(
            sse(
                ": ping\n\n" +
                    "event: status\ndata: {\"type\":\"status\",\"text\":\"Je regarde les données du collier…\"}\n\n" +
                    "event: response\r\ndata: {\"type\":\"response\",\"response\":{\"schema_version\":\"1\",\"response_text\":\"Nala — 11 h de sommeil.\"," +
                    "\"citations\":[],\"escalation\":{\"trigger\":false},\"suggested_actions\":[],\"metadata\":{\"thread_id\":\"t1\",\"turn_id\":\"u1\",\"latency_ms\":12}}}\r\n\r\n",
            ),
        )
        val events = client().streamTurn(request).toList()
        assertEquals(AssistantEvent.Status("Je regarde les données du collier…"), events[0])
        val response = (events[1] as AssistantEvent.Response).response
        assertEquals("Nala — 11 h de sommeil.", response.responseText)
        assertFalse(response.isUrgent)

        val recorded = server.takeRequest()
        assertEquals("/v1/turns/stream", recorded.url.encodedPath)
        assertEquals("text/event-stream", recorded.headers["Accept"])
        val body = Json.parseToJsonElement(recorded.body!!.utf8()).jsonObject
        assertEquals("t1", body["thread_id"]!!.jsonPrimitive.content)
        assertEquals("pet_demo_nala", body["pet_ref"]!!.jsonPrimitive.content)
        assertEquals("Et son sommeil ?", body["user_message"]!!.jsonPrimitive.content)
        assertEquals("owner", body["history"]!!.jsonArray[0].jsonObject["role"]!!.jsonPrimitive.content)
        assertFalse("alert_context" in body)
    }

    @Test
    fun `événement error et statut HTTP en échec`() = runTest {
        server.enqueue(sse("data: {\"type\":\"error\",\"message\":\"Service saturé.\"}\n\n"))
        try {
            client().streamTurn(request).toList()
            fail()
        } catch (error: AssistantException.Server) {
            assertEquals("Service saturé.", error.message)
        }

        server.enqueue(MockResponse.Builder().code(503).build())
        try {
            client().streamTurn(request).toList()
            fail()
        } catch (error: AssistantException.Unreachable) {
            assertTrue(error.message!!.startsWith("L'assistant Pawrise est injoignable"))
        }
    }

    @Test
    fun `analyseur SSE`() {
        val parser = SseParser()
        assertEquals(null, parser.consume("event: status"))
        assertEquals(null, parser.consume("data: ligne 1"))
        assertEquals(null, parser.consume("data:ligne 2"))
        assertEquals(SseParser.Message("status", "ligne 1\nligne 2"), parser.consume(""))
        assertEquals(null, parser.consume(""))
        assertEquals(null, parser.consume(": commentaire"))
        parser.consume("data: fin")
        assertEquals(SseParser.Message(null, "fin"), parser.flush())
    }
}
