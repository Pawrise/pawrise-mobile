import SwiftUI

/// Onglet Pawrise · Santé (Figma 01 Homepage, états normal et anomalie).
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
                    .padding(.horizontal, Spacing.page)
                    .padding(.vertical, 16)
            }
        }
        .scrollIndicators(.hidden)
        .pawrisePage()
        .task(id: currentKey) { await load() }
        .sheet(item: $sheet) { sheet in
            sheetContent(sheet)
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
                .presentationCornerRadius(Radius.card)
                .presentationBackground(Color.Pawrise.card)
        }
    }

    private var currentKey: LoadKey? {
        dogs.selectedDog.map { LoadKey(dogID: $0.id, scenario: environment.healthScenario, attempt: attempt) }
    }

    @ViewBuilder
    private func content(for dog: Dog) -> some View {
        VStack(alignment: .leading, spacing: Spacing.section) {
            if let snapshot, snapshotKey == currentKey {
                HealthHeaderCard(
                    dog: dog,
                    snapshot: snapshot,
                    canSwitchDog: dogs.dogs.count > 1,
                    onSwitchDog: { dogs.selectNextDog() },
                    onShowAnomaly: {
                        if let first = snapshot.indicators.first(where: { $0.state != .good }) {
                            sheet = .indicator(first.kind)
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
                    .font(.custom("Inter-SemiBold", size: 13, relativeTo: .footnote))
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                Text("Toucher pour le détail")
                    .font(Font.Pawrise.caption)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .accessibilityHidden(true)
            }
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 16), count: 3), spacing: 16) {
                ForEach(snapshot.indicators) { indicator in
                    HealthTile(
                        systemImage: indicator.systemImage,
                        headline: indicator.headline,
                        state: indicator.state,
                        accessibilityName: indicator.label
                    ) {
                        sheet = .indicator(indicator.kind)
                    }
                }
                HealthTile(
                    systemImage: CollarStatusSheet.batterySymbol(for: snapshot.batteryPercent),
                    headline: "\(snapshot.batteryPercent)%",
                    state: CollarStatusSheet.batteryState(for: snapshot.batteryPercent),
                    accessibilityName: "Batterie du collier"
                ) {
                    sheet = .collar
                }
            }
        }
    }

    @ViewBuilder
    private func sheetContent(_ sheet: HealthSheet) -> some View {
        switch sheet {
        case let .indicator(kind):
            if let indicator = snapshot?.indicators.first(where: { $0.kind == kind }) {
                IndicatorDetailSheet(indicator: indicator)
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

// MARK: - En-tête

private struct HealthHeaderCard: View {
    let dog: Dog
    let snapshot: HealthSnapshot
    let canSwitchDog: Bool
    let onSwitchDog: () -> Void
    let onShowAnomaly: () -> Void

    private var state: HealthState { snapshot.state }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                if canSwitchDog {
                    Button(action: onSwitchDog) {
                        Image(systemName: "arrow.left.arrow.right")
                            .font(.system(size: 18, weight: .medium))
                            .foregroundStyle(Color.Pawrise.textMuted)
                            .frame(width: 44, height: 44)
                    }
                    .accessibilityLabel("Changer de chien")
                }
                Spacer()
                anomalyIcon
            }
            .frame(minHeight: 44)
            .padding(.horizontal, -10)
            .padding(.top, -6)

            HStack(alignment: .center, spacing: 16) {
                DogAvatar(photo: dog.photo, size: 58)
                VStack(alignment: .leading, spacing: 4) {
                    Text(dog.name)
                        .font(Font.Pawrise.title2)
                        .foregroundStyle(Color.Pawrise.textPrimary)
                    if !dog.subtitle.isEmpty {
                        Text(dog.subtitle)
                            .font(Font.Pawrise.subheadline)
                            .foregroundStyle(Color.Pawrise.textMuted)
                    }
                    Text(state.label)
                        .font(Font.Pawrise.subheadline)
                        .foregroundStyle(Color.Pawrise.tint(for: state))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                ScoreRing(score: snapshot.score, state: state)
            }
            .accessibilityElement(children: .combine)
            .accessibilityValue("Score de bien-être \(snapshot.score) sur 100")

            Text(snapshot.summary)
                .font(Font.Pawrise.subheadline)
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
        let icon = Image(systemName: "exclamationmark.triangle")
            .font(.system(size: 18, weight: .medium))
            .frame(width: 44, height: 44)
        if snapshot.hasAnomaly {
            Button(action: onShowAnomaly) {
                icon.foregroundStyle(Color.Pawrise.alert)
            }
            .accessibilityLabel("Anomalie détectée, voir le détail")
        } else {
            icon
                .foregroundStyle(Color.Pawrise.textMuted)
                .accessibilityLabel("Aucune anomalie détectée")
        }
    }
}

/// Anneau du score de bien-être (0–100).
private struct ScoreRing: View {
    let score: Int
    let state: HealthState
    var size: CGFloat = 72

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.Pawrise.tint(for: state).opacity(0.15), lineWidth: 7)
            Circle()
                .trim(from: 0, to: CGFloat(min(max(score, 0), 100)) / 100)
                .stroke(Color.Pawrise.tint(for: state), style: StrokeStyle(lineWidth: 7, lineCap: .round))
                .rotationEffect(.degrees(-90))
            Text("\(score)")
                .font(Font.Pawrise.display(28))
                .foregroundStyle(Color.Pawrise.textPrimary)
                .monospacedDigit()
        }
        .frame(width: size, height: size)
        .animation(.easeOut(duration: 0.4), value: score)
    }
}

// MARK: - Tuile

private struct HealthTile: View {
    let systemImage: String
    let headline: String
    let state: HealthState
    let accessibilityName: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 10) {
                Image(systemName: systemImage)
                    .font(.system(size: 22, weight: .regular))
                Text(headline)
                    .font(.custom("Inter-SemiBold", size: 14, relativeTo: .subheadline))
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .minimumScaleFactor(0.8)
            }
            .foregroundStyle(Color.Pawrise.tint(for: state))
            .padding(8)
            .frame(maxWidth: .infinity, minHeight: 108)
            .background(Color.Pawrise.surface(for: state), in: .rect(cornerRadius: Radius.tile))
            .contentShape(.rect(cornerRadius: Radius.tile))
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
