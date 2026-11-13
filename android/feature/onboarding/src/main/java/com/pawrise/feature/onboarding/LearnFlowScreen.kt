package com.pawrise.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.component.PwFlowIconButton
import com.pawrise.core.designsystem.component.PwIcons
import com.pawrise.core.designsystem.component.PwPrimaryButton
import com.pawrise.core.designsystem.theme.PawriseTheme
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.designsystem.theme.PwRadius
import com.pawrise.core.designsystem.theme.PwText
import kotlinx.coroutines.launch

internal data class LearnStep(val short: String, @DrawableRes val icon: Int, val title: String, val hint: String)

/** Textes de la maquette (pawrise-figma `auth/LearnFlow.tsx`, Figma 2025:1885 → 2025:2011). */
internal val LEARN_STEPS = listOf(
    LearnStep(
        "App", R.drawable.ic_learn_paw, "Pawrise veille sur votre chien",
        "Le collier mesure sa santé et indique où il se trouve. Vous consultez tout dans l'application.",
    ),
    LearnStep(
        "Collier", R.drawable.ic_learn_signal, "Allumez le collier",
        "Maintenez le bouton du boîtier jusqu'au voyant. Le collier se réveille et se prépare à transmettre.",
    ),
    LearnStep(
        "Mesures", R.drawable.ic_learn_heart, "Les mesures arrivent",
        "Rythme cardiaque, activité, sommeil et température s'affichent dans Santé dès que le collier envoie ses données.",
    ),
    LearnStep(
        "Alerte", R.drawable.ic_learn_alert, "Un rendez-vous en cas de danger",
        "Si une alerte s'affiche, ouvrez Rendez-vous et réservez un vétérinaire pour votre chien.",
    ),
    LearnStep(
        "Position", R.drawable.ic_learn_map, "Suivez sa position",
        "L'onglet Localisation montre où se trouve votre chien, pour le retrouver rapidement.",
    ),
)

/** US-A · 03-07 Découverte (`LearnFlow`). Texte statique, hors API. [onDone] : « Connecter le collier ». */
@Composable
fun LearnFlowRoute(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState { LEARN_STEPS.size }
    val scope = rememberCoroutineScope()
    val step = pager.currentPage
    // Retour système : étape précédente ; à la première étape, comportement normal (quitter).
    BackHandler(enabled = step > 0) { scope.launch { pager.animateScrollToPage(step - 1) } }
    LearnFlowScreen(
        step = step,
        onStep = { scope.launch { pager.animateScrollToPage(it) } },
        onDone = onDone,
        modifier = modifier,
        pager = pager,
    )
}

/**
 * Maquette A03–A07 : bloc « Étapes » de 104 dp (« Étape n sur 5 » à 18 dp du haut, retour 20 dp à gauche,
 * pastilles 18 dp plus bas), pages balayables centrées dans la zone restante, bouton à 16 dp du bas.
 */
@Composable
internal fun LearnFlowScreen(
    step: Int,
    onStep: (Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    pager: PagerState = rememberPagerState(initialPage = step) { LEARN_STEPS.size },
) {
    val last = step == LEARN_STEPS.lastIndex
    Column(modifier.fillMaxSize().background(PwColors.SurfacePage).safeDrawingPadding()) {
        Box(Modifier.fillMaxWidth().height(104.dp)) {
            Text(
                "Étape ${step + 1} sur ${LEARN_STEPS.size}",
                style = PwText.inter(13f, FontWeight.SemiBold),
                color = PwColors.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp).semantics { heading() },
            )
            Box(Modifier.padding(start = 8.dp, top = 6.dp)) {
                PwFlowIconButton(
                    PwIcons.ChevronLeftBold,
                    "Retour",
                    onClick = if (step > 0) ({ onStep(step - 1) }) else null,
                    tint = PwColors.TextPrimary,
                )
            }
            StepChips(step, onStep, Modifier.align(Alignment.TopCenter).padding(top = 54.dp))
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page -> LearnPage(LEARN_STEPS[page]) }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)) {
            PwPrimaryButton(
                text = if (last) "Connecter le collier" else "Suivant",
                onClick = { if (last) onDone() else onStep(step + 1) },
            )
        }
    }
}

/** Icône 96 dp (pictogramme 40), titre Bricolage 700 26 (largeur 342), texte Inter 15 (largeur 320), écarts 16. */
@Composable
private fun LearnPage(page: LearnStep) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier.size(96.dp).background(PwColors.SurfaceElevated, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(painterResource(page.icon), contentDescription = null, tint = PwColors.Action, modifier = Modifier.size(40.dp)) }
        Text(
            page.title,
            style = PwText.display(26f),
            color = PwColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        Text(
            page.hint,
            style = PwText.inter(15f),
            color = PwColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
    }
}

/** Pastilles 64 × 28 : étape en cours #0A5C78 / texte blanc, étapes vues #E5F1F7, à venir blanches. */
@Composable
private fun StepChips(step: Int, onStep: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        LEARN_STEPS.forEachIndexed { index, item ->
            val selected = index == step
            Box(
                Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 28.dp)
                    .background(
                        when {
                            selected -> PwColors.Action
                            index < step -> PwColors.SurfaceElevated
                            else -> PwColors.SurfaceCard
                        },
                        PwRadius.Pill,
                    )
                    .semantics { this.selected = selected }
                    .clickable(role = Role.Tab, onClickLabel = "${item.short}, étape ${index + 1}") { onStep(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    item.short,
                    style = PwText.inter(11f, FontWeight.SemiBold),
                    color = if (selected) PwColors.ActionText else PwColors.TextMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LearnFlowPreview() {
    PawriseTheme { LearnFlowScreen(step = 1, onStep = {}, onDone = {}) }
}
