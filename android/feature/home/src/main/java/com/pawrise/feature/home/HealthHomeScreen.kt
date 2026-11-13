package com.pawrise.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.pets.Pet
import com.pawrise.feature.home.data.HealthSnapshot
import com.pawrise.feature.home.data.HealthState
import com.pawrise.feature.home.data.IndicatorKind
import com.pawrise.feature.home.data.batteryState
import com.pawrise.feature.home.ui.DogAvatar
import com.pawrise.feature.home.ui.MaquetteFont
import com.pawrise.feature.home.ui.PawriseGlyph
import com.pawrise.feature.home.ui.PawriseGlyphs
import com.pawrise.feature.home.ui.maquetteStyle
import com.pawrise.feature.home.ui.subtitle

/** Couleur d'un état (texte, icône, anneau). */
val HealthState.tint: Color
    get() = when (this) {
        HealthState.Good -> PwColors.StatusGood
        HealthState.Watch -> PwColors.StatusWatch
        HealthState.Alert -> PwColors.StatusAlert
    }

/** Fond clair d'un état (carte d'en-tête, tuiles, badges). */
val HealthState.surface: Color
    get() = when (this) {
        HealthState.Good -> PwColors.StatusGoodSurface
        HealthState.Watch -> PwColors.StatusWatchSurface
        HealthState.Alert -> PwColors.StatusAlertSurface
    }

/** Icône de la tuile (Figma : heart, pulse, activity, moon, thermometer). */
val IndicatorKind.glyph: ImageVector
    get() = when (this) {
        IndicatorKind.Heart -> PawriseGlyphs.Heart
        IndicatorKind.Afib -> PawriseGlyphs.Pulse
        IndicatorKind.Activity -> PawriseGlyphs.Activity
        IndicatorKind.Sleep -> PawriseGlyphs.Moon
        IndicatorKind.Temperature -> PawriseGlyphs.Thermometer
    }

/** Onglet Pawrise · Santé (Figma US-B 01 Homepage, états normal et anomalie). */
@Composable
fun HealthHomeRoute(viewModel: HealthHomeViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HealthHomeScreen(
        state = state,
        onSwitchDog = viewModel::selectNextDog,
        onShowAnomaly = viewModel::openFirstAnomaly,
        onOpen = viewModel::open,
        onRetryPets = viewModel::loadPets,
        onRetrySnapshot = viewModel::retrySnapshot,
        modifier = modifier,
    )
}

@Composable
fun HealthHomeScreen(
    state: HealthHomeUiState,
    onSwitchDog: () -> Unit,
    onShowAnomaly: () -> Unit,
    onOpen: (HealthSheet) -> Unit,
    onRetryPets: () -> Unit,
    onRetrySnapshot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(PwColors.SurfacePage)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val pet = state.selectedPet
        val snapshot = state.snapshot
        when {
            state.petsError != null && pet == null -> ErrorBlock(state.petsError, onRetryPets)
            pet != null && snapshot != null -> {
                HealthHeaderCard(
                    pet = pet,
                    snapshot = snapshot,
                    badgeState = state.focusedState,
                    canSwitchDog = state.canSwitchDog,
                    onSwitchDog = onSwitchDog,
                    onShowAnomaly = onShowAnomaly,
                )
                IndicatorsSection(snapshot, onOpen)
            }
            state.snapshotError != null -> ErrorBlock(state.snapshotError, onRetrySnapshot)
            state.loadingPets || pet != null -> Loading()
            else -> Unit
        }
    }
}

