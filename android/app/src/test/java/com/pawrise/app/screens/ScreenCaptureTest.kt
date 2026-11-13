package com.pawrise.app.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.pawrise.app.MainActivity
import com.pawrise.app.field
import com.pawrise.app.tap
import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.auth.SessionState
import com.pawrise.core.auth.di.AuthBindings
import com.pawrise.core.pets.PetRepository
import com.pawrise.core.pets.di.PetBindings
import com.pawrise.core.testing.FakeAuthRepository
import com.pawrise.core.testing.FakePetRepository
import dagger.hilt.android.testing.BindValue
import kotlinx.coroutines.CompletableDeferred
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import android.os.Looper
import java.time.Duration
import org.junit.Rule
import org.robolectric.Shadows.shadowOf
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
 * Parcours (équipe parcours) : Découverte A03–A07, Profil du chien A08–A16, Associer le collier A17–A20,
 * avec les valeurs de la maquette (photo d'exemple 1, Nala, 12/04/2023, Femelle, Golden Retriever, 28 kg, 55 cm,
 * PW-ABC123). Compte connecté sans chien : l'app ouvre l'ajout du chien, « Fermer » ramène à la découverte.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@UninstallModules(AuthBindings::class, PetBindings::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = CAPTURE_QUALIFIERS)
class ParcoursScreenCaptureTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    private val pets = FakePetRepository(pets = emptyList())
    @BindValue @JvmField val repository: AuthRepository = FakeAuthRepository(SessionState.SignedIn)
    @BindValue @JvmField val petRepository: PetRepository = pets

    /** Laisse finir les ondulations (animées par l'horloge du Looper, pas par celle de Compose) puis capture. */
    private fun shot(label: String) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        compose.snapshot(label)
    }

    /** Appui par l'action d'accessibilité : pas d'ondulation figée sur la capture suivante. */
    private fun press(text: String) {
        val node = compose.onNodeWithText(text)
        runCatching { node.performScrollTo() }
        node.performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    /** Sans clavier ni curseur, comme la maquette. */
    private fun unfocus() {
        compose.runOnUiThread { compose.activity.window.decorView.findFocus()?.clearFocus() }
        compose.waitForIdle()
    }

    private fun type(label: String, text: String) {
        compose.field(label).performTextInput(text)
        unfocus()
    }

    @Test
    fun parcours() {
        compose.waitUntilAtLeastOneExists(hasContentDescription("Fermer"), 5_000)
        compose.onNodeWithContentDescription("Fermer").performClick()
        compose.waitForIdle()

        listOf("A03", "A04", "A05", "A06").forEach {
            shot(it)
            press("Suivant")
        }
        shot("A07")
        press("Connecter le collier")

        compose.onNodeWithContentDescription("Exemple de photo 1").performSemanticsAction(SemanticsActions.OnClick)
        shot("A08")
        press("Suivant")
        type("Nom du chien", "Nala")
        shot("A09")
        press("Suivant")
        type("Date de naissance", "12042023")
        shot("A10")
        press("Suivant")
        press("Femelle")
        shot("A11")
        press("Suivant")
        type("Rechercher une race", "Golden")
        // Liste filtrée (recherche différée) : seule « Golden Retriever » reste.
        compose.waitUntilDoesNotExist(hasText("Croisé"), 5_000)
        press("Golden Retriever")
        shot("A12")
        press("Suivant")
        type("Poids en kilogrammes", "28")
        shot("A13")
        press("Suivant")
        type("Taille en centimètres", "55")
        shot("A14")
        press("Suivant")
        shot("A15")
        press("Suivant")
        shot("A16")
        press("Continuer")

        compose.waitUntilAtLeastOneExists(hasText("Connectons le collier de Nala"), 5_000)
        shot("A17")
        press("Scanner le QR code")
        shot("A18")
        compose.onNodeWithContentDescription("Retour").performClick()
        compose.waitForIdle()
        press("Saisir le code manuellement")
        type("Code du collier", "PW-ABC123")
        val gate = CompletableDeferred<Unit>().also { pets.gate = it }
        press("Activer le collier")
        shot("A19")
        gate.complete(Unit)
        compose.waitUntilAtLeastOneExists(hasText("Le collier de Nala est activé"), 5_000)
        shot("A20")
    }
}
