import Foundation
import Observation

/// État et règles du parcours « Ajouter un chien » : 9 étapes de profil, puis association du collier.
@MainActor
@Observable
final class AddDogFlowModel {
    enum Step: Int, CaseIterable, Comparable, Sendable {
        case photo, name, birthDate, sex, breed, weight, height, medical, summary

        /// Position 1-based pour `StepProgressBar`.
        var number: Int { rawValue + 1 }

        static func < (lhs: Step, rhs: Step) -> Bool { lhs.rawValue < rhs.rawValue }
    }

    enum Phase: Equatable, Sendable {
        case profile, collarIntro, scan, manual, activating, success
    }

    static let nameMaxLength = 40
    static let maxWeightKg: Double = 120
    static let maxHeightCm = 120
    /// Âge maximal raisonnable pour une date de naissance (évite les saisies aberrantes).
    static let maxAgeYears = 30
    static let unknownBreedLabel = DogCatalog.unknownBreedLabel

    // MARK: Navigation

    private(set) var step: Step = .photo
    private(set) var phase: Phase = .profile
    /// Vrai quand l’utilisateur modifie une valeur depuis le récapitulatif : « Suivant » y ramène.
    private(set) var isEditingFromSummary = false

    // MARK: Profil

    var photo: Dog.Photo?
    var name = ""
    /// Date de naissance, déduite de la saisie « JJ / MM / AAAA » (`birthDateText`).
    var birthDate: Date?
    /// Saisie de la date de naissance au format de la maquette A10 : « 12 / 04 / 2023 ».
    var birthDateText = "" {
        didSet { birthDate = Self.parseBirthDate(birthDateText, calendar: calendar) }
    }
    var sex: Dog.Sex?
    /// Texte saisi dans le champ de recherche de race.
    var breedQuery = ""
    /// Race choisie dans la liste (prioritaire sur le texte libre).
    var selectedBreed: String?
    var weightText = ""
    var heightText = ""
    var medicalNotes = ""

    // MARK: Collier

    var collarCode = ""
    private(set) var collarError: String?
    private var failedCollarCode: String?
    private(set) var collar: Collar?
    private(set) var isSaving = false
    /// Identifiant du brouillon : le même à chaque essai, pour ne pas créer deux fois le chien sur l'API-CORE.
    let draftID = UUID()
    private(set) var saveError: String?

    private let now: () -> Date
    private let calendar: Calendar

    init(now: @escaping () -> Date = { .now }, calendar: Calendar = .current) {
        self.now = now
        self.calendar = calendar
    }

    // MARK: Valeurs dérivées

