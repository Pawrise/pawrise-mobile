package com.pawrise.feature.home.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.component.PwIcons

/**
 * Icônes des écrans Santé et Chat IA : Lucide via [PwIcons] (même source que l'app iOS), plus la fleur du logo.
 */
object PawriseGlyphs {
    val Map = PwIcons.Map
    val Chat = PwIcons.Chat
    val Settings = PwIcons.Settings
    val Doc = PwIcons.Doc
    val Heart = PwIcons.Heart
    val Pulse = PwIcons.Pulse
    val Activity = PwIcons.Activity
    val Moon = PwIcons.Moon
    val Thermometer = PwIcons.Thermometer
    val Battery = PwIcons.Battery
    val Alert = PwIcons.Alert
    val Swap = PwIcons.Swap
    val Close = PwIcons.Close
    val Check = PwIcons.Check
    val Signal = PwIcons.Signal
    val Shield = PwIcons.Shield
    val Send = PwIcons.Send
    val Paw = PwIcons.Paw

    /** Fleur du logo (onglet Pawrise) : huit pétales elliptiques (viewBox 64, cx 32, cy 13, rx 6,5, ry 11). */
    val Brand: ImageVector = ImageVector.Builder("brand", 24.dp, 24.dp, 64f, 64f).apply {
        repeat(8) { index ->
            group(rotate = index * 45f, pivotX = 32f, pivotY = 32f) {
                addPath(
                    pathData = addPathNodes("M25.5 13a6.5 11 0 1 0 13 0a6.5 11 0 1 0 -13 0Z"),
                    fill = SolidColor(Color.Black),
                )
            }
        }
    }.build()
}

/** Icône décorative à la taille de la maquette (le trait suit la taille : 1,9 pour 24 dp). */
@Composable
fun PawriseGlyph(icon: ImageVector, size: Dp, tint: Color, modifier: Modifier = Modifier) {
    Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(size))
}
