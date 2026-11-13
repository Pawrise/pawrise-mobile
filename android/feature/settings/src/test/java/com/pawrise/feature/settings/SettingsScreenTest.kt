package com.pawrise.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pawrise.core.auth.PreferencesUpdate
import com.pawrise.core.auth.SessionState
import com.pawrise.core.auth.User
import com.pawrise.core.designsystem.theme.PawriseTheme
import com.pawrise.core.network.ErrorCodes
import com.pawrise.core.pets.Breed
import com.pawrise.core.pets.scenario.HealthScenario
import com.pawrise.core.pets.scenario.InMemoryHealthScenarioStore
import com.pawrise.core.testing.FakeAuthRepository
import com.pawrise.core.testing.FakePetRepository
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Paramètres : compte, confidentialité, données simulées, déconnexion et suppression, avec de faux dépôts. */
@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule val compose = createComposeRule()

    private val auth = FakeAuthRepository(SessionState.SignedIn, account = ALEX)
    private val pets = FakePetRepository(
        listOf(
            FakePetRepository.NALA.copy(
                breeds = listOf(Breed("golden_retriever", "Golden Retriever")),
                birthDate = LocalDate.now().minusYears(3).minusDays(10),
                collarSerial = "PW-ABC123",
            ),
        ),
    )
    private val scenarios = InMemoryHealthScenarioStore()
    private var signedOut = 0

    private fun launch() {
        val viewModel = SettingsViewModel(auth, pets, scenarios)
        compose.setContent { PawriseTheme { SettingsRoute(onSignedOut = { signedOut++ }, viewModel = viewModel) } }
        compose.waitForIdle()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun see(text: String) = compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()

    @Test
    fun `carte profil, confidentialite, chiens et version`() {
        launch()

        see("Alex Martin")
        see("alex@example.com")
        compose.onNodeWithText("Partage de localisation").assertIsOn()
        compose.onNodeWithText("Notifications santé").assertIsOn()
        compose.onNodeWithText("Données anonymisées").assertIsOff()
        see("Golden Retriever · 3 ans")
        see("PW-ABC123")
        compose.onNodeWithText("Renvoyer le lien de confirmation").assertDoesNotExist()
    }

    @Test
    fun `un interrupteur envoie seulement sa valeur`() {
        launch()
        tap("Données anonymisées")

        compose.onNodeWithText("Données anonymisées").assertIsOn()
        assertEquals(listOf(PreferencesUpdate(anonymizedDataSharing = true)), auth.preferenceUpdates)
    }

    @Test
    fun `interrupteur refuse revient a sa position avec un message`() {
        auth.updatePreferencesResult = FakeAuthRepository.NETWORK_DOWN
        launch()
        tap("Notifications santé")

        compose.onNodeWithText("Notifications santé").assertIsOn()
        see("Impossible de joindre le serveur. Réessayez dans un instant.")
    }

    @Test
    fun `preferences indisponibles proposent de reessayer`() {
        auth.preferencesResult = FakeAuthRepository.NETWORK_DOWN
        launch()
        see("Impossible de joindre le serveur. Réessayez dans un instant.")

        auth.preferencesResult = null
        tap("Réessayer")
        compose.onNodeWithText("Partage de localisation").assertIsOn()
    }

    @Test
    fun `adresse non confirmee - renvoi du lien`() {
        auth.account = ALEX.copy(emailVerified = false)
        launch()
        see("Adresse e-mail pas encore confirmée : ouvrez le lien reçu par e-mail.")

        tap("Renvoyer le lien de confirmation")

        assertEquals(1, auth.resendCalls)
        see("Nouveau lien envoyé. Il est valable 24 heures.")
    }

    @Test
    fun `donnees simulees partagees avec l'onglet Sante`() {
        launch()
        tap("Anomalie")

        assertEquals(HealthScenario.Anomaly, scenarios.scenario.value)
        compose.onNodeWithText("Anomalie").assertIsSelected()
    }

    @Test
    fun `cgu pas encore publiees`() {
        launch()
        tap("Conditions d’utilisation")

        compose.onNodeWithText("Le document sera disponible ici dès sa publication. Version des CGU acceptée : 2026-10.")
            .assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
    }

    @Test
    fun `deconnexion remet le scenario normal`() {
        scenarios.select(HealthScenario.Anomaly)
        launch()
        tap("Se déconnecter")

        assertEquals(1, auth.logoutCalls)
        assertEquals(1, signedOut)
        assertEquals(HealthScenario.Normal, scenarios.scenario.value)
    }

    @Test
    fun `suppression du compte apres confirmation`() {
        launch()
        tap("Supprimer mon compte")
        see("Cette action est définitive")
        see("Le profil de Nala")

        tap("Supprimer définitivement mon compte")
        compose.onNodeWithText("Vous ne pourrez pas revenir en arrière.").assertIsDisplayed()
        compose.onNodeWithText("Supprimer définitivement").performClick()
        compose.waitForIdle()

        assertEquals(1, auth.deleteCalls)
        assertEquals(1, signedOut)
    }

    @Test
    fun `suppression annulee ou refusee`() {
        auth.deleteResult = FakeAuthRepository.problem(409, ErrorCodes.CHIEF_TRANSFER_REQUIRED)
        launch()
        tap("Supprimer mon compte")
        tap("Supprimer définitivement mon compte")
        compose.onNodeWithText("Annuler").performClick()
        assertEquals(0, auth.deleteCalls)

        tap("Supprimer définitivement mon compte")
        compose.onNodeWithText("Supprimer définitivement").performClick()
        compose.waitForIdle()

        see(
            "Vous êtes le propriétaire principal d'un chien partagé. Transférez d'abord ce rôle à un autre propriétaire, " +
                "puis supprimez votre compte.",
        )
        assertEquals(0, signedOut)

        compose.onNodeWithContentDescription("Retour").performClick()
        see("Paramètres")
    }

    @Test
    fun `age et sous-titre du chien`() {
        val today = LocalDate.of(2026, 11, 13)
        assertEquals("5 mois", ageLabel(LocalDate.of(2026, 6, 1), today))
        assertEquals("1 an", ageLabel(LocalDate.of(2025, 6, 1), today))
        assertEquals("3 ans", ageLabel(LocalDate.of(2023, 1, 1), today))
        assertEquals("AM", initials("Alex Martin"))
        assertEquals("Les profils de Nala, Oslo et Rex", dogsConsequence(listOf("Nala", "Oslo", "Rex")))
    }

    private companion object {
        val ALEX = User("u1", "Alex", "Martin", "alex@example.com", null, emailVerified = true)
    }
}
