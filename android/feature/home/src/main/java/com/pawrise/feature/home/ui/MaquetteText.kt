package com.pawrise.feature.home.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.pawrise.core.designsystem.theme.BricolageGrotesque
import com.pawrise.core.designsystem.theme.DmMono

/** Familles de la maquette : Inter (texte), Bricolage Grotesque (titres), DM Mono 500 (données). */
enum class MaquetteFont(val family: FontFamily) { Inter(com.pawrise.core.designsystem.theme.Inter), Display(BricolageGrotesque), Mono(DmMono) }

/**
 * Style d'un calque texte Figma (ex. Inter 400 11 · 140 %) : hauteur de ligne de la maquette, texte centré dans
 * la ligne comme dans Figma, coupure « gloutonne » (chaque ligne prend autant de mots que possible).
 */
fun maquetteStyle(
    font: MaquetteFont,
    size: Float,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: Float? = null,
    color: Color = Color.Unspecified,
): TextStyle = TextStyle(
    fontFamily = font.family,
    // DM Mono : seule la graisse 500 est utilisée dans la maquette.
    fontWeight = if (font == MaquetteFont.Mono) FontWeight.Medium else weight,
    fontSize = size.sp,
    lineHeight = lineHeight?.let { (size * it).sp } ?: TextUnit.Unspecified,
    color = color,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    lineBreak = LineBreak.Simple,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)
