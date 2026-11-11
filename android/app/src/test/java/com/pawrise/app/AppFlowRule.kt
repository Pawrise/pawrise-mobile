package com.pawrise.app

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

fun AndroidComposeTestRule<*, *>.field(label: String): SemanticsNodeInteraction =
    onNode(hasSetTextAction() and hasContentDescription(label))

/** Appuie sur un texte visible, en faisant défiler l'écran s'il le faut (et s'il défile). */
fun AndroidComposeTestRule<*, *>.tap(text: String) {
    val node = onNodeWithText(text)
    runCatching { node.performScrollTo() }
    node.performClick()
    waitForIdle()
}
