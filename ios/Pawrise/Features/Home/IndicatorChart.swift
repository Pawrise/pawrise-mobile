import Charts
import SwiftUI

/// Graphique adapté à chaque indicateur (Figma 02 à 06).
struct IndicatorChart: View {
    let indicator: HealthIndicator
    let period: HealthPeriod

    private var points: [ChartPoint] { indicator.series[period] ?? [] }
    private var tint: Color { Color.Pawrise.tint(for: indicator.state) }

    var body: some View {
        if points.isEmpty {
            Text("Pas encore de mesures sur cette période.")
                .font(Font.Pawrise.subheadline)
                .foregroundStyle(Color.Pawrise.textMuted)
                .frame(maxWidth: .infinity, minHeight: 120)
        } else {
            switch indicator.kind {
            case .heart, .temperature:
                MeasureLineChart(points: points, band: indicator.normalBand, tint: tint, unit: indicator.unit)
            case .activity:
                MeasureBarChart(points: points, tint: tint, unit: indicator.unit)
            case .sleep:
                if period == .today {
                    SleepPhasesBar(points: points)
                } else {
                    MeasureBarChart(points: points, tint: tint, unit: indicator.unit)
                }
            case .afib:
                RhythmStrip(points: points, state: indicator.state)
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

/// Libellés espacés sous le graphique : début, milieu, fin · unité (« 6h   15h   23h · bpm »).
private struct SparseAxisLabels: View {
    let points: [ChartPoint]
    let unit: String

    var body: some View {
        HStack {
            if let first = points.first {
                Text(first.label)
            }
            Spacer()
            if points.count > 2 {
                Text(points[points.count / 2].label)
                Spacer()
            }
            if let last = points.last, points.count > 1 {
                Text(unit.isEmpty ? last.label : "\(last.label) · \(unit)")
            }
        }
        .font(Font.Pawrise.mono(12))
        .foregroundStyle(Color.Pawrise.textMuted)
        .accessibilityHidden(true)
    }
}

// MARK: - Courbe (rythme cardiaque, température)

private struct MeasureLineChart: View {
    let points: [ChartPoint]
    let band: ClosedRange<Double>?
    let tint: Color
    let unit: String

    private var domain: ClosedRange<Double> {
        var values = points.map(\.value)
        if let band { values += [band.lowerBound, band.upperBound] }
        let low = values.min() ?? 0
        let high = values.max() ?? 1
        let padding = high > low ? (high - low) * 0.15 : 1
        return (low - padding)...(high + padding)
    }

    var body: some View {
        let domain = domain
        let lastIndex = points.count - 1
        VStack(spacing: 12) {
            Chart {
                if let band, lastIndex > 0 {
                    RectangleMark(
                        xStart: .value("Début", 0),
                        xEnd: .value("Fin", lastIndex),
                        yStart: .value("Bas", band.lowerBound),
                        yEnd: .value("Haut", band.upperBound)
                    )
                    .foregroundStyle(tint.opacity(0.08))
                    .accessibilityLabel("Plage habituelle")
                    .accessibilityValue("\(ChartFormat.value(band.lowerBound, unit: unit)) à \(ChartFormat.value(band.upperBound, unit: unit))")
                }
                ForEach(Array(points.enumerated()), id: \.offset) { index, point in
                    AreaMark(
                        x: .value("Moment", index),
                        yStart: .value("Base", domain.lowerBound),
                        yEnd: .value("Mesure", point.value)
                    )
                    .interpolationMethod(.catmullRom)
                    .foregroundStyle(
                        LinearGradient(colors: [tint.opacity(0.22), tint.opacity(0.02)], startPoint: .top, endPoint: .bottom)
                    )
                    .accessibilityHidden(true)

                    LineMark(x: .value("Moment", index), y: .value("Mesure", point.value))
                        .interpolationMethod(.catmullRom)
                        .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round, lineJoin: .round))
                        .foregroundStyle(tint)
                        .accessibilityLabel(point.label)
                        .accessibilityValue(ChartFormat.value(point.value, unit: unit))

                    if index == lastIndex {
                        PointMark(x: .value("Moment", index), y: .value("Mesure", point.value))
                            .symbol {
                                Circle()
                                    .fill(tint)
                                    .frame(width: 11, height: 11)
                                    .overlay { Circle().stroke(Color.Pawrise.card, lineWidth: 2) }
                            }
                            .accessibilityHidden(true)
                    }
                }
            }
            .chartXScale(domain: 0...max(lastIndex, 1))
            .chartYScale(domain: domain)
            .chartXAxis(.hidden)
            .chartYAxis(.hidden)
            .frame(height: 170)

            SparseAxisLabels(points: points, unit: unit)
        }
    }
}

// MARK: - Barres (activité, sommeil sur 7 / 30 jours)

private struct MeasureBarChart: View {
    let points: [ChartPoint]
    let tint: Color
    let unit: String

    var body: some View {
        VStack(spacing: 12) {
            Chart {
                ForEach(Array(points.enumerated()), id: \.offset) { index, point in
                    BarMark(
                        x: .value("Moment", String(index)),
                        y: .value("Mesure", point.value),
                        width: .ratio(0.82)
                    )
                    .cornerRadius(4)
                    .foregroundStyle(index == points.count - 1 ? tint : tint.opacity(0.45))
                    .accessibilityLabel(point.label)
                    .accessibilityValue(ChartFormat.value(point.value, unit: unit))
                }
            }
            .chartXAxis(.hidden)
            .chartYAxis(.hidden)
            .frame(height: 170)

            SparseAxisLabels(points: points, unit: unit)
        }
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
        VStack(alignment: .leading, spacing: 14) {
            GeometryReader { proxy in
                let spacing: CGFloat = 6
                let available = proxy.size.width - spacing * CGFloat(max(points.count - 1, 0))
                HStack(spacing: spacing) {
                    ForEach(Array(points.enumerated()), id: \.offset) { index, point in
                        Capsule()
                            .fill(color(for: index))
                            .frame(width: max(proxy.size.height, available * CGFloat(max(point.value, 0) / max(total, 1))))
                    }
                }
            }
            .frame(height: 48)
            .accessibilityHidden(true)

            HStack(spacing: 16) {
                ForEach(Array(points.enumerated()), id: \.offset) { index, point in
                    HStack(spacing: 6) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(color(for: index))
                            .frame(width: 10, height: 10)
                        Text(point.label)
                            .font(Font.Pawrise.subheadline)
                            .foregroundStyle(Color.Pawrise.textMuted)
                        Text(ChartFormat.value(point.value, unit: "h"))
                            .font(Font.Pawrise.mono(14))
                            .foregroundStyle(Color.Pawrise.textPrimary)
                    }
                    .accessibilityElement(children: .combine)
                    if index < points.count - 1 { Spacer(minLength: 0) }
                }
            }
        }
    }
}

// MARK: - Tracé du rythme (fibrillation atriale)

/// Tracé façon ECG : un battement par mesure. Les intervalles irréguliers (valeur 1) sont décalés
/// et dessinés en couleur d'alerte.
private struct RhythmStrip: View {
    let points: [ChartPoint]
    let state: HealthState

