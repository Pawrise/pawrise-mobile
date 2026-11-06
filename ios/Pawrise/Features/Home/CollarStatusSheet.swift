import SwiftUI

/// Feuille « Collier » ouverte depuis la tuile batterie (Figma 07).
struct CollarStatusSheet: View {
    let dog: Dog
    let batteryPercent: Int

    private var batteryState: HealthState { Self.batteryState(for: batteryPercent) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HealthSheetHeader(title: "Collier")
                batteryCard
                if let collar = dog.collar {
                    detailsCard(collar)
                    Text("\(dog.name) · \(collar.connected ? "collier connecté" : "collier déconnecté")")
                        .font(Font.Pawrise.subheadline)
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .frame(maxWidth: .infinity)
                } else {
                    Text("Aucun collier n'est encore associé à \(dog.name).")
                        .font(Font.Pawrise.subheadline)
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .frame(maxWidth: .infinity)
                }
                VStack(spacing: 8) {
                    Button("Remplacer le collier") {}
                        .buttonStyle(MutedWideButtonStyle())
                        .disabled(true)
                    Text("Le remplacement du collier arrivera avec la gestion des colliers.")
                        .font(Font.Pawrise.caption)
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal, Spacing.page)
            .padding(.top, 24)
            .padding(.bottom, 32)
        }
        .scrollIndicators(.hidden)
    }

    private var batteryCard: some View {
        HStack(spacing: 18) {
            Image(systemName: Self.batterySymbol(for: batteryPercent))
                .font(.system(size: 24))
                .foregroundStyle(Color.Pawrise.tint(for: batteryState))
                .frame(width: 56, height: 56)
                .background(Color.Pawrise.surface(for: batteryState), in: .rect(cornerRadius: Radius.control))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 6) {
                Text("Batterie du collier")
                    .font(Font.Pawrise.callout)
                    .foregroundStyle(Color.Pawrise.textMuted)
                Text("\(batteryPercent)%")
                    .font(Font.Pawrise.display(30))
                    .foregroundStyle(Color.Pawrise.tint(for: batteryState))
                GeometryReader { proxy in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Color.Pawrise.border)
                        Capsule()
                            .fill(Color.Pawrise.tint(for: batteryState))
                            .frame(width: proxy.size.width * CGFloat(min(max(batteryPercent, 0), 100)) / 100)
                    }
                }
                .frame(height: 8)
                .accessibilityHidden(true)
            }
        }
        .padding(18)
        .background(Color.Pawrise.mutedSurface, in: .rect(cornerRadius: 24))
        .overlay { RoundedRectangle(cornerRadius: 24).strokeBorder(Color.Pawrise.border, lineWidth: 1) }
        .accessibilityElement(children: .combine)
    }

    private func detailsCard(_ collar: Collar) -> some View {
        VStack(spacing: 0) {
            row(symbol: "cellularbars", label: "Réseau", value: collar.signal.label,
                valueColor: collar.signal == .strong ? Color.Pawrise.good : Color.Pawrise.watch)
            row(symbol: "shield", label: "Code du collier", value: collar.code, valueColor: Color.Pawrise.textPrimary)
            row(symbol: "battery.100", label: "Modèle", value: collar.model, valueColor: Color.Pawrise.textPrimary)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 6)
        .background(Color.Pawrise.mutedSurface, in: .rect(cornerRadius: 24))
        .overlay { RoundedRectangle(cornerRadius: 24).strokeBorder(Color.Pawrise.border, lineWidth: 1) }
    }

    private func row(symbol: String, label: String, value: String, valueColor: Color) -> some View {
        HStack(spacing: 14) {
            Image(systemName: symbol)
                .font(.system(size: 16))
                .foregroundStyle(Color.Pawrise.textMuted)
                .frame(width: 24)
                .accessibilityHidden(true)
            Text(label)
                .font(Font.Pawrise.body)
                .foregroundStyle(Color.Pawrise.textMuted)
            Spacer(minLength: 12)
            Text(value)
                .font(Font.Pawrise.mono(15))
                .foregroundStyle(valueColor)
                .multilineTextAlignment(.trailing)
        }
        .frame(minHeight: 52)
        .accessibilityElement(children: .combine)
    }

    // MARK: Batterie

    static func batteryState(for percent: Int) -> HealthState {
        percent >= 20 ? .good : (percent >= 10 ? .watch : .alert)
    }

    static func batterySymbol(for percent: Int) -> String {
        switch percent {
        case 88...: "battery.100"
        case 63..<88: "battery.75"
        case 38..<63: "battery.50"
        case 13..<38: "battery.25"
        default: "battery.0"
        }
    }
}

/// Bouton large sur fond bleu très clair (« Remplacer le collier »).
private struct MutedWideButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(Font.Pawrise.body)
            .foregroundStyle(Color.Pawrise.textPrimary)
            .frame(maxWidth: .infinity, minHeight: 52)
            .background(Color.Pawrise.elevated, in: .rect(cornerRadius: Radius.control))
            .opacity(isEnabled ? (configuration.isPressed ? 0.7 : 1) : 0.5)
    }
}
