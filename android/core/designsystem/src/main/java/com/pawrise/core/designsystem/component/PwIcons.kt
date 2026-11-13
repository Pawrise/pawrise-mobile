package com.pawrise.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Icônes de la maquette, dessinées avec Lucide (`LucidePaths`, généré par scripts/lucide_icons.py) : les tracés
 * exportés de Figma sont déformés (traits vectorisés). Même source que l'app iOS. Teinte : `Icon(tint = …)`.
 */
object PwIcons {
    val ChevronLeft = lucide("ChevronLeft", LucidePaths.ChevronLeft)
    val ChevronRight = lucide("ChevronRight", LucidePaths.ChevronRight)
    val Close = lucide("Close", LucidePaths.Close)
    val Check = lucide("Check", LucidePaths.Check)
    val Plus = lucide("Plus", LucidePaths.Plus)
    val Heart = lucide("Heart", LucidePaths.Heart)
    val Signal = lucide("Signal", LucidePaths.Signal)
    val Alert = lucide("Alert", LucidePaths.Alert)
    val AlertCircle = lucide("AlertCircle", LucidePaths.AlertCircle)
    val Map = lucide("Map", LucidePaths.Map)
    val Pulse = lucide("Pulse", LucidePaths.Pulse)
    val Activity = lucide("Activity", LucidePaths.Activity)
    val Moon = lucide("Moon", LucidePaths.Moon)
    val Thermometer = lucide("Thermometer", LucidePaths.Thermometer)
    val Battery = lucide("Battery", LucidePaths.Battery)
    val Swap = lucide("Swap", LucidePaths.Swap)
    val Chat = lucide("Chat", LucidePaths.Chat)
    val Settings = lucide("Settings", LucidePaths.Settings)
    val Doc = lucide("Doc", LucidePaths.Doc)
    val Shield = lucide("Shield", LucidePaths.Shield)
    val Send = lucide("Send", LucidePaths.Send)
    val User = lucide("User", LucidePaths.User)
    val Lock = lucide("Lock", LucidePaths.Lock)
    val Trash = lucide("Trash", LucidePaths.Trash)
    val Logout = lucide("Logout", LucidePaths.Logout)
    val Search = lucide("Search", LucidePaths.Search)
    val Mail = lucide("Mail", LucidePaths.Mail)
    val Smartphone = lucide("Smartphone", LucidePaths.Smartphone)

    /** Œil du champ mot de passe : afficher (« eye ») / masquer (« eye-off »). */
    val Eye = lucide("Eye", LucidePaths.Eye)
    val EyeOff = lucide("EyeOff", LucidePaths.EyeOff)

    /** Patte (Lucide « paw-print ») : Découverte, avatar sans photo, Chat IA. */
    val Paw = lucide("Paw", LucidePaths.Paw)

    /** Chevron « Retour » des écrans Découverte (trait 2 pour 20 dp, maquette A04–A07). */
    val ChevronLeftBold = lucide("ChevronLeftBold", LucidePaths.ChevronLeft, width = 2.4f)

    /** QR code de la maquette (cadre de scan A18, 120 × 120). */
    val QrCode: ImageVector = ImageVector.Builder("QrCode", 120.dp, 120.dp, 120f, 120f).apply {
        addPath(
            pathData = addPathNodes(
                "M15 15h35v35H15zM25 25v15h15V25zM70 15h35v35H70zM80 25v15h15V25zM15 70h35v35H15zM25 80v15h15V80z" +
                    "M70 80h10v10H70zM80 90h10V80H80V60H70v10H60v10h10v20h10V90zM80 90h10v10H80zM90 90h10v10H90zM90 70h10v10H90z",
            ),
            fill = SolidColor(Color.Black),
        )
    }.build()

    /** Icône au trait sur la grille 24 de Lucide (trait 1,9 de la maquette), teinte donnée par `Icon(tint = …)`. */
    fun lucide(name: String, paths: List<String>, width: Float = 1.9f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