    private var irregularCount: Int { points.filter { $0.value >= 1 }.count }

    var body: some View {
        Canvas { context, size in
            let beats = max(points.count, 1)
            let width = size.width / CGFloat(beats)
            let mid = size.height * 0.5
            let amplitude = size.height * 0.42
            let baseColor = Color.Pawrise.tint(for: state)

            for (index, point) in points.enumerated() {
                let irregular = point.value >= 1
                let origin = CGFloat(index) * width
                var path = Path()
                for (step, (x, y)) in Self.beat(irregular: irregular, index: index).enumerated() {
                    let location = CGPoint(x: origin + CGFloat(x) * width, y: mid + CGFloat(y) * amplitude)
                    if step == 0 { path.move(to: location) } else { path.addLine(to: location) }
                }
                context.stroke(
                    path,
                    with: .color(irregular ? Color.Pawrise.alert : baseColor),
                    style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round)
                )
            }
        }
        .frame(height: 150)
        .padding(.vertical, 12)
        .background(
            state == .good ? Color.Pawrise.ink.opacity(0.05) : Color.Pawrise.alertSurface,
            in: .rect(cornerRadius: 12)
        )
        .accessibilityElement()
        .accessibilityLabel("Tracé du rythme cardiaque")
        .accessibilityValue(
            irregularCount == 0
                ? "Intervalles réguliers"
                : "\(irregularCount) intervalle\(irregularCount > 1 ? "s" : "") irrégulier\(irregularCount > 1 ? "s" : "") sur \(points.count)"
        )
    }

    /// Points d'un battement, en fractions de sa largeur (x) et de l'amplitude (y, positif vers le bas).
    private static func beat(irregular: Bool, index: Int) -> [(Double, Double)] {
        if !irregular {
            return [
                (0, 0), (0.16, 0), (0.22, -0.08), (0.28, 0), (0.40, 0),
                (0.44, 0.1), (0.50, -0.3), (0.57, 0.85), (0.63, 0),
                (0.74, 0), (0.82, -0.12), (0.90, 0), (1, 0),
            ]
        }
        // Pas d'onde P, ligne de base agitée et complexe déplacé dans l'intervalle.
        let center = index.isMultiple(of: 2) ? 0.3 : 0.72
        return [
            (0, 0), (0.06, -0.05), (0.12, 0.04), (center - 0.1, -0.04), (center - 0.06, 0.1),
            (center, -0.3), (center + 0.07, 0.85), (center + 0.13, 0), (center + 0.18, -0.05),
            (min(center + 0.24, 0.96), 0.04), (1, 0),
        ]
    }
}
