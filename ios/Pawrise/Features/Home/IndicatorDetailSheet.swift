import SwiftUI

/// Panneau de détail d'un indicateur (Figma US-B 02 à 06, états normal et anomalie).
struct IndicatorDetailSheet: View {
    let indicator: HealthIndicator
    let dog: Dog
    @State private var period: HealthPeriod = .today

    @Environment(AppEnvironment.self) private var environment
    @Environment(TabRouter.self) private var router
    @Environment(ChatConversations.self) private var conversations
    @Environment(\.closeHealthPanel) private var close

    private var tint: Color { Color.Pawrise.tint(for: indicator.state) }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HealthSheetHeader(
                title: indicator.label,
                closeTint: indicator.pillState == .good ? Color.Pawrise.textMuted : Color.Pawrise.tint(for: indicator.pillState)
            )

            HStack(alignment: .center, spacing: 10) {
                StateBadge(state: indicator.pillState)
                Text(indicator.headline)
                    .pawriseFont(.mono, 20, lineHeight: 1.35, relativeTo: .title3)
                    .foregroundStyle(indicator.state == .good ? Color.Pawrise.textPrimary : tint)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .accessibilityElement(children: .combine)

            // Coupure de lignes identique à la maquette (pas d'anti-orphelin iOS).
            VStack(alignment: .leading, spacing: 6) {
                MaquetteText(text: indicator.message, size: 14, lineHeight: 1.35, color: Color.Pawrise.textPrimary, relativeTo: .subheadline)
                MaquetteText(text: indicator.detail, size: 13, lineHeight: 1.35, color: Color.Pawrise.textMuted, relativeTo: .footnote)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if indicator.hasPeriods {
                PeriodSelector(selection: $period)
            }

            // Figma : filet extérieur, d'où 13 pt entre le bord et le contenu (cartes de 200 / 176 / 75 pt).
            IndicatorChart(indicator: indicator, period: indicator.hasPeriods ? period : .today)
                .padding(13)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.Pawrise.page, in: .rect(cornerRadius: 24))
                .overlay {
                    RoundedRectangle(cornerRadius: 24).strokeBorder(Color.Pawrise.border, lineWidth: 1)
                }
                .animation(.easeInOut(duration: 0.25), value: period)

            if indicator.offersAssistant {
                Button(action: askPawrise) {
                    Text("✦  Demander à Pawrise")
                        .pawriseFont(.inter, 15, .bold, relativeTo: .headline)
                        .foregroundStyle(Color.Pawrise.onPrimary)
                        .frame(maxWidth: .infinity, minHeight: 56)
                        .background(Color.Pawrise.primary, in: .rect(cornerRadius: 16))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Demander à Pawrise")
                .accessibilityHint("Ouvre le Chat IA avec une question sur cette mesure")
            }
        }
    }

    /// Ouvre le Chat IA et pose la question sur l'indicateur concerné.
    private func askPawrise() {
        let question = "\(indicator.message) Que dois-je faire ?"
        conversations.conversation(for: dog, environment: environment).send(question)
        router.open(.chat)
        close()
    }
}

/// Pastille d'état du panneau (120 × 28, rayon 14) : coche et libellé à la couleur de l'état,
/// fond vert clair dans tous les états comme dans la maquette.
private struct StateBadge: View {
    let state: HealthState

    var body: some View {
        HStack(spacing: 5) {
            PawriseIcon(.check, size: 14)
            Text(state.label)
                .pawriseFont(.inter, 12, .semibold, lineHeight: 1.35, relativeTo: .caption)
                .lineLimit(1)
        }
        .foregroundStyle(Color.Pawrise.tint(for: state))
        .frame(width: 120, height: 28)
        .background(Color.Pawrise.goodSurface, in: .capsule)
    }
}

/// Sélecteur « Aujourd'hui / 7 jours / 30 jours » (segment actif plein, fond #F4F8FB).
private struct PeriodSelector: View {
    @Binding var selection: HealthPeriod
    @Namespace private var namespace

    var body: some View {
        HStack(spacing: 4) {
            ForEach(HealthPeriod.allCases) { period in
                let isSelected = selection == period
                Button {
                    withAnimation(.snappy(duration: 0.25)) { selection = period }
                } label: {
                    Text(period.label)
                        .pawriseFont(.inter, 12, .semibold, lineHeight: 1.35, relativeTo: .caption)
                        .foregroundStyle(isSelected ? Color.Pawrise.onPrimary : Color.Pawrise.textMuted)
                        .frame(maxWidth: .infinity, minHeight: 32)
                        .background {
                            if isSelected {
                                Capsule()
                                    .fill(Color.Pawrise.primary)
                                    .matchedGeometryEffect(id: "period", in: namespace)
                            }
                        }
                        .contentShape(.capsule)
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
            }
        }
        .padding(4)
        .background(Color.Pawrise.page, in: .capsule)
    }
}
