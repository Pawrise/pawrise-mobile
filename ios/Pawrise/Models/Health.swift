import Foundation

enum HealthState: String, Sendable, Codable {
    case good, watch, alert

    var label: String {
        switch self {
        case .good: "Tout va bien"
        case .watch: "À surveiller"
        case .alert: "Alerte"
        }
    }

    /// Seuils du prototype pour le score de bien-être.
    static func forScore(_ score: Int) -> HealthState {
        score >= 80 ? .good : (score >= 55 ? .watch : .alert)
    }
}

enum HealthPeriod: String, CaseIterable, Identifiable, Sendable {
    case today, week, month
    var id: String { rawValue }
    var label: String {
        switch self {
        case .today: "Aujourd'hui"
        case .week: "7 jours"
        case .month: "30 jours"
        }
    }
}

struct ChartPoint: Identifiable, Hashable, Sendable {
    var id: String { label }
    let label: String
    let value: Double
}

enum IndicatorKind: String, CaseIterable, Identifiable, Sendable {
    case heart, afib, activity, sleep, temperature
    var id: String { rawValue }
}

struct HealthIndicator: Identifiable, Sendable {
    var id: IndicatorKind { kind }
    let kind: IndicatorKind
    let label: String
    let systemImage: String
    let unit: String
    let hasPeriods: Bool
    /// Plage habituelle affichée en fond du graphique.
    let normalBand: ClosedRange<Double>?
    let state: HealthState
    /// Valeur courte affichée sur la tuile : « 78 bpm », « Rythme régulier ».
    let headline: String
    /// Phrase simple, sans vocabulaire médical.
    let message: String
    let detail: String
    let series: [HealthPeriod: [ChartPoint]]
}

/// Ce que l'écran Santé affiche pour un chien à un instant donné.
struct HealthSnapshot: Sendable {
    let score: Int
    let summary: String
    let indicators: [HealthIndicator]
    let batteryPercent: Int

    var state: HealthState { HealthState.forScore(score) }
    var hasAnomaly: Bool { indicators.contains { $0.state != .good } }
}

/// Scénario de données simulées (bascule dans Paramètres en développement).
enum HealthScenario: String, CaseIterable, Identifiable, Sendable {
    case normal, anomaly
    var id: String { rawValue }
    var label: String { self == .normal ? "Normal" : "Anomalie" }
}
