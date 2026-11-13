package com.pawrise.feature.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pawrise.core.designsystem.component.PwIcons
import com.pawrise.core.designsystem.theme.BricolageGrotesque
import com.pawrise.core.designsystem.theme.Inter
import com.pawrise.core.designsystem.theme.PwColors

/** Style de texte de la maquette : police, graisse, taille et interligne Figma (en % de la taille). */
internal fun pwText(
    size: Float,
    weight: FontWeight = FontWeight.Normal,
    color: Color = PwColors.TextPrimary,
    lineHeight: Float = 1.35f,
    family: FontFamily = Inter,
    align: TextAlign = TextAlign.Unspecified,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
    color = color,
    textAlign = align,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

private val ControlShape = RoundedCornerShape(16.dp)

/**
 * Marge du haut d'un écran dont la maquette place le contenu à [designTop] du bord (zone de la barre d'état) :
 * même position sans barre d'état, et jamais sous la barre d'état sur un vrai téléphone.
 */
@Composable
internal fun topInsetAware(designTop: Dp, minGap: Dp = 0.dp): Dp {
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return maxOf(designTop, status + minGap)
}

/** Titre d'écran Bricolage 700 26 + sous-titre Inter 400 (interligne 135 %), écart 8 (maquette « Question »). */
@Composable
internal fun AuthTitle(title: String, subtitle: String, subtitleSize: Float = 14f) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = pwText(26f, FontWeight.Bold, family = BricolageGrotesque),
            modifier = Modifier.semantics { heading() },
        )
        Text(subtitle, style = pwText(subtitleSize, color = PwColors.TextMuted))
    }
}

/**
 * Champ libellé + saisie arrondie (maquette : libellé Inter 600 12, 6 d'écart, champ de 54, rayon 16).
 * Connexion : saisie Inter 15, filet #D5E3EC ; Créer un compte : Inter 17, filet #C1D4E0.
 */
@Composable
internal fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    textSize: Float = 15f,
    borderColor: Color = PwColors.Border,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = pwText(12f, FontWeight.SemiBold, PwColors.TextMuted))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = pwText(textSize, color = if (enabled) PwColors.TextPrimary else PwColors.TextMuted),
            cursorBrush = SolidColor(PwColors.Action),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            // Le libellé est affiché au-dessus du champ : on l'associe au champ pour TalkBack.
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .background(PwColors.SurfaceCard, ControlShape)
                        .border(1.dp, if (error == null) borderColor else PwColors.StatusAlert, ControlShape)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = pwText(textSize, color = PwColors.TextMuted.copy(alpha = 0.6f)))
                    }
                    inner()
                }
            },
        )
        if (error != null) FieldError(error)
    }
}

@Composable
internal fun FieldError(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = pwText(12f, color = PwColors.StatusAlert),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Bouton plein de la maquette : 56 de haut, rayon 16, Inter 700 15 blanc. [loading] : roue, clic ignoré. */
@Composable
internal fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .alpha(if (enabled || loading) 1f else 0.4f)
            .background(PwColors.Action, ControlShape)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .semantics { if (!enabled) disabled() },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = PwColors.ActionText, strokeWidth = 2.5.dp)
            // Garde le libellé dans l'arbre d'accessibilité pendant le chargement.
            Text(text, style = pwText(15f, FontWeight.Bold, Color.Transparent))
        } else {
            Text(text, style = pwText(15f, FontWeight.Bold, PwColors.ActionText, lineHeight = 1.4f))
        }
    }
}

/** Bouton blanc sans filet, texte action (« Créer un compte » sur Bienvenue). */
@Composable
internal fun AuthSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(PwColors.SurfaceCard, ControlShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = pwText(15f, FontWeight.Bold, PwColors.Action, lineHeight = 1.4f))
    }
}

/** Lien texte Inter 600 14 action, sans marge (zone tactile élargie par Compose). */
@Composable
internal fun AuthLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Text(
        text,
        style = pwText(14f, FontWeight.SemiBold, PwColors.Action, align = TextAlign.Center),
        modifier = modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    )
}

/** « ‹ Retour » en haut de Connexion (chevron 18, écart 6, Inter 600 14). */
@Composable
internal fun BackLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(PwIcons.ChevronLeft, contentDescription = null, tint = PwColors.Action, modifier = Modifier.size(18.dp))
        Text("Retour", style = pwText(14f, FontWeight.SemiBold, PwColors.Action))
    }
}

/** En-tête « chevron seul » de Créer un compte : rangée de 64 (barre d'état comprise), chevron 20 à 16 du bord. */
@Composable
internal fun ChevronHeader(onBack: () -> Unit) {
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(status))
        Box(
            Modifier.fillMaxWidth().height(maxOf(64.dp - status, 44.dp)).padding(start = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                PwIcons.ChevronLeft,
                contentDescription = "Retour",
                tint = PwColors.Action,
                modifier = Modifier.size(20.dp).clickable(role = Role.Button, onClick = onBack),
            )
        }
    }
}

/** Bandeau d'erreur sous un formulaire (fond alerte clair, icône, Inter 14). */
@Composable
internal fun ErrorBanner(message: String) {
    Banner(message, AlertCircle, PwColors.StatusAlert, PwColors.StatusAlertSurface)
}

/** Bandeau d'information (code envoyé). */
@Composable
internal fun InfoBanner(message: String) {
    Banner(message, Envelope, PwColors.StatusGood, PwColors.StatusGoodSurface)
}

@Composable
private fun Banner(message: String, icon: ImageVector, tint: Color, surface: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(surface, ControlShape)
            .padding(14.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(message, style = pwText(14f, color = tint))
    }
}

/**
 * Case des CGU (RG11) : obligatoire, enregistrée côté serveur avec la version acceptée.
 * Case 22 rayon 6, filet #C1D4E0, texte Inter 13,5 atténué, toute la ligne cochable (44 minimum).
 */
@Composable
internal fun TermsCheckbox(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean, error: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChange),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            val box = RoundedCornerShape(6.dp)
            Box(
                Modifier
                    .size(22.dp)
                    .background(if (checked) PwColors.Action else PwColors.SurfaceCard, box)
                    .border(
                        1.dp,
                        when {
                            checked -> PwColors.Action
                            error != null -> PwColors.StatusAlert
                            else -> PwColors.BorderStrong
                        },
                        box,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Canvas(Modifier.size(12.dp)) {
                        val w = size.width
                        drawLine(PwColors.ActionText, Offset(w * 0.12f, w * 0.52f), Offset(w * 0.4f, w * 0.8f), 2.dp.toPx(), StrokeCap.Round)
                        drawLine(PwColors.ActionText, Offset(w * 0.4f, w * 0.8f), Offset(w * 0.9f, w * 0.2f), 2.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
            Text(
                "J’accepte les conditions d’utilisation et la politique de confidentialité de Pawrise.",
                style = pwText(13.5f, color = PwColors.TextMuted),
                modifier = Modifier.weight(1f),
            )
        }
        if (error != null) FieldError(error)
    }
}

@Composable
internal fun Gap(height: Dp) = Spacer(Modifier.height(height))

@Composable
internal fun HGap(width: Dp) = Spacer(Modifier.width(width))

private fun strokeIcon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach {
            addPath(
                pathData = addPathNodes(it),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

private val AlertCircle = strokeIcon("AlertCircle", "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z", "M12 8v5M12 16h.01")
private val Envelope = strokeIcon("Envelope", "M4 6h16v12H4V6Z", "m4 7 8 6 8-6")
