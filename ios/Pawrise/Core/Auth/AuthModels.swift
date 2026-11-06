import Foundation

// Types du contrat pawrise-auth v1 (Pawrise/pawrise-auth/docs/api-contract.md).
// JSON en snake_case : l'encodeur/décodeur de APIClient convertit automatiquement.

struct TokenPair: Codable, Sendable, Equatable {
    let accessToken: String
    let refreshToken: String
    let tokenType: String
    let expiresIn: Int
}

struct AuthUser: Codable, Sendable, Equatable, Identifiable {
    enum Role: String, Codable, Sendable { case owner, vet, admin }
    enum Provider: String, Codable, Sendable { case password, apple, google }

    let id: UUID
    let email: String
    let name: String
    let phone: String?
    let emailVerified: Bool
    let role: Role
    let providers: [Provider]
    let createdAt: Date

    var firstName: String { name.split(separator: " ").first.map(String.init) ?? name }
}

struct AuthResponse: Codable, Sendable, Equatable {
    let tokens: TokenPair
    let user: AuthUser
    let isNewUser: Bool
}

struct RefreshResponse: Codable, Sendable {
    let tokens: TokenPair
}

struct RegisterRequest: Encodable, Sendable {
    let email: String
    let password: String
    let name: String
    let phone: String?
    let termsAccepted: Bool
}

struct LoginRequest: Encodable, Sendable {
    let email: String
    let password: String
}

struct AppleSignInRequest: Encodable, Sendable {
    let idToken: String
    let authorizationCode: String?
    /// Valeur brute ; Apple a reçu son SHA-256.
    let nonce: String
    let name: String?
}

struct GoogleSignInRequest: Encodable, Sendable {
    let idToken: String
    let nonce: String?
    let name: String?
}

struct RefreshRequest: Encodable, Sendable {
    let refreshToken: String
}

/// Règles de saisie côté app, alignées sur le contrat (le serveur reste la référence).
enum CredentialRules {
    static let passwordMinLength = 8
    static let passwordMaxLength = 128

    static func normalizedEmail(_ raw: String) -> String {
        raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }

    static func isValidEmail(_ raw: String) -> Bool {
        let email = normalizedEmail(raw)
        guard email.count <= 254, let at = email.firstIndex(of: "@") else { return false }
        let local = email[..<at], domain = email[email.index(after: at)...]
        return !local.isEmpty && domain.contains(".") && !domain.hasPrefix(".") && !domain.hasSuffix(".")
            && !email.contains(" ")
    }

    static func isValidPassword(_ password: String) -> Bool {
        (passwordMinLength...passwordMaxLength).contains(password.count)
    }
}
