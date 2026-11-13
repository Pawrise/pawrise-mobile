package com.pawrise.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.feature.home.data.ChartPoint
import com.pawrise.feature.home.data.HealthIndicator
import com.pawrise.feature.home.data.HealthPeriod
import com.pawrise.feature.home.data.HealthState
import com.pawrise.feature.home.data.IndicatorKind
import com.pawrise.feature.home.ui.AFIB_STRIP_PATH
import com.pawrise.feature.home.ui.MaquetteFont
import com.pawrise.feature.home.ui.maquetteStyle
import java.text.NumberFormat
import java.util.Locale

/**
 * Graphique adapté à chaque indicateur (Figma US-B 02 à 06), dessiné à la géométrie de la maquette :
 * zone de 150 dp de haut, tracé de x = 8 à x = largeur − 12, ligne de base à 128 dp.
 */
@Composable
internal fun IndicatorChart(indicator: HealthIndicator, period: HealthPeriod) {
    val points = indicator.series[period].orEmpty()
    if (points.isEmpty()) {
        Text(
            "Pas encore de mesures sur cette période.",
            style = maquetteStyle(MaquetteFont.Inter, 13f, lineHeight = 1.35f, color = PwColors.TextMuted).copy(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
        )
        return
    }
    when (indicator.kind) {
        IndicatorKind.Heart, IndicatorKind.Temperature -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MeasureLineChart(points, indicator.normalBand, indicator.pillState, indicator.unit)
            SparseAxisLabels(points, indicator.unit)
        }
        IndicatorKind.Activity -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MeasureBarChart(points, indicator.pillState, indicator.unit)
            SparseAxisLabels(points, indicator.unit)
        }
        IndicatorKind.Sleep -> if (period == HealthPeriod.Today) {
            SleepPhasesBar(points)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MeasureBarChart(points, indicator.pillState, indicator.unit)
                SparseAxisLabels(points, indicator.unit)
            }
        }
        IndicatorKind.Afib -> RhythmStrip(points, indicator.pillState)
    }
}

private object ChartFormat {
    private val number = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 1
    }

    fun value(value: Double, unit: String): String {
        val text = number.format(value).replace(' ', ' ').replace(' ', ' ')
        return if (unit.isEmpty()) text else "$text $unit"
    }
}

/** Géométrie commune des graphiques de la maquette (zone de 150 dp). */
private object ChartGeometry {
    val height = 150.dp
    const val LEFT = 8f
    const val RIGHT_INSET = 12f
    const val BASELINE = 128f
    /** Plage habituelle : de 27 à 115 dp. */
    const val BAND_TOP = 27f
    const val BAND_BOTTOM = 115f
    /** Barre la plus haute : 114 dp. */
    const val BAR_MAX_HEIGHT = 114f
}

/** Libellés sous le graphique : début, milieu, fin · unité (« 6h   15h   23h · bpm »), en trois colonnes égales. */
@Composable
private fun SparseAxisLabels(points: List<ChartPoint>, unit: String) {
    val style = maquetteStyle(MaquetteFont.Mono, 10f, lineHeight = 1.35f, color = PwColors.TextMuted)
    val last = points.lastOrNull()?.takeIf { points.size > 1 }?.let { if (unit.isEmpty()) it.label else "${it.label} · $unit" }.orEmpty()
    Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
        Text(points.firstOrNull()?.label.orEmpty(), style = style, maxLines = 1, modifier = Modifier.weight(1f))
        Text(
            if (points.size > 2) points[points.size / 2].label else "",
            style = style.copy(textAlign = TextAlign.Center), maxLines = 1, modifier = Modifier.weight(1f),
        )
        Text(last, style = style.copy(textAlign = TextAlign.End), maxLines = 1, modifier = Modifier.weight(1f))
    }
}

// Courbe (rythme cardiaque, température)

