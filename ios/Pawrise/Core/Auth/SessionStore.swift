import Foundation
import Observation

/// État de session observé par l'interface : qui est connecté, et les actions de connexion/déconnexion.
@MainActor
@Observable
final class SessionStore {
    enum Phase: Equatable {
        case restoring
        case signedOut
        case signedIn(AuthUser)
    }

    private(set) var phase: Phase = .restoring
    /// `true` juste après une inscription : l'app enchaîne sur la découverte.
    private(set) var isNewAccount = false

    var user: AuthUser? {
        if case let .signedIn(user) = phase { return user }
        return nil
    }

    private let auth: AuthService
    private let tokens: TokenStore
    private let keychain: KeychainStore
    private let userKey = "cached_user"

    init(auth: AuthService, tokens: TokenStore, keychain: KeychainStore = KeychainStore()) {
        self.auth = auth
        self.tokens = tokens
        self.keychain = keychain
    }

    /// Au lancement : affiche tout de suite le compte en cache, puis vérifie la session auprès du serveur.
    func restore() async {
        guard await tokens.tokens != nil,
              let data = keychain.data(for: userKey),
              let cached = try? APIClient.decoder.decode(AuthUser.self, from: data) else {
            await clearLocalSession()
            return
        }
        phase = .signedIn(cached)
        do {
            let fresh = try await auth.currentUser()
            cache(fresh)
            phase = .signedIn(fresh)
        } catch APIError.sessionExpired {
            await clearLocalSession()
        } catch {
            // Hors ligne ou serveur indisponible : on garde la session en cache.
        }
    }

    func register(name: String, email: String, phone: String?, password: String) async throws {
        let request = RegisterRequest(
            email: CredentialRules.normalizedEmail(email),
            password: password,
            name: name.trimmingCharacters(in: .whitespaces),
            phone: phone?.isEmpty == true ? nil : phone,
            termsAccepted: true
        )
        try await complete(auth.register(request))
    }

    func login(email: String, password: String) async throws {
        try await complete(auth.login(LoginRequest(email: CredentialRules.normalizedEmail(email), password: password)))
    }

    func signInWithApple(_ request: AppleSignInRequest) async throws {
        try await complete(auth.signInWithApple(request))
    }

    func signInWithGoogle(_ request: GoogleSignInRequest) async throws {
        try await complete(auth.signInWithGoogle(request))
    }

    func logout() async {
        if let refresh = await tokens.tokens?.refreshToken {
            try? await auth.logout(refreshToken: refresh)
        }
        await clearLocalSession()
    }

    /// Appelé quand le refresh échoue ailleurs dans l'app.
    func sessionDidExpire() async {
        await clearLocalSession()
    }

    func finishNewAccountIntro() {
        isNewAccount = false
    }

    private func complete(_ response: AuthResponse) async {
        await tokens.save(response.tokens)
        cache(response.user)
        isNewAccount = response.isNewUser
        phase = .signedIn(response.user)
    }

    private func cache(_ user: AuthUser) {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        encoder.dateEncodingStrategy = .iso8601
        if let data = try? encoder.encode(user) { keychain.set(data, for: userKey) }
    }

    private func clearLocalSession() async {
        await tokens.clear()
        keychain.remove(userKey)
        isNewAccount = false
        phase = .signedOut
    }
}
