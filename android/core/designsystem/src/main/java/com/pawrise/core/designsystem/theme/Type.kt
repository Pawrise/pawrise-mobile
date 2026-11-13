package com.pawrise.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.pawrise.core.designsystem.R
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * Polices du jeton `font` : display = Bricolage Grotesque, body = Inter, data = DM Mono.
 * Instances statiques (une seule taille optique) : gardées pour la compatibilité ; préférer
 * [bricolageGrotesque] ou [PwText.display], qui suivent la taille optique de la maquette.
 */
val BricolageGrotesque = FontFamily(
    Font(R.font.bricolage_grotesque_500, FontWeight.Medium),
    Font(R.font.bricolage_grotesque_600, FontWeight.SemiBold),
    Font(R.font.bricolage_grotesque_700, FontWeight.Bold),
    Font(R.font.bricolage_grotesque_800, FontWeight.ExtraBold),
)

val Inter = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
    Font(R.font.inter_700, FontWeight.Bold),
)

val DmMono = FontFamily(
    Font(R.font.dm_mono_400, FontWeight.Normal),
    Font(R.font.dm_mono_500, FontWeight.Medium),
)

/**
 * ♀ et ♂ : Inter ne les contient pas ; Figma les dessine avec Noto Sans Symbols
 * (sous-ensemble embarqué, licence OFL).
 */
val PawriseSymbols = FontFamily(Font(R.font.pawrise_symbols, FontWeight.Normal))

private val bricolageCache = ConcurrentHashMap<Pair<Float, Int>, FontFamily>()

/**
 * Bricolage Grotesque variable (opsz 12–96, wght 200–800) réglée comme Figma : taille optique = taille du
 * texte. Les titres sont ainsi plus étroits et plus fins qu'avec une instance statique « 14 pt ».
 */
fun bricolageGrotesque(size: Float, weight: FontWeight = FontWeight.Bold): FontFamily =
    bricolageCache.getOrPut(size to weight.weight) {
        FontFamily(
            Font(
                R.font.bricolage_grotesque_variable,
                weight,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(weight.weight.coerceIn(200, 800)),
                    FontVariation.Setting("opsz", size.coerceIn(12f, 96f)),
                    FontVariation.Setting("wdth", 100f),
                ),
            ),
        )
    }

/** Interligne Figma : demi-interligne réparti au-dessus et au-dessous de la ligne, sans rognage. */
val FigmaLineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

/** Interligne par défaut de la maquette (« lh=135 % »). */
const val FIGMA_LINE_HEIGHT = 1.35f

/** Styles exacts de la maquette : taille, graisse et interligne Figma (135 % par défaut). */
object PwText {
    /** Bricolage Grotesque (titres d'écran 26, 24, 22, 20…), taille optique = [size]. */
    fun display(size: Float, weight: FontWeight = FontWeight.Bold, lineHeight: Float = FIGMA_LINE_HEIGHT): TextStyle =
        style(bricolageGrotesque(size, weight), size, weight, lineHeight)

    /** Inter (textes, libellés, boutons). */
    fun inter(size: Float, weight: FontWeight = FontWeight.Normal, lineHeight: Float = FIGMA_LINE_HEIGHT): TextStyle =
        // Figma tolère un dépassement de largeur inférieur au point (ligne de 350,3 pt dans une boîte de 350,
        // A17) : un très léger resserrement (0,01 sp par glyphe) donne les mêmes coupures de ligne.
        style(Inter, size, weight, lineHeight).copy(letterSpacing = (-0.01).sp)

    /** DM Mono (valeurs : 87 bpm, PW-ABC123…). */
    fun mono(size: Float, weight: FontWeight = FontWeight.Medium, lineHeight: Float = FIGMA_LINE_HEIGHT): TextStyle =
        style(DmMono, size, weight, lineHeight)

    /** Noto Sans Symbols (♀, ♂). */
    fun symbols(size: Float, lineHeight: Float = FIGMA_LINE_HEIGHT): TextStyle =
        style(PawriseSymbols, size, FontWeight.Normal, lineHeight)

    /** Titre de question des parcours : Bricolage 700 26. */
    val Question: TextStyle get() = display(26f)

    /** Aide sous un titre : Inter 400 13,5. */
    val Hint: TextStyle get() = inter(13.5f)

    /** Saisie des champs de parcours : Inter 400 17. */
    val FlowField: TextStyle get() = inter(17f)

    /** Bouton plein ou blanc : Inter 700 15, interligne 140 %. */
    val Button: TextStyle get() = inter(15f, FontWeight.Bold, 1.4f)

    private fun style(family: FontFamily, size: Float, weight: FontWeight, lineHeight: Float) = TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = figmaLineHeight(size, lineHeight),
        lineHeightStyle = FigmaLineHeightStyle,
        // Positions de glyphes fractionnaires (sans arrondi au pixel du hinting), comme Figma : mêmes largeurs
        // de ligne et mêmes coupures que la maquette, y compris aux tailles 12,5 / 13,5.
        textMotion = TextMotion.Animated,
        // Coupure gloutonne, comme Figma (la coupure « équilibrée » par défaut déplace des mots à la ligne suivante).
        lineBreak = LineBreak.Simple,
    )
}

/** Interligne Figma : taille × pourcentage, arrondi au point entier comme dans la maquette (13,5 × 135 % → 18). */
fun figmaLineHeight(size: Float, multiple: Float = FIGMA_LINE_HEIGHT): TextUnit = (size * multiple).roundToInt().sp

private fun lh(size: Float): TextUnit = figmaLineHeight(size)

private fun materialStyle(family: FontFamily, weight: FontWeight, size: Float) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lh(size),
    lineHeightStyle = FigmaLineHeightStyle,
    textMotion = TextMotion.Animated,
    lineBreak = LineBreak.Simple,
)

// Tailles de l'échelle Material conservées ; police variable (taille optique = taille) et interlignes Figma 135 %.
internal val PawriseTypography = Typography(
    displaySmall = materialStyle(bricolageGrotesque(32f), FontWeight.Bold, 32f),
    headlineMedium = materialStyle(bricolageGrotesque(28f), FontWeight.Bold, 28f),
    headlineSmall = materialStyle(bricolageGrotesque(22f), FontWeight.Bold, 22f),
    titleMedium = materialStyle(Inter, FontWeight.SemiBold, 16f),
    bodyLarge = materialStyle(Inter, FontWeight.Normal, 16f),
    bodyMedium = materialStyle(Inter, FontWeight.Normal, 15f),
    bodySmall = materialStyle(Inter, FontWeight.Normal, 13f),
    labelLarge = materialStyle(Inter, FontWeight.SemiBold, 16f),
    labelMedium = materialStyle(Inter, FontWeight.Medium, 13f),
)
