package com.pawrise.feature.dogprofile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrise.core.designsystem.component.PwFieldError
import com.pawrise.core.designsystem.component.PwIcons
import com.pawrise.core.designsystem.component.PwPrimaryButton
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.designsystem.theme.PwRadius
import com.pawrise.core.designsystem.theme.PwSpacing
import com.pawrise.core.pets.SamplePhoto
import com.pawrise.core.pets.Sex

/**
 * US-A · 08-16 Profil du chien (`AddDogFlow · profile`, Figma 2025:2043 → 2025:2303).
 * [onLeave] : fermer (×) ou retour depuis la première étape. [onCreated] : chien créé → collier.
 */
@Composable
fun AddDogRoute(
    onLeave: () -> Unit,
    onCreated: (CreatedDog) -> Unit,
    viewModel: AddDogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.created) { state.created?.let(onCreated) }
    BackHandler(enabled = state.step != AddDogStep.Photo) { viewModel.back() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onOwnPhotoPicked(it.toString()) }
    }
    AddDogScreen(
        state = state,
        actions = AddDogActions(
            onBack = { if (!viewModel.back()) onLeave() },
            onClose = onLeave,
            onNext = viewModel::next,
            onPickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onSample = viewModel::onSample,
            onName = viewModel::onName,
            onBirthDate = viewModel::onBirthDate,
            onSex = viewModel::onSex,
            onBreedQuery = viewModel::onBreedQuery,
            onBreed = viewModel::onBreed,
            onRetryBreeds = viewModel::retryBreeds,
            onWeight = viewModel::onWeight,
            onHeight = viewModel::onHeight,
            onMedical = viewModel::onMedical,
            onEdit = viewModel::edit,
        ),
    )
}

internal data class AddDogActions(
    val onBack: () -> Unit,
    val onClose: () -> Unit,
    val onNext: () -> Unit,
    val onPickPhoto: () -> Unit,
    val onSample: (SamplePhoto) -> Unit,
    val onName: (String) -> Unit,
    val onBirthDate: (String) -> Unit,
    val onSex: (Sex) -> Unit,
    val onBreedQuery: (String) -> Unit,
    val onBreed: (BreedChoice) -> Unit,
    val onRetryBreeds: () -> Unit,
    val onWeight: (String) -> Unit,
    val onHeight: (String) -> Unit,
    val onMedical: (String) -> Unit,
    val onEdit: (AddDogStep) -> Unit,
)

@Composable
internal fun AddDogScreen(state: AddDogUiState, actions: AddDogActions) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        FlowHeader(onBack = actions.onBack, onClose = actions.onClose) { StepProgress(state.step) }
        Box(Modifier.weight(1f).padding(horizontal = PwSpacing.Section)) {
            AddDogStepContent(state, actions)
        }
        Column(Modifier.padding(horizontal = PwSpacing.Section, vertical = PwSpacing.Page)) {
            if (!state.canContinue) {
                Text(
                    AddDogMessages.INVALID_VALUE,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            state.saveError?.let { PwFieldError(it, Modifier.padding(bottom = 8.dp)) }
            PwPrimaryButton(
                text = if (state.step == AddDogStep.Summary) "Continuer" else "Suivant",
                onClick = actions.onNext,
                enabled = state.canContinue,
                loading = state.saving,
            )
        }
    }
}

/** En-tête des parcours plein écran : retour rond, contenu central, fermer rond. */
@Composable
fun FlowHeader(onBack: () -> Unit, onClose: () -> Unit, center: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = PwSpacing.Page, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RoundIconButton(PwIcons.ChevronLeft, "Retour", onBack)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { center() }
        RoundIconButton(PwIcons.Close, "Fermer", onClose)
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = PwColors.SurfaceCard),
        modifier = Modifier.size(40.dp),
    ) { Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp)) }
}

/** Barre segmentée (9 étapes). */
@Composable
private fun StepProgress(step: AddDogStep) {
    val total = AddDogStep.entries.size
    Row(
        Modifier.fillMaxWidth().semantics {
            contentDescription = "Étape ${step.ordinal + 1} sur $total"
            progressBarRangeInfo = ProgressBarRangeInfo((step.ordinal + 1).toFloat(), 0f..total.toFloat(), total)
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { i ->
            Box(
                Modifier.weight(1f).height(6.dp).background(
                    if (i <= step.ordinal) PwColors.Action else PwColors.TextPrimary.copy(alpha = 0.15f),
                    PwRadius.Pill,
                ),
            )
        }
    }
}
