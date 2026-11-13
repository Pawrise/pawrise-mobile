package com.pawrise.app.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
            compose.snapshot(it)
            compose.tap("Suivant")
        }
        compose.snapshot("A07")
        compose.tap("Connecter le collier")

        compose.onNodeWithContentDescription("Exemple de photo 1").performClick()
        compose.snapshot("A08")
        compose.tap("Suivant")
        type("Nom du chien", "Nala")
        compose.snapshot("A09")
        compose.tap("Suivant")
        type("Date de naissance", "12042023")
        compose.snapshot("A10")
        compose.tap("Suivant")
        compose.tap("Femelle")
        compose.snapshot("A11")
        compose.tap("Suivant")
        type("Rechercher une race", "Golden")
        // Liste filtrée (recherche différée) : seule « Golden Retriever » reste.
        compose.waitUntilDoesNotExist(hasText("Croisé"), 5_000)
        compose.tap("Golden Retriever")
        compose.snapshot("A12")
        compose.tap("Suivant")
        type("Poids en kilogrammes", "28")
        compose.snapshot("A13")
        compose.tap("Suivant")
        type("Taille en centimètres", "55")
        compose.snapshot("A14")
        compose.tap("Suivant")
        compose.snapshot("A15")
        compose.tap("Suivant")
        compose.snapshot("A16")
        compose.tap("Continuer")

        compose.waitUntilAtLeastOneExists(hasText("Connectons le collier de Nala"), 5_000)
        compose.snapshot("A17")
        compose.tap("Scanner le QR code")
        compose.snapshot("A18")
        compose.onNodeWithContentDescription("Retour").performClick()
        compose.waitForIdle()
        compose.tap("Saisir le code manuellement")
        type("Code du collier", "PW-ABC123")
        val gate = CompletableDeferred<Unit>().also { pets.gate = it }
        compose.tap("Activer le collier")
        compose.snapshot("A19")
        gate.complete(Unit)
        compose.waitUntilAtLeastOneExists(hasText("Le collier de Nala est activé"), 5_000)
        compose.snapshot("A20")
    }
}
