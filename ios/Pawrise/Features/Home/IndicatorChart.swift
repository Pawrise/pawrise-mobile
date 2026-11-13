import SwiftUI

/// Graphique adapté à chaque indicateur (Figma US-B 02 à 06), dessiné à la géométrie de la maquette :
/// zone de 150 pt de haut, tracé de x = 8 à x = largeur − 14, ligne de base à 128 pt.
struct IndicatorChart: View {
    let indicator: HealthIndicator
    let period: HealthPeriod

    private var points: [ChartPoint] { indicator.series[period] ?? [] }

    var body: some View {
        if points.isEmpty {
            Text("Pas encore de mesures sur cette période.")
                .pawriseFont(.inter, 13, lineHeight: 1.35, relativeTo: .footnote)
                .foregroundStyle(Color.Pawrise.textMuted)
                .frame(maxWidth: .infinity, minHeight: 120)
        } else {
            switch indicator.kind {
            case .heart, .temperature:
                VStack(spacing: 10) {
                    MeasureLineChart(points: points, band: indicator.normalBand, state: indicator.pillState, unit: indicator.unit)
                    SparseAxisLabels(points: points, unit: indicator.unit)
                }
            case .activity:
                VStack(spacing: 10) {
                    MeasureBarChart(points: points, state: indicator.pillState, unit: indicator.unit)
                    SparseAxisLabels(points: points, unit: indicator.unit)
                }
            case .sleep:
                if period == .today {
                    SleepPhasesBar(points: points)
                } else {
                    VStack(spacing: 10) {
                        MeasureBarChart(points: points, state: indicator.pillState, unit: indicator.unit)
                        SparseAxisLabels(points: points, unit: indicator.unit)
                    }
                }
            case .afib:
                RhythmStrip(points: points, state: indicator.pillState)
            }
        }
    }
}

// MARK: - Mise en forme

private enum ChartFormat {
    static let locale = Locale(identifier: "fr_FR")

    static func value(_ value: Double, unit: String) -> String {
        let number = value.formatted(.number.precision(.fractionLength(0...1)).locale(locale))
        return unit.isEmpty ? number : "\(number) \(unit)"
    }
}

/// Géométrie commune des graphiques de la maquette (zone de 150 pt).
private enum ChartGeometry {
    static let height: CGFloat = 150
    static let left: CGFloat = 8
    static let rightInset: CGFloat = 12
    static let baseline: CGFloat = 128
    /// Plage habituelle : de 27 à 115 pt.
    static let bandTop: CGFloat = 27
    static let bandBottom: CGFloat = 115
    /// Barre la plus haute : 114 pt.
    static let barMaxHeight: CGFloat = 114
}

/// Libellés sous le graphique : début, milieu, fin · unité (« 6h   15h   23h · bpm »), en trois colonnes égales.
private struct SparseAxisLabels: View {
    let points: [ChartPoint]
    let unit: String

    var body: some View {
        HStack(spacing: 0) {
            Text(points.first?.label ?? "")
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(points.count > 2 ? points[points.count / 2].label : "")
                .frame(maxWidth: .infinity, alignment: .center)
            Text(lastLabel)
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .pawriseFont(.mono, 10, lineHeight: 1.35, relativeTo: .caption2)
        .foregroundStyle(Color.Pawrise.textMuted)
        .lineLimit(1)
        .accessibilityHidden(true)
    }

    private var lastLabel: String {
        guard let last = points.last, points.count > 1 else { return "" }
        return unit.isEmpty ? last.label : "\(last.label) · \(unit)"
    }
}

// MARK: - Courbe (rythme cardiaque, température)

private struct MeasureLineChart: View {
    let points: [ChartPoint]
    let band: ClosedRange<Double>?
    let state: HealthState
    let unit: String

    private var tint: Color { Color.Pawrise.tint(for: state) }