@Composable
private fun MeasureLineChart(points: List<ChartPoint>, band: ClosedFloatingPointRange<Double>?, state: HealthState, unit: String) {
    val tint = state.tint
    val values = points.map { it.value }
    val summary = buildString {
        val low = values.min()
        val high = values.max()
        append("De ${ChartFormat.value(low, unit)} à ${ChartFormat.value(high, unit)}, dernière mesure ${ChartFormat.value(values.last(), unit)}")
        band?.let { append(". Plage habituelle ${ChartFormat.value(it.start, unit)} à ${ChartFormat.value(it.endInclusive, unit)}") }
    }
    Canvas(
        Modifier.fillMaxWidth().height(ChartGeometry.height).semantics {
            contentDescription = "Courbe de la période"
            stateDescription = summary
        },
    ) {
        var low = values.min()
        var high = values.max()
        if (band != null) {
            low = minOf(low, band.start)
            high = maxOf(high, band.endInclusive)
        }
        if (high <= low) high = low + 1
        val left = ChartGeometry.LEFT.dp.toPx()
        val right = size.width - ChartGeometry.RIGHT_INSET.dp.toPx()
        val bandTop = ChartGeometry.BAND_TOP.dp.toPx()
        val bandBottom = ChartGeometry.BAND_BOTTOM.dp.toPx()
        val baseline = ChartGeometry.BASELINE.dp.toPx()
        fun y(value: Double) = bandBottom - ((value - low) / (high - low)).toFloat() * (bandBottom - bandTop)
        val step = if (values.size > 1) (right - left) / (values.size - 1) else 0f
        val locations = values.mapIndexed { index, value -> Offset(left + index * step, y(value)) }

        drawLine(if (state == HealthState.Good) PwColors.Border else tint, Offset(left, baseline), Offset(right, baseline), 1.dp.toPx())

        if (band != null) {
            val top = y(band.endInclusive)
            drawRect(
                (if (state == HealthState.Good) PwColors.BrandInk else tint).copy(alpha = 0.08f),
                topLeft = Offset(left, top),
                size = Size(right - left, y(band.start) - top),
            )
        }

        val line = Path().apply {
            moveTo(locations.first().x, locations.first().y)
            locations.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(locations.last().x, baseline)
            lineTo(locations.first().x, baseline)
            close()
        }
        drawPath(area, tint.copy(alpha = 0.12f))
        drawPath(line, tint, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        val last = locations.last()
        drawCircle(tint, 4.dp.toPx(), last)
        drawCircle(if (state == HealthState.Good) PwColors.SurfaceCard else tint, 4.dp.toPx(), last, style = Stroke(2.dp.toPx()))
    }
}

// Barres (activité, sommeil sur 7 / 30 jours)

@Composable
private fun MeasureBarChart(points: List<ChartPoint>, state: HealthState, unit: String) {
    val tint = state.tint
    Canvas(
        Modifier.fillMaxWidth().height(ChartGeometry.height).semantics {
            contentDescription = "Barres de la période"
            stateDescription = points.joinToString(", ") { "${it.label} : ${ChartFormat.value(it.value, unit)}" }
        },
    ) {
        val left = ChartGeometry.LEFT.dp.toPx()
        val right = size.width - ChartGeometry.RIGHT_INSET.dp.toPx()
        val baseline = ChartGeometry.BASELINE.dp.toPx()
        drawLine(if (state == HealthState.Good) PwColors.Border else tint, Offset(left, baseline), Offset(right, baseline), 1.dp.toPx())
        val maxValue = maxOf(points.maxOf { it.value }, 0.0001)
        val step = (right - left) / maxOf(points.size, 1)
        val width = step * 0.83f
        points.forEachIndexed { index, point ->
            val height = (maxOf(point.value, 0.0) / maxValue).toFloat() * ChartGeometry.BAR_MAX_HEIGHT.dp.toPx()
            val radius = minOf(4.dp.toPx(), height / 2, width / 2)
            val isLast = index == points.lastIndex
            drawRoundRectPath(
                Rect(left + index * step + (step - width) / 2, baseline - height, left + index * step + (step + width) / 2, baseline),
                radius,
                if (isLast) tint else tint.copy(alpha = 0.45f),
            )
        }
    }
}

private fun DrawScope.drawRoundRectPath(rect: Rect, radius: Float, color: Color) {
    drawPath(Path().apply { addRoundRect(RoundRect(rect, CornerRadius(radius))) }, color)
}

// Phases de la nuit (sommeil, aujourd'hui)

@Composable
private fun SleepPhasesBar(points: List<ChartPoint>) {
    fun color(index: Int) = when (index) {
        0 -> PwColors.StatusGood
        1 -> PwColors.Action
        else -> PwColors.StatusWatch
    }
    val total = points.sumOf { maxOf(it.value, 0.0) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.fillMaxWidth().height(24.dp).clearAndSetSemantics { }) {
            val spacing = 4.dp.toPx()
            val available = size.width - spacing * maxOf(points.size - 1, 0)
            var x = 0f
            points.forEachIndexed { index, point ->
                val width = maxOf(size.height, available * (maxOf(point.value, 0.0) / maxOf(total, 1.0)).toFloat())
                drawRoundRectPath(Rect(x, 0f, x + width, size.height), 12.dp.toPx(), color(index))
                x += width + spacing
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            points.forEachIndexed { index, point ->
                Row(
                    Modifier.weight(1f).semantics(mergeDescendants = true) { },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).background(color(index), RoundedCornerShape(2.dp)))
                    Text(point.label, style = maquetteStyle(MaquetteFont.Inter, 11f, lineHeight = 1.35f, color = PwColors.TextMuted), maxLines = 1)
                    Text(
                        ChartFormat.value(point.value, "h"),
                        style = maquetteStyle(MaquetteFont.Mono, 11f, lineHeight = 1.35f, color = PwColors.TextPrimary),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// Tracé du rythme (fibrillation atriale)

private val afibPath: Path by lazy { PathParser().parsePathString(AFIB_STRIP_PATH).toPath().apply { fillType = PathFillType.NonZero } }

/**
 * Tracé façon ECG copié de la maquette (zone 320 × 150, fond teinté à 8 % arrondi à 8 dp, trait de 1,8) ;
 * en alerte, tracé et fond prennent la couleur d'alerte. L'accessibilité donne le nombre d'intervalles irréguliers.
 */
@Composable
private fun RhythmStrip(points: List<ChartPoint>, state: HealthState) {
    val irregular = points.count { it.value >= 1 }
    val tint = state.tint
    val backdrop = if (state == HealthState.Good) PwColors.BrandInk else tint
    Canvas(
        Modifier.width(320.dp).height(ChartGeometry.height).semantics {
            contentDescription = "Tracé du rythme cardiaque"
            stateDescription = if (irregular == 0) {
                "Intervalles réguliers"
            } else {
                val plural = if (irregular > 1) "s" else ""
                "$irregular intervalle$plural irrégulier$plural sur ${points.size}"
            }
        },
    ) {
        val radius = CornerRadius(8.dp.toPx())
        drawRoundRect(backdrop.copy(alpha = 0.08f), cornerRadius = radius)
        if (state != HealthState.Good) {
            // Maquette (alerte) : filet intérieur de 1 dp à 8 %.
            val inset = 0.5.dp.toPx()
            drawRoundRect(
                backdrop.copy(alpha = 0.08f),
                topLeft = Offset(inset, inset),
                size = Size(size.width - 2 * inset, size.height - 2 * inset),
                cornerRadius = radius,
                style = Stroke(1.dp.toPx()),
            )
        }
        translate(top = 61.dp.toPx()) {
            scale(density, pivot = Offset.Zero) { drawPath(afibPath, tint) }
        }
    }
}
