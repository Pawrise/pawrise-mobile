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

    /// S20 Inscription. `phoneNumber` est déjà au format E.164 (ou nil) ; les CGU affichées valent acceptation.
    func register(firstName: String, lastName: String, email: String, phoneNumber: String?, password: String) async throws {
        let request = RegisterRequest(
            firstName: firstName.trimmingCharacters(in: .whitespaces),
            lastName: lastName.trimmingCharacters(in: .whitespaces),
            email: CredentialRules.normalizedEmail(email),
            phoneNumber: phoneNumber,
            password: password,
            acceptedTermsVersion: LegalTerms.version
        )
        try await complete(auth.register(request), isNew: true)
    }

    func login(email: String, password: String) async throws {
        try await complete(auth.login(LoginRequest(email: CredentialRules.normalizedEmail(email), password: password)), isNew: false)
    }

    func signInWithApple(_ request: AppleSignInRequest) async throws {
        let response = try await auth.signInWithApple(request)
        await complete(response, isNew: response.isNewUser ?? false)
    }

    func signInWithGoogle(_ request: GoogleSignInRequest) async throws {
        let response = try await auth.signInWithGoogle(request)
        await complete(response, isNew: response.isNewUser ?? false)
    }

    /// Mot de passe oublié : le serveur envoie un code à 6 chiffres par e-mail (réponse identique si le compte n'existe pas).
    func requestPasswordReset(email: String) async throws {
        try await auth.requestPasswordReset(ForgotPasswordRequest(email: CredentialRules.normalizedEmail(email)))
    }

    /// Nouveau mot de passe avec le code reçu, puis connexion directe.
    func resetPassword(email: String, code: String, newPassword: String) async throws {
        let email = CredentialRules.normalizedEmail(email)
        try await auth.resetPassword(ResetPasswordRequest(email: email, code: code, newPassword: newPassword))
        try await login(email: email, password: newPassword)
    }

    func resendVerificationEmail() async throws {
        try await auth.resendVerificationEmail()
    }

    /// Recharge le compte (après avoir confirmé son adresse depuis l'e-mail, par exemple).
    func refreshUser() async {
        guard let fresh = try? await auth.currentUser() else { return }
        cache(fresh)
        phase = .signedIn(fresh)
    }

    func preferences() async throws -> Preferences {
        try await auth.preferences()
    }

    func updatePreferences(_ update: PreferencesUpdate) async throws -> Preferences {
        try await auth.updatePreferences(update)
    }

    /// Supprime le compte sur le serveur puis efface la session locale. En cas d'erreur, rien n'est effacé.
    func deleteAccount() async throws {
        try await auth.deleteAccount()
        await clearLocalSession()
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

    private func complete(_ response: AuthResponse, isNew: Bool) async {
        await tokens.save(response.tokens)
        cache(response.user)
        isNewAccount = isNew
        phase = .signedIn(response.user)
    }

    private func cache(_ user: AuthUser) {
        if let data = try? APIClient.encoder.encode(user) { keychain.set(data, for: userKey) }
    }

    private func clearLocalSession() async {
        await tokens.clear()
        keychain.remove(userKey)
        isNewAccount = false
        phase = .signedOut
    }
}