    var body: some View {
        Canvas { context, size in
            let values = points.map(\.value)
            var low = values.min() ?? 0
            var high = values.max() ?? 1
            if let band {
                low = min(low, band.lowerBound)
                high = max(high, band.upperBound)
            }
            if high <= low { high = low + 1 }
            let left = ChartGeometry.left
            let right = size.width - ChartGeometry.rightInset
            let span = ChartGeometry.bandBottom - ChartGeometry.bandTop
            func y(_ value: Double) -> CGFloat {
                ChartGeometry.bandBottom - CGFloat((value - low) / (high - low)) * span
            }
            let step = values.count > 1 ? (right - left) / CGFloat(values.count - 1) : 0
            let locations = values.enumerated().map { CGPoint(x: left + CGFloat($0.offset) * step, y: y($0.element)) }

            // Ligne de base
            var baseline = Path()
            baseline.move(to: CGPoint(x: left, y: ChartGeometry.baseline))
            baseline.addLine(to: CGPoint(x: right, y: ChartGeometry.baseline))
            context.stroke(baseline, with: .color(state == .good ? Color.Pawrise.border : tint), lineWidth: 1)

            // Plage habituelle
            if let band {
                let rect = CGRect(x: left, y: y(band.upperBound), width: right - left, height: y(band.lowerBound) - y(band.upperBound))
                context.fill(Path(rect), with: .color((state == .good ? Color.Pawrise.ink : tint).opacity(0.08)))
            }

            let line = Self.polyline(locations)
            var area = line
            if let last = locations.last, let first = locations.first {
                area.addLine(to: CGPoint(x: last.x, y: ChartGeometry.baseline))
                area.addLine(to: CGPoint(x: first.x, y: ChartGeometry.baseline))
                area.closeSubpath()
            }
            context.fill(area, with: .color(tint.opacity(0.12)))
            context.stroke(line, with: .color(tint), style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))

            if let last = locations.last {
                let dot = Path(ellipseIn: CGRect(x: last.x - 4, y: last.y - 4, width: 8, height: 8))
                context.fill(dot, with: .color(tint))
                context.stroke(dot, with: .color(state == .good ? Color.Pawrise.card : tint), lineWidth: 2)
            }
        }
        .frame(height: ChartGeometry.height)
        .accessibilityElement()
        .accessibilityLabel("Courbe de la période")
        .accessibilityValue(accessibilitySummary)
    }

    private var accessibilitySummary: String {
        let values = points.map(\.value)
        guard let low = values.min(), let high = values.max(), let last = values.last else { return "" }
        var text = "De \(ChartFormat.value(low, unit: unit)) à \(ChartFormat.value(high, unit: unit)), dernière mesure \(ChartFormat.value(last, unit: unit))"
        if let band {
            text += ". Plage habituelle \(ChartFormat.value(band.lowerBound, unit: unit)) à \(ChartFormat.value(band.upperBound, unit: unit))"
        }
        return text
    }

    /// Ligne brisée passant par tous les points (comme la courbe vectorielle de la maquette).
    static func polyline(_ points: [CGPoint]) -> Path {
        var path = Path()
        guard let first = points.first else { return path }
        path.move(to: first)
        points.dropFirst().forEach { path.addLine(to: $0) }
        return path
    }

    /// Courbe lissée (Catmull-Rom) passant par tous les points.
    static func smooth(_ points: [CGPoint]) -> Path {
        var path = Path()
        guard let first = points.first else { return path }
        path.move(to: first)
        guard points.count > 1 else { return path }
        for index in 0..<(points.count - 1) {
            let p0 = points[max(index - 1, 0)]
            let p1 = points[index]
            let p2 = points[index + 1]
            let p3 = points[min(index + 2, points.count - 1)]
            let control1 = CGPoint(x: p1.x + (p2.x - p0.x) / 6, y: p1.y + (p2.y - p0.y) / 6)
            let control2 = CGPoint(x: p2.x - (p3.x - p1.x) / 6, y: p2.y - (p3.y - p1.y) / 6)
            path.addCurve(to: p2, control1: control1, control2: control2)
        }
        return path
    }
}

// MARK: - Barres (activité, sommeil sur 7 / 30 jours)

private struct MeasureBarChart: View {
    let points: [ChartPoint]
    let state: HealthState
    let unit: String

    private var tint: Color { Color.Pawrise.tint(for: state) }

    var body: some View {
        Canvas { context, size in
            let left = ChartGeometry.left
            let right = size.width - ChartGeometry.rightInset
            var baseline = Path()
            baseline.move(to: CGPoint(x: left, y: ChartGeometry.baseline))
            baseline.addLine(to: CGPoint(x: right, y: ChartGeometry.baseline))
            context.stroke(baseline, with: .color(state == .good ? Color.Pawrise.border : tint), lineWidth: 1)

            let maxValue = max(points.map(\.value).max() ?? 1, 0.0001)
            let step = (right - left) / CGFloat(max(points.count, 1))
            let width = step * 0.83
            for (index, point) in points.enumerated() {
                let height = CGFloat(max(point.value, 0) / maxValue) * ChartGeometry.barMaxHeight
                let rect = CGRect(
                    x: left + CGFloat(index) * step + (step - width) / 2,
                    y: ChartGeometry.baseline - height,
                    width: width,
                    height: height
                )
                let isLast = index == points.count - 1
                let radius = min(4, rect.height / 2, rect.width / 2)
                context.fill(Path(roundedRect: rect, cornerRadius: radius, style: .circular), with: .color(isLast ? tint : tint.opacity(0.45)))
            }
        }
        .frame(height: ChartGeometry.height)
        .accessibilityElement()
        .accessibilityLabel("Barres de la période")
        .accessibilityValue(
            points.map { "\($0.label) : \(ChartFormat.value($0.value, unit: unit))" }.joined(separator: ", ")
        )
    }
}

// MARK: - Phases de la nuit (sommeil, aujourd'hui)

