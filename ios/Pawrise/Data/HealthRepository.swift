import Foundation

/// Source des mesures du collier. Aujourd'hui : données simulées.
/// Plus tard : Core API (séries TimescaleDB alimentées par l'ingestion du collier).
protocol HealthRepository: Sendable {
    func snapshot(for dog: Dog, scenario: HealthScenario) async throws -> HealthSnapshot
}

/// Données reprises du prototype (pawrise-figma/src/data/health.ts) : 5 indicateurs,
/// chacun en version normale et anomalie, avec des séries Aujourd'hui / 7 jours / 30 jours.
struct SeedHealthRepository: HealthRepository {
    func snapshot(for dog: Dog, scenario: HealthScenario) async throws -> HealthSnapshot {
        let anomaly = scenario == .anomaly
        let name = dog.name
        let indicators: [HealthIndicator] = [
            heart(name, anomaly), afib(name, anomaly), activity(name, anomaly),
            sleep(name, anomaly), temperature(name, anomaly),
        ]
        return HealthSnapshot(
            score: anomaly ? 64 : 87,
            summary: anomaly
                ? "Des indicateurs méritent votre attention aujourd'hui."
                : "Tout semble normal. Rien à signaler pour le moment.",
            indicators: indicators,
            batteryPercent: dog.collar?.batteryPercent ?? 100
        )
    }

    // MARK: Indicateurs

    private func heart(_ name: String, _ anomaly: Bool) -> HealthIndicator {
        HealthIndicator(
            kind: .heart, label: "Rythme cardiaque", systemImage: "heart", unit: "bpm", hasPeriods: true,
            normalBand: 60...100,
            state: anomaly ? .watch : .good,
            headline: anomaly ? "128 bpm" : "78 bpm",
            message: anomaly
                ? "Le rythme cardiaque de \(name) est inhabituellement élevé depuis 45 minutes."
                : "Le rythme cardiaque de \(name) est dans ses valeurs habituelles.",
            detail: anomaly
                // Textes de la maquette (US-B · alerte, écran 02).
                ? "Moyenne de 128 bpm sur la journée, avec des variations normales pendant le repos et l'activité."
                : "Moyenne de 78 bpm sur la journée, avec des variations normales pendant le repos et l'activité.",
            series: anomaly
                ? [
                    .today: points(Self.hours(6, 18), [76, 78, 74, 80, 79, 77, 82, 81, 79, 84, 88, 96, 108, 118, 124, 128, 127, 126]),
                    .week: points(Self.days7, [77, 79, 78, 80, 82, 90, 112]),
                    .month: points(Self.days30, Self.wave(80, 6, 30, seed: 1, drift: 0.9)),
                ]
                : [
                    .today: points(Self.hours(6, 18), Self.wave(78, 9, 18, seed: 2)),
                    .week: points(Self.days7, Self.wave(78, 5, 7, seed: 3)),
                    .month: points(Self.days30, Self.wave(78, 6, 30, seed: 1)),
                ],
            offersAssistant: anomaly
        )
    }

    private func afib(_ name: String, _ anomaly: Bool) -> HealthIndicator {
        // 0 = battement régulier, 1 = intervalle irrégulier détecté
        let irregular: [Double] = [0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0]
        let hours24 = (0..<24).map { "\($0)" }
        return HealthIndicator(
            kind: .afib, label: "Fibrillation atriale", systemImage: "waveform.path.ecg", unit: "", hasPeriods: false,
            normalBand: nil,
            state: anomaly ? .alert : .good,
            headline: anomaly ? "Rythme irrégulier" : "Rythme régulier",
            message: anomaly
                ? "Des battements irréguliers ont été détectés chez \(name)."
                : "Aucun signe de rythme irrégulier détecté chez \(name).",
            detail: anomaly
                ? "Plusieurs intervalles irréguliers repérés sur la dernière heure. À faire vérifier par un vétérinaire."
                : "Le collier analyse le rythme en continu. Les intervalles entre battements sont réguliers.",
            series: [
                .today: points(hours24, anomaly ? irregular : Array(repeating: 0, count: 24)),
                .week: points(Self.days7, anomaly ? [0, 0, 0, 0, 1, 0, 1] : Array(repeating: 0, count: 7)),
                .month: points(Self.days30, (0..<30).map { anomaly && $0 > 24 ? 1 : 0 }),
            ]
        )
    }

