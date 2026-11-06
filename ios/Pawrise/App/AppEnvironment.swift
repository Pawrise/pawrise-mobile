import Foundation
import Observation

/// Assemble les services selon la configuration (mock ou serveur) et les expose aux vues.
@MainActor
@Observable
final class AppEnvironment {
    let config: AppConfig
    let session: SessionStore
    let dogs: DogStore
    let health: HealthRepository
    let collars: CollarService
    let assistant: AssistantService
    let social = SocialSignIn()
    let onboarding = OnboardingProgress()

    /// Données simulées affichées dans Santé : normal ou anomalie (réglable dans Paramètres).
    var healthScenario: HealthScenario = .normal

    init(config: AppConfig = .current) {
        self.config = config
        let tokens = TokenStore()
        let auth: AuthService = switch config.authMode {
        case .mock: MockAuthService()
        case .remote: RemoteAuthService(client: APIClient(baseURL: config.apiBaseURL, tokens: tokens))
        }
        Task { await tokens.setRefresher { try await auth.refresh(refreshToken: $0) } }

        self.session = SessionStore(auth: auth, tokens: tokens)
        self.dogs = DogStore(repository: LocalDogRepository())
        self.health = SeedHealthRepository()
        self.collars = MockCollarService()
        self.assistant = AssistantClient(baseURL: config.assistantBaseURL, tokens: tokens)
    }

    /// Identifiant du chien côté assistant. En dev, forcé sur un chien de démo de pawrise-assistant.
    func assistantPetRef(for dog: Dog) -> String {
        config.assistantPetRefOverride ?? dog.id.uuidString.lowercased()
    }

    #if DEBUG
    /// Repart d'un état vierge (session, chiens, découverte) pour les tests d'interface.
    static func resetLocalDataForUITests() {
        let keychain = KeychainStore()
        keychain.remove("token_pair")
        keychain.remove("cached_user")
        try? FileManager.default.removeItem(at: URL.applicationSupportDirectory.appending(path: "dogs"))
        UserDefaults.standard.removeObject(forKey: "onboarding.learnSeen")
    }
    #endif

    func logout() async {
        await session.logout()
        dogs.reset()
        healthScenario = .normal
    }
}

/// Mémorise, par compte, si la découverte (5 écrans) a été vue.
@MainActor
@Observable
final class OnboardingProgress {
    private let defaults: UserDefaults
    private var seen: Set<UUID> = []

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        let stored = defaults.stringArray(forKey: "onboarding.learnSeen") ?? []
        seen = Set(stored.compactMap(UUID.init(uuidString:)))
    }

    func hasSeenLearnFlow(_ userID: UUID) -> Bool {
        seen.contains(userID)
    }

    func setLearnFlowSeen(_ value: Bool, for userID: UUID) {
        if value { seen.insert(userID) } else { seen.remove(userID) }
        defaults.set(seen.map(\.uuidString), forKey: "onboarding.learnSeen")
    }
}
