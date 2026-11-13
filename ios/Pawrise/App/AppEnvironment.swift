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
    /// En mode remote, aussi envoyé à l'API-CORE pour que le chatbot voie le même scénario.
    var healthScenario: HealthScenario = .normal {
        didSet {
            guard healthScenario != oldValue else { return }
            syncHealthScenario(reportingFailure: true)
        }
    }

    /// Message discret quand le scénario n'a pas pu être envoyé à l'API-CORE (le changement local reste).
    private(set) var scenarioSyncError: String?
    @ObservationIgnored private var scenarioSyncTask: Task<Void, Never>?

    init(config: AppConfig = .current) {
        self.config = config
        let tokens = TokenStore()
        let auth: AuthService = switch config.authMode {
        case .mock: MockAuthService()
        case .remote: RemoteAuthService(client: APIClient(baseURL: config.apiBaseURL, tokens: tokens))
        }
        Task { await tokens.setRefresher { try await auth.refresh(refreshToken: $0) } }

        let session = SessionStore(auth: auth, tokens: tokens)
        self.session = session
        switch config.authMode {
        case .mock:
            self.dogs = DogStore(repository: LocalDogRepository())
            self.collars = MockCollarService()
        case .remote:
            let core = APIClient(baseURL: config.coreBaseURL, tokens: tokens)
            let dogs = DogStore(backend: .remote(RemotePetService(client: core)))
            self.dogs = dogs
            self.collars = RemoteCollarService(store: dogs)
        }
        self.health = SeedHealthRepository()
        self.assistant = AssistantClient(baseURL: config.assistantBaseURL, tokens: tokens)
        self.dogs.onSessionExpired = { [weak session, weak dogs = self.dogs] in
            dogs?.reset()
            await session?.sessionDidExpire()
        }
    }

    /// Chiens du compte à la connexion. En mode remote, le scénario affiché est renvoyé à l'API-CORE
    /// (il a pu changer depuis un autre appareil ou une session précédente), sans message en cas d'échec.
    func loadDogs(for user: AuthUser) async {
        await dogs.load(for: user)
        if dogs.isRemote, dogs.isLoaded { syncHealthScenario(reportingFailure: false) }
    }

    private func syncHealthScenario(reportingFailure: Bool) {
        guard dogs.isRemote else { return }
        scenarioSyncTask?.cancel()
        scenarioSyncError = nil
        let scenario = healthScenario
        scenarioSyncTask = Task { [weak self] in
            guard let self else { return }
            do {
                try await dogs.syncHealthScenario(scenario)
            } catch is CancellationError {
            } catch {
                guard !Task.isCancelled, reportingFailure else { return }
                scenarioSyncError = "Scénario non transmis au serveur : l'assistant peut encore voir l'ancien."
            }
        }
    }

    /// Identifiant du chien côté assistant : l'id API-CORE du chien courant, sauf `pet_ref` forcé en configuration.
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
        resetAfterSignOut()
    }

    /// S25 : le compte est supprimé côté serveur, puis tout ce que l'app garde sur cet appareil est effacé.
    func deleteAccount() async throws {
        let userID = session.user?.id
        try await session.deleteAccount()
        if let userID {
            onboarding.setLearnFlowSeen(false, for: userID)
            await dogs.eraseAll(ownerID: userID)
        }
        resetAfterSignOut()
    }

    private func resetAfterSignOut() {
        dogs.reset()
        healthScenario = .normal
        scenarioSyncTask?.cancel()
        scenarioSyncError = nil
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
