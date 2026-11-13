package com.pawrise.feature.home.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Icônes du Figma « Pawrise-App » (grille 24 × 24, trait 1,9 mis à l'échelle avec l'icône), reprises du composant
 * `Icon` du prototype (pawrise-figma/src/components/Icon.tsx), mêmes tracés que l'app iOS (`PawriseIcon.swift`).
 */
object PawriseGlyphs {
    val Map = stroke("map", "M9 4 3 6v14l6-2 6 2 6-2V4l-6 2-6-2Z", "M9 4v14M15 6v14")
    val Chat = stroke("chat", "M4 5h16v11H9l-5 4V5Z")
    val Settings = stroke(
        "settings",
        "M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 0 1 1.37.49l1.296 2.247a1.125 1.125 0 0 1-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 0 1 0 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 0 1-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 0 1-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.019-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 0 1-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 0 1-1.369-.49l-1.297-2.247a1.125 1.125 0 0 1 .26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 0 1 0-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 0 1-.26-1.43l1.297-2.247a1.125 1.125 0 0 1 1.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.086.22-.128.332-.183.582-.495.644-.869l.214-1.281Z",
        "M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z",
    )
    val Doc = stroke("doc", "M6 3h8l4 4v14H6V3Z", "M14 3v4h4M9 13h6M9 17h6")
    val Heart = stroke("heart", "M12 20s-7-4.5-9.5-9C1 8 2.5 4.5 6 4.5c2 0 3.2 1.2 4 2.5.8-1.3 2-2.5 4-2.5 3.5 0 5 3.5 3.5 6.5C19 15.5 12 20 12 20Z")
    val Pulse = stroke("pulse", "M2 12h4l2-5 4 12 2.5-9 1.5 2H22")
    val Activity = stroke("activity", "M2 12h4l3 8 4-16 3 8h6")
    val Moon = stroke("moon", "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5Z")
    val Thermometer = stroke("thermometer", "M14 14.7V5a2 2 0 1 0-4 0v9.7a4 4 0 1 0 4 0Z", "M12 9v6")
    val Battery = stroke("battery", "M4 8h12a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2Z", "M22 11v2")
    val Alert = stroke("alert", "M12 3 2 20h20L12 3Z", "M12 10v4M12 17h.01")
    val Swap = stroke("swap", "M7.5 21 3 16.5m0 0L7.5 12M3 16.5h13.5m0-13.5L21 7.5m0 0L16.5 12M21 7.5H7.5")
    val Close = stroke("close", "M6 6l12 12M18 6 6 18")
    val Check = stroke("check", "m5 12 5 5L20 6")
    val Signal = stroke("signal", "M4 18v-3M9 18v-6M14 18v-9M19 18V6")
    val Shield = stroke("shield", "M12 3 5 6v6c0 4.5 3 7.5 7 9 4-1.5 7-4.5 7-9V6l-7-3Z")
    val Send = stroke("send", "M4 12 20 4l-6 16-3-6-7-2Z")
    val Paw = stroke(
        "paw",
        circle(7.0, 9.0, 1.7), circle(12.0, 7.0, 1.7), circle(17.0, 9.0, 1.7),
        "M12 12c-2.5 0-4.5 2-4.5 4S9 21 12 21s4.5-1 4.5-3-2-4-4.5-4Z",
    )

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

    private fun circle(cx: Double, cy: Double, r: Double): String = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0Z"

    private fun stroke(name: String, vararg paths: String): ImageVector =
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
}

/** Icône décorative à la taille de la maquette (le trait suit la taille : 1,9 pour 24 dp). */
@Composable
fun PawriseGlyph(icon: ImageVector, size: Dp, tint: Color, modifier: Modifier = Modifier) {
    Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(size))
}
