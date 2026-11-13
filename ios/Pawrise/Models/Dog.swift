import Foundation

struct Dog: Codable, Identifiable, Hashable, Sendable {
    enum Sex: String, Codable, Sendable, CaseIterable {
        case female, male
        var label: String { self == .female ? "Femelle" : "Mâle" }
    }

    enum Photo: Codable, Hashable, Sendable {
        /// Photo d'exemple proposée pendant l'onboarding.
        case remote(URL)
        /// Photo choisie dans la photothèque (JPEG compressé).
        case local(Data)
    }

    var id: UUID
    var name: String
    var breed: String
    var birthDate: Date?
    var sex: Sex?
    var weightKg: Double?
    var heightCm: Int?
    var medicalNotes: String?
    var photo: Photo?
    var collar: Collar?

    /// Libellé d'âge comme dans le Figma : « 3 ans », « 8 mois ».
    func ageLabel(now: Date = .now, calendar: Calendar = .current) -> String? {
        guard let birthDate else { return nil }
        let months = calendar.dateComponents([.month], from: birthDate, to: now).month ?? 0
        if months < 12 { return "\(max(0, months)) mois" }
        let years = months / 12
        return years >= 2 ? "\(years) ans" : "1 an"
    }

    var subtitle: String {
        [breed, ageLabel()].compactMap { $0 }.joined(separator: " · ")
    }
}

struct Collar: Codable, Hashable, Sendable {
    enum Signal: String, Codable, Sendable {
        case strong, weak
        var label: String { self == .strong ? "Signal fort" : "Signal faible" }
    }

    var code: String
    var model: String
    var batteryPercent: Int
    var signal: Signal
    var connected: Bool

    /// Format du code imprimé sous le collier : PW- suivi de 6 caractères (US-12).
    static func normalize(_ raw: String) -> String {
        raw.uppercased().filter { !$0.isWhitespace }
    }

    static func isValidCode(_ raw: String) -> Bool {
        normalize(raw).wholeMatch(of: #/PW-[A-Z0-9]{6}/#) != nil
    }
}

enum DogCatalog {
    /// Race affichée quand elle n'est pas connue (« Je ne sais pas », ou liste de races vide côté API).
    static let unknownBreedLabel = "Race inconnue"
    /// Choix « Je ne sais pas » de la liste : aucune race envoyée à l'API.
    static let unknownBreedChoice = "Je ne sais pas"
    /// « Croisé » = race `mixed` de l'API-CORE (contrat, D2).
    static let mixedBreedChoice = "Croisé"
    static let mixedBreedCode = "mixed"

    /// Races proposées à la saisie (ordre du prototype), extensible vers une vraie base.
    static let breeds = [
        "Je ne sais pas", "Croisé", "Akita Inu", "Beagle", "Berger Allemand", "Berger Australien",
        "Border Collie", "Bouledogue Français", "Caniche", "Chihuahua", "Golden Retriever", "Husky Sibérien",
    ]

    /// Photos d'exemple du prototype (Unsplash).
    static let samplePhotos: [URL] = [
        "https://images.unsplash.com/photo-1633722715463-d30f4f325e24?crop=entropy&fit=crop&fm=jpg&w=320&h=320&q=80",
        "https://images.unsplash.com/photo-1602241628512-459cdd3234fe?crop=entropy&fit=crop&fm=jpg&w=320&h=320&q=80",
        "https://images.unsplash.com/photo-1568572933382-74d440642117?crop=entropy&fit=crop&fm=jpg&w=320&h=320&q=80",
        "https://images.unsplash.com/photo-1587300003388-59208cc962cb?crop=entropy&fit=crop&fm=jpg&w=320&h=320&q=80",
    ].compactMap(URL.init(string:))

    /// Nala, le chien du Figma, attribué au compte de démo.
    static var nala: Dog {
        Dog(
            id: UUID(uuidString: "6E1D3C2A-0B5F-4C7E-9A11-2B3C4D5E6F70")!,
            name: "Nala",
            breed: "Golden Retriever",
            birthDate: Calendar.current.date(from: DateComponents(year: 2023, month: 4, day: 12)),
            sex: .female,
            weightKg: 28,
            heightCm: 55,
            medicalNotes: nil,
            // Même photo que la maquette (Golden Retriever assis dans l'herbe, 1re photo d'exemple).
            photo: .remote(samplePhotos[0]),
            collar: Collar(code: "PW-ABC123", model: "Pawrise Collar C2", batteryPercent: 100, signal: .strong, connected: true)
        )
    }
}
