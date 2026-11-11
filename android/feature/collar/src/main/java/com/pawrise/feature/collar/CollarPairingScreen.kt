package com.pawrise.feature.collar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrise.core.designsystem.component.PwFieldError
import com.pawrise.core.designsystem.component.PwFlowHeader
import com.pawrise.core.designsystem.component.PwIcons
import com.pawrise.core.designsystem.component.PwPrimaryButton
import com.pawrise.core.designsystem.component.PwQuestion
import com.pawrise.core.designsystem.theme.DmMono
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.designsystem.theme.PwRadius
import com.pawrise.core.designsystem.theme.PwSpacing
import com.pawrise.feature.dogprofile.PetPhoto
import com.pawrise.feature.dogprofile.PhotoChoice

/**
 * US-A · 17-20 Associer le collier (`AddDogFlow · collarIntro / scan / activating / success`,
 * Figma 2025:2366 → 2025:2435). Le chien existe déjà : quitter ici le laisse sans collier
 * (contrat : `collarSerial: null`), associable plus tard.
 *
 * [scanner] : aperçu caméra ; remplaçable dans les tests (pas de caméra sur la JVM).
 */
@Composable
fun CollarPairingRoute(
    petId: String,
    petName: String,
    photo: PhotoChoice,
    onDone: () -> Unit,
    viewModel: CollarPairingViewModel = hiltViewModel(),
    scanner: @Composable (onQrCode: (String) -> Unit, modifier: Modifier) -> Unit = { onQr, m -> CameraQrScanner(onQr, m) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler { if (!viewModel.back()) onDone() }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        val busy = state.phase == CollarPhase.Activating
        PwFlowHeader(
            onBack = if (busy || state.phase == CollarPhase.Success) null else ({ if (!viewModel.back()) onDone() }),
            onClose = if (busy) null else onDone,
        ) {
            Text("Collier Pawrise", style = MaterialTheme.typography.labelMedium, color = PwColors.TextMuted)
        }
        Box(Modifier.weight(1f).padding(horizontal = PwSpacing.Section)) {
            when (state.phase) {
                CollarPhase.Intro -> Intro(petName, viewModel::openScanner, viewModel::openManual)
                CollarPhase.Scan -> Scan(state.error, viewModel::openManual) { modifier ->
                    scanner({ raw -> viewModel.onScanned(petId, raw) }, modifier)
                }
                CollarPhase.Manual -> Manual(state, viewModel::onManualCode) { viewModel.submitManual(petId) }
                CollarPhase.Activating -> Activating(petName)
                CollarPhase.Success -> Success(petName, photo)
            }
        }
        if (state.phase == CollarPhase.Success) {
            PwPrimaryButton(
                "Voir le tableau de bord de $petName",
                onClick = onDone,
                modifier = Modifier.padding(horizontal = PwSpacing.Section, vertical = PwSpacing.Page),
            )
        }
    }
}

@Composable
private fun Intro(petName: String, onScan: () -> Unit, onManual: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PwQuestion(
            "Connectons le collier de $petName",
            "Chaque chien Pawrise a son propre collier. Le QR code se trouve directement sur le boîtier du collier.",
        )
        ChoiceCard(PwIcons.Qr, "Scanner le QR code", "Recommandé · rapide", primary = true, onClick = onScan)
        Spacer(Modifier.height(12.dp))
        ChoiceCard(PwIcons.Pulse, "Saisir le code manuellement", "Code inscrit sous le collier", primary = false, onClick = onManual)
    }
}

@Composable
private fun ChoiceCard(icon: ImageVector, title: String, subtitle: String, primary: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (primary) PwColors.Action else PwColors.SurfaceCard,
        contentColor = if (primary) PwColors.ActionText else PwColors.TextPrimary,
        border = if (primary) null else BorderStroke(1.dp, PwColors.BorderStrong),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(if (primary) Color.Black.copy(alpha = 0.15f) else PwColors.SurfaceElevated, PwRadius.Control),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = if (primary) PwColors.ActionText else PwColors.Action, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (primary) PwColors.ActionText.copy(alpha = 0.8f) else PwColors.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun Scan(error: String?, onManual: () -> Unit, camera: @Composable (Modifier) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Scannez le QR code", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Placez le QR code du collier dans le cadre.",
            style = MaterialTheme.typography.bodyMedium,
            color = PwColors.TextMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(256.dp).clip(RoundedCornerShape(24.dp)).background(PwColors.BrandInk)
                .border(2.dp, PwColors.Action, RoundedCornerShape(24.dp))
                .semantics { contentDescription = "Aperçu de la caméra" },
        ) { camera(Modifier.fillMaxSize()) }
        error?.let { PwFieldError(it, Modifier.padding(top = 16.dp)) }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onManual) {
            Text("Saisir le code manuellement", style = MaterialTheme.typography.titleMedium, color = PwColors.Action)
        }
    }
}

@Composable
private fun Manual(state: CollarUiState, onCode: (String) -> Unit, onSubmit: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PwQuestion("Saisir le code d'activation", "Format attendu : PW- suivi de 6 lettres ou chiffres.")
        OutlinedTextField(
            value = state.manualCode,
            onValueChange = onCode,
            placeholder = { Text("PW-XXXXXX", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontFamily = DmMono, letterSpacing = 4.sp, textAlign = TextAlign.Center),
            shape = PwRadius.Control,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PwColors.SurfaceCard,
                unfocusedContainerColor = PwColors.SurfaceCard,
                unfocusedBorderColor = PwColors.BorderStrong,
                focusedBorderColor = PwColors.Action,
            ),
            isError = state.error != null,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Code du collier" },
        )
        state.error?.let { PwFieldError(it, Modifier.padding(start = 4.dp, top = 8.dp)) }
        Spacer(Modifier.height(20.dp))
        PwPrimaryButton("Activer le collier", onClick = onSubmit, enabled = state.manualValid)
    }
}

@Composable
private fun Activating(petName: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.fillMaxSize(), strokeWidth = 4.dp, trackColor = PwColors.BorderStrong)
            Icon(PwIcons.Signal, null, tint = PwColors.Action, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Activation du collier…", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Nous connectons le collier de $petName à Pawrise. Un instant.",
            style = MaterialTheme.typography.bodyMedium,
            color = PwColors.TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Success(petName: String, photo: PhotoChoice) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box {
            PetPhoto(photo, "Photo de $petName", Modifier.size(112.dp).clip(CircleShape))
            Surface(
                shape = CircleShape, color = PwColors.StatusGood, contentColor = PwColors.ActionText,
                modifier = Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(40.dp),
            ) { Box(contentAlignment = Alignment.Center) { Icon(PwIcons.Check, null, Modifier.size(22.dp)) } }
        }
        Spacer(Modifier.height(20.dp))
        Text("Le collier de $petName est activé", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "$petName est maintenant connecté à Pawrise. Sa localisation et sa santé sont suivies en temps réel.",
            style = MaterialTheme.typography.bodyMedium,
            color = PwColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}
