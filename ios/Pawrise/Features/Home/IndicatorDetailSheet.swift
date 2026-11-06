import SwiftUI

/// Feuille de détail d'un indicateur (Figma 02 à 06, états normal et anomalie).
struct IndicatorDetailSheet: View {
    let indicator: HealthIndicator
    @State private var period: HealthPeriod = .today

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HealthSheetHeader(title: indicator.label, closeTint: indicator.state == .alert ? Color.Pawrise.alert : Color.Pawrise.textMuted)

                HStack(alignment: .center, spacing: 16) {
                    StatePill(state: indicator.state)
                    Text(indicator.headline)
                        .font(Font.Pawrise.mono(26))
                        .foregroundStyle(indicator.state == .good ? Color.Pawrise.textPrimary : Color.Pawrise.tint(for: indicator.state))
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                }
                .accessibilityElement(children: .combine)

                VStack(alignment: .leading, spacing: 8) {
                    Text(indicator.message)
                        .font(Font.Pawrise.body)
                        .foregroundStyle(Color.Pawrise.textPrimary)
                    Text(indicator.detail)
                        .font(Font.Pawrise.callout)
                        .foregroundStyle(Color.Pawrise.textMuted)
                }
                .fixedSize(horizontal: false, vertical: true)

                if indicator.hasPeriods {
                    PeriodSelector(selection: $period)
                }

                IndicatorChart(indicator: indicator, period: indicator.hasPeriods ? period : .today)
                    .padding(16)
                    .frame(maxWidth: .infinity)
                    .background(Color.Pawrise.mutedSurface, in: .rect(cornerRadius: 24))
                    .overlay {
                        RoundedRectangle(cornerRadius: 24).strokeBorder(Color.Pawrise.border, lineWidth: 1)
                    }
                    .animation(.easeInOut(duration: 0.25), value: period)
            }
            .padding(.horizontal, Spacing.page)
            .padding(.top, 24)
            .padding(.bottom, 32)
        }
        .scrollIndicators(.hidden)
    }
}

/// Titre de feuille + bouton de fermeture rond (✕).
struct HealthSheetHeader: View {
    let title: String
    var closeTint: Color = Color.Pawrise.textMuted
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        HStack(alignment: .center) {
            Text(title)
                .font(Font.Pawrise.title)
                .foregroundStyle(Color.Pawrise.textPrimary)
                .accessibilityAddTraits(.isHeader)
            Spacer()
            Button {
                dismiss()
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(closeTint)
                    .frame(width: 40, height: 40)
                    .background(Color.Pawrise.elevated, in: .circle)
                    .frame(width: 44, height: 44)
            }
            .accessibilityLabel("Fermer")
        }
    }
}

/// Sélecteur « Aujourd'hui / 7 jours / 30 jours » en capsules (le segment actif est plein).
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
                        .font(.custom("Inter-SemiBold", size: 15, relativeTo: .subheadline))
                        .foregroundStyle(isSelected ? Color.Pawrise.onPrimary : Color.Pawrise.textMuted)
                        .frame(maxWidth: .infinity, minHeight: 40)
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
        .background(Color.Pawrise.mutedSurface, in: .capsule)
    }
}
