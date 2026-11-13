package com.pawrise.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.feature.chat.ChatRoute
import com.pawrise.feature.home.HealthHomeRoute
import com.pawrise.feature.home.HealthPanelHost
import com.pawrise.feature.home.healthHomeViewModel
import com.pawrise.feature.home.ui.MaquetteFont
import com.pawrise.feature.home.ui.PawriseGlyph
import com.pawrise.feature.home.ui.PawriseGlyphs
import com.pawrise.feature.home.ui.maquetteStyle
import com.pawrise.feature.settings.SettingsRoute

/** Onglets de l'app connectée, dans l'ordre de la maquette (US-B 01 · barre du bas). */
enum class MainTab(val title: String, val icon: ImageVector) {
    Location("Localisation", PawriseGlyphs.Map),
    Chat("Chat IA", PawriseGlyphs.Chat),
    Health("Pawrise", PawriseGlyphs.Brand),
    Appointments("Rendez-vous", PawriseGlyphs.Doc),
    Settings("Paramètres", PawriseGlyphs.Settings),
}

/**
 * Coque de l'app connectée : contenu de l'onglet, barre d'onglets de la maquette, et par-dessus tout l'écran
 * le panneau d'indicateur de l'onglet Santé (fond assombri jusque sur la barre, comme dans la maquette).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainShell(onSignedOut: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Health) }
    // Question envoyée au Chat IA depuis un panneau d'indicateur (« Demander à Pawrise »).
    var pendingQuestion by rememberSaveable { mutableStateOf<String?>(null) }
    val health = healthHomeViewModel()
    val dog by health.selectedPet.collectAsStateWithLifecycle()
    // Comme la barre système, la barre Pawrise s'efface quand le clavier est ouvert. On mesure la hauteur réelle :
    // une barre d'outils de saisie (clavier physique, émulateur) compte comme « clavier visible » sans le cacher,
    // et la barre d'onglets ne revenait plus.
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val keyboardVisible = with(LocalDensity.current) { imeBottom > 120.dp.roundToPx() }

    Box(Modifier.fillMaxSize().background(PwColors.SurfacePage)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Box(Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
                when (tab) {
                    MainTab.Health -> HealthHomeRoute(health)
                    MainTab.Chat -> ChatRoute(
                        dog = dog,
                        pendingQuestion = pendingQuestion,
                        onQuestionConsumed = { pendingQuestion = null },
                        onContactVet = { tab = MainTab.Appointments },
                        onShowIndicators = { tab = MainTab.Health },
                    )
                    MainTab.Settings -> SettingsRoute(onSignedOut = onSignedOut)
                    MainTab.Location -> ComingSoonTab(
                        title = "Localisation",
                        icon = PawriseGlyphs.Map,
                        message = "La position de votre chien et ses zones arrivent dans la prochaine livraison.",
                    )
                    MainTab.Appointments -> ComingSoonTab(
                        title = "Rendez-vous",
                        icon = PawriseGlyphs.Doc,
                        message = "La prise de rendez-vous avec un vétérinaire arrive dans la prochaine livraison.",
                    )
                }
            }
            if (!keyboardVisible) {
                PawriseTabBar(selection = tab, onSelect = { tab = it })
            }
        }
        if (tab == MainTab.Health) {
            HealthPanelHost(health, onAskAssistant = { question ->
                pendingQuestion = question
                tab = MainTab.Chat
            })
        }
    }
}

/**
 * Barre de la maquette (80 dp, fond #F4F8FB, filet supérieur #D5E3EC) : cinq onglets de 72 dp répartis,
 * capsule pleine de 54 × 36 derrière l'icône de l'onglet actif, libellé Inter 10. Elle descend jusqu'au
 * bas de l'écran (sous la barre de navigation système).
 */
@Composable
internal fun PawriseTabBar(selection: MainTab, onSelect: (MainTab) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(PwColors.SurfacePage)
            .drawBehind { drawLine(PwColors.Border, Offset(0f, 0.5.dp.toPx()), Offset(size.width, 0.5.dp.toPx()), 1.dp.toPx()) }
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.entries.forEach { item -> TabItem(item, item == selection) { onSelect(item) } }
        }
    }
}

@Composable
private fun TabItem(tab: MainTab, selected: Boolean, onClick: () -> Unit) {
    val capsule by animateColorAsState(if (selected) PwColors.Action else PwColors.Action.copy(alpha = 0f), tween(200), label = "tab")
    val iconTint by animateColorAsState(if (selected) PwColors.ActionText else PwColors.TextMuted, tween(200), label = "tabIcon")
    Column(
        Modifier
            .width(72.dp)
            .height(80.dp)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = tab.title
                role = Role.Tab
                this.selected = selected
                onClick { onClick(); true }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(54.dp, 36.dp).background(capsule, CircleShape), contentAlignment = Alignment.Center) {
            PawriseGlyph(tab.icon, 24.dp, iconTint)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            tab.title,
            style = maquetteStyle(MaquetteFont.Inter, 10f, lineHeight = 1.4f, color = PwColors.TextPrimary).copy(textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false,
        )
    }
}

/** Onglet pas encore livré : message clair, sans fonctionnalité simulée (comme l'app iOS). */
@Composable
private fun ComingSoonTab(title: String, icon: ImageVector, message: String) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PawriseGlyph(icon, 44.dp, PwColors.Action)
        Spacer(Modifier.height(12.dp))
        Text(title, style = maquetteStyle(MaquetteFont.Display, 24f, FontWeight.Bold, color = PwColors.TextPrimary))
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = maquetteStyle(MaquetteFont.Inter, 15f, lineHeight = 1.35f, color = PwColors.TextMuted).copy(textAlign = TextAlign.Center),
        )
    }
}
