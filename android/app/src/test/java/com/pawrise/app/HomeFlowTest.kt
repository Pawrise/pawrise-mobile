package com.pawrise.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.auth.SessionState
import com.pawrise.core.auth.di.AuthBindings
import com.pawrise.core.pets.PetRepository
import com.pawrise.core.pets.di.PetBindings
import com.pawrise.core.pets.scenario.HealthScenario
import com.pawrise.core.pets.scenario.HealthScenarioStore
import com.pawrise.core.testing.FakeAuthRepository
import com.pawrise.core.testing.FakePetRepository
import com.pawrise.feature.chat.assistant.AssistantBindings
import com.pawrise.feature.chat.assistant.AssistantService
import com.pawrise.feature.chat.assistant.ScriptedAssistantService
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Coque connectée : onglets, panneaux d'indicateurs, « Demander à Pawrise » → Chat IA. */
@HiltAndroidTest
@UninstallModules(AuthBindings::class, PetBindings::class, AssistantBindings::class)
@RunWith(RobolectricTestRunner::class)
class HomeFlowTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @BindValue @JvmField val repository: AuthRepository = FakeAuthRepository(SessionState.SignedIn)
    @BindValue @JvmField val petRepository: PetRepository = FakePetRepository()
    @BindValue @JvmField val assistant: AssistantService = ScriptedAssistantService()

    @Inject lateinit var scenarioStore: HealthScenarioStore

    @Test
    fun `homepage, panneau puis fermeture`() {
        compose.onNodeWithContentDescription("Pawrise").assertIsSelected()
        compose.onNodeWithText("Tout semble normal. Rien à signaler pour le moment.").assertIsDisplayed()
        compose.onNode(hasContentDescription("Rythme cardiaque, 78 bpm, Tout va bien")).performClick()
        compose.onNodeWithText("Le rythme cardiaque de Nala est dans ses valeurs habituelles.").assertIsDisplayed()
        compose.onNodeWithText("✦  Demander à Pawrise").assertDoesNotExist()
        compose.onNodeWithContentDescription("7 jours").performClick()
        compose.onNodeWithContentDescription("7 jours").assertIsSelected()
        compose.onNodeWithContentDescription("Fermer").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Le rythme cardiaque de Nala est dans ses valeurs habituelles.").assertDoesNotExist()

        compose.onNode(hasContentDescription("Batterie du collier", substring = true)).performClick()
        compose.onNodeWithText("PW-C28A4F").assertIsDisplayed()
        compose.onNodeWithText("Nala · collier connecté").assertIsDisplayed()
    }

    @Test
    fun `anomalie, demander à Pawrise ouvre le Chat IA avec la question`() {
        hilt.inject()
        scenarioStore.select(HealthScenario.Anomaly)
        compose.waitForIdle()
        compose.onNodeWithText("Des indicateurs méritent votre attention aujourd'hui.").assertIsDisplayed()

        compose.onNodeWithContentDescription("Anomalie détectée, voir le détail").performClick()
        compose.onNodeWithText("Le rythme cardiaque de Nala est inhabituellement élevé depuis 45 minutes.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Demander à Pawrise").performClick()
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Chat IA").assertIsSelected()
        val question = "Le rythme cardiaque de Nala est inhabituellement élevé depuis 45 minutes. Que dois-je faire ?"
        compose.onNodeWithContentDescription("Vous : $question").assertIsDisplayed()
        assertEquals(question, (assistant as ScriptedAssistantService).requests.single().userMessage)
        // Le chatbot lit le chien affiché via API-CORE : pet_ref = son identifiant.
        assertEquals(FakePetRepository.NALA.id, assistant.requests.single().petRef)
    }

    @Test
    fun `chat IA, suggestions, envoi désactivé, liens`() {
        compose.onNodeWithContentDescription("Chat IA").performClick()
        compose.onNodeWithText("Bonjour Camille 👋 Je suis Pawrise, l'assistant de Nala. Que souhaitez-​vous consulter ?").assertIsDisplayed()
        compose.onNodeWithContentDescription("Envoyer").assertIsNotEnabled()
        compose.onNodeWithText("Et son sommeil ?").performClick()
        compose.onNodeWithText("Nala — 11 h de sommeil.").assertIsDisplayed()
        compose.onNodeWithText("Voir les indicateurs").assertDoesNotExist()

        compose.onNodeWithText("Contacter un vétérinaire").performClick()
        compose.onNodeWithContentDescription("Rendez-vous").assertIsSelected()
        compose.onNodeWithText("La prise de rendez-vous avec un vétérinaire arrive dans la prochaine livraison.").assertIsDisplayed()

        // La conversation est conservée d'un onglet à l'autre.
        compose.onNodeWithContentDescription("Chat IA").performClick()
        compose.onNodeWithText("Nala — 11 h de sommeil.").assertIsDisplayed()
    }
}
