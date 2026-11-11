package com.pawrise.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.pawrise.core.auth.RegistrationForm
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

/** US-A · 02 Créer un compte : saisie, contrôles, erreurs serveur, avec un faux serveur. */
@RunWith(RobolectricTestRunner::class)
class SignupScreenTest {

    @get:Rule val compose = createComposeRule()

    private val repository = FakeAuthRepository()
    private var registered = 0
    private var back = 0
    private var login = 0

    @Before
    fun setUp() {
        val viewModel = SignupViewModel(repository)
        compose.setContent {
            PawriseTheme {
                SignupRoute(onBack = { back++ }, onLogin = { login++ }, onRegistered = { registered++ }, viewModel = viewModel)
            }
        }
    }

    private fun type(label: String, text: String) = compose.field(label).performScrollTo().performTextInput(text)

    private fun tap(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun see(text: String) = compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()

    private fun fillValid(phone: String = "06 12 34 56 78") {
        type("Prénom", " Camille ")
        type("Nom", "Martin")
        type("E-mail", "camille.martin@example.com")
        if (phone.isNotEmpty()) type("Téléphone (facultatif)", phone)
        type("Mot de passe", "chien-oslo-2026")
        tap("J'accepte les conditions d'utilisation et la politique de confidentialité.")
    }

    @Test
    fun `inscription valide cree le compte et enchaine`() {
        fillValid()
        tap("Continuer")

        assertEquals(1, registered)
        assertEquals(
            listOf(RegistrationForm(" Camille ", "Martin", "camille.martin@example.com", "+33612345678", "chien-oslo-2026")),
            repository.registerCalls,
        )
    }

    @Test
    fun `telephone facultatif`() {
        fillValid(phone = "")
        tap("Continuer")

        assertEquals(1, registered)
        assertEquals(null, repository.registerCalls.single().phoneNumber)
    }

    @Test
    fun `formulaire vide signale chaque champ sans appel au serveur`() {
        tap("Continuer")

        see("Indiquez votre prénom.")
        see("Indiquez votre nom.")
        see("Indiquez un e-mail valide.")
        see("Choisissez un mot de passe d'au moins 8 caractères.")
        see("Acceptez les conditions pour créer votre compte.")
        assertTrue(repository.registerCalls.isEmpty())
    }

    @Test
    fun `mot de passe de 7 caracteres refuse (M2)`() {
        type("Prénom", "Camille")
        type("Nom", "Martin")
        type("E-mail", "camille.martin@example.com")
        type("Mot de passe", "chien12")
        tap("J'accepte les conditions d'utilisation et la politique de confidentialité.")
        tap("Continuer")

        see("Choisissez un mot de passe d'au moins 8 caractères.")
        assertTrue(repository.registerCalls.isEmpty())
    }

    @Test
    fun `cgu non acceptees bloquent l'inscription`() {
        type("Prénom", "Camille")
        type("Nom", "Martin")
        type("E-mail", "camille.martin@example.com")
        type("Mot de passe", "chien-oslo-2026")
        tap("Continuer")

        see("Acceptez les conditions pour créer votre compte.")
        assertTrue(repository.registerCalls.isEmpty())
    }

    @Test
    fun `telephone invalide signale`() {
        fillValid(phone = "06 12")
        tap("Continuer")

        see("Indiquez un numéro de téléphone valide.")
        assertTrue(repository.registerCalls.isEmpty())
    }

    @Test
    fun `e-mail deja utilise affiche sous le champ puis s'efface a la correction`() {
        repository.registerResult = FakeAuthRepository.problem(409, ErrorCodes.EMAIL_ALREADY_USED)
        fillValid()
        tap("Continuer")

        see("Un compte existe déjà avec cet e-mail.")
        assertEquals(0, registered)

        type("E-mail", "x")
        compose.onNodeWithText("Un compte existe déjà avec cet e-mail.").assertDoesNotExist()
    }

    @Test
    fun `erreurs de validation du serveur placees sous les champs`() {
        repository.registerResult = FakeAuthRepository.problem(
            400,
            ErrorCodes.VALIDATION_FAILED,
            "phoneNumber" to ErrorCodes.INVALID_PHONE,
            "password" to ErrorCodes.PASSWORD_TOO_SHORT,
        )
        fillValid()
        tap("Continuer")

        see("Indiquez un numéro de téléphone valide.")
        see("Choisissez un mot de passe d'au moins 8 caractères.")
    }

    @Test
    fun `reseau coupe affiche un message general`() {
        repository.registerResult = FakeAuthRepository.NETWORK_DOWN
        fillValid()
        tap("Continuer")

        see("Connexion impossible. Vérifiez votre réseau et réessayez.")
        assertEquals(0, registered)
    }

    @Test
    fun `liens Retour et J'ai deja un compte`() {
        tap("Retour")
        tap("J'ai déjà un compte")

        assertEquals(1, back)
        assertEquals(1, login)
    }
}
