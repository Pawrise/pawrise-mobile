import Foundation

/// Réglages lus dans Info.plist, eux-mêmes alimentés par Config/*.xcconfig.
struct AppConfig: Sendable {
    enum AuthMode: String, Sendable {
        /// Comptes simulés dans l'app, sans serveur.
        case mock
        /// Appelle pawrise-auth via `apiBaseURL`.
        case remote
    }

    let authMode: AuthMode
    let apiBaseURL: URL
    let assistantBaseURL: URL
    /// `pet_ref` envoyé à l'assistant en développement, tant que la Core API n'existe pas.
    let assistantPetRefOverride: String?
    let googleClientID: String?

    static let current = AppConfig(bundle: .main)

    init(
        authMode: AuthMode,
        apiBaseURL: URL,
        assistantBaseURL: URL,
        assistantPetRefOverride: String? = nil,
        googleClientID: String? = nil
    ) {
        self.authMode = authMode
        self.apiBaseURL = apiBaseURL
        self.assistantBaseURL = assistantBaseURL
        self.assistantPetRefOverride = assistantPetRefOverride
        self.googleClientID = googleClientID
    }

    init(bundle: Bundle) {
        func value(_ key: String) -> String? {
            guard let raw = bundle.object(forInfoDictionaryKey: key) as? String else { return nil }
            let trimmed = raw.trimmingCharacters(in: .whitespaces)
            return trimmed.isEmpty || trimmed.hasPrefix("$(") ? nil : trimmed
        }
        self.init(
            authMode: value("PawriseAuthMode").flatMap(AuthMode.init(rawValue:)) ?? .mock,
            apiBaseURL: value("PawriseAPIBaseURL").flatMap(URL.init(string:)) ?? URL(string: "http://localhost:8200")!,
            assistantBaseURL: value("PawriseAssistantBaseURL").flatMap(URL.init(string:)) ?? URL(string: "http://127.0.0.1:8100")!,
            assistantPetRefOverride: value("PawriseAssistantPetRef"),
            googleClientID: value("GIDClientID")
        )
    }
}
