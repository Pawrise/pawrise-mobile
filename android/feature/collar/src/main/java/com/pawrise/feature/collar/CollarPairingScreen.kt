package com.pawrise.feature.collar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrise.core.designsystem.component.PwFieldError
import com.pawrise.core.designsystem.component.PwFlowFooter
import com.pawrise.core.designsystem.component.PwFlowHeader
import com.pawrise.core.designsystem.component.PwFlowTextField
import com.pawrise.core.designsystem.component.PwFlowTitle
import com.pawrise.core.designsystem.component.PwIcons
import com.pawrise.core.designsystem.component.PwPrimaryButton
import com.pawrise.core.designsystem.component.PwQuestion
import com.pawrise.core.designsystem.component.PwSecondaryButton
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.designsystem.theme.PwText
import com.pawrise.feature.dogprofile.PetPhoto
import com.pawrise.feature.dogprofile.PhotoChoice

/**
 * US-A · 17-20 Associer le collier (`AddDogFlow · collarIntro / scan / activating / success`,
 * Figma 2025:2366 → 2025:2435). Le chien existe déjà : quitter ici le laisse sans collier
 * (contrat : `collarSerial: null`), associable plus tard.
 *
 * Retour (comme iOS) : scan / saisie → choix ; A19 annule l'activation ; A20 revient au choix.
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
    val back = { if (!viewModel.back()) onDone() }
    BackHandler(onBack = back)

    // Maquette A17–A20 : en-tête 64 dp (« Collier Pawrise »), contenu, zone d'action de 80 dp.
    Column(Modifier.fillMaxSize().background(PwColors.SurfacePage).safeDrawingPadding().imePadding()) {
        PwFlowHeader(onBack = back, onClose = { viewModel.cancel(); onDone() }) { PwFlowTitle("Collier Pawrise") }
        Box(Modifier.weight(1f)) {
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
        PwFlowFooter {
            when (state.phase) {
                CollarPhase.Manual -> PwPrimaryButton(
                    "Activer le collier",
                    onClick = { viewModel.submitManual(petId) },
                    enabled = state.manualValid,
                )
                CollarPhase.Success -> PwPrimaryButton("Voir le tableau de bord de $petName", onClick = onDone)
                // Zone vide mais réservée, comme le cadre « Action principale » de la maquette.
                else -> Unit
            }
        }
    }
}

/** Colonne des écrans à question (A17, saisie) : 16 dp sous l'en-tête, marges 20, écarts 24. */
@Composable
private fun QuestionColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        content = content,
    )
}

/** Bloc centré des écrans 18 à 20 : centré dans la zone qui commence 16 dp sous l'en-tête, écarts 24. */
@Composable
private fun CenteredColumn(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val height = maxHeight
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = height)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            content = content,
        )
    }
}

/** Maquette A17 : question, bouton principal, mention, bouton secondaire, mention, écarts 24. */
@Composable
private fun Intro(petName: String, onScan: () -> Unit, onManual: () -> Unit) {
    QuestionColumn {
        PwQuestion(
            "Connectons le collier de $petName",
            "Chaque chien Pawrise a son propre collier. Le QR code se trouve directement sur le boîtier du collier.",
        )
        PwPrimaryButton("Scanner le QR code", onClick = onScan)
        Caption("Recommandé · rapide")
        PwSecondaryButton("Saisir le code manuellement", onClick = onManual, border = null)
        Caption("Code inscrit sous le collier")
    }
}

@Composable
private fun Caption(text: String) {
    Text(text, style = PwText.inter(12.5f), color = PwColors.TextMuted)
}

/**
 * Maquette A18 : titre Bricolage 700 22, consigne Inter 13,5, cadre 256 rayon 24 (fond #E5F1F7, bordure
 * #0A5C78 1 dp, QR 120 tant que la caméra n'affiche rien), puis lien de saisie manuelle ; écarts 24.
 */