private struct SleepPhasesBar: View {
    let points: [ChartPoint]

    private var total: Double { points.reduce(0) { $0 + max($1.value, 0) } }

    private func color(for index: Int) -> Color {
        switch index {
        case 0: Color.Pawrise.good
        case 1: Color.Pawrise.primary
        default: Color.Pawrise.watch
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            GeometryReader { proxy in
                let spacing: CGFloat = 4
                let available = proxy.size.width - spacing * CGFloat(max(points.count - 1, 0))
                HStack(spacing: spacing) {
                    ForEach(Array(points.enumerated()), id: \.offset) { index, point in
                        RoundedRectangle(cornerRadius: 12, style: .circular)
                            .fill(color(for: index))
                            .frame(width: max(proxy.size.height, available * CGFloat(max(point.value, 0) / max(total, 1))))
                    }
                }
            }
            .frame(height: 24)
            .accessibilityHidden(true)

            HStack(spacing: 10) {
                ForEach(Array(points.enumerated()), id: \.offset) { index, point in
                    HStack(spacing: 4) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(color(for: index))
                            .frame(width: 8, height: 8)
                        Text(point.label)
                            .pawriseFont(.inter, 11, lineHeight: 1.35, relativeTo: .caption)
                            .foregroundStyle(Color.Pawrise.textMuted)
                        Text(ChartFormat.value(point.value, unit: "h"))
                            .pawriseFont(.mono, 11, lineHeight: 1.35, relativeTo: .caption)
                            .foregroundStyle(Color.Pawrise.textPrimary)
                    }
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityElement(children: .combine)
                }
            }
        }
    }
}

// MARK: - Tracé du rythme (fibrillation atriale)

/// Tracé façon ECG de la maquette (zone 320 × 150, fond teinté arrondi à 6 pt, trait 1,8) : un battement de
/// 13,3 pt par mesure (petite bosse, chute de 29 pt sous la ligne de base, pic de 14 pt, plateau).
/// Les intervalles irréguliers (valeur 1) sont décalés, sans bosse, et dessinés en couleur d'alerte.
private struct RhythmStrip: View {
    let points: [ChartPoint]
    let state: HealthState

    private var irregularCount: Int { points.filter { $0.value >= 1 }.count }

    var body: some View {
        Canvas { context, size in
            let baseColor = Color.Pawrise.tint(for: state)
            context.fill(
                Path(roundedRect: CGRect(origin: .zero, size: size), cornerRadius: 6, style: .circular),
                with: .color((state == .good ? Color.Pawrise.ink : baseColor).opacity(0.08))
            )
            let beats = max(points.count, 1)
            let lead: CGFloat = 1.9
            let width = (size.width - lead) / CGFloat(beats)
            let base: CGFloat = 74.75
            var start = Path()
            start.move(to: CGPoint(x: 0, y: base))
            start.addLine(to: CGPoint(x: lead, y: base))
            context.stroke(start, with: .color(baseColor), style: StrokeStyle(lineWidth: 1.8, lineCap: .round, lineJoin: .round))

            for index in 0..<beats {
                let irregular = points[min(index, points.count - 1)].value >= 1
                let origin = lead + CGFloat(index) * width
                var path = Path()
                for (step, (x, y)) in Self.beat(irregular: irregular, index: index).enumerated() {
                    let location = CGPoint(x: origin + CGFloat(x) * width, y: base + CGFloat(y))
                    if step == 0 { path.move(to: location) } else { path.addLine(to: location) }
                }
                context.stroke(
                    path,
                    with: .color(irregular ? Color.Pawrise.alert : baseColor),
                    style: StrokeStyle(lineWidth: 1.8, lineCap: .butt, lineJoin: .miter, miterLimit: 1.5)
                )
            }
        }
        .frame(width: 320, height: ChartGeometry.height)
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement()
        .accessibilityLabel("Tracé du rythme cardiaque")
        .accessibilityValue(
            irregularCount == 0
                ? "Intervalles réguliers"
                : "\(irregularCount) intervalle\(irregularCount > 1 ? "s" : "") irrégulier\(irregularCount > 1 ? "s" : "") sur \(points.count)"
        )
    }

    /// Points d'un battement : x en fraction de sa largeur, y en points sous la ligne de base (négatif = au-dessus).
    private static func beat(irregular: Bool, index: Int) -> [(Double, Double)] {
        if !irregular {
            return [(0, 0), (0.187, -6), (0.346, 29.4), (0.505, -13.75), (0.72, 0), (1, 0)]
        }
        // Pas de bosse, complexe déplacé dans l'intervalle.
        let shift = index.isMultiple(of: 2) ? -0.12 : 0.14
        return [(0, 0), (0.346 + shift - 0.08, 0), (0.346 + shift, 29.4), (0.505 + shift, -13.75), (0.72 + shift, 0), (1, 0)]
    }
}