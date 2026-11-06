import Foundation

/// Lecture du contenu du QR code imprimé sur le boîtier du collier.
enum CollarQRCode {
    /// Accepte le code brut (« PW-ABC123 ») ou une URL dont le paramètre `code`
    /// ou le dernier segment du chemin est le code. Retourne le code normalisé, ou `nil`.
    static func code(from payload: String) -> String? {
        let trimmed = payload.trimmingCharacters(in: .whitespacesAndNewlines)
        if Collar.isValidCode(trimmed) { return Collar.normalize(trimmed) }

        guard let components = URLComponents(string: trimmed), components.scheme != nil else { return nil }
        if let query = components.queryItems?.first(where: { $0.name.lowercased() == "code" })?.value,
           Collar.isValidCode(query) {
            return Collar.normalize(query)
        }
        let lastSegment = components.path.split(separator: "/").last.map(String.init) ?? ""
        if let decoded = lastSegment.removingPercentEncoding, Collar.isValidCode(decoded) {
            return Collar.normalize(decoded)
        }
        return nil
    }
}
