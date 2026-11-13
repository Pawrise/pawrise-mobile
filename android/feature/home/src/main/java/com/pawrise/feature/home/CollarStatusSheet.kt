package com.pawrise.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.pets.Pet
import com.pawrise.feature.home.data.batteryState
import com.pawrise.feature.home.ui.CollarInfo
import com.pawrise.feature.home.ui.MaquetteFont
import com.pawrise.feature.home.ui.PawriseGlyph
import com.pawrise.feature.home.ui.PawriseGlyphs
import com.pawrise.feature.home.ui.collar
import com.pawrise.feature.home.ui.maquetteStyle

/** Panneau « Collier » ouvert depuis la tuile batterie (Figma US-B 07). */
@Composable
internal fun CollarStatusSheet(pet: Pet, batteryPercent: Int, onClose: () -> Unit) {
    var showsReplaceInfo by remember { mutableStateOf(false) }
    val collar = pet.collar
    val caption = maquetteStyle(MaquetteFont.Inter, 12f, lineHeight = 1.35f, color = PwColors.TextMuted).copy(textAlign = TextAlign.Center)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HealthSheetHeader("Collier", onClose)
        BatteryCard(batteryPercent)
        if (collar != null) {
            DetailsCard(collar)
            Text(
                "${pet.name} · ${if (collar.connected) "collier connecté" else "collier déconnecté"}",
                style = caption,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text("Aucun collier n'est encore associé à ${pet.name}.", style = caption, modifier = Modifier.fillMaxWidth())
        }
        Box(
            Modifier
                .fillMaxWidth()
                .background(PwColors.SurfaceElevated, RoundedCornerShape(16.dp))
                .clickable(role = Role.Button) { showsReplaceInfo = true }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Remplacer le collier", style = maquetteStyle(MaquetteFont.Inter, 14f, lineHeight = 1.35f, color = PwColors.TextPrimary))
        }
    }
    if (showsReplaceInfo) {
        AlertDialog(
            onDismissRequest = { showsReplaceInfo = false },
            confirmButton = { TextButton(onClick = { showsReplaceInfo = false }) { Text("OK") } },
            title = { Text("Remplacer le collier") },
            text = { Text("Le remplacement du collier arrivera avec la gestion des colliers.") },
        )
    }
}

@Composable
private fun BatteryCard(percent: Int) {
    val state = batteryState(percent)
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(PwColors.SurfacePage, shape)
            .border(1.dp, PwColors.Border, shape)
            .padding(horizontal = 17.dp, vertical = 17.dp)
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).background(state.surface, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            PawriseGlyph(PawriseGlyphs.Battery, 26.dp, state.tint)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Batterie du collier", style = maquetteStyle(MaquetteFont.Inter, 13f, lineHeight = 1.35f, color = PwColors.TextMuted))
            Text("$percent%", style = maquetteStyle(MaquetteFont.Inter, 24f, FontWeight.Bold, 1.35f, state.tint))
            Box(Modifier.fillMaxWidth().height(8.dp).background(PwColors.Border, CircleShape)) {
                Box(
                    Modifier
                        .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                        .height(8.dp)
                        .background(state.tint, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun DetailsCard(collar: CollarInfo) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(PwColors.SurfacePage, shape)
            .border(1.dp, PwColors.Border, shape)
            .padding(vertical = 1.dp),
    ) {
        DetailRow(
            PawriseGlyphs.Signal, "Réseau", if (collar.signalStrong) "Signal fort" else "Signal faible",
            if (collar.signalStrong) PwColors.StatusGood else PwColors.StatusWatch,
        )
        DetailRow(PawriseGlyphs.Shield, "Code du collier", collar.code, PwColors.TextPrimary)
        DetailRow(PawriseGlyphs.Battery, "Modèle", collar.model, PwColors.TextPrimary)
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, valueColor: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 17.dp)
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PawriseGlyph(icon, 18.dp, PwColors.TextMuted)
        Text(label, style = maquetteStyle(MaquetteFont.Inter, 14f, lineHeight = 1.35f, color = PwColors.TextMuted), modifier = Modifier.weight(1f))
        Text(value, style = maquetteStyle(MaquetteFont.Mono, 12f, lineHeight = 1.35f, color = valueColor).copy(textAlign = TextAlign.End))
    }
}
