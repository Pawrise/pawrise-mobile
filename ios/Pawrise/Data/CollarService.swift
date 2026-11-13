import Foundation

/// Association d'un collier au chien en cours d'ajout (`dog` : profil saisi, sans collier).
/// Simulée en mode mock ; en mode remote, `PUT /v1/pets/{petId}/collar` sur l'API-CORE.
protocol CollarService: Sendable {
    func activate(code: String, for dog: Dog) async throws -> Collar
}

/// Le contrat associe le collier à un chien existant : le chien est d'abord créé sur l'API-CORE
/// (une seule fois par brouillon), puis le collier lui est associé. Voir `DogStore.registerCollar`.
struct RemoteCollarService: CollarService {
    let store: DogStore

    func activate(code: String, for dog: Dog) async throws -> Collar {
        try await store.registerCollar(code: code, draft: dog)
    }
}

enum CollarActivationError: LocalizedError, Equatable {
    case invalidCode
    case alreadyPaired
    case unreachable

    var errorDescription: String? {
        switch self {
        case .invalidCode:
            "Ce code n'est pas reconnu. Il commence par PW- suivi de 6 caractères, et se trouve sous le collier."
        case .alreadyPaired:
            "Ce collier est déjà associé à un autre chien. Retirez-le de l'autre profil ou contactez le support."
        case .unreachable:
            "Le collier ne répond pas. Vérifiez que son voyant est allumé et qu'il est à portée, puis réessayez."
        }
    }
}

struct MockCollarService: CollarService {
    /// Codes réservés pour tester les cas d'erreur à la main.
    static let alreadyPairedCode = "PW-000000"
    static let unreachableCode = "PW-999999"

    var latency: Duration = .milliseconds(1500)

    func activate(code: String, for dog: Dog) async throws -> Collar {
        let normalized = Collar.normalize(code)
        guard Collar.isValidCode(normalized) else { throw CollarActivationError.invalidCode }
        try await Task.sleep(for: latency)
        switch normalized {
        case Self.alreadyPairedCode: throw CollarActivationError.alreadyPaired
        case Self.unreachableCode: throw CollarActivationError.unreachable
        default: return Collar(code: normalized, model: "Pawrise Collar C2", batteryPercent: 100, signal: .strong, connected: true)
        }
    }
}
