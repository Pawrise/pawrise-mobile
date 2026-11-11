package com.pawrise.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.component.PwIcons
import com.pawrise.core.designsystem.component.PwPrimaryButton
import com.pawrise.core.designsystem.theme.PawriseTheme
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.designsystem.theme.PwRadius
import com.pawrise.core.designsystem.theme.PwSpacing

internal data class LearnStep(val short: String, val icon: ImageVector, val title: String, val hint: String)

/** Textes de la maquette (pawrise-figma `auth/LearnFlow.tsx`, Figma 2025:1885 → 2025:2011). */
internal val LEARN_STEPS = listOf(
    LearnStep(
        "App", PwIcons.Paw, "Pawrise veille sur votre chien",
        "Le collier mesure sa santé et indique où il se trouve. Vous consultez tout dans l'application.",
    ),
    LearnStep(
        "Collier", PwIcons.Signal, "Allumez le collier",
        "Maintenez le bouton du boîtier jusqu'au voyant. Le collier se réveille et se prépare à transmettre.",
    ),
    LearnStep(
        "Mesures", PwIcons.Heart, "Les mesures arrivent",
        "Rythme cardiaque, activité, sommeil et température s'affichent dans Santé dès que le collier envoie ses données.",
    ),
    LearnStep(
        "Alerte", PwIcons.Alert, "Un rendez-vous en cas de danger",
        "Si une alerte s'affiche, ouvrez Rendez-vous et réservez un vétérinaire pour votre chien.",
    ),
    LearnStep(
        "Position", PwIcons.Map, "Suivez sa position",
        "L'onglet Localisation montre où se trouve votre chien, pour le retrouver rapidement.",
    ),
)

/** US-A · 03-07 Découverte (`LearnFlow`). Texte statique, hors API. [onDone] : « Connecter le collier ». */
@Composable
fun LearnFlowRoute(onDone: () -> Unit, modifier: Modifier = Modifier) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    // Retour système : étape précédente ; à la première étape, comportement normal (quitter).
    BackHandler(enabled = step > 0) { step-- }
    LearnFlowScreen(step = step, onStep = { step = it }, onDone = onDone, modifier = modifier)
}

@Composable
internal fun LearnFlowScreen(step: Int, onStep: (Int) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val current = LEARN_STEPS[step]
    val last = step == LEARN_STEPS.lastIndex
    Column(modifier.fillMaxSize().safeDrawingPadding().padding(PwSpacing.Page)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (step > 0) {
                    IconButton(onClick = { onStep(step - 1) }) {
                        Icon(PwIcons.ChevronLeft, contentDescription = "Retour", modifier = Modifier.size(20.dp))
                    }
                }
            }
            Text(
                "Étape ${step + 1} sur ${LEARN_STEPS.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(12.dp))
        StepChips(step, onStep)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = PwSpacing.Section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(96.dp).background(PwColors.SurfaceElevated, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(current.icon, contentDescription = null, tint = PwColors.Action, modifier = Modifier.size(40.dp)) }
            Spacer(Modifier.height(24.dp))
            Text(current.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                current.hint,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        PwPrimaryButton(
            text = if (last) "Connecter le collier" else "Suivant",
            onClick = { if (last) onDone() else onStep(step + 1) },
        )
    }
}

/** Pastilles App · Collier · Mesures · Alerte · Position : on peut revenir sur une étape vue, pas sauter en avant. */
@Composable
private fun StepChips(step: Int, onStep: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
        LEARN_STEPS.forEachIndexed { index, item ->
            val selected = index == step
            val done = index < step
            Surface(
                onClick = { onStep(index) },
                enabled = index <= step,
                shape = PwRadius.Pill,
                color = when {
                    selected -> PwColors.Action
                    done -> PwColors.Action.copy(alpha = 0.1f)
                    else -> PwColors.SurfaceCard
                },
                contentColor = when {
                    selected -> PwColors.ActionText
                    done -> PwColors.Action
                    else -> PwColors.TextMuted
                },
                modifier = Modifier.semantics { this.selected = selected },
            ) {
                Text(item.short, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LearnFlowPreview() {
    PawriseTheme { LearnFlowScreen(step = 1, onStep = {}, onDone = {}) }
}
