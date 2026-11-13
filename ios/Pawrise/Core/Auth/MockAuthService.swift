import Foundation

/// Implémentation sans serveur, active quand `PAWRISE_AUTH_MODE = mock` (Debug par défaut).
/// Reproduit les règles et les codes d'erreur de pawrise-auth pour que les écrans se comportent
/// comme en production. Les comptes sont gardés en mémoire pendant l'exécution de l'app.
///
/// Compte de démo : demo@pawrise.fr / pawrise2026. Code de réinitialisation simulé : 123456.
actor MockAuthService: AuthService {
    private struct Account {
        var user: AuthUser
        var password: String?
    }

    private var accounts: [String: Account] = [:]
    private var sessions: [String: UUID] = [:] // refresh token -> user id
    private var failures: [String: Int] = [:]
    private var pendingResets: Set<String> = []
    private var preferencesByUser: [UUID: Preferences] = [:]
    private let latency: Duration

    static let demoEmail = "demo@pawrise.fr"
    static let demoPassword = "pawrise2026"
    static let resetCode = "123456"
    private static let commonPasswords: Set<String> = ["password", "password1", "12345678", "azertyuiop", "motdepasse", "123456789", "qwertyuiop"]

    init(latency: Duration = .milliseconds(600)) {
        self.latency = latency
        let demo = AuthUser(
            id: UUID(uuidString: "8F0F6B1E-6A3C-4A8E-9D5B-0F4F1C9E2A11")!,
            firstName: "Alex", lastName: "Martin", email: Self.demoEmail, phoneNumber: nil,
            registeredAt: .now, emailVerified: true
        )
        accounts[Self.demoEmail] = Account(user: demo, password: Self.demoPassword)
    }

    func register(_ request: RegisterRequest) async throws -> AuthResponse {
        try await Task.sleep(for: latency)
        var fields: [ProblemDetails.FieldError] = []
        if request.firstName.trimmingCharacters(in: .whitespaces).isEmpty { fields.append(.init(field: "firstName", code: "REQUIRED")) }
        if request.lastName.trimmingCharacters(in: .whitespaces).isEmpty { fields.append(.init(field: "lastName", code: "REQUIRED")) }
        if !CredentialRules.isValidEmail(request.email) { fields.append(.init(field: "email", code: "INVALID_EMAIL")) }
        if let code = Self.passwordProblem(request.password) { fields.append(.init(field: "password", code: code)) }
        if request.acceptedTermsVersion != LegalTerms.version { fields.append(.init(field: "acceptedTermsVersion", code: "TERMS_NOT_ACCEPTED")) }
        if !fields.isEmpty { throw APIError.problem(400, "VALIDATION_FAILED", fields: fields) }

        let email = CredentialRules.normalizedEmail(request.email)
        guard accounts[email] == nil else { throw APIError.problem(409, "EMAIL_ALREADY_USED") }
        let user = AuthUser(
            id: UUID(), firstName: request.firstName.trimmingCharacters(in: .whitespaces),
            lastName: request.lastName.trimmingCharacters(in: .whitespaces), email: email,
            phoneNumber: request.phoneNumber, registeredAt: .now, emailVerified: false
        )
        accounts[email] = Account(user: user, password: request.password)
        return issue(for: user, isNew: nil)
    }

    func login(_ request: LoginRequest) async throws -> AuthResponse {
        try await Task.sleep(for: latency)
        let email = CredentialRules.normalizedEmail(request.email)
        if failures[email, default: 0] >= 5 { throw APIError.problem(429, "TOO_MANY_ATTEMPTS") }
        guard let account = accounts[email], let password = account.password, password == request.password else {
            failures[email, default: 0] += 1
            throw APIError.problem(401, "INVALID_CREDENTIALS")
        }
        failures[email] = 0
        return issue(for: account.user, isNew: nil)
    }

    func signInWithApple(_ request: AppleSignInRequest) async throws -> AuthResponse {
        try await Task.sleep(for: latency)
        return federated(email: "apple.user@privaterelay.appleid.com", firstName: request.firstName, lastName: request.lastName)
    }

    func signInWithGoogle(_ request: GoogleSignInRequest) async throws -> AuthResponse {
        try await Task.sleep(for: latency)
        return federated(email: "google.user@gmail.com", firstName: nil, lastName: nil)
    }

    func refresh(refreshToken: String) async throws -> TokenPair {
        guard let userID = sessions.removeValue(forKey: refreshToken) else {
            throw APIError.problem(401, "INVALID_REFRESH_TOKEN")
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

    func requestPasswordReset(_ request: ForgotPasswordRequest) async throws {
        try await Task.sleep(for: latency)
        let email = CredentialRules.normalizedEmail(request.email)
        if accounts[email] != nil { pendingResets.insert(email) }
    }

    func resetPassword(_ request: ResetPasswordRequest) async throws {
        try await Task.sleep(for: latency)
        if let code = Self.passwordProblem(request.newPassword) { throw APIError.invalidField("newPassword", code) }
        let email = CredentialRules.normalizedEmail(request.email)
        guard pendingResets.contains(email), request.code == Self.resetCode, var account = accounts[email] else {
            throw APIError.problem(400, "INVALID_OR_EXPIRED_TOKEN")
        }
        pendingResets.remove(email)
        account.password = request.newPassword
        accounts[email] = account
        failures[email] = 0
        sessions = sessions.filter { $0.value != account.user.id }
    }

    func resendVerificationEmail() async throws {
        try await Task.sleep(for: latency)
    }

    func preferences() async throws -> Preferences {
        try await Task.sleep(for: latency)
        let user = try await currentUser()
        return preferencesByUser[user.id] ?? .defaults
    }

    func updatePreferences(_ update: PreferencesUpdate) async throws -> Preferences {
        try await Task.sleep(for: latency)
        guard update != PreferencesUpdate() else { throw APIError.problem(400, "VALIDATION_FAILED", fields: []) }
        let user = try await currentUser()
        var preferences = preferencesByUser[user.id] ?? .defaults
        if let value = update.deviceLocationSharing { preferences.deviceLocationSharing = value }
        if let value = update.healthNotifications { preferences.healthNotifications = value }
        if let value = update.anonymizedDataSharing { preferences.anonymizedDataSharing = value }
        preferencesByUser[user.id] = preferences
        return preferences
    }

    func deleteAccount() async throws {
        try await Task.sleep(for: latency)
        let user = try await currentUser()
        accounts = accounts.filter { $0.value.user.id != user.id }
        sessions = sessions.filter { $0.value != user.id }
        preferencesByUser[user.id] = nil
        failures[user.email] = nil
        pendingResets.remove(user.email)
    }

    // MARK: - Helpers

    private static func passwordProblem(_ password: String) -> String? {
        if password.count < CredentialRules.passwordMinLength { return "PASSWORD_TOO_SHORT" }
        if password.count > CredentialRules.passwordMaxLength { return "PASSWORD_TOO_LONG" }
        if commonPasswords.contains(password.lowercased()) { return "PASSWORD_TOO_COMMON" }
        return nil
    }

    private func federated(email: String, firstName: String?, lastName: String?) -> AuthResponse {
        if let existing = accounts[email] {
            return issue(for: existing.user, isNew: false)
        }
        let user = AuthUser(
            id: UUID(), firstName: firstName ?? email.components(separatedBy: "@")[0], lastName: lastName ?? "",
            email: email, phoneNumber: nil, registeredAt: .now, emailVerified: true
        )
        accounts[email] = Account(user: user, password: nil)
        return issue(for: user, isNew: true)
    }

    private func issue(for user: AuthUser, isNew: Bool?) -> AuthResponse {
        let pair = Self.makeTokens()
        sessions[pair.refreshToken] = user.id
        return AuthResponse(accessToken: pair.accessToken, refreshToken: pair.refreshToken, user: user, isNewUser: isNew)
    }

    private static func makeTokens() -> TokenPair {
        TokenPair(accessToken: "mock_at_\(UUID().uuidString)", refreshToken: "prt_mock_\(UUID().uuidString)")
    }
}
