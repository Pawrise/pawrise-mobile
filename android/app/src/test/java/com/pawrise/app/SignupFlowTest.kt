package com.pawrise.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.auth.di.AuthBindings
import com.pawrise.core.testing.FakeAuthRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** US-A, inscription dans l'app réelle : Bienvenue → Créer un compte → découverte. */
@HiltAndroidTest
@UninstallModules(AuthBindings::class)
@RunWith(RobolectricTestRunner::class)
class SignupFlowTest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @BindValue @JvmField val repository: AuthRepository = FakeAuthRepository()

    private fun signUp() {
        compose.tap("Créer un compte")
        compose.field("Prénom").performTextInput("Camille")
        compose.field("Nom").performTextInput("Martin")
        compose.field("E-mail").performTextInput("camille.martin@example.com")
        compose.field("Mot de passe").performScrollTo().performTextInput("chien-oslo-2026")
        compose.tap("J'accepte les conditions d'utilisation et la politique de confidentialité.")
        compose.tap("Continuer")
    }

    @Test
    fun `inscription puis decouverte`() {
        signUp()

        compose.onNodeWithText("Découverte", substring = true).assertIsDisplayed()
    }

    @Test
    fun `retour systeme apres inscription ne revient pas au formulaire`() {
        signUp()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        compose.onNodeWithText("Créer un compte").assertDoesNotExist()
    }

    @Test
    fun `j'ai deja un compte mene a la connexion`() {
        compose.tap("Créer un compte")
        compose.tap("J'ai déjà un compte")

        compose.onNodeWithText("Connexion").assertIsDisplayed()
    }
}
