package com.pawrise.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.auth.di.AuthBindings
import com.pawrise.core.pets.PetRepository
import com.pawrise.core.pets.di.PetBindings
import com.pawrise.core.testing.FakeAuthRepository
import com.pawrise.core.testing.FakePetRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** US-A, inscription dans l'app réelle : Bienvenue → Créer un compte → découverte. */
@HiltAndroidTest
@UninstallModules(AuthBindings::class, PetBindings::class)
@RunWith(RobolectricTestRunner::class)
class SignupFlowTest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @BindValue @JvmField val repository: AuthRepository = FakeAuthRepository()
    @BindValue @JvmField val petRepository: PetRepository = FakePetRepository(pets = emptyList())

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

        compose.onNodeWithText("Pawrise veille sur votre chien").assertIsDisplayed()
        compose.onNodeWithText("Étape 1 sur 5").assertIsDisplayed()
    }

    @Test
    fun `retour systeme apres inscription ne revient pas au formulaire`() {
        signUp()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        compose.onNodeWithText("Créer un compte").assertDoesNotExist()
    }

    @Test
    fun `inscription, decouverte, profil du chien puis collier`() {
        signUp()
        repeat(4) { compose.tap("Suivant") }
        compose.tap("Connecter le collier")

        compose.onNodeWithContentDescription("Exemple de photo 1").performClick()
        compose.tap("Suivant")
        compose.onNode(hasSetTextAction() and hasContentDescription("Nom du chien")).performTextInput("Nala")
        compose.tap("Suivant")
        compose.tap("Suivant")
        compose.tap("Femelle")
        repeat(5) { compose.tap("Suivant") }
        compose.tap("Continuer")

        compose.onNodeWithText("Associer le collier de Nala", substring = true).assertIsDisplayed()
        assertEquals("Nala", (petRepository as FakePetRepository).created.single().name)
    }

    @Test
    fun `fermer l'ajout du chien pendant l'inscription revient a la decouverte`() {
        signUp()
        repeat(4) { compose.tap("Suivant") }
        compose.tap("Connecter le collier")

        compose.onNodeWithContentDescription("Fermer").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Suivez sa position").assertIsDisplayed()
    }

    @Test
    fun `j'ai deja un compte mene a la connexion`() {
        compose.tap("Créer un compte")
        compose.tap("J'ai déjà un compte")

        compose.onNodeWithText("Connexion").assertIsDisplayed()
    }
}
