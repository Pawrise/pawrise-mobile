package com.pawrise.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.pawrise.core.designsystem.theme.PawriseTheme
import com.pawrise.core.network.ErrorCodes
import com.pawrise.core.testing.FakeAuthRepository
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** US-J · 02 Connexion : ce que voit et fait l'utilisateur, avec un faux serveur. */
@RunWith(RobolectricTestRunner::class)
class LoginScreenTest {

    @get:Rule val compose = createComposeRule()

    private val repository = FakeAuthRepository()
    private var loggedIn = 0
    private var back = 0
    private var signup = 0

    @Before
    fun setUp() {
        val viewModel = LoginViewModel(repository)
        compose.setContent {
            PawriseTheme {
                LoginRoute(
                    onBack = { back++ },
                    onSignup = { signup++ },
                    onLoggedIn = { loggedIn++ },
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun fillAndSubmit(email: String = "camille.martin@example.com", password: String = "chien-oslo-2026") {
        compose.field("E-mail").performTextInput(email)
        compose.field("Mot de passe").performTextInput(password)
        compose.onNodeWithText("Se connecter").performClick()
        compose.waitForIdle()
    }

    @Test
    fun `identifiants valides ouvrent l'app`() {
        fillAndSubmit(email = "  camille.martin@example.com ")

        assertEquals(1, loggedIn)
        assertEquals(listOf("  camille.martin@example.com " to "chien-oslo-2026"), repository.loginCalls)
    }

    @Test
    fun `identifiants refuses affichent le message de la maquette`() {
        repository.loginResult = FakeAuthRepository.problem(401, ErrorCodes.INVALID_CREDENTIALS)

        fillAndSubmit()

        compose.onNodeWithText("E-mail ou mot de passe incorrect.").assertIsDisplayed()
        assertEquals(0, loggedIn)
    }

    @Test
    fun `trop d'essais indique le delai d'attente`() {
        repository.loginResult = FakeAuthRepository.problem(429, ErrorCodes.TOO_MANY_ATTEMPTS, retryAfter = 900)

        fillAndSubmit()

        compose.onNodeWithText("Trop d'essais. Réessayez dans 15 min.").assertIsDisplayed()
    }

    @Test
    fun `reseau coupe propose de reessayer`() {
        repository.loginResult = FakeAuthRepository.NETWORK_DOWN

        fillAndSubmit()

        compose.onNodeWithText("Connexion impossible. Vérifiez votre réseau et réessayez.").assertIsDisplayed()
        assertEquals(0, loggedIn)
    }

    @Test
    fun `champs vides bloques sans appel au serveur`() {
        compose.onNodeWithText("Se connecter").performClick()

        compose.onNodeWithText("Renseignez votre e-mail et votre mot de passe.").assertIsDisplayed()
        assertEquals(0, repository.loginCalls.size)
    }

    @Test
    fun `e-mail mal forme bloque sans appel au serveur`() {
        fillAndSubmit(email = "camille.example.com")

        compose.onNodeWithText("Indiquez un e-mail valide.").assertIsDisplayed()
        assertEquals(0, repository.loginCalls.size)
    }

    @Test
    fun `le message d'erreur disparait quand on corrige`() {
        repository.loginResult = FakeAuthRepository.problem(401, ErrorCodes.INVALID_CREDENTIALS)
        fillAndSubmit()

        compose.field("Mot de passe").performTextInput("x")

        compose.onNodeWithText("E-mail ou mot de passe incorrect.").assertDoesNotExist()
    }

    @Test
    fun `double appui pendant la connexion n'envoie qu'une requete`() {
        val gate = CompletableDeferred<Unit>().also { repository.gate = it }
        compose.field("E-mail").performTextInput("camille.martin@example.com")
        compose.field("Mot de passe").performTextInput("chien-oslo-2026")

        compose.onNodeWithText("Se connecter").performClick()
        compose.waitForIdle()
        // Pendant l'appel, le libellé laisse place à une roue : un second appui ne relance rien.
        compose.onNodeWithText("Se connecter").assertDoesNotExist()
        assertEquals(1, repository.loginCalls.size)

        gate.complete(Unit)
        compose.waitForIdle()
        assertEquals(1, loggedIn)
    }

    @Test
    fun `touche Terminé du clavier connecte`() {
        compose.field("E-mail").performTextInput("camille.martin@example.com")
        compose.field("Mot de passe").performTextInput("chien-oslo-2026")

        compose.field("Mot de passe").performImeAction()
        compose.waitForIdle()

        assertEquals(1, loggedIn)
    }

    @Test
    fun `liens Retour et Creer un compte`() {
        compose.onNodeWithText("Retour").performClick()
        compose.onNodeWithText("Créer un compte").performScrollTo().performClick()

        assertEquals(1, back)
        assertEquals(1, signup)
    }
}
