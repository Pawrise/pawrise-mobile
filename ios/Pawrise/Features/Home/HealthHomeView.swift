import SwiftUI

/// Onglet Pawrise · Santé (Figma US-B 01 Homepage, états normal et anomalie).
struct HealthHomeView: View {
    @Environment(AppEnvironment.self) private var environment
    @Environment(DogStore.self) private var dogs

    @State private var snapshot: HealthSnapshot?
    @State private var snapshotKey: LoadKey?
    @State private var errorMessage: String?
    @State private var attempt = 0
    @State private var sheet: HealthSheet?

    private struct LoadKey: Hashable {
        let dogID: Dog.ID
        let scenario: HealthScenario
        let attempt: Int
    }

    var body: some View {
        ScrollView {
            if let dog = dogs.selectedDog {
                content(for: dog)
                    .padding(.horizontal, 16)
                    .padding(.top, 20)
                    .padding(.bottom, 16)
            }
        }
        .scrollIndicators(.hidden)
        .pawrisePage()
        .task(id: currentKey) { await load() }
        // Panneau du bas de la maquette (fond assombri à 50 %, coins de 30 pt) : présenté sans l'animation
        // système, le panneau gère lui-même son entrée et sa sortie.
        .fullScreenCover(item: $sheet) { sheet in
            HealthPanel(onDismissed: { present(nil) }) {
                sheetContent(sheet)
            }
            .presentationBackground(.clear)
        }
    }

    private var currentKey: LoadKey? {
        dogs.selectedDog.map { LoadKey(dogID: $0.id, scenario: environment.healthScenario, attempt: attempt) }
    }

    /// Panneau d'un indicateur ouvert : le badge de l'en-tête prend l'état de cet indicateur
    /// (maquette US-B · alerte : « Alerte » derrière le panneau Fibrillation atriale).
    private func focusedState(in snapshot: HealthSnapshot) -> HealthState? {
        guard case let .indicator(kind) = sheet else { return nil }
        return snapshot.indicators.first { $0.kind == kind }?.pillState
    }

    private func present(_ newValue: HealthSheet?) {
        var transaction = Transaction()
        transaction.disablesAnimations = true
        withTransaction(transaction) { sheet = newValue }
    }

    @ViewBuilder
    private func content(for dog: Dog) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            if let snapshot, snapshotKey == currentKey {
                HealthHeaderCard(
                    dog: dog,
                    snapshot: snapshot,
                    badgeState: focusedState(in: snapshot),
                    canSwitchDog: dogs.dogs.count > 1,
                    onSwitchDog: { dogs.selectNextDog() },
                    onShowAnomaly: {
                        if let first = snapshot.indicators.first(where: { $0.state != .good }) {
                            present(.indicator(first.kind))
                        }
                    }
                )
                indicatorsSection(snapshot)
            } else if let errorMessage {
                VStack(alignment: .leading, spacing: 12) {
                    ErrorBanner(message: errorMessage)
                    Button("Réessayer") { attempt += 1 }
                        .buttonStyle(.pawriseSecondary)
                }
            } else {
                ProgressView("Chargement des mesures…")
                    .font(Font.Pawrise.subheadline)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .frame(maxWidth: .infinity, minHeight: 320)
            }
        }
    }

    private func indicatorsSection(_ snapshot: HealthSnapshot) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .firstTextBaseline) {
                Text("INDICATEURS")
                    .pawriseFont(.inter, 13, .semibold, lineHeight: 1.4, relativeTo: .footnote)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                Text("Toucher pour le détail")
                    .pawriseFont(.inter, 11, lineHeight: 1.4, relativeTo: .caption)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .accessibilityHidden(true)
            }
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 16), count: 3), spacing: 16) {
                ForEach(snapshot.indicators) { indicator in
                    HealthTile(
                        glyph: indicator.kind.glyph,
                        headline: indicator.headline,
                        state: indicator.state,
                        accessibilityName: indicator.label
                    ) {
                        present(.indicator(indicator.kind))
                    }
                }
                HealthTile(
                    glyph: .battery,
                    headline: "\(snapshot.batteryPercent)%",
                    state: CollarStatusSheet.batteryState(for: snapshot.batteryPercent),
                    accessibilityName: "Batterie du collier"
                ) {
                    present(.collar)
                }
            }
        }
    }

    @ViewBuilder
    private func sheetContent(_ sheet: HealthSheet) -> some View {
        switch sheet {
        case let .indicator(kind):
            if let indicator = snapshot?.indicators.first(where: { $0.kind == kind }), let dog = dogs.selectedDog {
                IndicatorDetailSheet(indicator: indicator, dog: dog)
            }
        case .collar:
            if let dog = dogs.selectedDog {
                CollarStatusSheet(dog: dog, batteryPercent: snapshot?.batteryPercent ?? dog.collar?.batteryPercent ?? 0)
            }
        }
    }

    private func load() async {
        guard let dog = dogs.selectedDog, let key = currentKey else { return }
        errorMessage = nil
        do {
            let result = try await environment.health.snapshot(for: dog, scenario: key.scenario)
            guard !Task.isCancelled else { return }
            snapshot = result
            snapshotKey = key
        } catch is CancellationError {
            return
        } catch {
            guard !Task.isCancelled else { return }
            snapshot = nil
            snapshotKey = nil
            errorMessage = "Impossible de charger les mesures de \(dog.name). \(error.localizedDescription)"
        }
    }
}

