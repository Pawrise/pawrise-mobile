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

/// Barre blanche avec filet supérieur ; l'onglet actif a une capsule pleine derrière son icône.
struct PawriseTabBar: View {
    @Binding var selection: AppTab

    var body: some View {
        HStack(spacing: 0) {
            ForEach(AppTab.allCases) { tab in
                item(tab)
            }
        }
        .padding(.top, 10)
        .padding(.bottom, 6)
        .padding(.horizontal, 4)
        .background {
            Color.Pawrise.card
                .ignoresSafeArea(edges: .bottom)
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
            VStack(spacing: 6) {
                ZStack {
                    Capsule()
                        .fill(Color.Pawrise.primary)
                        .frame(width: 54, height: 36)
                        .opacity(isSelected ? 1 : 0)
                    icon(for: tab)
                        .foregroundStyle(isSelected ? Color.Pawrise.onPrimary : Color.Pawrise.textMuted)
                }
                .frame(height: 36)
                Text(tab.title)
                    .font(.custom("Inter-Regular", size: 12, relativeTo: .caption2))
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

    @ViewBuilder
    private func icon(for tab: AppTab) -> some View {
        if let symbol = tab.systemImage {
            Image(systemName: symbol)
                .font(.system(size: 19, weight: .regular))
        } else {
            PawriseFlower()
                .frame(width: 22, height: 22)
        }
    }
}

/// Fleur du logo Pawrise (huit pétales autour d'un cœur évidé), utilisée pour l'onglet central.
struct PawriseFlower: View {
    var body: some View {
        GeometryReader { proxy in
            let side = min(proxy.size.width, proxy.size.height)
            ZStack {
                ForEach(0..<8, id: \.self) { index in
                    Capsule()
                        .frame(width: side * 0.3, height: side * 0.5)
                        .offset(y: -side * 0.25)
                        .rotationEffect(.degrees(Double(index) * 45))
                }
                Circle()
                    .frame(width: side * 0.28, height: side * 0.28)
                    .blendMode(.destinationOut)
            }
            .compositingGroup()
            .frame(width: side, height: side)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .accessibilityHidden(true)
    }
}
