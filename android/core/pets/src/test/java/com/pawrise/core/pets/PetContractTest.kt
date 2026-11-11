package com.pawrise.core.pets

import com.pawrise.core.network.ApiError
import com.pawrise.core.network.ApiResult
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/** Requêtes « Animaux » et « Colliers » validées par le mock Prism (contrat pawrise-doc). */
class PetContractTest {

    private val contractUrl: String? = System.getProperty("pawrise.contractUrl")
    private lateinit var repository: DefaultPetRepository

    @Before
    fun setUp() {
        assumeTrue("pawrise.contractUrl absent : test de contrat ignoré", contractUrl != null)
        repository = petRepository(contractUrl!!)
    }

    @Test
    fun `ajout d'un chien complet`() = runBlocking {
        assertSuccess(
            repository.createPet(
                NewPet(
                    name = "Nala",
                    birthDate = LocalDate.of(2023, 4, 12),
                    sex = Sex.Female,
                    breeds = listOf(Breed("golden_retriever", "Golden Retriever")),
                    weightKg = 28.0,
                    heightCm = 55.0,
                    medicalNotes = "Allergie au poulet.",
                ),
            ),
        )
    }

    @Test
    fun `ajout d'un chien minimal (race inconnue, champs facultatifs vides)`() = runBlocking {
        assertSuccess(repository.createPet(NewPet("Rio", null, Sex.Male, emptyList(), null, null, "   ")))
    }

    @Test
    fun `photo d'exemple`() = runBlocking {
        assertSuccess(repository.setSamplePhoto(PET_ID, SamplePhoto.Sample2))
    }

    @Test
    fun `ma propre photo en multipart`() = runBlocking {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(64)
        assertSuccess(repository.uploadPhoto(PET_ID, PhotoFile(jpeg, "image/jpeg")))
    }

    @Test
    fun `recherche de races avec et sans texte`() = runBlocking {
        assertSuccess(repository.searchBreeds(""))
        assertSuccess(repository.searchBreeds("gold"))
    }

    @Test
    fun `liste de mes chiens`() = runBlocking {
        assertSuccess(repository.listPets())
    }

    @Test
    fun `association du collier, code saisi en minuscules avec espaces`() = runBlocking {
        assertSuccess(repository.setCollar(PET_ID, " pw-c28a4f "))
    }

    @Test
    fun `chien inexistant au format invalide refuse`() = runBlocking {
        val result = repository.setCollar("pas-un-uuid", "PW-C28A4F")
        val problem = (result as ApiResult.Failure).error as ApiError.Problem
        assertTrue("statut ${problem.status}", problem.status in 400..499)
    }

    private fun assertSuccess(result: ApiResult<*>) = assertTrue("attendu un succès, reçu $result", result is ApiResult.Success)

    private companion object {
        const val PET_ID = "3f6c2a1e-8b4d-4c1a-9f2e-1d2c3b4a5e6f"
    }
}
