package com.pawrise.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.theme.BricolageGrotesque
import com.pawrise.core.designsystem.theme.PwColors

/**
 * S25 Détruire le compte (US-35, US-36) : ce qui va disparaître, puis une confirmation finale.
 * Appelle `DELETE /v1/me` ; en cas de succès l'app revient à l'accueil, déconnectée.
 */
@Composable
internal fun DeleteAccountScreen(
    email: String?,
    dogNames: List<String>,
    deleting: Boolean,
    error: String?,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(PwColors.SurfacePage).statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 16.dp)) {
            Icon(
                SettingsIcons.ChevronLeft,
                contentDescription = "Retour",
                tint = if (deleting) PwColors.TextMuted else PwColors.Action,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .clickable(enabled = !deleting, role = Role.Button, onClick = onBack),
            )
            Text(
                "Supprimer le compte",
                style = pwText(17f, FontWeight.SemiBold),
                modifier = Modifier.align(Alignment.Center).semantics { heading() },
            )
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(SettingsIcons.Alert, contentDescription = null, tint = PwColors.StatusAlert, modifier = Modifier.size(32.dp))
                    Text("Cette action est définitive", style = pwText(20f, FontWeight.Bold, family = BricolageGrotesque))
                    Text(
                        "Votre compte ne pourra pas être récupéré. Pour utiliser Pawrise à nouveau, il faudra en créer un autre.",
                        style = pwText(14f, color = PwColors.TextMuted),
                    )
                }
            }

            Column {
                SettingsSectionTitle("Ce qui sera supprimé")
                SettingsCard {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Consequence(SettingsIcons.User, "Votre compte : nom, e-mail, téléphone, mot de passe")
                        Consequence(SettingsIcons.Devices, "Vos connexions sur tous vos appareils")
                        Consequence(SettingsIcons.Shield, "Vos choix de confidentialité et votre acceptation des CGU")
                        Consequence(SettingsIcons.Trash, dogsConsequence(dogNames))
                    }
                }
            }

            email?.let {
                SettingsCard {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Compte", style = pwText(14f), modifier = Modifier.weight(1f))
                        Text(it, style = pwText(14f, color = PwColors.TextMuted))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsActionButton(
                    "Supprimer définitivement mon compte",
                    tint = PwColors.StatusAlert,
                    border = PwColors.StatusAlert.copy(alpha = 0.4f),
                    onClick = { confirming = true },
                    enabled = !deleting,
                    loading = deleting,
                )
                error?.let {
                    Text(
                        it,
                        style = pwText(13f, color = PwColors.StatusAlert),
                        modifier = Modifier.padding(horizontal = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Supprimer votre compte ?", textAlign = TextAlign.Start) },
            text = { Text("Vous ne pourrez pas revenir en arrière.") },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onConfirm()
                }) { Text("Supprimer définitivement", color = PwColors.StatusAlert) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun Consequence(icon: ImageVector, text: String) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = PwColors.StatusAlert, modifier = Modifier.size(20.dp))
        Text(text, style = pwText(14f))
    }
}

/**
 * « Le profil de Nala », « Les profils de Nala, Oslo et Rex ». Sur Android les chiens sont enregistrés
 * sur le serveur (pas « sur ce téléphone » comme sur iOS) : la mention est retirée.
 */
internal fun dogsConsequence(names: List<String>): String = when (names.size) {
    0 -> "Les profils de vos chiens"
    1 -> "Le profil de ${names.single()}"
    else -> "Les profils de ${names.dropLast(1).joinToString(", ")} et ${names.last()}"
}
