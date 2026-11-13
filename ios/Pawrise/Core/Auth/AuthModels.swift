import Foundation

// Types du contrat d'auth : tag `Compte` de pawrise-doc/contrat-interface/openapi.yaml,
// plus les ajouts de pawrise-auth (ECARTS.md : Apple / Google, vérification d'e-mail, mot de passe oublié).
// JSON en camelCase, comme les noms Swift : aucune conversion.

/// `AuthTokens` : jeton d'accès (≈ 15 min) et jeton de rafraîchissement (usage unique).
struct TokenPair: Codable, Sendable, Equatable {
    let accessToken: String
    let refreshToken: String
}

/// `User` du contrat (+ `emailVerified`, ajout E6).
struct AuthUser: Codable, Sendable, Equatable, Identifiable {
    let id: UUID
    let firstName: String
    let lastName: String
    let email: String
    let phoneNumber: String?
    let registeredAt: Date
    let emailVerified: Bool

    var fullName: String {
        [firstName, lastName].filter { !$0.isEmpty }.joined(separator: " ")
    }
}

/// `AuthSession` : paire de jetons + compte. `isNewUser` n'existe que pour Apple / Google.
struct AuthResponse: Codable, Sendable, Equatable {
    let accessToken: String
    let refreshToken: String
    let user: AuthUser
    let isNewUser: Bool?

    var tokens: TokenPair { TokenPair(accessToken: accessToken, refreshToken: refreshToken) }
}

struct RegisterRequest: Encodable, Sendable {
    let firstName: String
    let lastName: String
    let email: String
    let phoneNumber: String?
    let password: String
    let acceptedTermsVersion: String
}

struct LoginRequest: Encodable, Sendable {
    let email: String
    let password: String
}

struct AppleSignInRequest: Encodable, Sendable {
    let identityToken: String
    /// Valeur brute ; Apple a reçu son SHA-256.
    let nonce: String
    /// Apple ne donne le nom qu'à la toute première autorisation.
    let firstName: String?
    let lastName: String?
    let acceptedTermsVersion: String
}

struct GoogleSignInRequest: Encodable, Sendable {
    let idToken: String
    let nonce: String?
    let acceptedTermsVersion: String
}

struct RefreshRequest: Encodable, Sendable {
    let refreshToken: String
}

struct ForgotPasswordRequest: Encodable, Sendable {
    let email: String
}

struct ResetPasswordRequest: Encodable, Sendable {
    let email: String
    let code: String
    let newPassword: String
}

/// S17 Confidentialité : les trois interrupteurs de `GET/PATCH /v1/me/preferences` (D6).
struct Preferences: Codable, Sendable, Equatable {
    var deviceLocationSharing: Bool
    var healthNotifications: Bool
    var anonymizedDataSharing: Bool

    /// Valeurs d'un compte neuf côté serveur.
    static let defaults = Preferences(deviceLocationSharing: true, healthNotifications: true, anonymizedDataSharing: false)
}

/// PATCH : seuls les champs non nil sont envoyés (convention 9 : absent = inchangé).
struct PreferencesUpdate: Encodable, Sendable, Equatable {
    var deviceLocationSharing: Bool?
    var healthNotifications: Bool?
    var anonymizedDataSharing: Bool?
}

/// Version des CGU affichées dans l'app ; le serveur n'accepte que celle en vigueur (RG11, E12).
enum LegalTerms {
    static let version = "2026-10"
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

    /// Convertit un numéro saisi en E.164 (format du contrat) avec PhoneNumberKit, région par défaut FR :
    /// 06 12 34 56 78 devient +33612345678. Renvoie nil si le numéro n'est pas valide.
    static func e164Phone(_ raw: String) -> String? {
        PhoneInput.e164(raw)
    }

    static func isValidResetCode(_ code: String) -> Bool {
        code.count == 6 && code.allSatisfy { $0.isASCII && $0.isNumber }
    }
}
