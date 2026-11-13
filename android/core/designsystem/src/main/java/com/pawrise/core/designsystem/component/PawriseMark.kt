package com.pawrise.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.theme.PwColors

/** Logo Pawrise : 8 pétales (pawrise-figma `PawriseMark.tsx`, viewBox 64). */
@Composable
fun PawriseMark(modifier: Modifier = Modifier, size: Dp = 48.dp, color: Color = PwColors.Action) {
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 64f
        val petal = Size(13f * unit, 22f * unit)
        repeat(8) { i ->
            rotate(degrees = i * 45f, pivot = center) {
                drawOval(color, topLeft = Offset(center.x - petal.width / 2, 2f * unit), size = petal)
            }
        }
    }
}
