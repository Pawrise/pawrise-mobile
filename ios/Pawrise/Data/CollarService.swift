import Foundation

/// Association d'un collier à un chien. Aujourd'hui simulée ; plus tard via la Core API
/// (vérification du code, liaison au certificat step-ca du collier, premier message MQTT).
protocol CollarService: Sendable {
    func activate(code: String, for dogName: String) async throws -> Collar
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

    func activate(code: String, for dogName: String) async throws -> Collar {
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
