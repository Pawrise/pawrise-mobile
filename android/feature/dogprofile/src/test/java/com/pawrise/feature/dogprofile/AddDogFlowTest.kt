package com.pawrise.feature.dogprofile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.pawrise.core.designsystem.theme.PawriseTheme
import com.pawrise.core.network.ApiResult
import com.pawrise.core.network.ErrorCodes
import com.pawrise.core.pets.Breed
import com.pawrise.core.pets.PhotoFile
import com.pawrise.core.pets.SamplePhoto
import com.pawrise.core.pets.Sex
import com.pawrise.core.testing.FakeAuthRepository
import com.pawrise.core.testing.FakePetRepository
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** US-A · 08-16 Profil du chien : ce que l'utilisateur saisit arrive tel quel au serveur. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class AddDogFlowTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val pets = FakePetRepository(pets = emptyList())
    private var loadedPhoto: PhotoFile? = PhotoFile(byteArrayOf(1, 2, 3), "image/jpeg")
    private val viewModel = AddDogViewModel(pets) { loadedPhoto }
    private var created: CreatedDog? = null
    private var left = 0

    private fun start() {
        compose.setContent {
            PawriseTheme { AddDogRoute(onLeave = { left++ }, onCreated = { created = it }, viewModel = viewModel) }
        }
    }

    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasContentDescription(label))
    private fun type(label: String, text: String) = field(label).performTextInput(text)

    private fun tap(text: String) {
        val node = compose.onNodeWithText(text)
        runCatching { node.performScrollTo() } // seulement si la zone défile
        node.performClick()
        compose.waitForIdle()
    }

    private fun next() = tap("Suivant")

    private fun waitFor(text: String) = compose.waitUntilAtLeastOneExists(hasText(text), 3_000)

    private fun see(text: String) {
        val node = compose.onNodeWithText(text)
        runCatching { node.performScrollTo() }
        node.assertIsDisplayed()
    }

    /** Parcours complet de la maquette (Nala). */
    private fun fillAll() {
        compose.onNodeWithContentDescription("Exemple de photo 2").performClick()
        next()
        type("Nom du chien", "Nala")
        next()
        type("Date de naissance", "12042023")
        next()
        tap("Femelle")
        next()
        type("Rechercher une race", "gold")
        waitFor("Golden Retriever")
        tap("Golden Retriever")
        next()
        type("Poids en kilogrammes", "27,5")
        next()
        type("Taille en centimètres", "55")
        next()
        type("Infos santé", "Allergie au poulet.")
        next()
    }

    @Test
    fun `parcours complet - le serveur recoit le profil et la photo d'exemple`() {
        start()
        fillAll()

        see("Le profil de Nala")
        see("12 avril 2023")
        see("Femelle")
        see("Golden Retriever")
        see("27,5 kg")
        see("55 cm")
        tap("Continuer")

        val sent = pets.created.single()
        assertEquals("Nala", sent.name)
        assertEquals(LocalDate.of(2023, 4, 12), sent.birthDate)
        assertEquals(Sex.Female, sent.sex)
        assertEquals(listOf(Breed("golden_retriever", "Golden Retriever")), sent.breeds)
        assertEquals(27.5, sent.weightKg)
        assertEquals(55.0, sent.heightCm)
        assertEquals("Allergie au poulet.", sent.medicalNotes)
        assertEquals(listOf("pet-new" to SamplePhoto.Sample2), pets.samplePhotos)
        assertEquals(CreatedDog("pet-new", "Nala", PhotoChoice.Sample(SamplePhoto.Sample2)), created)
    }

    @Test
    fun `seuls le nom et le sexe sont obligatoires`() {
        start()
        next() // photo facultative
        compose.onNodeWithText("Suivant").assertIsNotEnabled()
        type("Nom du chien", "Rio")
        next()
        next() // date facultative
        compose.onNodeWithText("Suivant").assertIsNotEnabled()
        tap("Mâle")
        repeat(5) { next() } // race, poids, taille, santé facultatifs
        see("Le profil de Rio")
        see("Aucune")
        tap("Continuer")

        val sent = pets.created.single()
        assertEquals(Sex.Male, sent.sex)
        assertEquals(emptyList<Breed>(), sent.breeds)
        assertNull(sent.birthDate)
        assertNull(sent.weightKg)
        assertNull(sent.medicalNotes)
        assertTrue("pas de photo choisie : aucun envoi", pets.samplePhotos.isEmpty() && pets.uploadedPhotos.isEmpty())
    }

    @Test
    fun `date incomplete ou dans le futur bloquee`() {
        start()
        next()
        type("Nom du chien", "Nala")
        next()
        type("Date de naissance", "1204")
        compose.onNodeWithText("Suivant").assertIsNotEnabled()

        field("Date de naissance").performTextInput("2999")
        compose.onNodeWithText("Suivant").assertIsNotEnabled()
    }

    @Test
    fun `race facultative - sans choix, aucune race envoyee`() {
        start()
        next()
        type("Nom du chien", "Rio")
        next(); next()
        tap("Mâle")
        next()
        repeat(4) { next() }
        see("Non renseignée")
        tap("Continuer")

        assertEquals(emptyList<Breed>(), pets.created.single().breeds)
    }

    @Test
    fun `date hors limites - message de la maquette iOS`() {
        start()
        next()
        type("Nom du chien", "Nala")
        next()
        type("Date de naissance", "01011950")
        see("Indiquez une date réelle, au format JJ / MM / AAAA, dans les 30 dernières années.")
        compose.onNodeWithText("Suivant").assertIsNotEnabled()
    }

    @Test
    fun `poids au-dela de 120 kg refuse`() {
        start()
        next()
        type("Nom du chien", "Nala")
        next(); next()
        tap("Femelle")
        next(); next()
        type("Poids en kilogrammes", "130")
        see("Indiquez un poids entre 0 et 120 kg.")
        compose.onNodeWithText("Suivant").assertIsNotEnabled()
    }

    @Test
    fun `correction depuis le recapitulatif puis retour au recapitulatif`() {
        start()
        fillAll()
        compose.onNodeWithText("Poids").performScrollTo().performClick()
        compose.waitForIdle()
        see("Combien pèse Nala ?")
        next()
        see("Le profil de Nala")
    }

    @Test
    fun `races indisponibles - message puis reessayer`() {
        pets.breedsResult = FakeAuthRepository.NETWORK_DOWN
        start()
        next()
        type("Nom du chien", "Rio")
        next(); next()
        tap("Mâle")
        next()
        waitFor("Impossible de charger les races.")

        pets.breedsResult = null
        tap("Réessayer")
        waitFor("Golden Retriever")
    }

    @Test
    fun `modifier une ligne du recapitulatif`() {
        start()
        fillAll()
        compose.onNodeWithText("Poids").performScrollTo().performClick()
        compose.waitForIdle()

        see("Combien pèse Nala ?")
    }

    @Test
    fun `echec de creation - message, rien n'est perdu, nouvel essai`() {
        pets.createResult = FakeAuthRepository.NETWORK_DOWN
        start()
        fillAll()
        tap("Continuer")

        see("Connexion impossible. Vérifiez votre réseau et réessayez.")
        assertNull(created)

        pets.createResult = null
        tap("Continuer")
        assertEquals("Nala", created?.name)
    }

    @Test
    fun `date refusee par le serveur`() {
        pets.createResult = FakeAuthRepository.problem(400, ErrorCodes.VALIDATION_FAILED, "birthDate" to ErrorCodes.BIRTH_DATE_IN_FUTURE)
        start()
        fillAll()
        tap("Continuer")

        see("La date de naissance ne peut pas être dans le futur.")
    }

    @Test
    fun `photo de la galerie envoyee apres creation`() {
        start()
        compose.runOnUiThread { viewModel.onOwnPhotoPicked("content://media/42") }
        compose.waitForIdle()
        next()
        type("Nom du chien", "Nala")
        next(); next()
        tap("Femelle")
        repeat(5) { next() }
        tap("Continuer")

        assertEquals("pet-new", pets.uploadedPhotos.single().first)
        assertEquals(PhotoChoice.Own("content://media/42"), created?.photo)
    }

    @Test
    fun `photo illisible refusee`() {
        loadedPhoto = null
        start()
        compose.runOnUiThread { viewModel.onOwnPhotoPicked("content://media/broken") }
        compose.waitForIdle()

        see("Impossible d'utiliser cette photo. Choisissez-en une autre.")
    }

    @Test
    fun `echec de la photo ne bloque pas la suite`() {
        pets.photoResult = FakeAuthRepository.NETWORK_DOWN
        start()
        fillAll()
        tap("Continuer")

        assertEquals("Nala", created?.name)
    }

    @Test
    fun `retour et fermer`() {
        start()
        next()
        type("Nom du chien", "Nala")
        compose.onNodeWithContentDescription("Retour").performClick()
        see("Ajoutez une photo de votre chien")
        assertEquals(0, left)

        compose.onNodeWithContentDescription("Retour").performClick()
        assertEquals(1, left)
        compose.onNodeWithContentDescription("Fermer").performClick()
        assertEquals(2, left)
    }

    @Test
    fun `retour systeme revient a l'etape precedente`() {
        start()
        next()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        see("Ajoutez une photo de votre chien")
        compose.onNodeWithText("Suivant").assertIsEnabled()
    }
}
