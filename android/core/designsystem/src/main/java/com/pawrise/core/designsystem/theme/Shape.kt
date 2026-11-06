package com.pawrise.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Rayons `--pw-radius-*` : control 16, card 28, pill 999. */
object PwRadius {
    val Control = RoundedCornerShape(16.dp)
    val Card = RoundedCornerShape(28.dp)
    val Pill = RoundedCornerShape(percent = 50)
}

/** Espacements `--pw-space-*`. */
object PwSpacing {
    val Page = 16.dp
    val Section = 24.dp
}

internal val PawriseShapes = Shapes(
    small = PwRadius.Control,
    medium = PwRadius.Control,
    large = PwRadius.Card,
    extraLarge = PwRadius.Card,
)
