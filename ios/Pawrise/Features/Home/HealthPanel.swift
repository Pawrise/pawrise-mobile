import SwiftUI

extension EnvironmentValues {
    /// Ferme le panneau du bas avec son animation de sortie.
    @Entry var closeHealthPanel: @MainActor () -> Void = {}
}

/// Panneau du bas des écrans US-B 02 à 07 : fond assombri à 50 %, panneau blanc collé au bas de l'écran,
/// coins supérieurs de 30 pt, filet #D5E3EC, marges 12 / 20 / 32. Se ferme avec ✕, en touchant le fond
/// ou en glissant vers le bas.
struct HealthPanel<Content: View>: View {
    let onDismissed: () -> Void
    @ViewBuilder var content: Content

    @State private var isShown = false
    @State private var dragOffset: CGFloat = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .bottom) {
                Color.black
                    .opacity(isShown ? 0.5 : 0)
                    .contentShape(.rect)
                    .onTapGesture(perform: close)
                    .accessibilityHidden(true)

                if isShown {
                    panel(maxHeight: proxy.size.height + proxy.safeAreaInsets.bottom - 8)
                        .offset(y: dragOffset)
                        .transition(reduceMotion ? .opacity : .move(edge: .bottom))
                }
            }
            .ignoresSafeArea()
        }
        .gesture(dragToClose)
        .environment(\.closeHealthPanel, close)
        .onAppear {
            withAnimation(reduceMotion ? .easeOut(duration: 0.2) : .spring(duration: 0.38, bounce: 0.08)) {
                isShown = true
            }
        }
    }

    private func panel(maxHeight: CGFloat) -> some View {
        // Le fond n'enveloppe que le contenu : le cadre de hauteur maximale reste transparent.
        ViewThatFits(in: .vertical) {
            padded(content)
                .background(panelBackground)
            ScrollView { padded(content) }
                .scrollIndicators(.hidden)
                .background(panelBackground)
        }
        .frame(maxWidth: .infinity)
        .frame(maxHeight: maxHeight, alignment: .bottom)
        .accessibilityAddTraits(.isModal)
        .accessibilityAction(.escape, close)
    }

    private var panelBackground: some View {
        UnevenRoundedRectangle(topLeadingRadius: 30, topTrailingRadius: 30)
            .fill(Color.Pawrise.card)
            .overlay {
                UnevenRoundedRectangle(topLeadingRadius: 30, topTrailingRadius: 30)
                    .strokeBorder(Color.Pawrise.border, lineWidth: 1)
                    .padding(.bottom, -2)
            }
            .clipped()
    }

    private func padded(_ content: Content) -> some View {
        content
            .padding(.top, 13)
            .padding(.horizontal, 20)
            .padding(.bottom, 32)
    }

    private var dragToClose: some Gesture {
        DragGesture(minimumDistance: 12)
            .onChanged { value in
                dragOffset = max(0, value.translation.height)
            }
            .onEnded { value in
                if value.translation.height > 90 || value.predictedEndTranslation.height > 260 {
                    close()
                } else {
                    withAnimation(.spring(duration: 0.3)) { dragOffset = 0 }
                }
            }
    }

    private func close() {
        withAnimation(.easeIn(duration: 0.22)) {
            isShown = false
        } completion: {
            onDismissed()
        }
    }
}

/// En-tête du panneau : poignée 40 × 6, puis titre (Bricolage 700 20) et bouton ✕ rond de 36 pt.
struct HealthSheetHeader: View {
    let title: String
    var closeTint: Color = Color.Pawrise.textMuted
    @Environment(\.closeHealthPanel) private var close

    var body: some View {
        VStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 3)
                .fill(Color.Pawrise.border)
                .frame(width: 40, height: 6)
                .frame(maxWidth: .infinity)
                .accessibilityHidden(true)
            HStack(alignment: .center, spacing: 8) {
                Text(title)
                    .pawriseFont(.display, 20, .bold, lineHeight: 1.35, relativeTo: .title2)
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityAddTraits(.isHeader)
                Button(action: close) {
                    PawriseIcon(.close, size: 18)
                        .foregroundStyle(closeTint)
                        .frame(width: 36, height: 36)
                        .background(Color.Pawrise.elevated, in: .circle)
                        .padding(4)
                        .contentShape(.rect)
                        .padding(-4)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Fermer")
            }
            .frame(minHeight: 36)
        }
    }
}
