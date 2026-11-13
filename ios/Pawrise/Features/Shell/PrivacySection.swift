import SwiftUI

/// S17 Paramètres → Confidentialité (US-24) : trois interrupteurs, un appel `PATCH /v1/me/preferences` par bascule.
/// Carte au style de la maquette (icône, titre Inter 400 14, détail Inter 400 11,5, lignes de 48 pt, interrupteur 44 × 24).
/// L'interrupteur change tout de suite ; s'il est refusé par le serveur, il revient à sa position et un message s'affiche.
struct PrivacySection: View {
    @Environment(SessionStore.self) private var session

    private enum LoadState: Equatable {
        case loading
        case loaded
        case failed(String)
    }

    @State private var preferences = Preferences.defaults
    @State private var state: LoadState = .loading
    @State private var saving: Set<PartialKeyPath<Preferences>> = []
    @State private var errorMessage: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingsCard {
                switch state {
                case .loading:
                    HStack {
                        Text("Chargement…")
                            .pawriseFont(.inter, 14, lineHeight: 1.35, relativeTo: .subheadline)
                            .foregroundStyle(Color.Pawrise.textMuted)
                        Spacer()
                        ProgressView()
                    }
                    .padding(16)
                case let .failed(message):
                    VStack(alignment: .leading, spacing: 8) {
                        Text(message)
                            .pawriseFont(.inter, 13, lineHeight: 1.35, relativeTo: .subheadline)
                            .foregroundStyle(Color.Pawrise.alert)
                        Button("Réessayer") { Task { await load() } }
                            .pawriseFont(.inter, 14, .semibold, relativeTo: .subheadline)
                            .foregroundStyle(Color.Pawrise.primary)
                    }
                    .padding(16)
                case .loaded:
                    Color.clear.frame(height: 8)
                    toggle(
                        .map, "Partage de localisation",
                        detail: "Partager la position en temps réel",
                        \.deviceLocationSharing, id: "privacy-location"
                    ) { PreferencesUpdate(deviceLocationSharing: $0) }
                    toggle(
                        .heart, "Notifications santé",
                        detail: "Alertes en cas d'anomalie détectée",
                        \.healthNotifications, id: "privacy-health-notifications"
                    ) { PreferencesUpdate(healthNotifications: $0) }
                    toggle(
                        .lock, "Données anonymisées",
                        detail: "Contribuer à la recherche vétérinaire",
                        \.anonymizedDataSharing, id: "privacy-anonymized-data"
                    ) { PreferencesUpdate(anonymizedDataSharing: $0) }
                    Color.clear.frame(height: 8)
                }
            }
            if let errorMessage {
                Text(errorMessage)
                    .pawriseFont(.inter, 12, lineHeight: 1.35, relativeTo: .caption)
                    .foregroundStyle(Color.Pawrise.alert)
                    .padding(.horizontal, 4)
            }
        }
        .task { await load() }
    }

    private func toggle(
        _ glyph: PawriseGlyph, _ title: String, detail: String, _ keyPath: WritableKeyPath<Preferences, Bool>, id: String,
        update: @escaping (Bool) -> PreferencesUpdate
    ) -> some View {
        Toggle(isOn: Binding(
            get: { preferences[keyPath: keyPath] },
            set: { save($0, keyPath, update($0)) }
        )) {
            HStack(spacing: 12) {
                SettingsIconTile(glyph: glyph)
                VStack(alignment: .leading, spacing: 0) {
                    Text(title)
                        .pawriseFont(.inter, 14, lineHeight: 1.35, relativeTo: .subheadline)
                        .foregroundStyle(Color.Pawrise.textPrimary)
                    Text(detail)
                        .pawriseFont(.inter, 11.5, lineHeight: 1.35, relativeTo: .caption)
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
        }
        .toggleStyle(PawriseSwitchStyle())
        .disabled(saving.contains(keyPath))
        .frame(minHeight: 48)
        .padding(.leading, 14)
        .padding(.trailing, 11)
        .accessibilityIdentifier(id)
    }

    private func load() async {
        state = .loading
        do {
            preferences = try await session.preferences()
            state = .loaded
        } catch {
            state = .failed(error.localizedDescription)
        }
    }

    private func save(_ value: Bool, _ keyPath: WritableKeyPath<Preferences, Bool>, _ update: PreferencesUpdate) {
        let previous = preferences[keyPath: keyPath]
        preferences[keyPath: keyPath] = value
        saving.insert(keyPath)
        errorMessage = nil
        Task {
            defer { saving.remove(keyPath) }
            do {
                // On ne reprend que la valeur de cet interrupteur : un autre peut être en cours d'envoi.
                let saved = try await session.updatePreferences(update)
                preferences[keyPath: keyPath] = saved[keyPath: keyPath]
            } catch {
                preferences[keyPath: keyPath] = previous
                errorMessage = error.localizedDescription
            }
        }
    }
}
