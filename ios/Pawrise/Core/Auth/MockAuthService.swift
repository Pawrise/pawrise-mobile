import Foundation

/// Implémentation sans serveur, active quand `PAWRISE_AUTH_MODE = mock` (Debug par défaut).
/// Reproduit les règles et les codes d'erreur du contrat pawrise-auth pour que les écrans se comportent
/// comme en production. Les comptes sont gardés en mémoire pendant l'exécution de l'app.
///
/// Compte de démo : demo@pawrise.fr / pawrise2026
actor MockAuthService: AuthService {
    private struct Account {
        var user: AuthUser
        var password: String?
    }

    private var accounts: [String: Account] = [:]
    private var sessions: [String: UUID] = [:] // refresh token -> user id
    private var failures: [String: Int] = [:]
    private let latency: Duration

    static let demoEmail = "demo@pawrise.fr"
    static let demoPassword = "pawrise2026"
    private static let commonPasswords: Set<String> = ["password", "12345678", "azertyuiop", "motdepasse", "123456789", "qwertyuiop"]

    init(latency: Duration = .milliseconds(600)) {
        self.latency = latency
        let demo = AuthUser(
            id: UUID(uuidString: "8F0F6B1E-6A3C-4A8E-9D5B-0F4F1C9E2A11")!,
            email: Self.demoEmail, name: "Alex Martin", phone: nil,
            emailVerified: true, role: .owner, providers: [.password], createdAt: .now
        )
        accounts[Self.demoEmail] = Account(user: demo, password: Self.demoPassword)
    }

    func register(_ request: RegisterRequest) async throws -> AuthResponse {
        try await Task.sleep(for: latency)
        let email = CredentialRules.normalizedEmail(request.email)
        guard CredentialRules.isValidEmail(email), !request.name.trimmingCharacters(in: .whitespaces).isEmpty, request.termsAccepted else {
            throw Self.problem(422, "validation_failed")
        }
        guard accounts[email] == nil else { throw Self.problem(409, "email_taken") }
        guard CredentialRules.isValidPassword(request.password), !Self.commonPasswords.contains(request.password.lowercased()) else {
            throw Self.problem(422, "weak_password")
        }
        let user = AuthUser(
            id: UUID(), email: email, name: request.name.trimmingCharacters(in: .whitespaces), phone: request.phone,
            emailVerified: false, role: .owner, providers: [.password], createdAt: .now
        )
        accounts[email] = Account(user: user, password: request.password)
        return issue(for: user, isNew: true)
    }

    func login(_ request: LoginRequest) async throws -> AuthResponse {
        try await Task.sleep(for: latency)
        let email = CredentialRules.normalizedEmail(request.email)
        if failures[email, default: 0] >= 5 { throw Self.problem(423, "account_locked") }
        guard let account = accounts[email], account.password == request.password else {
            failures[email, default: 0] += 1
            throw Self.problem(401, "invalid_credentials")
        }
        failures[email] = 0
        return issue(for: account.user, isNew: false)
    }

    func signInWithApple(_ request: AppleSignInRequest) async throws -> AuthResponse {
        try await Task.sleep(for: latency)
        return federated(provider: .apple, email: "apple.user@privaterelay.appleid.com", name: request.name)
    }

    func signInWithGoogle(_ request: GoogleSignInRequest) async throws -> AuthResponse {
        try await Task.sleep(for: latency)
        return federated(provider: .google, email: "google.user@gmail.com", name: request.name)
    }

    func refresh(refreshToken: String) async throws -> TokenPair {
        guard let userID = sessions.removeValue(forKey: refreshToken) else {
            throw Self.problem(401, "invalid_refresh_token")
        }
        let pair = Self.makeTokens()
        sessions[pair.refreshToken] = userID
        return pair
    }

    func logout(refreshToken: String) async throws {
        sessions.removeValue(forKey: refreshToken)
    }

    func currentUser() async throws -> AuthUser {
        // Le mock n'inspecte pas l'access token : il renvoie le dernier compte connecté.
        guard let id = sessions.values.first, let account = accounts.values.first(where: { $0.user.id == id }) else {
            throw APIError.sessionExpired
        }
        return account.user
    }

    // MARK: - Helpers

    private func federated(provider: AuthUser.Provider, email: String, name: String?) -> AuthResponse {
        if let existing = accounts[email] {
            return issue(for: existing.user, isNew: false)
        }
        let user = AuthUser(
            id: UUID(), email: email, name: name ?? "Nouveau propriétaire", phone: nil,
            emailVerified: true, role: .owner, providers: [provider], createdAt: .now
        )
        accounts[email] = Account(user: user, password: nil)
        return issue(for: user, isNew: true)
    }

    private func issue(for user: AuthUser, isNew: Bool) -> AuthResponse {
        let pair = Self.makeTokens()
        sessions[pair.refreshToken] = user.id
        return AuthResponse(tokens: pair, user: user, isNewUser: isNew)
    }

    private static func makeTokens() -> TokenPair {
        TokenPair(
            accessToken: "mock_at_\(UUID().uuidString)",
            refreshToken: "prt_mock_\(UUID().uuidString)",
            tokenType: "Bearer",
            expiresIn: 900
        )
    }

    private static func problem(_ status: Int, _ code: String) -> APIError {
        .problem(status: status, ProblemDetails(type: nil, title: nil, status: status, code: code, detail: nil, errors: nil))
    }
}
