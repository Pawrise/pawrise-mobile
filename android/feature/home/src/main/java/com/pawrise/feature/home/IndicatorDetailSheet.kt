package com.pawrise.feature.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.feature.home.data.HealthIndicator
import com.pawrise.feature.home.data.HealthPeriod
import com.pawrise.feature.home.data.HealthState
import com.pawrise.feature.home.ui.MaquetteFont
import com.pawrise.feature.home.ui.PawriseGlyph
import com.pawrise.feature.home.ui.PawriseGlyphs
import com.pawrise.feature.home.ui.maquetteStyle

/** Panneau de détail d'un indicateur (Figma US-B 02 à 06, états normal et anomalie). */
@Composable
internal fun IndicatorDetailSheet(indicator: HealthIndicator, onClose: () -> Unit, onAskAssistant: (String) -> Unit) {
    var period by rememberSaveable(indicator.kind) { mutableStateOf(HealthPeriod.Today) }
    val pill = indicator.pillState
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HealthSheetHeader(
            title = indicator.label,
            onClose = onClose,
            closeTint = if (pill == HealthState.Good) PwColors.TextMuted else pill.tint,
        )

        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StateBadge(pill)
            Text(
                indicator.headline,
                style = maquetteStyle(
                    MaquetteFont.Mono, 20f, lineHeight = 1.35f,
                    color = if (indicator.state == HealthState.Good) PwColors.TextPrimary else indicator.state.tint,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(indicator.message, style = maquetteStyle(MaquetteFont.Inter, 14f, lineHeight = 1.35f, color = PwColors.TextPrimary))
            Text(indicator.detail, style = maquetteStyle(MaquetteFont.Inter, 13f, lineHeight = 1.35f, color = PwColors.TextMuted))
        }

        if (indicator.hasPeriods) {
            PeriodSelector(period) { period = it }
        }

        // Figma : filet intérieur, d'où 13 dp entre le bord et le contenu (cartes de 200 / 176 / 75 dp).
        val cardShape = RoundedCornerShape(24.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .background(PwColors.SurfacePage, cardShape)
                .border(1.dp, PwColors.Border, cardShape)
                .padding(13.dp),
        ) {
            IndicatorChart(indicator, if (indicator.hasPeriods) period else HealthPeriod.Today)
        }

        if (indicator.offersAssistant) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .background(PwColors.Action, RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button) { onAskAssistant("${indicator.message} Que dois-je faire ?") }
                    .semantics { contentDescription = "Demander à Pawrise" },
                contentAlignment = Alignment.Center,
            ) {
                Text("✦  Demander à Pawrise", style = maquetteStyle(MaquetteFont.Inter, 15f, FontWeight.Bold, color = PwColors.ActionText))
            }
        }
    }
}

/** Pastille d'état du panneau (120 × 28, rayon 14) : coche et libellé à la couleur de l'état, fond vert clair. */
@Composable
private fun StateBadge(state: HealthState) {
    Row(
        Modifier.size(120.dp, 28.dp).background(PwColors.StatusGoodSurface, CircleShape),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PawriseGlyph(PawriseGlyphs.Check, 14.dp, state.tint)
        Text(state.label, style = maquetteStyle(MaquetteFont.Inter, 12f, FontWeight.SemiBold, 1.35f, state.tint), maxLines = 1)
    }
}

/** Sélecteur « Aujourd'hui / 7 jours / 30 jours » (segment actif plein, fond #F4F8FB). */
@Composable
private fun PeriodSelector(selection: HealthPeriod, onSelect: (HealthPeriod) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(PwColors.SurfacePage, CircleShape).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HealthPeriod.entries.forEach { period ->
            val selected = period == selection
            val background by animateColorAsState(if (selected) PwColors.Action else PwColors.SurfacePage, label = "period")
            Box(
                Modifier
                    .weight(1f)
                    .height(32.dp)
                    .background(background, CircleShape)
                    .clickable { onSelect(period) }
                    .clearAndSetSemantics {
                        contentDescription = period.label
                        role = Role.Tab
                        this.selected = selected
                        onClick { onSelect(period); true }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    period.label,
                    style = maquetteStyle(
                        MaquetteFont.Inter, 12f, FontWeight.SemiBold, 1.35f,
                        if (selected) PwColors.ActionText else PwColors.TextMuted,
                    ),
                )
            }
        }
    }
}
