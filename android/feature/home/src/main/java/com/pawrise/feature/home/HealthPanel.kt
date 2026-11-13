package com.pawrise.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.feature.home.ui.MaquetteFont
import com.pawrise.feature.home.ui.PawriseGlyph
import com.pawrise.feature.home.ui.PawriseGlyphs
import com.pawrise.feature.home.ui.maquetteStyle
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Panneau ouvert depuis la homepage (US-B 02 à 07), dessiné par la coque au-dessus de tout l'écran (barre
 * d'onglets comprise) : fond assombri à 50 %, panneau blanc collé au bas, coins supérieurs de 30 dp.
 * [onAskAssistant] : « ✦ Demander à Pawrise » (panneaux en alerte) → Chat IA avec la question.
 */
@Composable
fun HealthPanelHost(viewModel: HealthHomeViewModel, onAskAssistant: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheet = state.sheet
    // Contenu gardé pendant l'animation de sortie.
    var displayed by remember { mutableStateOf(sheet) }
    LaunchedEffect(sheet) { if (sheet != null) displayed = sheet }
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = sheet != null
    val close = { viewModel.open(null) }

    BackHandler(enabled = sheet != null, onBack = close)
    if (!visible.currentState && !visible.targetState) return

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(250)), exit = fadeOut(tween(220))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = close)
                    .clearAndSetSemantics { },
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.92f, stiffness = 500f)) { it },
            exit = slideOutVertically(tween(220)) { it },
        ) {
            val pet = state.selectedPet
            val snapshot = state.snapshot
            HealthPanel(onClose = close) {
                when (val current = displayed) {
                    is HealthSheet.Indicator -> snapshot?.indicators?.firstOrNull { it.kind == current.kind }?.let { indicator ->
                        IndicatorDetailSheet(indicator, onClose = close, onAskAssistant = { question ->
                            onAskAssistant(question)
                            close()
                        })
                    }
                    HealthSheet.Collar -> if (pet != null) {
                        CollarStatusSheet(pet, snapshot?.batteryPercent ?: 100, onClose = close)
                    }
                    null -> Unit
                }
            }
        }
    }
}

/** Panneau du bas : bord à bord, coins hauts 30, filet #D5E3EC, marges 12 / 20 / 32, glisser vers le bas pour fermer. */
@Composable
private fun HealthPanel(onClose: () -> Unit, content: @Composable () -> Unit) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .offset { IntOffset(0, offset.value.roundToInt()) }
                .fillMaxWidth()
                .heightIn(max = maxHeight - 8.dp)
                .background(PwColors.SurfaceCard, shape)
                .border(1.dp, PwColors.Border, shape)
                .nestedScroll(remember(onClose) { DragToClose(offset, scope, onClose) })
                .semantics { paneTitle = "Détail" }
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 20.dp, end = 20.dp, top = 13.dp, bottom = 32.dp),
        ) {
            content()
        }
    }
}

/** En-tête du panneau : poignée 40 × 6, titre (Bricolage 700 20) et bouton ✕ rond de 36 dp. */
@Composable
internal fun HealthSheetHeader(title: String, onClose: () -> Unit, closeTint: Color = PwColors.TextMuted) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(40.dp, 6.dp).background(PwColors.Border, RoundedCornerShape(3.dp)))
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = maquetteStyle(MaquetteFont.Display, 20f, FontWeight.Bold, 1.35f, PwColors.TextPrimary),
                modifier = Modifier.weight(1f).padding(end = 8.dp).semantics { heading() },
            )
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .requiredSize(44.dp)
                        .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClose)
                        .semantics { contentDescription = "Fermer" },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(36.dp).background(PwColors.SurfaceElevated, CircleShape), contentAlignment = Alignment.Center) {
                        PawriseGlyph(PawriseGlyphs.Close, 18.dp, closeTint)
                    }
                }
            }
        }
    }
}

/** Glisser le panneau vers le bas (au-delà du haut de son contenu) le ferme ; sinon il revient en place. */
private class DragToClose(
    private val offset: Animatable<Float, *>,
    private val scope: CoroutineScope,
    private val onClose: () -> Unit,
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y >= 0f || offset.value <= 0f) return Offset.Zero
        val used = maxOf(available.y, -offset.value)
        scope.launch { offset.snapTo(offset.value + used) }
        return Offset(0f, used)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
        scope.launch { offset.snapTo(offset.value + available.y) }
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (offset.value <= 0f) return Velocity.Zero
        if (offset.value > 270f || available.y > 2400f) onClose() else offset.animateTo(0f, spring())
        return available
    }
}
