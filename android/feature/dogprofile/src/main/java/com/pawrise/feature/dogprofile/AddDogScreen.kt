package com.pawrise.feature.dogprofile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrise.core.designsystem.component.PwFieldError
import com.pawrise.core.designsystem.component.PwFlowFooter
import com.pawrise.core.designsystem.component.PwFlowHeader
import com.pawrise.core.designsystem.component.PwPrimaryButton
import com.pawrise.core.designsystem.component.PwStepProgress
import com.pawrise.core.designsystem.theme.PwColors
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

/**
 * Maquette A08–A16 : en-tête 64 dp (progression 9 × 26 dp), contenu à 16 dp sous l'en-tête et 20 dp des bords,
 * zone d'action de 80 dp (bouton à 16 dp du bas).
 */
@Composable
internal fun AddDogScreen(state: AddDogUiState, actions: AddDogActions) {
    Column(Modifier.fillMaxSize().background(PwColors.SurfacePage).safeDrawingPadding().imePadding()) {
        PwFlowHeader(onBack = actions.onBack, onClose = actions.onClose) {
            PwStepProgress(total = AddDogStep.entries.size, current = state.step.ordinal + 1)
        }
        Box(Modifier.weight(1f)) { AddDogStepContent(state, actions) }
        PwFlowFooter {
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
