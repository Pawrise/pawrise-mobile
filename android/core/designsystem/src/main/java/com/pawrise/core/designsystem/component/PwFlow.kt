package com.pawrise.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.designsystem.theme.PwText

/**
 * En-tête des parcours plein écran (maquette « En-tête », 64 dp) : rangée calée à gauche — marge 16,
 * chevron 20, écart 12, zone centrale de 282 dp (progression ou titre centré), écart 12, croix 20.
 * Les icônes ont une zone tactile de 44 dp qui ne décale pas la mise en page.
 * [onBack] / [onClose] nuls : l'emplacement reste réservé, sans bouton.
 */
@Composable
fun PwFlowHeader(
    onBack: (() -> Unit)?,
    onClose: (() -> Unit)?,
    center: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PwFlowIconButton(PwIcons.ChevronLeft, "Retour", onBack, tint = PwColors.Action)
        Box(Modifier.width(282.dp), contentAlignment = Alignment.Center) { center() }
        PwFlowIconButton(PwIcons.Close, "Fermer", onClose, tint = PwColors.TextMuted)
    }
}

/**
 * Icône de [iconSize] dp (20 dans les en-têtes) au centre d'une zone tactile de 44 dp.
 * [onClick] nul : emplacement vide de même taille.
 */
@Composable
fun PwFlowIconButton(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)?,
    tint: Color = PwColors.Action,
    iconSize: Dp = 20.dp,
) {
    val base = Modifier.size(44.dp)
    if (onClick == null) return Spacer(base)
    Box(
        base.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Button,
            onClickLabel = label,
            onClick = onClick,
        ).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize)) }
}

/** Barre segmentée des parcours : [total] segments de 26 × 6 dp, rayon 3, écart 5 ; [current] à partir de 1. */
@Composable
fun PwStepProgress(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics {
            contentDescription = "Étape $current sur $total"
            progressBarRangeInfo = ProgressBarRangeInfo(current.toFloat(), 0f..total.toFloat(), total)
        },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(total) { i ->
            Box(
                Modifier.size(26.dp, 6.dp)
                    .background(if (i < current) PwColors.Action else PwColors.Border, RoundedCornerShape(3.dp)),
            )
        }
    }
}

/** Titre centré de l'en-tête (« Collier Pawrise ») : Inter 600 13, #5A7082. */
@Composable
fun PwFlowTitle(text: String) {
    Text(
        text,
        style = PwText.inter(13f, FontWeight.SemiBold),
        color = PwColors.TextMuted,
        modifier = Modifier.semantics { heading() },
    )
}

/**
 * Titre d'étape + aide facultative (maquette « Question ») : Bricolage 700 26 et Inter 400 13,5,
 * interligne 135 %, écart 8. Sans marge extérieure : l'écran gère les écarts.
 */
@Composable
fun PwQuestion(title: String, hint: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = PwText.Question, color = PwColors.TextPrimary, modifier = Modifier.semantics { heading() })
        if (hint != null) Text(hint, style = PwText.Hint, color = PwColors.TextMuted)
    }
}

/** Cadre de saisie des parcours (maquette « Champ ») : 56 dp, rayon 16, bordure 1 dp, fond blanc. */
fun Modifier.pwFieldBox(invalid: Boolean = false, selected: Boolean = false, minHeight: Dp = 56.dp): Modifier =
    heightIn(min = minHeight)
        .background(if (selected) PwColors.SurfaceElevated else PwColors.SurfaceCard, RoundedCornerShape(16.dp))
        .border(
            1.dp,
            when {
                invalid -> PwColors.StatusAlert
                selected -> PwColors.Action
                else -> PwColors.BorderStrong
            },
            RoundedCornerShape(16.dp),
        )

/**
 * Champ de saisie des parcours : Inter 400 17, marges 16, texte d'exemple #5A7082.
 * [trailing] : contenu à droite du texte (unité kg / cm…), le texte occupant alors [fieldWidth].
 * Multiligne : [singleLine] = false et [minHeight].
 */
@Composable
fun PwFlowTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    invalid: Boolean = false,
    singleLine: Boolean = true,
    minHeight: Dp = 56.dp,
    textStyle: TextStyle = PwText.FlowField,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    fieldWidth: Dp? = null,
) {
    val style = textStyle.copy(color = PwColors.TextPrimary)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = style,
        cursorBrush = SolidColor(PwColors.Action),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = label },
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().pwFieldBox(invalid = invalid, minHeight = minHeight)
                    .padding(horizontal = 16.dp, vertical = if (singleLine) 0.dp else 14.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                Box(if (fieldWidth != null) Modifier.width(fieldWidth) else Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = style.copy(color = PwColors.TextMuted))
                    }
                    inner()
                }
                trailing?.invoke()
            }
        },
    )
}

/**
 * Zone d'action en bas des parcours (maquette « Action principale ») : 8 dp au-dessus, 20 sur les côtés,
 * 16 en bas ; hauteur réservée même vide.
 */
@Composable
fun PwFlowFooter(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
        content = content,
    )
}
