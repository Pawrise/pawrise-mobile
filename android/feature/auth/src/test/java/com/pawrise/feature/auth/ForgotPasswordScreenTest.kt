package com.pawrise.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.pawrise.core.designsystem.theme.PawriseTheme
import com.pawrise.core.network.ErrorCodes
import com.pawrise.core.testing.FakeAuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Mot de passe oublié : code par e-mail, nouveau mot de passe, connexion directe. */
@RunWith(RobolectricTestRunner::class)
class ForgotPasswordScreenTest {

    @get:Rule val compose = createComposeRule()

    private val repository = FakeAuthRepository()
    private var signedIn = 0
    private var back = 0

    @Before
    fun setUp() {
        val viewModel = ForgotPasswordViewModel(repository)
        compose.setContent {
            PawriseTheme {
                ForgotPasswordRoute(
                    initialEmail = "camille.martin@example.com",
                    onBack = { back++ },
                    onSignedIn = { signedIn++ },
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun see(text: String) = compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()

    @Test
    fun `parcours complet jusqu'a la connexion`() {
        tap("Recevoir un code")

        assertEquals(listOf("camille.martin@example.com"), repository.forgotCalls)
        see("Si un compte existe avec cette adresse, un code vient de lui être envoyé. Il est valable 15 minutes.")
        compose.onNode(hasContentDescription("E-mail")).assertIsNotEnabled()

        compose.field("Code à 6 chiffres").performTextInput("12a3456789")
        compose.field("Nouveau mot de passe").performTextInput("nouveau-mot-2026")
        tap("Changer le mot de passe")

        assertEquals(listOf(Triple("camille.martin@example.com", "123456", "nouveau-mot-2026")), repository.resetCalls)
        assertEquals(listOf("camille.martin@example.com" to "nouveau-mot-2026"), repository.loginCalls)
        assertEquals(1, signedIn)
    }

    @Test
    fun `e-mail invalide bloque sans appel`() {
        compose.field("E-mail").performTextReplacement("x")
        tap("Recevoir un code")

        see("Saisissez une adresse e-mail valide, par exemple alex@exemple.fr.")
        assertTrue(repository.forgotCalls.isEmpty())
    }

    @Test
    fun `code incomplet puis mot de passe trop court refuses localement`() {
        tap("Recevoir un code")
        compose.field("Code à 6 chiffres").performTextInput("123")
        tap("Changer le mot de passe")
        see("Le code contient 6 chiffres.")

        compose.field("Code à 6 chiffres").performTextInput("456")
        compose.field("Nouveau mot de passe").performTextInput("court")
        tap("Changer le mot de passe")
        see("Le mot de passe doit faire au moins 8 caractères.")
        assertTrue(repository.resetCalls.isEmpty())
    }

    @Test
    fun `code expire affiche le message du contrat`() {
        repository.resetResult = FakeAuthRepository.problem(400, ErrorCodes.INVALID_OR_EXPIRED_TOKEN)
        tap("Recevoir un code")
        compose.field("Code à 6 chiffres").performTextInput("000000")
        compose.field("Nouveau mot de passe").performTextInput("nouveau-mot-2026")
        tap("Changer le mot de passe")

        see("Code incorrect ou expiré. Vérifiez-le ou demandez-en un nouveau.")
        assertEquals(0, signedIn)
    }

    @Test
    fun `renvoyer un code redemande un code`() {
        tap("Recevoir un code")
        tap("Renvoyer un code")

        assertEquals(2, repository.forgotCalls.size)
    }

    @Test
    fun `trop de demandes`() {
        repository.forgotResult = FakeAuthRepository.problem(429, ErrorCodes.TOO_MANY_ATTEMPTS, retryAfter = 300)
        tap("Recevoir un code")

        see("Trop d'essais ratés. Réessayez dans 5 minutes ou réinitialisez votre mot de passe.")
    }

    @Test
    fun `retour`() {
        tap("Retour")
        assertEquals(1, back)
    }
}
