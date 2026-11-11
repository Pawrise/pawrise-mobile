package com.pawrise.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Icônes de la maquette (tracés SVG 24×24 de pawrise-figma `components/Icon.tsx`).
 * Trait couleur courante : la teinte vient de `Icon(tint = …)`.
 */
object PwIcons {
    val ChevronLeft = stroke("ChevronLeft", "m15 6-6 6 6 6")
    val ChevronRight = stroke("ChevronRight", "m9 6 6 6-6 6")
    val Close = stroke("Close", "M6 6l12 12M18 6 6 18")
    val Check = stroke("Check", "m5 12 5 5L20 6")
    val Plus = stroke("Plus", "M12 5v14M5 12h14")
    val Heart = stroke("Heart", "M12 20s-7-4.5-9.5-9C1 8 2.5 4.5 6 4.5c2 0 3.2 1.2 4 2.5.8-1.3 2-2.5 4-2.5 3.5 0 5 3.5 3.5 6.5C19 15.5 12 20 12 20Z")
    val Signal = stroke("Signal", "M4 18v-3M9 18v-6M14 18v-9M19 18V6")
    val Alert = stroke("Alert", "M12 3 2 20h20L12 3Z", "M12 10v4M12 17h.01")
    val Map = stroke("Map", "M9 4 3 6v14l6-2 6 2 6-2V4l-6 2-6-2Z", "M9 4v14M15 6v14")
    val Pulse = stroke("Pulse", "M2 12h4l2-5 4 12 2.5-9 1.5 2H22")
    val Female = stroke("Female", "M12 15a5 5 0 1 0 0-10 5 5 0 0 0 0 10Z", "M12 15v6M9 18h6")
    val Male = stroke("Male", "M10 20a6 6 0 1 0 0-12 6 6 0 0 0 0 12Z", "M14.5 9.5 20 4M15 4h5v5")
    val Search = stroke("Search", "M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14Z", "m20 20-4-4")

    /** Patte pleine (icône « paw » de la maquette). */
    val Paw: ImageVector = ImageVector.Builder("Paw", 24.dp, 24.dp, 24f, 24f).apply {
        listOf(
            "M7 7.3a1.7 1.7 0 1 1 0 3.4 1.7 1.7 0 0 1 0-3.4Z",
            "M12 5.3a1.7 1.7 0 1 1 0 3.4 1.7 1.7 0 0 1 0-3.4Z",
            "M17 7.3a1.7 1.7 0 1 1 0 3.4 1.7 1.7 0 0 1 0-3.4Z",
            "M12 12c-2.5 0-4.5 2-4.5 4S9 21 12 21s4.5-1 4.5-3-2-4-4.5-4Z",
        ).forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
    }.build()

    /** QR code stylisé (bouton « Scanner le QR code »). */
    val Qr: ImageVector = ImageVector.Builder("Qr", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = addPathNodes(
                "M3 3h7v7H3V3Zm2 2v3h3V5H5Zm9-2h7v7h-7V3Zm2 2v3h3V5h-3ZM3 14h7v7H3v-7Zm2 2v3h3v-3H5Zm9 0h2v2h-2v-2Zm2 2h2v-2h-2v-4h-2v2h-2v2h2v4h2v-2Zm0 0h2v2h-2v-2Zm2 0h2v2h-2v-2Zm0-4h2v2h-2v-2Z",
            ),
            fill = SolidColor(Color.Black),
        )
    }.build()

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
