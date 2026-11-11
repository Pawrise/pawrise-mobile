package com.pawrise.feature.collar

import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import androidx.compose.ui.test.performTextInput
import com.pawrise.core.designsystem.theme.PawriseTheme
import com.pawrise.core.network.ErrorCodes
import com.pawrise.core.pets.SamplePhoto
import com.pawrise.core.testing.FakeAuthRepository
import com.pawrise.core.testing.FakePetRepository
import com.pawrise.feature.dogprofile.PhotoChoice
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** US-A · 17-20 Associer le collier de Nala ; la caméra est remplacée par un faux scanner. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class CollarPairingTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val pets = FakePetRepository()
    private val viewModel = CollarPairingViewModel(pets).apply { minActivationMillis = 0 }
    private var done = 0
    /** Contenu du prochain QR « lu » par le faux scanner. */
    private var qr = "PW-8A4F2C"

    private fun start() {
        compose.setContent {
            PawriseTheme {
                CollarPairingRoute(
                    petId = "pet-nala",
                    petName = "Nala",
                    photo = PhotoChoice.Sample(SamplePhoto.Sample1),
                    onDone = { done++ },
                    viewModel = viewModel,
                    scanner = { onQr, _ -> Button(onClick = { onQr(qr) }) { Text("QR devant la caméra") } },
                )
            }
        }
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    private fun codeField() = compose.onNode(hasSetTextAction() and hasContentDescription("Code du collier"))
    private fun waitFor(text: String) = compose.waitUntilAtLeastOneExists(hasText(text), 3_000)

    @Test
    fun `saisie manuelle, activation, succes puis tableau de bord`() {
        start()
        compose.onNodeWithText("Connectons le collier de Nala").assertIsDisplayed()
        tap("Saisir le code manuellement")
        compose.onNodeWithText("Activer le collier").assertIsNotEnabled()

        codeField().performTextInput("pw-8a4f2c")
        compose.onNodeWithText("Activer le collier").assertIsEnabled()
        tap("Activer le collier")

        waitFor("Le collier de Nala est activé")
        assertEquals(listOf("pet-nala" to "PW-8A4F2C"), pets.collars)
        tap("Voir le tableau de bord de Nala")
        assertEquals(1, done)
    }

    @Test
    fun `ecran d'activation pendant l'appel, sans retour ni fermer`() {
        val gate = CompletableDeferred<Unit>().also { pets.gate = it }
        start()
        tap("Saisir le code manuellement")
        codeField().performTextInput("PW-8A4F2C")
        tap("Activer le collier")

        compose.onNodeWithText("Activation du collier…").assertIsDisplayed()
        compose.onNodeWithText("Nous connectons le collier de Nala à Pawrise. Un instant.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Fermer").assertDoesNotExist()
        compose.onNodeWithContentDescription("Retour").assertDoesNotExist()

        gate.complete(Unit)
        waitFor("Le collier de Nala est activé")
    }

    @Test
    fun `code inconnu - retour a la saisie avec le message, code conserve`() {
        pets.collarResult = FakeAuthRepository.problem(400, ErrorCodes.VALIDATION_FAILED, "serialNumber" to ErrorCodes.UNKNOWN_COLLAR)
        start()
        tap("Saisir le code manuellement")
        codeField().performTextInput("PW-000000")
        tap("Activer le collier")

        waitFor("Code inconnu. Vérifiez le code inscrit sous le collier.")
        compose.onNodeWithText("PW-000000").assertIsDisplayed()
    }

    @Test
    fun `collier deja associe a un autre chien`() {
        pets.collarResult = FakeAuthRepository.problem(409, ErrorCodes.COLLAR_ALREADY_ASSIGNED)
        start()
        tap("Saisir le code manuellement")
        codeField().performTextInput("PW-C28A4F")
        tap("Activer le collier")

        waitFor("Ce collier est déjà associé à un autre chien.")
    }

    @Test
    fun `reseau coupe`() {
        pets.collarResult = FakeAuthRepository.NETWORK_DOWN
        start()
        tap("Saisir le code manuellement")
        codeField().performTextInput("PW-C28A4F")
        tap("Activer le collier")

        waitFor("Connexion impossible. Vérifiez votre réseau et réessayez.")
    }

    @Test
    fun `scan d'un QR contenant le code dans une URL`() {
        qr = "https://pawrise.fr/c/PW-8A4F2C"
        start()
        tap("Scanner le QR code")
        compose.onNodeWithText("Scannez le QR code").assertIsDisplayed()

        tap("QR devant la caméra")

        waitFor("Le collier de Nala est activé")
        assertEquals(listOf("pet-nala" to "PW-8A4F2C"), pets.collars)
    }

    @Test
    fun `QR qui n'est pas un collier Pawrise`() {
        qr = "https://example.com/menu"
        start()
        tap("Scanner le QR code")
        tap("QR devant la caméra")

        compose.onNodeWithText("Ce QR code n'est pas celui d'un collier Pawrise.").assertIsDisplayed()
        assertTrue(pets.collars.isEmpty())
    }

    @Test
    fun `QR refuse - la camera qui le relit ne renvoie pas la requete en boucle`() {
        pets.collarResult = FakeAuthRepository.problem(409, ErrorCodes.COLLAR_ALREADY_ASSIGNED)
        start()
        tap("Scanner le QR code")
        tap("QR devant la caméra")
        waitFor("Ce collier est déjà associé à un autre chien.")

        tap("QR devant la caméra")
        tap("QR devant la caméra")

        assertEquals(1, pets.collars.size)
    }

    @Test
    fun `depuis le scan, passer a la saisie manuelle`() {
        start()
        tap("Scanner le QR code")
        tap("Saisir le code manuellement")

        compose.onNodeWithText("Saisir le code d'activation").assertIsDisplayed()
    }

    @Test
    fun `retour depuis la saisie revient au choix, fermer termine sans collier`() {
        start()
        tap("Saisir le code manuellement")
        compose.onNodeWithContentDescription("Retour").performClick()
        compose.onNodeWithText("Connectons le collier de Nala").assertIsDisplayed()
        assertEquals(0, done)

        compose.onNodeWithContentDescription("Fermer").performClick()
        assertEquals(1, done)
        assertTrue(pets.collars.isEmpty())
    }

    @Test
    fun `retour systeme sur le choix termine sans collier`() {
        start()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        assertEquals(1, done)
    }
}
