package com.pawrise.feature.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.pawrise.core.designsystem.theme.PawriseTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** US-A · 03-07 Découverte : 5 étapes, pastilles, retour, fin. */
@RunWith(RobolectricTestRunner::class)
class LearnFlowTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var done = 0

    @Before
    fun setUp() {
        compose.setContent { PawriseTheme { LearnFlowRoute(onDone = { done++ }) } }
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    private fun systemBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun `les cinq etapes de la maquette puis connecter le collier`() {
        val titles = listOf(
            "Pawrise veille sur votre chien",
            "Allumez le collier",
            "Les mesures arrivent",
            "Un rendez-vous en cas de danger",
            "Suivez sa position",
        )
        titles.forEachIndexed { index, title ->
            compose.onNodeWithText("Étape ${index + 1} sur 5").assertIsDisplayed()
            compose.onNodeWithText(title).assertIsDisplayed()
            if (index < titles.lastIndex) tap("Suivant")
        }
        compose.onNodeWithText("Suivant").assertDoesNotExist()

        tap("Connecter le collier")
        assertEquals(1, done)
    }

    @Test
    fun `pas de retour a la premiere etape`() {
        compose.onNodeWithContentDescription("Retour").assertDoesNotExist()
    }

    @Test
    fun `fleche retour et retour systeme reviennent a l'etape precedente`() {
        tap("Suivant")
        tap("Suivant")
        compose.onNodeWithText("Les mesures arrivent").assertIsDisplayed()

        compose.onNodeWithContentDescription("Retour").performClick()
        compose.onNodeWithText("Allumez le collier").assertIsDisplayed()

        systemBack()
        compose.onNodeWithText("Pawrise veille sur votre chien").assertIsDisplayed()
    }

    @Test
    fun `pastilles - aller directement a une etape, comme sur iOS`() {
        tap("Suivant")
        tap("Suivant")
        compose.onNodeWithText("Mesures").assertIsSelected()

        tap("App")
        compose.onNodeWithText("Pawrise veille sur votre chien").assertIsDisplayed()

        tap("Position")
        compose.onNodeWithText("Suivez sa position").assertIsDisplayed()
        compose.onNodeWithText("Connecter le collier").assertIsDisplayed()
        assertEquals(0, done)
    }

    @Test
    fun `balayer change d'etape`() {
        compose.onNodeWithText("Pawrise veille sur votre chien").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("Étape 2 sur 5").assertIsDisplayed()
        compose.onNodeWithText("Allumez le collier").assertIsDisplayed()
    }

    @Test
    fun `apostrophes droites de la maquette`() {
        compose.onNodeWithText("Le collier mesure sa santé et indique où il se trouve. Vous consultez tout dans l'application.")
            .assertIsDisplayed()
    }
}