private enum HealthSheet: Identifiable, Hashable {
    case indicator(IndicatorKind)
    case collar

    var id: String {
        switch self {
        case let .indicator(kind): kind.rawValue
        case .collar: "collar"
        }
    }
}

extension IndicatorKind {
    /// Icône de la tuile (Figma : heart, pulse, activity, moon, thermometer).
    var glyph: PawriseGlyph {
        switch self {
        case .heart: .heart
        case .afib: .pulse
        case .activity: .activity
        case .sleep: .moon
        case .temperature: .thermometer
        }
    }
}

// MARK: - En-tête (WellbeingScore)

private struct HealthHeaderCard: View {
    let dog: Dog
    let snapshot: HealthSnapshot
    var badgeState: HealthState?
    let canSwitchDog: Bool
    let onSwitchDog: () -> Void
    let onShowAnomaly: () -> Void

    private var state: HealthState { snapshot.state }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                // Toujours affiché comme dans la maquette ; actif dès qu'il y a un deuxième chien.
                Button {
                    if canSwitchDog { onSwitchDog() }
                } label: {
                    PawriseIcon(.swap, size: 20)
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .modifier(LargeHitArea())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Changer de chien")
                .accessibilityHidden(!canSwitchDog)
                Spacer()
                anomalyIcon
            }
            .frame(height: 20)

            HStack(alignment: .center, spacing: 12) {
                DogAvatar(photo: dog.photo, size: 58)
                VStack(alignment: .leading, spacing: 3) {
                    Text(dog.name)
                        .pawriseFont(.display, 18, .bold, lineHeight: 1.4, relativeTo: .title3)
                        .foregroundStyle(Color.Pawrise.textPrimary)
                    if !dog.subtitle.isEmpty {
                        Text(dog.subtitle)
                            .pawriseFont(.inter, 11, lineHeight: 1.4, relativeTo: .caption)
                            .foregroundStyle(Color.Pawrise.textMuted)
                    }
                    let badge = badgeState ?? state
                    Text(badge.label)
                        .pawriseFont(.inter, 11, lineHeight: 1.4, relativeTo: .caption)
                        .foregroundStyle(Color.Pawrise.tint(for: badge))
                        .padding(.vertical, 4)
                        .padding(.horizontal, 8)
                        .background(Color.Pawrise.surface(for: badge), in: .capsule)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                ScoreRing(score: snapshot.score, state: state)
            }
            .accessibilityElement(children: .combine)
            .accessibilityValue("Score de bien-être \(snapshot.score) sur 100")

            Text(snapshot.summary)
                .pawriseFont(.inter, 11, lineHeight: 1.4, relativeTo: .caption)
                .foregroundStyle(Color.Pawrise.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .background(Color.Pawrise.surface(for: state), in: .rect(cornerRadius: Radius.card))
        .overlay {
            RoundedRectangle(cornerRadius: Radius.card)
                .strokeBorder(Color.Pawrise.border, lineWidth: 1)
        }
    }

    @ViewBuilder
    private var anomalyIcon: some View {
        if snapshot.hasAnomaly {
            Button(action: onShowAnomaly) {
                PawriseIcon(.alert, size: 20)
                    .foregroundStyle(Color.Pawrise.alert)
                    .modifier(LargeHitArea())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Anomalie détectée, voir le détail")
        } else {
            PawriseIcon(.alert, size: 20)
                .foregroundStyle(Color.Pawrise.textMuted)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("Aucune anomalie détectée")
        }
    }
}

/// Zone tactile de 44 pt autour d'une icône de 20 pt, sans changer la mise en page.
private struct LargeHitArea: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(12)
            .contentShape(.rect)
            .padding(-12)
    }
}

/// Anneau du score de bien-être : cercle plein de 6 pt à la couleur de l'état (maquette).
private struct ScoreRing: View {
    let score: Int
    let state: HealthState

    var body: some View {
        ZStack {
            Circle()
                .strokeBorder(Color.Pawrise.tint(for: state), lineWidth: 6)
            Text("\(score)")
                .pawriseFont(.mono, 28, lineHeight: 1.4, relativeTo: .title)
                .foregroundStyle(Color.Pawrise.textPrimary)
        }
        .frame(width: 72, height: 72)
    }
}

// MARK: - Tuile

private struct HealthTile: View {
    let glyph: PawriseGlyph
    let headline: String
    let state: HealthState
    let accessibilityName: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                PawriseIcon(glyph, size: 28)
                Text(headline)
                    .pawriseFont(.inter, 11, .semibold, lineHeight: 1.4, relativeTo: .caption)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .minimumScaleFactor(0.8)
            }
            .foregroundStyle(Color.Pawrise.tint(for: state))
            .padding(6)
            .frame(maxWidth: .infinity, minHeight: 108)
            .background(Color.Pawrise.surface(for: state), in: .rect(cornerRadius: 16))
            .contentShape(.rect(cornerRadius: 16))
        }
        .buttonStyle(TilePressStyle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(accessibilityName), \(headline), \(state.label)")
        .accessibilityHint("Afficher le détail")
        .accessibilityAddTraits(.isButton)
    }
}

private struct TilePressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}
