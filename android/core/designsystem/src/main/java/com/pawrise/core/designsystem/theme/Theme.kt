package com.pawrise.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Mode clair uniquement pour l'instant (EXPERIENCE.md : « mode clair par défaut »).
private val PawriseColorScheme = lightColorScheme(
    primary = PwColors.Action,
    onPrimary = PwColors.ActionText,
    primaryContainer = PwColors.SurfaceElevated,
    onPrimaryContainer = PwColors.TextPrimary,
    secondary = PwColors.BrandLime,
    onSecondary = PwColors.ActionText,
    tertiary = PwColors.StatusGood,
    background = PwColors.SurfacePage,
    onBackground = PwColors.TextPrimary,
    surface = PwColors.SurfacePage,
    onSurface = PwColors.TextPrimary,
    surfaceVariant = PwColors.SurfaceSecondary,
    onSurfaceVariant = PwColors.TextMuted,
    surfaceContainerLowest = PwColors.SurfaceCard,
    surfaceContainerLow = PwColors.SurfaceMuted,
    surfaceContainer = PwColors.SurfaceSecondary,
    surfaceContainerHigh = PwColors.SurfaceElevated,
    outline = PwColors.BorderStrong,
    outlineVariant = PwColors.Border,
    error = PwColors.StatusAlert,
    onError = PwColors.ActionText,
    errorContainer = PwColors.StatusAlertSurface,
    onErrorContainer = PwColors.StatusAlert,
)

@Composable
fun PawriseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PawriseColorScheme,
        typography = PawriseTypography,
        shapes = PawriseShapes,
        content = content,
    )
}
