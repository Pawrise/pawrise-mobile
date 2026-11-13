package com.pawrise.app.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.pawrise.app.MainActivity
import com.pawrise.app.field
import com.pawrise.app.tap
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captures des écrans de l'app réelle (Hilt + faux dépôts), nommées comme la maquette. */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@UninstallModules(AuthBindings::class, PetBindings::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = CAPTURE_QUALIFIERS)
class ScreenCaptureTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @BindValue @JvmField val repository: AuthRepository = FakeAuthRepository()
    @BindValue @JvmField val petRepository: PetRepository = FakePetRepository(pets = emptyList())

    @Test
    fun auth() {
        compose.snapshot("A01")
        compose.tap("Se connecter")
        compose.field("E-mail").performTextInput("alex@example.com")
        compose.field("Mot de passe").performTextInput("chien-26")
        compose.snapshot("J02")
    }
}

/**
 * Captures de l'équipe Accueil (US-B Santé normal / anomalie, US-H Chat IA) : session ouverte, Nala (photo
 * d'exemple 1, Golden Retriever, 3 ans, collier PW-ABC123), faux assistant. Noms qualifiés pour ne pas toucher
 * aux imports partagés du fichier.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@UninstallModules(AuthBindings::class, PetBindings::class, com.pawrise.feature.chat.assistant.AssistantBindings::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = CAPTURE_QUALIFIERS)
class AccueilScreenCaptureTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @BindValue @JvmField val repository: AuthRepository =
        FakeAuthRepository(com.pawrise.core.auth.SessionState.SignedIn)
    @BindValue @JvmField val petRepository: PetRepository = FakePetRepository(pets = listOf(DEMO_NALA))
    @BindValue @JvmField val assistant: com.pawrise.feature.chat.assistant.AssistantService =
        com.pawrise.feature.chat.assistant.ScriptedAssistantService()

    @javax.inject.Inject lateinit var scenarioStore: com.pawrise.core.pets.scenario.HealthScenarioStore

    @Test
    fun sante() {
        compose.snapshot("A21")
        compose.snapshot("B01")
        indicatorPanels(prefix = "B")
    }

    @Test
    fun santeAnomalie() {
        hilt.inject()
        scenarioStore.set(com.pawrise.core.pets.scenario.HealthScenario.Anomaly)
        compose.waitForIdle()
        compose.snapshot("BA01")
        indicatorPanels(prefix = "BA")
    }

    @Test
    fun chat() {
        val fake = assistant as com.pawrise.feature.chat.assistant.ScriptedAssistantService
        compose.onNode(androidx.compose.ui.test.hasContentDescription("Chat IA")).performClick()
        compose.snapshot("H02")
        fake.gate = kotlinx.coroutines.CompletableDeferred()
        compose.tap("Et son sommeil ?")
        compose.snapshot("H03")
        fake.gate?.complete(Unit)
        fake.gate = null
        compose.snapshot("H04")
        compose.field("Écrire un message…").performTextInput("Et son activité ?")
        compose.snapshot("H05")
        compose.onNode(androidx.compose.ui.test.hasContentDescription("Envoyer")).performClick()
        compose.snapshot("H06")
    }

    /** Ouvre chaque panneau depuis sa tuile (02 à 07), capture, puis le ferme. */
    private fun indicatorPanels(prefix: String) {
        listOf("Rythme cardiaque", "Fibrillation atriale", "Activité", "Sommeil", "Température", "Batterie du collier")
            .forEachIndexed { index, tile ->
                compose.onNode(androidx.compose.ui.test.hasContentDescription(tile, substring = true)).performClick()
                compose.snapshot("$prefix%02d".format(index + 2))
                compose.onNode(androidx.compose.ui.test.hasContentDescription("Fermer")).performClick()
                compose.waitForIdle()
                compose.mainClock.advanceTimeBy(1_000)
                compose.waitForIdle()
            }
    }

    companion object {
        val DEMO_NALA = com.pawrise.core.pets.Pet(
            id = "pet-nala",
            name = "Nala",
            photoUrl = null,
            photoSample = com.pawrise.core.pets.SamplePhoto.Sample1,
            birthDate = java.time.LocalDate.now().minusYears(3).minusMonths(7),
            sex = com.pawrise.core.pets.Sex.Female,
            breeds = listOf(com.pawrise.core.pets.Breed("golden_retriever", "Golden Retriever")),
            weightKg = 28.0,
            heightCm = 55.0,
            medicalNotes = null,
            collarSerial = "PW-ABC123",
        )
    }
}
