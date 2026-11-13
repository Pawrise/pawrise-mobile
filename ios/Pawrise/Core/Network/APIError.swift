import Foundation

/// Corps d'erreur RFC 9457 des services Pawrise (`application/problem+json`, convention 4 de pawrise-doc).
/// `code` est stable : c'est lui que l'app traduit, jamais `title` ni `detail`.
struct ProblemDetails: Decodable, Sendable, Equatable {
    struct FieldError: Decodable, Sendable, Equatable {
        let field: String
        let code: String
    }

    let status: Int?
    let code: String?
    let title: String?
    let detail: String?
    let errors: [FieldError]?
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

    /// Erreurs par champ de `VALIDATION_FAILED`.
    var fieldErrors: [ProblemDetails.FieldError] {
        if case let .problem(_, details) = self { return details.errors ?? [] }
        return []
    }

    static func problem(_ status: Int, _ code: String, fields: [ProblemDetails.FieldError]? = nil) -> APIError {
        .problem(status: status, ProblemDetails(status: status, code: code, title: nil, detail: nil, errors: fields))
    }

    static func invalidField(_ field: String, _ code: String) -> APIError {
        problem(400, "VALIDATION_FAILED", fields: [.init(field: field, code: code)])
    }
}

extension APIError: LocalizedError {
    var errorDescription: String? {
        switch self {
        case let .problem(_, details):
            if details.code == "VALIDATION_FAILED", let first = details.errors?.first,
               let message = Self.fieldMessage(field: first.field, code: first.code) {
                return message
            }
            return Self.message(for: details.code) ?? "Une erreur est survenue. Réessayez dans un instant."
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

    /// Codes d'erreur du contrat (openapi.yaml) et de pawrise-auth (ECARTS.md, E11).
    static func message(for code: String?) -> String? {
        switch code {
        case "EMAIL_ALREADY_USED": "Un compte existe déjà avec cette adresse. Connectez-vous plutôt."
        case "INVALID_CREDENTIALS": "E-mail ou mot de passe incorrect."
        case "TOO_MANY_ATTEMPTS": "Trop d'essais ratés. Réessayez dans 15 minutes ou réinitialisez votre mot de passe."
        case "INVALID_REFRESH_TOKEN", "UNAUTHORIZED": "Votre session a expiré. Reconnectez-vous."
        case "INVALID_PROVIDER_TOKEN": "La connexion n'a pas pu être vérifiée. Réessayez."
        case "PROVIDER_EMAIL_NOT_VERIFIED": "Votre adresse Google n'est pas vérifiée. Vérifiez-la chez Google ou utilisez un autre moyen de connexion."
        case "PROVIDER_NOT_CONFIGURED": "Ce moyen de connexion n'est pas encore disponible."
        case "INVALID_OR_EXPIRED_TOKEN": "Code incorrect ou expiré. Vérifiez-le ou demandez-en un nouveau."
        case "CHIEF_TRANSFER_REQUIRED": "Vous êtes le propriétaire principal d'un chien partagé. Transférez d'abord ce rôle à un autre propriétaire, puis supprimez votre compte."
        case "VALIDATION_FAILED": "Certains champs sont invalides. Vérifiez le formulaire."
        default: nil
        }
    }

    /// Codes par champ de `VALIDATION_FAILED`.
    static func fieldMessage(field: String, code: String) -> String? {
        switch code {
        case "REQUIRED":
            switch field {
            case "firstName": "Indiquez votre prénom."
            case "lastName": "Indiquez votre nom."
            case "email": "Indiquez votre adresse e-mail."
            default: "Ce champ est obligatoire."
            }
        case "INVALID_EMAIL": "Saisissez une adresse e-mail valide, par exemple alex@exemple.fr."
        case "INVALID_PHONE": "Saisissez un numéro valide, par exemple 06 12 34 56 78."
        case "PASSWORD_TOO_SHORT": "Le mot de passe doit faire au moins \(CredentialRules.passwordMinLength) caractères."
        case "PASSWORD_TOO_LONG": "Le mot de passe doit faire au plus \(CredentialRules.passwordMaxLength) caractères."
        case "PASSWORD_TOO_COMMON": "Ce mot de passe est trop courant. Choisissez-en un plus personnel."
        case "PASSWORD_BREACHED": "Ce mot de passe apparaît dans une fuite de données connue. Choisissez-en un autre."
        case "TERMS_NOT_ACCEPTED": "Acceptez les conditions d'utilisation pour continuer."
        case "INVALID_CODE": "Le code contient 6 chiffres."
        case "TOO_LONG": "Ce champ est trop long."
        default: nil
        }
    }
}
