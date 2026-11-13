package com.pawrise.feature.chat

import com.pawrise.feature.chat.assistant.AssistantException
import com.pawrise.feature.chat.assistant.ScriptedAssistantService
import com.pawrise.feature.chat.assistant.TurnRole
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatConversationTest {

    private fun TestScope.conversation(service: ScriptedAssistantService) =
        ChatConversation("pet_demo_nala", service, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

    @Test
    fun `envoi, points de saisie puis réponse, historique sans le message en cours`() = runTest {
        val service = ScriptedAssistantService().apply { gate = CompletableDeferred() }
        val chat = conversation(service)
        assertTrue(chat.state.value.showsQuickReplies)
        assertFalse(chat.send("   "))

        assertTrue(chat.send(" Et son sommeil ? "))
        assertTrue(chat.state.value.isResponding)
        assertFalse(chat.state.value.canSend)
        assertFalse(chat.state.value.showsQuickReplies)
        assertFalse(chat.send("Encore ?"))

        service.gate!!.complete(Unit)
        assertFalse(chat.state.value.isResponding)
        assertEquals(listOf("Et son sommeil ?", "Nala — 11 h de sommeil."), chat.state.value.messages.map { it.text })
        assertEquals(emptyList<Any>(), service.requests[0].history)
        assertEquals("pet_demo_nala", service.requests[0].petRef)

        service.gate = null
        chat.send("Et son activité ?")
        assertEquals(listOf(TurnRole.Owner, TurnRole.Assistant), service.requests[1].history.map { it.role })
        assertEquals(chat.threadId, service.requests[1].threadId)
        assertEquals("Nala — 94 min actives.", chat.state.value.messages.last().text)
    }

    @Test
    fun `erreur puis réessayer`() = runTest {
        val service = ScriptedAssistantService().apply { failure = AssistantException.Unreachable() }
        val chat = conversation(service)
        chat.send("Comment va Nala aujourd'hui ?")
        assertEquals("L'assistant Pawrise est injoignable pour le moment. Réessayez dans un instant.", chat.state.value.failure)
        assertFalse(chat.state.value.showsQuickReplies)

        service.failure = null
        chat.retry()
        assertNull(chat.state.value.failure)
        assertEquals(2, chat.state.value.messages.size)
        assertEquals(1, chat.state.value.messages.count { it.role == TurnRole.Owner })
    }

    @Test
    fun `tour interrompu quand on quitte l'onglet`() = runTest {
        val service = ScriptedAssistantService().apply { gate = CompletableDeferred() }
        val chat = conversation(service)
        chat.send("Et son sommeil ?")
        chat.cancel()
        assertFalse(chat.state.value.isResponding)
        assertEquals(ChatConversation.INTERRUPTED, chat.state.value.failure)
        service.gate!!.complete(Unit)
        assertEquals(1, chat.state.value.messages.size)
    }

    @Test
    fun `coupure après les traits d'union`() {
        assertEquals("souhaitez-​vous", "souhaitez-vous".breakAfterHyphens())
        assertEquals("38 - 39", "38 - 39".breakAfterHyphens())
    }
}