    var trimmedName: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }

    /// Nom utilisé dans les titres (« Quand est né Nala ? »).
    var displayName: String { trimmedName.isEmpty ? "votre chien" : trimmedName }

    var breed: String? {
        if let selectedBreed { return selectedBreed }
        let typed = breedQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        return typed.isEmpty ? nil : typed
    }

    var breedSuggestions: [String] {
        let query = breedQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return DogCatalog.breeds }
        return DogCatalog.breeds.filter {
            $0.range(of: query, options: [.caseInsensitive, .diacriticInsensitive]) != nil
        }
    }

    var weightKg: Double? { Self.parseDecimal(weightText) }
    var heightCm: Int? { Int(heightText.trimmingCharacters(in: .whitespaces)) }

    var birthDateRange: ClosedRange<Date> {
        let today = now()
        let earliest = calendar.date(byAdding: .year, value: -Self.maxAgeYears, to: today) ?? today
        return earliest...today
    }

    var trimmedMedicalNotes: String { medicalNotes.trimmingCharacters(in: .whitespacesAndNewlines) }

    // MARK: Validation

    var isNameValid: Bool { !trimmedName.isEmpty && trimmedName.count <= Self.nameMaxLength }

    var isBirthDateValid: Bool {
        if birthDateText.isEmpty { return true }
        guard let birthDate else { return false }
        return birthDateRange.contains(birthDate)
    }

    /// Vrai quand la date est entièrement saisie mais impossible ou hors de la plage acceptée.
    var showsBirthDateError: Bool {
        birthDateText.filter(\.isNumber).count == 8 && !isBirthDateValid
    }

    var isWeightValid: Bool {
        if weightText.trimmingCharacters(in: .whitespaces).isEmpty { return true }
        guard let weightKg else { return false }
        return weightKg > 0 && weightKg <= Self.maxWeightKg
    }

    var isHeightValid: Bool {
        if heightText.trimmingCharacters(in: .whitespaces).isEmpty { return true }
        guard let heightCm else { return false }
        return heightCm > 0 && heightCm <= Self.maxHeightCm
    }

    func isValid(_ step: Step) -> Bool {
        switch step {
        case .photo, .breed, .medical, .summary: true
        case .name: isNameValid
        case .birthDate: isBirthDateValid
        case .sex: sex != nil
        case .weight: isWeightValid
        case .height: isHeightValid
        }
    }

    var canAdvance: Bool { isValid(step) }

    var isCollarCodeValid: Bool { Collar.isValidCode(collarCode) }

    // MARK: Actions profil

    func advance() {
        guard phase == .profile, canAdvance else { return }
        if step == .summary {
            phase = .collarIntro
        } else if isEditingFromSummary {
            isEditingFromSummary = false
            step = .summary
        } else if let next = Step(rawValue: step.rawValue + 1) {
            step = next
        }
    }

    /// Ouvre une étape depuis le récapitulatif.
    func edit(_ target: Step) {
        guard phase == .profile, target != .summary else { return }
        isEditingFromSummary = true
        step = target
    }

    var canGoBack: Bool {
        switch phase {
        case .profile: step != .photo
        case .collarIntro, .scan, .manual, .activating, .success: true
        }
    }

    func goBack() {
        switch phase {
        case .profile:
            if isEditingFromSummary, canAdvance {
                isEditingFromSummary = false
                step = .summary
            } else if let previous = Step(rawValue: step.rawValue - 1) {
                step = previous
            }
        case .collarIntro:
            phase = .profile
            step = .summary
        case .scan, .manual:
            collarError = nil
            failedCollarCode = nil
            phase = .collarIntro
        case .activating, .success:
            // A19 : annule l’activation ; A20 : le collier activé est oublié. Retour à « Associer le collier ».
            collar = nil
            collarError = nil
            failedCollarCode = nil
            phase = .collarIntro
        }
    }

    func selectBreed(_ breed: String) {
        selectedBreed = breed
    }

    /// Appelé quand l’utilisateur modifie le texte : la sélection précédente ne correspond plus.
    func breedQueryDidChange() {
        if let selectedBreed, selectedBreed != breedQuery {
            self.selectedBreed = nil
        }
    }

    // MARK: Actions collier

    func startScan() {
        collarError = nil
        failedCollarCode = nil
        phase = .scan
    }

    func startManualEntry() {
        phase = .manual
    }

    /// Efface l’erreur dès que l’utilisateur saisit un autre code que celui refusé.
    func collarCodeDidChange() {
        guard Collar.normalize(collarCode) != failedCollarCode else { return }
        collarError = nil
        failedCollarCode = nil
    }

    /// Lance l’activation. En cas d’échec, retour à la saisie manuelle avec le code prérempli.
    func activate(code rawCode: String, using service: CollarService, existingCodes: Set<String>) async {
        guard phase != .activating else { return }
        let code = Collar.normalize(rawCode)
        collarCode = code
        guard Collar.isValidCode(code) else {
            fail(with: CollarActivationError.invalidCode)
            return
        }
        guard !existingCodes.contains(code) else {
            fail(with: CollarActivationError.alreadyPaired)
            return
        }
        collarError = nil
        failedCollarCode = nil
        phase = .activating
        do {
            let activated = try await service.activate(code: code, for: makeDraft())
            // L’utilisateur a pu revenir en arrière pendant l’activation : résultat ignoré.
            guard phase == .activating else { return }
            collar = activated
            phase = .success
        } catch is CancellationError {
            if phase == .activating { phase = .manual }
        } catch {
            guard phase == .activating else { return }
            fail(with: error)
        }
    }

    private func fail(with error: Error) {
        collarError = (error as? LocalizedError)?.errorDescription
            ?? "L’activation a échoué. Vérifiez votre connexion puis réessayez."
        failedCollarCode = Collar.normalize(collarCode)
        phase = .manual
    }

    // MARK: Création du chien

    /// Chien complet, disponible une fois le collier activé.
    func makeDog() -> Dog? {
        guard let collar else { return nil }
        var dog = makeDraft()
        dog.collar = collar
        return isNameValid ? dog : nil
    }

    /// Profil saisi jusqu'ici, sans collier (envoyé à l'activation du collier).
    func makeDraft() -> Dog {
        Dog(
            id: draftID,
            name: trimmedName,
            breed: breed ?? Self.unknownBreedLabel,
            birthDate: birthDate.map { calendar.startOfDay(for: $0) },
            sex: sex,
            weightKg: isWeightValid ? weightKg : nil,
            heightCm: isHeightValid ? heightCm : nil,
            medicalNotes: trimmedMedicalNotes.isEmpty ? nil : trimmedMedicalNotes,
            photo: photo,
            collar: nil
        )
    }

    /// Ajoute le chien au compte. Retourne `true` en cas de succès.
    func save(to store: DogStore) async -> Bool {
        guard !isSaving, let dog = makeDog() else { return false }
        isSaving = true
        saveError = nil
        defer { isSaving = false }
        do {
            try await store.add(dog)
            return true
        } catch {
            saveError = "Le profil n’a pas pu être enregistré. Réessayez."
            return false
        }
    }

    // MARK: Formats

    static let frenchLocale = Locale(identifier: "fr_FR")

    /// Accepte « 28 », « 28,5 » ou « 28.5 ».
    static func parseDecimal(_ text: String) -> Double? {
        let cleaned = text.trimmingCharacters(in: .whitespaces).replacingOccurrences(of: ",", with: ".")
        guard !cleaned.isEmpty else { return nil }
        return Double(cleaned)
    }

    /// Garde uniquement chiffres et un séparateur décimal, une décimale au plus.
    static func sanitizeDecimalInput(_ text: String) -> String {
        var result = ""
        var hasSeparator = false
        var decimals = 0
        for character in text {
            if character.isASCII, character.isNumber {
                if hasSeparator {
                    guard decimals < 1 else { continue }
                    decimals += 1
                }
                result.append(character)
            } else if (character == "," || character == "."), !hasSeparator, !result.isEmpty {
                hasSeparator = true
                result.append(",")
            }
        }
        return String(result.prefix(5))
    }

    /// Garde 8 chiffres au plus et les présente « JJ / MM / AAAA » au fil de la saisie.
    static func formatBirthDateInput(_ text: String) -> String {
        let digits = Array(text.filter { $0.isASCII && $0.isNumber }.prefix(8))
        var result = ""
        for (index, digit) in digits.enumerated() {
            if index == 2 || index == 4 { result += " / " }
            result.append(digit)
        }
        return result
    }

    /// « 12 / 04 / 2023 » → 12 avril 2023 ; nil si incomplète ou impossible (31 / 02…).
    static func parseBirthDate(_ text: String, calendar: Calendar) -> Date? {
        let digits = text.filter { $0.isASCII && $0.isNumber }
        guard digits.count == 8,
              let day = Int(digits.prefix(2)),
              let month = Int(digits.dropFirst(2).prefix(2)),
              let year = Int(digits.suffix(4)) else { return nil }
        let components = DateComponents(year: year, month: month, day: day)
        guard let date = calendar.date(from: components) else { return nil }
        let check = calendar.dateComponents([.year, .month, .day], from: date)
        guard check.year == year, check.month == month, check.day == day else { return nil }
        return date
    }

    static func sanitizeIntegerInput(_ text: String) -> String {
        String(text.filter { $0.isASCII && $0.isNumber }.prefix(3))
    }

    func formattedBirthDate() -> String? {
        birthDate?.formatted(Date.FormatStyle(date: .long, time: .omitted).locale(Self.frenchLocale))
    }

    func formattedWeight() -> String? {
        guard let weightKg, isWeightValid else { return nil }
        let number = weightKg.formatted(.number.precision(.fractionLength(0...1)).locale(Self.frenchLocale))
        return "\(number) kg"
    }

    func formattedHeight() -> String? {
        guard let heightCm, isHeightValid else { return nil }
        return "\(heightCm) cm"
    }
}
