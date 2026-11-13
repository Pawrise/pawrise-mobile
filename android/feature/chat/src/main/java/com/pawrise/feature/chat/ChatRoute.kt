package com.pawrise.feature.chat

import androidx.compose.runtime.Composable
import com.pawrise.core.designsystem.component.PlaceholderScreen

/** Onglet Chat IA (US-H). Squelette, remplacé par l'équipe Accueil. */
@Composable
fun ChatRoute(pendingQuestion: String? = null, onQuestionConsumed: () -> Unit = {}) {
    PlaceholderScreen("Chat IA")
}
