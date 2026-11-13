package com.pawrise.core.pets

import com.pawrise.core.network.ApiError
import com.pawrise.core.network.ApiResult
import com.pawrise.core.network.ErrorCodes
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Forme exacte des requêtes envoyées et lecture des réponses. */
class PetRequestsTest {

    private val server = MockWebServer()
    private lateinit var repository: DefaultPetRepository

    @Before
    fun setUp() {
        server.start()
        repository = petRepository(server.url("/").toString())
    }

    @After
    fun tearDown() = server.close()

    private fun respond(code: Int, body: String) =
        server.enqueue(MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build())

    @Test
    fun `creation envoie le corps du contrat sans champs vides`() = runBlocking {
        respond(201, PET)

        val result = repository.createPet(NewPet(" Nala ", LocalDate.of(2023, 4, 12), Sex.Female, listOf(Breed("golden_retriever", "Golden Retriever")), 28.5, null, "  "))

        val request = server.takeRequest()
        assertEquals("/pets", request.url.encodedPath)
        assertEquals(
            """{"name":"Nala","birthDate":"2023-04-12","sex":"female","breedCodes":["golden_retriever"],"weightKg":28.5}""",
            request.body!!.utf8(),
        )
        val pet = (result as ApiResult.Success).value
        assertEquals(SamplePhoto.Sample2, pet.photoSample)
        assertEquals(LocalDate.of(2023, 4, 12), pet.birthDate)
        assertEquals(Sex.Female, pet.sex)
    }

    @Test
    fun `race inconnue - breedCodes omis, vaut liste vide par defaut dans le contrat`() = runBlocking {
        respond(201, PET)

        repository.createPet(NewPet("Rio", null, Sex.Male, emptyList(), null, null, null))

        assertEquals("""{"name":"Rio","sex":"male"}""", server.takeRequest().body!!.utf8())
    }

    @Test
    fun `photo perso envoyee en multipart dans le champ file`() = runBlocking {
        respond(200, PET)

        repository.uploadPhoto("p1", PhotoFile(byteArrayOf(1, 2, 3), "image/jpeg"))

        val request = server.takeRequest()
        assertEquals("/pets/p1/photo", request.url.encodedPath)
        assertTrue(request.headers["Content-Type"]!!.startsWith("multipart/form-data"))
        val body = request.body!!.utf8()
        assertTrue(body.contains("name=\"file\"; filename=\"photo.jpg\""))
        assertTrue(body.contains("Content-Type: image/jpeg"))
    }

    @Test
    fun `code collier normalise avant envoi`() = runBlocking {
        respond(200, """{"serialNumber":"PW-C28A4F","model":"C2","batteryPct":null,"connected":false,"lastSeenAt":null,"assignedAt":"2026-11-11T10:00:00Z"}""")

        repository.setCollar("p1", " pw-c28a4f ")

        assertEquals("""{"serialNumber":"PW-C28A4F"}""", server.takeRequest().body!!.utf8())
    }

    @Test
    fun `collier deja associe expose le code`() = runBlocking {
        server.enqueue(
            MockResponse.Builder().code(409).addHeader("Content-Type", "application/problem+json")
                .body("""{"status":409,"code":"COLLAR_ALREADY_ASSIGNED","title":"Collar already assigned"}""").build(),
        )

        val problem = (repository.setCollar("p1", "PW-C28A4F") as ApiResult.Failure).error as ApiError.Problem

        assertEquals(ErrorCodes.COLLAR_ALREADY_ASSIGNED, problem.code)
    }

    @Test
    fun `recherche de races sans texte n'envoie pas q`() = runBlocking {
        respond(200, """{"items":[{"code":"mixed","name":"Croisé"}]}""")

        val breeds = (repository.searchBreeds("  ") as ApiResult.Success).value

        val url = server.takeRequest().url
        assertEquals("/breeds", url.encodedPath)
        assertEquals(null, url.queryParameter("q"))
        assertEquals("50", url.queryParameter("limit"))
        assertEquals(listOf(Breed("mixed", "Croisé")), breeds)
    }

    @Test
    fun `format du code collier`() {
        assertTrue(isValidCollarCode("PW-8A4F2C"))
        assertTrue(isValidCollarCode(" pw-8a4f2c "))
        assertFalse(isValidCollarCode("PW-8A4F2"))
        assertFalse(isValidCollarCode("XX-8A4F2C"))
        assertFalse(isValidCollarCode("PW-8A4F2C9"))
    }

    private companion object {
        const val PET = """{"id":"p1","name":"Nala","photoUrl":null,"photoSample":"sample_2","birthDate":"2023-04-12","sex":"female","breeds":[{"code":"golden_retriever","name":"Golden Retriever"}],"weightKg":28.5,"heightCm":null,"medicalNotes":null,"myRole":"chief","collarSerial":null}"""
    }
}
