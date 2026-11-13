import SwiftUI

/// Coquille principale (compte avec au moins un chien) : cinq onglets et barre personnalisée du Figma.
struct MainTabView: View {
    @State private var router = TabRouter()
    @State private var conversations = ChatConversations()
    @State private var isKeyboardVisible = false

    var body: some View {
        VStack(spacing: 0) {
            TabView(selection: $router.selection) {
                ForEach(AppTab.allCases) { tab in
                    content(for: tab)
                        .tag(tab)
                        .toolbar(.hidden, for: .tabBar)
                }
            }
            // Comme la barre système, la barre Pawrise s'efface quand le clavier est ouvert.
            if !isKeyboardVisible {
                PawriseTabBar(selection: $router.selection)
                    .transition(.opacity)
            }
        }
        // La barre descend jusqu'au bas de l'écran (sous l'indicateur d'accueil), comme dans la maquette.
        .ignoresSafeArea(.container, edges: isKeyboardVisible ? [] : .bottom)
        .background(Color.Pawrise.page.ignoresSafeArea())
        .environment(router)
        .environment(conversations)
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { _ in
            isKeyboardVisible = true
        }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in
            isKeyboardVisible = false
        }
    }

    @ViewBuilder
    private func content(for tab: AppTab) -> some View {
        switch tab {
        case .location:
            ComingSoonTabView(
                title: "Localisation",
                systemImage: "map",
                message: "La position de votre chien et ses zones arrivent dans la prochaine livraison."
            )
        case .chat:
            ChatView()
        case .health:
            HealthHomeView()
        case .appointments:
            ComingSoonTabView(
                title: "Rendez-vous",
                systemImage: "stethoscope",
                message: "La prise de rendez-vous avec un vétérinaire arrive dans la prochaine livraison."
            )
        case .settings:
            SettingsView()
        }
    }
}

/// Onglet pas encore livré : message clair, sans fonctionnalité simulée.
private struct ComingSoonTabView: View {
    let title: String
    let systemImage: String
    let message: String

    var body: some View {
        ContentUnavailableView {
            Label {
                Text(title)
                    .font(Font.Pawrise.title)
                    .foregroundStyle(Color.Pawrise.textPrimary)
            } icon: {
                Image(systemName: systemImage)
                    .foregroundStyle(Color.Pawrise.primary)
            }
        } description: {
            Text(message)
                .font(Font.Pawrise.body)
                .foregroundStyle(Color.Pawrise.textMuted)
        }
        .pawrisePage()
    }
}

// MARK: - Barre d'onglets

/// Barre de la maquette (80 pt, fond #F4F8FB, filet supérieur) : l'onglet actif a une capsule pleine
/// de 54 × 36 derrière son icône. Comme dans la maquette, elle descend jusqu'au bas de l'écran.
struct PawriseTabBar: View {
    @Binding var selection: AppTab

    var body: some View {
        HStack(spacing: 0) {
            ForEach(AppTab.allCases) { tab in
                item(tab)
            }
        }
        .padding(.horizontal, 8)
        .padding(.top, 13)
        .padding(.bottom, 13)
        .background {
            Color.Pawrise.page
                .overlay(alignment: .top) {
                    Rectangle().fill(Color.Pawrise.border).frame(height: 1)
                }
        }
    }

    private func item(_ tab: AppTab) -> some View {
        let isSelected = selection == tab
        return Button {
            selection = tab
        } label: {
            VStack(spacing: 4) {
                PawriseIcon(tab.glyph, size: 24)
                    .foregroundStyle(isSelected ? Color.Pawrise.onPrimary : Color.Pawrise.textMuted)
                    .frame(width: 54, height: 36)
                    .background(Color.Pawrise.primary.opacity(isSelected ? 1 : 0), in: .capsule)
                Text(tab.title)
                    .pawriseFont(.inter, 10, lineHeight: 1.4, relativeTo: .caption2)
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .frame(maxWidth: .infinity)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(tab.title)
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
        .animation(.easeOut(duration: 0.2), value: isSelected)
    }
}

private extension AppTab {
    /// Icônes du Figma (map, chat, brand, doc, settings).
    var glyph: PawriseGlyph {
        switch self {
        case .location: .map
        case .chat: .chat
        case .health: .brand
        case .appointments: .doc
        case .settings: .settings
        }
    }
}
