import SwiftUI

/// Panneau « Collier » ouvert depuis la tuile batterie (Figma US-B 07).
struct CollarStatusSheet: View {
    let dog: Dog
    let batteryPercent: Int

    @State private var showsReplaceInfo = false

    private var batteryState: HealthState { Self.batteryState(for: batteryPercent) }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HealthSheetHeader(title: "Collier")
            batteryCard
            if let collar = dog.collar {
                detailsCard(collar)
                Text("\(dog.name) · \(collar.connected ? "collier connecté" : "collier déconnecté")")
                    .pawriseFont(.inter, 12, lineHeight: 1.35, relativeTo: .caption)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .frame(maxWidth: .infinity)
            } else {
                Text("Aucun collier n'est encore associé à \(dog.name).")
                    .pawriseFont(.inter, 12, lineHeight: 1.35, relativeTo: .caption)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .frame(maxWidth: .infinity)
            }
            Button("Remplacer le collier") { showsReplaceInfo = true }
                .buttonStyle(MutedWideButtonStyle())
                .alert("Remplacer le collier", isPresented: $showsReplaceInfo) {
                    Button("OK", role: .cancel) {}
                } message: {
                    Text("Le remplacement du collier arrivera avec la gestion des colliers.")
                }
        }
    }

    private var batteryCard: some View {
        HStack(spacing: 16) {
            PawriseIcon(.battery, size: 26)
                .foregroundStyle(Color.Pawrise.tint(for: batteryState))
                .frame(width: 56, height: 56)
                .background(Color.Pawrise.surface(for: batteryState), in: .rect(cornerRadius: 16))
            VStack(alignment: .leading, spacing: 4) {
                Text("Batterie du collier")
                    .pawriseFont(.inter, 13, lineHeight: 1.35, relativeTo: .footnote)
                    .foregroundStyle(Color.Pawrise.textMuted)
                Text("\(batteryPercent)%")
                    .pawriseFont(.inter, 24, .bold, lineHeight: 1.35, relativeTo: .title2)
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
        .padding(.horizontal, 16)
        .padding(.vertical, 17)
        .background(Color.Pawrise.page, in: .rect(cornerRadius: 24))
        .overlay { RoundedRectangle(cornerRadius: 24).strokeBorder(Color.Pawrise.border, lineWidth: 1) }
        .accessibilityElement(children: .combine)
    }

    private func detailsCard(_ collar: Collar) -> some View {
        VStack(spacing: 0) {
            row(glyph: .signal, label: "Réseau", value: collar.signal.label,
                valueColor: collar.signal == .strong ? Color.Pawrise.good : Color.Pawrise.watch)
            row(glyph: .shield, label: "Code du collier", value: collar.code, valueColor: Color.Pawrise.textPrimary)
            row(glyph: .battery, label: "Modèle", value: collar.model, valueColor: Color.Pawrise.textPrimary)
        }
        .padding(.vertical, 1)
        .background(Color.Pawrise.page, in: .rect(cornerRadius: 24))
        .overlay { RoundedRectangle(cornerRadius: 24).strokeBorder(Color.Pawrise.border, lineWidth: 1) }
    }

    private func row(glyph: PawriseGlyph, label: String, value: String, valueColor: Color) -> some View {
        HStack(spacing: 10) {
            PawriseIcon(glyph, size: 18)
                .foregroundStyle(Color.Pawrise.textMuted)
            Text(label)
                .pawriseFont(.inter, 14, lineHeight: 1.35, relativeTo: .subheadline)
                .foregroundStyle(Color.Pawrise.textMuted)
            Spacer(minLength: 10)
            Text(value)
                .pawriseFont(.mono, 12, lineHeight: 1.35, relativeTo: .caption)
                .foregroundStyle(valueColor)
                .multilineTextAlignment(.trailing)
        }
        .padding(.vertical, 14)
        .padding(.horizontal, 16)
        .accessibilityElement(children: .combine)
    }

    // MARK: Batterie

    static func batteryState(for percent: Int) -> HealthState {
        percent >= 20 ? .good : (percent >= 10 ? .watch : .alert)
    }
}

/// Bouton large sur fond bleu très clair (« Remplacer le collier » : Inter 400 14, rayon 16, 43 pt).
private struct MutedWideButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .pawriseFont(.inter, 14, lineHeight: 1.35, relativeTo: .subheadline)
            .foregroundStyle(Color.Pawrise.textPrimary)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            .background(Color.Pawrise.elevated, in: .rect(cornerRadius: 16))
            .opacity(configuration.isPressed ? 0.7 : 1)
    }
}