@Composable
private fun Loading() {
    Column(
        Modifier.fillMaxWidth().heightIn(min = 320.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = PwColors.TextMuted, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text("Chargement des mesures…", style = maquetteStyle(MaquetteFont.Inter, 14f, color = PwColors.TextMuted))
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            message,
            style = maquetteStyle(MaquetteFont.Inter, 14f, lineHeight = 1.35f, color = PwColors.StatusAlert),
            modifier = Modifier
                .fillMaxWidth()
                .background(PwColors.StatusAlertSurface, RoundedCornerShape(16.dp))
                .padding(14.dp),
        )
        Text(
            "Réessayer",
            style = maquetteStyle(MaquetteFont.Inter, 15f, FontWeight.Bold, color = PwColors.TextPrimary).copy(textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(PwColors.SurfaceElevated, RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClick = onRetry)
                .padding(vertical = 16.dp),
        )
    }
}

// En-tête (WellbeingScore)

@Composable
private fun HealthHeaderCard(
    pet: Pet,
    snapshot: HealthSnapshot,
    badgeState: HealthState?,
    canSwitchDog: Boolean,
    onSwitchDog: () -> Unit,
    onShowAnomaly: () -> Unit,
) {
    val state = snapshot.state
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(state.surface, shape)
            .border(1.dp, PwColors.Border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
            // Toujours affiché comme dans la maquette ; actif dès qu'il y a un deuxième chien.
            HeaderIcon(
                icon = PawriseGlyphs.Swap,
                tint = PwColors.TextMuted,
                label = if (canSwitchDog) "Changer de chien" else null,
                onClick = if (canSwitchDog) onSwitchDog else null,
            )
            Spacer(Modifier.weight(1f))
            if (snapshot.hasAnomaly) {
                HeaderIcon(PawriseGlyphs.Alert, PwColors.StatusAlert, "Anomalie détectée, voir le détail", onShowAnomaly)
            } else {
                HeaderIcon(PawriseGlyphs.Alert, PwColors.TextMuted, "Aucune anomalie détectée", null)
            }
        }

        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                contentDescription = "Score de bien-être ${snapshot.score} sur 100"
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DogAvatar(pet, 58.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(pet.name, style = maquetteStyle(MaquetteFont.Display, 18f, FontWeight.Bold, 1.4f, PwColors.TextPrimary))
                val subtitle = pet.subtitle()
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = maquetteStyle(MaquetteFont.Inter, 11f, lineHeight = 1.4f, color = PwColors.TextMuted))
                }
                val badge = badgeState ?: state
                Text(
                    badge.label,
                    style = maquetteStyle(MaquetteFont.Inter, 11f, lineHeight = 1.4f, color = badge.tint),
                    modifier = Modifier.background(badge.surface, CircleShape).padding(vertical = 4.dp, horizontal = 8.dp),
                )
            }
            ScoreRing(snapshot.score, state)
        }

        Text(snapshot.summary, style = maquetteStyle(MaquetteFont.Inter, 11f, lineHeight = 1.4f, color = PwColors.TextPrimary))
    }
}

/** Icône de 20 dp avec une zone tactile de 44 dp, sans changer la mise en page. */
@Composable
private fun HeaderIcon(icon: ImageVector, tint: Color, label: String?, onClick: (() -> Unit)?) {
    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .requiredSize(44.dp)
                .then(
                    when {
                        onClick != null -> Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClickLabel = null,
                            onClick = onClick,
                        ).semantics { contentDescription = label.orEmpty() }
                        label != null -> Modifier.semantics { contentDescription = label }
                        else -> Modifier.clearAndSetSemantics { invisibleToUser() }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            PawriseGlyph(icon, 20.dp, tint)
        }
    }
}

/** Anneau du score de bien-être : cercle de 6 dp à la couleur de l'état (maquette). */
@Composable
private fun ScoreRing(score: Int, state: HealthState) {
    Box(
        Modifier.size(72.dp).border(BorderStroke(6.dp, state.tint), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("$score", style = maquetteStyle(MaquetteFont.Mono, 28f, lineHeight = 1.4f, color = PwColors.TextPrimary))
    }
}

// Tuiles

@Composable
private fun IndicatorsSection(snapshot: HealthSnapshot, onOpen: (HealthSheet) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                "INDICATEURS",
                style = maquetteStyle(MaquetteFont.Inter, 13f, FontWeight.SemiBold, 1.4f, PwColors.TextMuted),
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Toucher pour le détail",
                style = maquetteStyle(MaquetteFont.Inter, 11f, lineHeight = 1.4f, color = PwColors.TextMuted),
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
        val tiles = snapshot.indicators.map { indicator ->
            TileModel(indicator.kind.glyph, indicator.headline, indicator.state, indicator.label) {
                onOpen(HealthSheet.Indicator(indicator.kind))
            }
        } + TileModel(PawriseGlyphs.Battery, "${snapshot.batteryPercent}%", batteryState(snapshot.batteryPercent), "Batterie du collier") {
            onOpen(HealthSheet.Collar)
        }
        tiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { tile -> HealthTile(tile, Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private class TileModel(
    val icon: ImageVector,
    val headline: String,
    val state: HealthState,
    val name: String,
    val onClick: () -> Unit,
)

@Composable
private fun HealthTile(tile: TileModel, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .scale(if (pressed) 0.97f else 1f)
            .heightIn(min = 108.dp)
            .background(tile.state.surface, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = tile.onClick)
            .clearAndSetSemantics {
                contentDescription = "${tile.name}, ${tile.headline}, ${tile.state.label}"
                role = Role.Button
                onClick(label = "Afficher le détail") { tile.onClick(); true }
            }
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PawriseGlyph(tile.icon, 28.dp, tile.state.tint)
        Text(
            tile.headline,
            style = maquetteStyle(MaquetteFont.Inter, 11f, FontWeight.SemiBold, 1.4f, tile.state.tint).copy(textAlign = TextAlign.Center),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