@Composable
private fun Scan(error: String?, onManual: () -> Unit, camera: @Composable (Modifier) -> Unit) {
    CenteredColumn {
        Text(
            "Scannez le QR code",
            style = PwText.display(22f),
            color = PwColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Placez le QR code du collier dans le cadre.",
            style = PwText.Hint,
            color = PwColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        val shape = RoundedCornerShape(24.dp)
        Box(
            Modifier.size(256.dp).clip(shape).background(PwColors.SurfaceElevated)
                .border(1.dp, PwColors.Action, shape)
                .semantics { contentDescription = "Caméra : visez le QR code du collier" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(PwIcons.QrCode, contentDescription = null, tint = PwColors.TextPrimary, modifier = Modifier.size(120.dp))
            camera(Modifier.fillMaxSize())
        }
        error?.let { ErrorBanner(it) }
        // Même hauteur que le bouton « QR code détecté » de la maquette (56) : le bloc reste centré au même endroit.
        Box(Modifier.heightIn(min = 56.dp), contentAlignment = Alignment.Center) {
            TextButton(onClick = onManual) {
                Text("Saisir le code manuellement", style = PwText.inter(14f, FontWeight.SemiBold), color = PwColors.Action)
            }
        }
    }
}

/** Saisie manuelle (comme iOS) : question, champ libellé « Code du collier » en DM Mono 20, erreurs. */
@Composable
private fun Manual(state: CollarUiState, onCode: (String) -> Unit, onSubmit: () -> Unit) {
    QuestionColumn {
        PwQuestion(
            "Saisir le code d’activation",
            "Format attendu : PW- suivi de 6 lettres ou chiffres. Le code est inscrit sous le collier.",
        )
        val formatError = state.manualCode.length >= 9 && !state.manualValid
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Code du collier", style = PwText.inter(12f, FontWeight.SemiBold), color = PwColors.TextMuted)
            PwFlowTextField(
                value = state.manualCode,
                onValueChange = onCode,
                label = "Code du collier",
                placeholder = "PW-ABC123",
                invalid = formatError,
                minHeight = 54.dp,
                textStyle = PwText.mono(20f),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
            )
            if (formatError) PwFieldError("Le code commence par PW- suivi de 6 lettres ou chiffres.")
        }
        state.error?.let { ErrorBanner(it) }
    }
}

/** Bandeau d'erreur (comme iOS) : icône d'alerte, Inter 14 #B4233A sur #FDECEE, rayon 16. */
@Composable
private fun ErrorBanner(message: String) {
    Row(
        Modifier.fillMaxWidth().background(PwColors.StatusAlertSurface, RoundedCornerShape(16.dp)).padding(14.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PwIcons.Alert, contentDescription = null, tint = PwColors.StatusAlert, modifier = Modifier.size(18.dp))
        Text(message, style = PwText.inter(14f), color = PwColors.StatusAlert)
    }
}

/**
 * Maquette A19 : anneau 80 dp (trait 4 #C1D4E0) avec l'icône signal 28, titre Bricolage 700 20, texte Inter 13,5 ;
 * écarts 24. Un arc #0A5C78 tourne sur l'anneau tant que l'activation dure (comme iOS).
 */
@Composable
private fun Activating(petName: String) {
    val rotation by rememberInfiniteTransition(label = "activation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "arc",
    )
    CenteredColumn {
        Box(Modifier.size(80.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = 4.dp.toPx()
                val inset = w / 2
                val arcSize = Size(size.width - w, size.height - w)
                drawCircle(PwColors.BorderStrong, radius = size.minDimension / 2 - inset, style = Stroke(w))
                drawArc(
                    PwColors.Action, startAngle = rotation - 90f, sweepAngle = 90f, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize, style = Stroke(w, cap = StrokeCap.Round),
                )
            }
            Icon(PwIcons.Signal, contentDescription = null, tint = PwColors.Action, modifier = Modifier.size(28.dp))
        }
        Text(
            "Activation du collier…",
            style = PwText.display(20f),
            color = PwColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Nous connectons le collier de $petName à Pawrise. Un instant.",
            style = PwText.Hint,
            color = PwColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Maquette A20 : photo ronde 112, coche 28 (#0E7C74), titre Bricolage 700 24, texte Inter 14 sur 256 dp ; écarts 24. */
@Composable
private fun Success(petName: String, photo: PhotoChoice) {
    CenteredColumn {
        PetPhoto(photo, "Photo de $petName", Modifier.size(112.dp).clip(CircleShape))
        Icon(PwIcons.Check, contentDescription = null, tint = PwColors.StatusGood, modifier = Modifier.size(28.dp))
        Text(
            "Le collier de $petName est activé",
            style = PwText.display(24f),
            color = PwColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        Text(
            "$petName est maintenant connecté à Pawrise. Sa localisation et sa santé sont suivies en temps réel.",
            style = PwText.inter(14f),
            color = PwColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 256.dp),
        )
    }
}
