package com.pawrise.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Jetons de couleur Pawrise. Source : variables Figma `--pw-color-*`
 * (fichier Pawrise-App) et `pawrise-figma/src/theme/tokens.json`.
 * Règle : pas de nouveau hex dans les écrans, on passe par ces jetons.
 */
object PwColors {
    val BrandInk = Color(0xFF123044)
    val BrandLime = Color(0xFF14A3A8)

    val SurfacePage = Color(0xFFF4F8FB)
    val SurfaceCard = Color(0xFFFFFFFF)
    val SurfaceElevated = Color(0xFFE5F1F7)
    val SurfaceSecondary = Color(0xFFEEF5F8)
    val SurfaceMuted = Color(0xFFF3F7FA)

    val TextPrimary = Color(0xFF123044)
    val TextMuted = Color(0xFF5A7082)

    val Action = Color(0xFF0A5C78)
    val ActionText = Color(0xFFFFFFFF)

    val StatusGood = Color(0xFF0E7C74)
    val StatusGoodSurface = Color(0xFFE4F6F4)
    val StatusWatch = Color(0xFF8A5A00)
    val StatusWatchSurface = Color(0xFFFFF4D8)
    val StatusAlert = Color(0xFFB4233A)
    val StatusAlertSurface = Color(0xFFFDECEE)

    val Border = Color(0xFFD5E3EC)
    val BorderStrong = Color(0xFFC1D4E0)
    val AvatarBackdrop = Color(0xFFE4F0F6)
}
