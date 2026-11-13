package com.pawrise.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** US-J de bout en bout dans l'app réelle (MainActivity, navigation, Hilt) ; seul le serveur est simulé. */
@HiltAndroidTest
@UninstallModules(AuthBindings::class, PetBindings::class)
@RunWith(RobolectricTestRunner::class)
class LoginFlowTest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @BindValue @JvmField val repository: AuthRepository = FakeAuthRepository()
    @BindValue @JvmField val petRepository: PetRepository = FakePetRepository()
    private val fake get() = repository as FakeAuthRepository

    @Test
    fun `compte sans chien - connexion mene a l'ajout du chien`() {
        (petRepository as FakePetRepository).pets = emptyList()
        compose.tap("Se connecter")
        compose.field("E-mail").performTextInput("camille.martin@example.com")
        compose.field("Mot de passe").performTextInput("chien-oslo-2026")
        compose.tap("Se connecter")

        compose.onNodeWithText("Ajoutez une photo de votre chien").assertIsDisplayed()
    }

    @Test
    fun `bienvenue, connexion puis homepage`() {
        compose.tap("Se connecter")
        compose.onNodeWithText("Connexion").assertIsDisplayed()

        compose.field("E-mail").performTextInput("camille.martin@example.com")
        compose.field("Mot de passe").performTextInput("chien-oslo-2026")
        compose.tap("Se connecter")

        compose.onNodeWithText("INDICATEURS").assertIsDisplayed()
    }

    @Test
    fun `retour systeme depuis la homepage ne revient pas a la connexion`() {
        compose.tap("Se connecter")
        compose.field("E-mail").performTextInput("camille.martin@example.com")
        compose.field("Mot de passe").performTextInput("chien-oslo-2026")
        compose.tap("Se connecter")

        // Historique vidé à la connexion : rien sous la homepage.
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText("Connexion").assertDoesNotExist()
    }

    @Test
    fun `retour depuis la connexion ramene a bienvenue`() {
        compose.tap("Se connecter")
        compose.tap("Retour")

        compose.onNodeWithText("Pawrise").assertIsDisplayed()
        compose.onNodeWithText("Créer un compte").assertIsDisplayed()
    }

    @Test
    fun `lien creer un compte depuis la connexion`() {
        compose.tap("Se connecter")
        compose.tap("Créer un compte")

        compose.onNodeWithText("Quelques informations, puis on vous montre Pawrise.").assertIsDisplayed()
    }

    @Test
    fun `session expiree ramene a bienvenue`() {
        compose.tap("Se connecter")
        compose.field("E-mail").performTextInput("camille.martin@example.com")
        compose.field("Mot de passe").performTextInput("chien-oslo-2026")
        compose.tap("Se connecter")

        fake.expireSession()
        compose.waitForIdle()

        compose.onNodeWithText("Le collier connecté qui suit la santé et la position de votre chien.").assertIsDisplayed()
    }
}
