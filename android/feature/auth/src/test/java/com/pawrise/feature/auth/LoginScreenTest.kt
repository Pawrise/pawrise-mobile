package com.pawrise.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
    private val forgot = mutableListOf<String>()

    @Before
    fun setUp() {
        val viewModel = LoginViewModel(repository)
        compose.setContent {
            PawriseTheme {
                LoginRoute(
                    onBack = { back++ },
                    onSignup = { signup++ },
                    onLoggedIn = { loggedIn++ },
                    onForgotPassword = { forgot += it },
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
        assertEquals(listOf("camille.martin@example.com" to "chien-oslo-2026"), repository.loginCalls)
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

        compose.onNodeWithText("Trop d'essais ratés. Réessayez dans 15 minutes ou réinitialisez votre mot de passe.").assertIsDisplayed()
    }

    @Test
    fun `reseau coupe propose de reessayer`() {
        repository.loginResult = FakeAuthRepository.NETWORK_DOWN

        fillAndSubmit()

        compose.onNodeWithText("Impossible de joindre le serveur. Réessayez dans un instant.").assertIsDisplayed()
        assertEquals(0, loggedIn)
    }

    @Test
    fun `bouton inactif tant qu'un champ est vide`() {
        compose.onNodeWithText("Se connecter").assertIsNotEnabled()
        compose.field("E-mail").performTextInput("camille.martin@example.com")
        compose.onNodeWithText("Se connecter").assertIsNotEnabled().performClick()

        assertEquals(0, repository.loginCalls.size)
        compose.field("Mot de passe").performTextInput("chien-oslo-2026")
        compose.onNodeWithText("Se connecter").assertIsEnabled()
    }

    @Test
    fun `mot de passe oublie transmet l'e-mail saisi`() {
        compose.field("E-mail").performTextInput("camille.martin@example.com")
        compose.onNodeWithText("Mot de passe oublié ?").performClick()

        assertEquals(listOf("camille.martin@example.com"), forgot)
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
        // Pendant l'appel, une roue remplace le libellé : un second appui ne relance rien.
        compose.onNodeWithText("Se connecter").performClick()
        compose.waitForIdle()
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
