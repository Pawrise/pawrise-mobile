import Foundation

/// Corps d'erreur RFC 9457 renvoyé par les services Pawrise (`application/problem+json`).
struct ProblemDetails: Decodable, Sendable, Equatable {
    let type: String?
    let title: String?
    let status: Int?
    let code: String?
    let detail: String?
    let errors: [String: [String]]?
}

enum APIError: Error, Sendable, Equatable {
    /// Le serveur a répondu avec une erreur métier identifiée par `code`.
    case problem(status: Int, ProblemDetails)
    /// Session expirée et refresh impossible : l'utilisateur doit se reconnecter.
    case sessionExpired
    case network(URLError.Code)
    case decoding(String)
    case unexpectedStatus(Int)

    var code: String? {
        if case let .problem(_, details) = self { return details.code }
        return nil
    }
}

extension APIError: LocalizedError {
    var errorDescription: String? {
        switch self {
        case let .problem(_, details):
            return Self.message(for: details.code) ?? details.detail ?? details.title ?? "Une erreur est survenue."
        case .sessionExpired:
            return "Votre session a expiré. Reconnectez-vous."
        case .network(.notConnectedToInternet), .network(.networkConnectionLost):
            return "Pas de connexion internet. Vérifiez votre réseau puis réessayez."
        case .network(.timedOut):
            return "Le serveur met trop de temps à répondre. Réessayez dans un instant."
        case .network:
            return "Impossible de joindre le serveur. Réessayez dans un instant."
        case .decoding, .unexpectedStatus:
            return "Réponse inattendue du serveur. Réessayez dans un instant."
        }
    }

    /// Messages affichés pour les codes du contrat pawrise-auth.
    static func message(for code: String?) -> String? {
        switch code {
        case "email_taken": "Un compte existe déjà avec cette adresse. Connectez-vous plutôt."
        case "weak_password": "Ce mot de passe est trop simple. Utilisez au moins 8 caractères, sans mot courant."
        case "password_breached": "Ce mot de passe apparaît dans une fuite de données connue. Choisissez-en un autre."
        case "invalid_credentials": "E-mail ou mot de passe incorrect."
        case "account_locked": "Trop de tentatives. Réessayez dans 15 minutes ou réinitialisez votre mot de passe."
        case "rate_limited": "Trop de tentatives. Patientez un instant puis réessayez."
        case "provider_token_invalid": "La connexion n'a pas pu être vérifiée. Réessayez."
        case "provider_email_unverified": "Votre adresse Google n'est pas vérifiée. Vérifiez-la chez Google ou utilisez un autre moyen de connexion."
        case "validation_failed": "Certains champs sont invalides. Vérifiez le formulaire."
        default: nil
        }
    }
}
