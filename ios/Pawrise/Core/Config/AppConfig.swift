import Foundation

/// Réglages lus dans Info.plist, eux-mêmes alimentés par Config/*.xcconfig.
struct AppConfig: Sendable {
    enum AuthMode: String, Sendable {
        /// Comptes simulés dans l'app, sans serveur.
        case mock
        /// Appelle pawrise-auth via `apiBaseURL` et l'API-CORE (chiens) via `coreBaseURL`.
        case remote
    }

    let authMode: AuthMode
    let apiBaseURL: URL
    /// API-CORE (chiens, photos, races, collier), version comprise : `…/v1` (convention 7).
    let coreBaseURL: URL
    let assistantBaseURL: URL
    /// `pet_ref` forcé pour l'assistant, seulement s'il est renseigné explicitement (Local.xcconfig).
    /// Sinon l'app envoie l'id du chien courant, que l'assistant lit sur l'API-CORE.
    let assistantPetRefOverride: String?
    let googleClientID: String?
    /// Pages publiques des CGU et de la politique de confidentialité (Paramètres → Légal). Nil tant qu'elles ne sont pas publiées.
    let termsURL: URL?
    let privacyURL: URL?

    static let current = AppConfig(bundle: .main)

    init(
        authMode: AuthMode,
        apiBaseURL: URL,
        coreBaseURL: URL = URL(string: "http://localhost:3000/v1")!,
        assistantBaseURL: URL,
        assistantPetRefOverride: String? = nil,
        googleClientID: String? = nil,
        termsURL: URL? = nil,
        privacyURL: URL? = nil
    ) {
        self.authMode = authMode
        self.apiBaseURL = apiBaseURL
        self.coreBaseURL = coreBaseURL
        self.assistantBaseURL = assistantBaseURL
        self.assistantPetRefOverride = assistantPetRefOverride
        self.googleClientID = googleClientID
        self.termsURL = termsURL
        self.privacyURL = privacyURL
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
            coreBaseURL: value("PawriseCoreBaseURL").flatMap(URL.init(string:)) ?? URL(string: "http://localhost:3000/v1")!,
            assistantBaseURL: value("PawriseAssistantBaseURL").flatMap(URL.init(string:)) ?? URL(string: "http://127.0.0.1:8100")!,
            assistantPetRefOverride: value("PawriseAssistantPetRef"),
            googleClientID: value("GIDClientID"),
            termsURL: value("PawriseTermsURL").flatMap(URL.init(string:)),
            privacyURL: value("PawrisePrivacyURL").flatMap(URL.init(string:))
        )
    }
}
