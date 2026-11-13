package com.pawrise.feature.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pawrise.core.designsystem.component.PwIcons
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

internal val CardShape = RoundedCornerShape(24.dp)
internal val ButtonShape = RoundedCornerShape(16.dp)

/** Titre de section en capitales (Inter 600 12, atténué, référence I02). */
@Composable
internal fun SettingsSectionTitle(title: String) {
    Text(
        title.uppercase(),
        style = pwText(12f, FontWeight.SemiBold, PwColors.TextMuted, lineHeight = 1.4f),
        modifier = Modifier.padding(start = 1.dp, bottom = 19.5.dp).semantics { heading() },
    )
}

/** Carte blanche de rayon 24 avec filet #D5E3EC, lignes empilées. */
@Composable
internal fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(PwColors.SurfaceCard, CardShape)
            .border(1.dp, PwColors.Border, CardShape),
        content = content,
    )
}

@Composable
internal fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 64.dp).height(1.dp).background(PwColors.Border))
}

/** Icône de ligne (18, trait 1,9, couleur atténuée), sans pastille comme dans la maquette. */
@Composable
internal fun SettingsIcon(icon: ImageVector) {
    Box(Modifier.size(width = 16.dp, height = 20.dp), contentAlignment = Alignment.Center) {
        Icon(
            icon,
            contentDescription = null,
            tint = PwColors.TextMuted,
            // 18 de large dans une colonne de 16 (le tracé déborde de 1 de chaque côté), mesuré sur I02.
            modifier = Modifier.requiredSize(18.dp),
        )
    }
}

/**
 * Interrupteur de la maquette : rail 44 × 24 (#0A5C78 activé, #C1D4E0 désactivé), pastille blanche de 20.
 * Toute la ligne est l'interrupteur pour TalkBack (rôle Switch, état coché).
 */
@Composable
internal fun SettingsSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    label: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(start = 14.dp, end = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        label()
        PawriseSwitch(checked)
    }
}

@Composable
private fun PawriseSwitch(checked: Boolean) {
    val thumb by animateDpAsState(if (checked) 22.dp else 2.dp, tween(200), label = "thumb")
    Box(
        Modifier
            .size(width = 44.dp, height = 24.dp)
            .background(if (checked) PwColors.Action else PwColors.BorderStrong, CircleShape),
    ) {
        Box(
            Modifier
                .offset(x = thumb, y = 2.dp)
                .size(20.dp)
                .background(Color.White, CircleShape),
        )
    }
}

/** Sélecteur segmenté Normal / Anomalie (rail gris, segment choisi blanc). */
@Composable
internal fun <T> SegmentedPicker(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(SegmentTrack, CircleShape)
            .padding(2.dp)
            .selectableGroup(),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(28.dp)
                    .then(
                        if (isSelected) {
                            Modifier.shadow(2.dp, CircleShape, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
                                .background(Color.White, CircleShape)
                        } else {
                            Modifier
                        },
                    )
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = pwText(14f, if (isSelected) FontWeight.SemiBold else FontWeight.Medium, Color(0xFF111111), lineHeight = 1.2f),
                )
            }
        }
    }
}

private val SegmentTrack = Color(0xFFEEEEF0)

/** Bouton large blanc à filet (« Se déconnecter », « Supprimer mon compte »), 48 de haut, rayon 16. */
@Composable
internal fun SettingsActionButton(
    text: String,
    tint: Color,
    border: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .background(PwColors.SurfaceCard, ButtonShape)
            .border(1.dp, border, ButtonShape)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = pwText(14f, FontWeight.SemiBold, tint))
        if (loading) CircularProgressIndicator(Modifier.size(16.dp), color = tint, strokeWidth = 2.dp)
    }
}

/** Icônes Lucide des Paramètres (via [PwIcons], même source que l'app iOS). */
internal object SettingsIcons {
    val Map = PwIcons.Map
    val Heart = PwIcons.Heart
    val Lock = PwIcons.Lock
    val Send = PwIcons.Send
    val ChevronLeft = PwIcons.ChevronLeft
    val Alert = PwIcons.Alert
    val User = PwIcons.User
    val Devices = PwIcons.Smartphone
    val Shield = PwIcons.Shield
    val Trash = PwIcons.Trash
}
