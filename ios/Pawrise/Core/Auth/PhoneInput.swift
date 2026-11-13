import Foundation
import PhoneNumberKit

/// Téléphone facultatif de l'inscription, avec PhoneNumberKit (métadonnées libphonenumber de Google).
/// Région par défaut : France (« 06 12 34 56 78 »). Les numéros internationaux (« +32 … », « 0032 … ») sont acceptés.
/// Le contrat attend le format E.164 (`+33612345678`) ; pawrise-auth refuse le reste avec `INVALID_PHONE`.
enum PhoneInput {
    static let defaultRegion = "FR"
    /// Exemple affiché en placeholder.
    static let placeholder = "06 12 34 56 78"

    /// Chargement des métadonnées coûteux : une seule instance, protégée par un verrou (PhoneNumberKit n'est pas Sendable).
    nonisolated(unsafe) private static let utility = PhoneNumberUtility()
    private static let lock = NSLock()

    /// Numéro valide converti en E.164, ou nil s'il n'est pas reconnu.
    static func e164(_ raw: String) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        // Aucun indicatif pays ne commence par 0 : libphonenumber lirait « +06… » comme un numéro national.
        if trimmed.hasPrefix("+"), trimmed.dropFirst().first(where: \.isNumber) == "0" { return nil }
        return lock.withLock {
            guard let number = try? utility.parse(trimmed, withRegion: defaultRegion),
                  utility.isValidPhoneNumber(trimmed, withRegion: defaultRegion) else { return nil }
            return utility.format(number, toType: .e164)
        }
    }

    /// Mise en forme pendant la saisie : « 0612345678 » → « 06 12 34 56 78 », « +32470123456 » → « +32 470 12 34 56 ».
    static func formatWhileTyping(_ raw: String) -> String {
        lock.withLock {
            PartialFormatter(utility: utility, defaultRegion: defaultRegion, withPrefix: true).formatPartial(raw)
        }
    }
}
