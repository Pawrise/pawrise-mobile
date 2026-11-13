package com.pawrise.app.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.pawrise.core.designsystem.component.PlaceholderScreen
import com.pawrise.feature.chat.ChatRoute
import com.pawrise.feature.home.HealthHomeRoute
import com.pawrise.feature.settings.SettingsRoute

/** Onglets de l'app connectée, dans l'ordre de la maquette (US-B 01 · barre du bas). */
enum class MainTab(val title: String) {
    Location("Localisation"), Chat("Chat IA"), Health("Pawrise"), Appointments("Rendez-vous"), Settings("Paramètres")
}

/**
 * Coque de l'app connectée : contenu de l'onglet + barre d'onglets.
 * Squelette fonctionnel : l'équipe Accueil lui donne l'apparence exacte de la maquette (barre de 80 pt,
 * capsule 54 × 36 sur l'onglet actif, icônes Figma, Inter 10).
 */
@Composable
fun MainShell(onSignedOut: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Health) }
    // Question envoyée au Chat IA depuis un détail d'indicateur (« Demander à Pawrise »).
    var pendingQuestion by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                MainTab.Health -> HealthHomeRoute(onAskAssistant = { question ->
                    pendingQuestion = question
                    tab = MainTab.Chat
                })
                MainTab.Chat -> ChatRoute(pendingQuestion = pendingQuestion, onQuestionConsumed = { pendingQuestion = null })
                MainTab.Settings -> SettingsRoute(onSignedOut = onSignedOut)
                MainTab.Location -> PlaceholderScreen("Localisation")
                MainTab.Appointments -> PlaceholderScreen("Rendez-vous")
            }
        }
        Row(Modifier.fillMaxWidth()) {
            MainTab.entries.forEach { item ->
                TextButton(
                    onClick = { tab = item },
                    modifier = Modifier.weight(1f).semantics {
                        role = Role.Tab
                        selected = item == tab
                    },
                ) { Text(item.title) }
            }
        }
    }
}