    private func activity(_ name: String, _ anomaly: Bool) -> HealthIndicator {
        HealthIndicator(
            kind: .activity, label: "Activité", systemImage: "waveform.path", unit: "min", hasPeriods: true,
            normalBand: nil,
            state: anomaly ? .watch : .good,
            headline: anomaly ? "31 min actives" : "94 min actives",
            message: anomaly
                ? "\(name) bouge nettement moins que d'habitude aujourd'hui."
                : "\(name) a bougé autant que d'habitude aujourd'hui.",
            detail: anomaly
                ? "Seulement 31 minutes actives, contre 95 min en moyenne. À surveiller."
                : "94 minutes actives, réparties sur ses trois promenades.",
            series: anomaly
                ? [
                    .today: points(Self.hours(6, 12, step: 1.5), [2, 6, 4, 3, 2, 5, 3, 1, 1, 2, 1, 1]),
                    .week: points(Self.days7, [92, 98, 88, 96, 90, 70, 31]),
                    .month: points(Self.days30, Self.wave(90, 18, 30, seed: 5, drift: -1.2)),
                ]
                : [
                    .today: points(Self.hours(6, 12, step: 1.5), [4, 22, 12, 8, 6, 18, 10, 6, 4, 14, 20, 8]),
                    .week: points(Self.days7, [88, 102, 76, 94, 110, 130, 96]),
                    .month: points(Self.days30, Self.wave(95, 20, 30, seed: 5)),
                ],
            offersAssistant: anomaly
        )
    }

    private func sleep(_ name: String, _ anomaly: Bool) -> HealthIndicator {
        HealthIndicator(
            kind: .sleep, label: "Sommeil", systemImage: "moon", unit: "h", hasPeriods: true,
            normalBand: nil,
            state: anomaly ? .watch : .good,
            headline: anomaly ? "7 h · agité" : "11 h de sommeil",
            message: anomaly ? "Le sommeil de \(name) a été agité cette nuit." : "\(name) a bien dormi cette nuit.",
            detail: anomaly
                // Texte et répartition de la maquette (US-B · alerte, écran 05).
                ? "7 h · agité, dont 6 h de sommeil profond. Peu de réveils."
                : "11 h de sommeil, dont 6 h de sommeil profond. Peu de réveils.",
            series: [
                // « Aujourd'hui » = répartition des phases de la nuit
                .today: points(["Profond", "Léger", "Éveillé"], [6, 5, 1]),
                .week: points(Self.days7, anomaly ? [11, 10, 11, 10, 9, 8, 7] : [11, 10, 12, 11, 10, 11, 11]),
                .month: points(Self.days30, anomaly ? Self.wave(10, 2, 30, seed: 2, drift: -0.05) : Self.wave(11, 1, 30, seed: 2)),
            ],
            offersAssistant: anomaly
        )
    }

    private func temperature(_ name: String, _ anomaly: Bool) -> HealthIndicator {
        let tenth: ([Double]) -> [Double] = { $0.map { $0 / 10 } }
        return HealthIndicator(
            kind: .temperature, label: "Température", systemImage: "thermometer.medium", unit: "°C", hasPeriods: true,
            normalBand: 38...39.2,
            state: anomaly ? .alert : .good,
            headline: anomaly ? "39,8 °C" : "38,5 °C",
            message: anomaly ? "La température de \(name) est plus élevée que la normale." : "La température de \(name) est normale.",
            detail: anomaly
                // Texte de la maquette (US-B · alerte, écran 06).
                ? "Stable autour de 39,8 °C sur la journée."
                : "Stable autour de 38,5 °C sur la journée.",
            series: anomaly
                ? [
                    .today: points(Self.hours(6, 12, step: 1.5), tenth([385, 386, 387, 388, 389, 391, 393, 395, 396, 397, 398, 398])),
                    .week: points(Self.days7, tenth([385, 386, 385, 387, 388, 391, 398])),
                    .month: points(Self.days30, tenth(Self.wave(386, 3, 30, seed: 1, drift: 0.3))),
                ]
                : [
                    .today: points(Self.hours(6, 12, step: 1.5), tenth([385, 384, 386, 385, 387, 386, 385, 384, 386, 385, 387, 386])),
                    .week: points(Self.days7, tenth([385, 386, 384, 385, 387, 386, 385])),
                    .month: points(Self.days30, tenth(Self.wave(385, 3, 30, seed: 1))),
                ],
            // Maquette : tuile en alerte, pastille « À surveiller » dans la feuille.
            badgeState: anomaly ? .watch : nil,
            offersAssistant: anomaly
        )
    }

    // MARK: Séries

    private func points(_ labels: [String], _ values: [Double]) -> [ChartPoint] {
        labels.enumerated().map { index, label in
            ChartPoint(label: label, value: values.indices.contains(index) ? values[index] : values.last ?? 0)
        }
    }

    static let days7 = ["Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"]
    static let days30 = (1...30).map { "\($0)" }

    static func hours(_ from: Double, _ count: Int, step: Double = 1) -> [String] {
        (0..<count).map { index in
            let hour = (from + Double(index) * step).truncatingRemainder(dividingBy: 24)
            return hour == hour.rounded() ? "\(Int(hour))h" : String(format: "%.1fh", hour)
        }
    }

    /// Même générateur que le prototype, pour des courbes crédibles et reproductibles.
    static func wave(_ base: Double, _ amplitude: Double, _ count: Int, seed: Double, drift: Double = 0) -> [Double] {
        (0..<count).map { index in
            let i = Double(index)
            return (base + sin((i + seed) * 0.7) * amplitude + cos((i + seed) * 0.31) * amplitude * 0.5 + drift * i).rounded()
        }
    }
}
