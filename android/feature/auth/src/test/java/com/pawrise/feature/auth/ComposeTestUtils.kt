package com.pawrise.feature.auth

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule

/** Champ de formulaire retrouvé par son libellé visible (comme le lit TalkBack). */
fun ComposeContentTestRule.field(label: String): SemanticsNodeInteraction =
    onNode(hasSetTextAction() and hasContentDescription(label))
