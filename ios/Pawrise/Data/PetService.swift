import Foundation

// Schémas « Animaux » et « Colliers » de l'API-CORE (pawrise-doc/contrat-interface/openapi.yaml, JSON camelCase).
// Même lecture du contrat que l'app Android (core/pets/data/PetDtos.kt).

struct BreedDTO: Codable, Sendable, Equatable {
    let code: String
    let name: String
}

struct PetDTO: Decodable, Sendable, Equatable {
    let id: String
    let name: String
    /// Ma propre photo, hébergée par Pawrise.
    let photoUrl: String?
    /// Clé d'une photo d'exemple embarquée (`sample_1` … `sample_4`).
    let photoSample: String?
    /// Date calendaire `YYYY-MM-DD` (convention 1).
    let birthDate: String?
    /// `male`, `female` ou `unknown`.
    let sex: String
    /// Race principale en premier ; vide = race inconnue.
    let breeds: [BreedDTO]?
    let weightKg: Double?
    let heightCm: Double?
    let medicalNotes: String?
    let myRole: String?
    let collarSerial: String?
}

struct CollarDTO: Decodable, Sendable, Equatable {
    let serialNumber: String
    /// `collar.hardware_version` : l'app affiche « Pawrise Collar <model> ».
    let model: String
    let batteryPct: Int?
    let connected: Bool
    let lastSeenAt: Date?
    let assignedAt: Date?
}

struct ItemList<Item: Decodable & Sendable>: Decodable, Sendable {
    let items: [Item]
}

struct CreatePetRequest: Encodable, Sendable, Equatable {
    let name: String
    let birthDate: String?
    let sex: String
    let breedCodes: [String]
    let weightKg: Double?
    let heightCm: Double?
    let medicalNotes: String?
}

/// Champ de PATCH (convention 9) : absent = inchangé, `.clear` = `null` (effacé), `.set` = remplacé.
enum PatchValue<Value: Encodable & Sendable & Equatable>: Sendable, Equatable {
    case set(Value)
    case clear
}

struct UpdatePetRequest: Encodable, Sendable, Equatable {
    var name: String?
    var birthDate: PatchValue<String>?
    var sex: String?
    var breedCodes: [String]?
    /// Ajoute une pesée ; `null` est refusé par l'API, donc jamais effacé.
    var weightKg: Double?
    var heightCm: PatchValue<Double>?
    var medicalNotes: PatchValue<String>?

    var isEmpty: Bool { self == UpdatePetRequest() }

    private enum CodingKeys: String, CodingKey {
        case name, birthDate, sex, breedCodes, weightKg, heightCm, medicalNotes
    }

    func encode(to encoder: any Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encodeIfPresent(name, forKey: .name)
        try encode(birthDate, .birthDate, in: &container)
        try container.encodeIfPresent(sex, forKey: .sex)
        try container.encodeIfPresent(breedCodes, forKey: .breedCodes)
        try container.encodeIfPresent(weightKg, forKey: .weightKg)
        try encode(heightCm, .heightCm, in: &container)
        try encode(medicalNotes, .medicalNotes, in: &container)
    }

    private func encode<Value>(
        _ value: PatchValue<Value>?, _ key: CodingKeys, in container: inout KeyedEncodingContainer<CodingKeys>
    ) throws {
        switch value {
        case nil: break
        case .clear: try container.encodeNil(forKey: key)
        case let .set(wrapped): try container.encode(wrapped, forKey: key)
        }
    }
}

struct SamplePhotoRequest: Encodable, Sendable {
    let sample: String
}

struct SetCollarRequest: Encodable, Sendable {
    let serialNumber: String
}

/// Route de développement de l'API-CORE (hors openapi.yaml) : scénario des données de santé simulées,
/// pour que pawrise-assistant voie le même que l'app (Paramètres → Données simulées).
struct HealthScenarioRequest: Encodable, Sendable {
    let scenario: String
}

/// Photo envoyée en multipart : JPEG ou PNG, 1,5 Mo maximum (contrat).
struct PetPhotoUpload: Sendable, Equatable {
    static let maxBytes = 1_500_000

    let data: Data
    let mimeType: String

    /// Reconnaît JPEG et PNG à leurs premiers octets ; nil pour tout autre format.
    init?(data: Data) {
        if data.starts(with: [0xFF, 0xD8, 0xFF]) {
            mimeType = "image/jpeg"
        } else if data.starts(with: [0x89, 0x50, 0x4E, 0x47]) {
            mimeType = "image/png"
        } else {
            return nil
        }
        self.data = data
    }

    var fileName: String { mimeType == "image/png" ? "photo.png" : "photo.jpg" }

    /// Corps `multipart/form-data` avec le seul champ `file`.
    func multipartBody(boundary: String = "pawrise-\(UUID().uuidString)") -> Endpoint.RawBody {
        var body = Data()
        body.append(Data("--\(boundary)\r\n".utf8))
        body.append(Data("Content-Disposition: form-data; name=\"file\"; filename=\"\(fileName)\"\r\n".utf8))
        body.append(Data("Content-Type: \(mimeType)\r\n\r\n".utf8))
        body.append(data)
        body.append(Data("\r\n--\(boundary)--\r\n".utf8))
        return Endpoint.RawBody(data: body, contentType: "multipart/form-data; boundary=\(boundary)")
    }
}

/// Chiens, races et collier sur l'API-CORE (`/v1`).
protocol PetService: Sendable {
    func listPets() async throws -> [PetDTO]
    func createPet(_ request: CreatePetRequest) async throws -> PetDTO
    func updatePet(id: String, _ request: UpdatePetRequest) async throws -> PetDTO
    func uploadPhoto(petID: String, _ photo: PetPhotoUpload) async throws -> PetDTO
    func setSamplePhoto(petID: String, sample: String) async throws -> PetDTO
    func listBreeds(query: String?, limit: Int) async throws -> [BreedDTO]
    func collar(petID: String) async throws -> CollarDTO
    func setCollar(petID: String, serialNumber: String) async throws -> CollarDTO
    func setHealthScenario(petID: String, _ scenario: HealthScenario) async throws
}

struct RemotePetService: PetService {
    let client: APIClient

    func listPets() async throws -> [PetDTO] {
        try await client.send(.get("pets"), as: ItemList<PetDTO>.self).items
    }

    func createPet(_ request: CreatePetRequest) async throws -> PetDTO {
        try await client.send(.post("pets", body: request))
    }

    func updatePet(id: String, _ request: UpdatePetRequest) async throws -> PetDTO {
        try await client.send(.patch("pets/\(id)", body: request))
    }

    func uploadPhoto(petID: String, _ photo: PetPhotoUpload) async throws -> PetDTO {
        var endpoint = Endpoint.post("pets/\(petID)/photo")
        endpoint.rawBody = photo.multipartBody()
        return try await client.send(endpoint)
    }

    func setSamplePhoto(petID: String, sample: String) async throws -> PetDTO {
        try await client.send(.post("pets/\(petID)/photo", body: SamplePhotoRequest(sample: sample)))
    }

    func listBreeds(query: String?, limit: Int = 50) async throws -> [BreedDTO] {
        var endpoint = Endpoint.get("breeds")
        let trimmed = query?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !trimmed.isEmpty { endpoint.query.append(URLQueryItem(name: "q", value: String(trimmed.prefix(100)))) }
        endpoint.query.append(URLQueryItem(name: "limit", value: String(min(max(limit, 1), 100))))
        return try await client.send(endpoint, as: ItemList<BreedDTO>.self).items
    }

    func collar(petID: String) async throws -> CollarDTO {
        try await client.send(.get("pets/\(petID)/collar"))
    }

    func setCollar(petID: String, serialNumber: String) async throws -> CollarDTO {
        try await client.send(.put("pets/\(petID)/collar", body: SetCollarRequest(serialNumber: Collar.normalize(serialNumber))))
    }

    func setHealthScenario(petID: String, _ scenario: HealthScenario) async throws {
        try await client.sendWithoutResponse(.put("dev/pets/\(petID)/scenario", body: HealthScenarioRequest(scenario: scenario.rawValue)))
    }
}

/// Conversion entre les objets du contrat et le modèle de l'app.
enum PetMapping {
    static let sampleKeys = ["sample_1", "sample_2", "sample_3", "sample_4"]

    /// Identifiant du chien dans les routes (`/pets/{petId}`) : UUID en minuscules.
    static func petID(_ id: Dog.ID) -> String { id.uuidString.lowercased() }

    /// Date calendaire `YYYY-MM-DD`, lue et écrite dans le fuseau du téléphone (le jour saisi reste le jour saisi).
    static func calendarDateFormatter(calendar: Calendar = .current) -> DateFormatter {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = calendar.timeZone
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }

    static func wireSex(_ sex: Dog.Sex?) -> String { sex?.rawValue ?? "unknown" }

    /// Photo d'exemple embarquée → sa clé `sample_N` ; nil pour une photo personnelle.
    static func sampleKey(for photo: Dog.Photo?, samples: [Data]) -> String? {
        guard case let .local(data) = photo, let index = samples.firstIndex(of: data), index < sampleKeys.count else { return nil }
        return sampleKeys[index]
    }

    static func photo(url: String?, sample: String?, samples: [Data]) -> Dog.Photo? {
        if let url, let parsed = URL(string: url) { return .remote(parsed) }
        // `photoUrl` et `photoSample` à null : l'app affiche `sample_1` (contrat, schéma Pet).
        let index = sample.flatMap { sampleKeys.firstIndex(of: $0) } ?? 0
        return samples.indices.contains(index) ? .local(samples[index]) : nil
    }

    static func collar(from dto: CollarDTO) -> Collar {
        Collar(
            code: dto.serialNumber,
            model: dto.model.hasPrefix("Pawrise") ? dto.model : "Pawrise Collar \(dto.model)",
            // Pas encore de mesure reçue : batterie affichée pleine, comme les données simulées de Santé.
            batteryPercent: dto.batteryPct ?? 100,
            signal: dto.connected ? .strong : .weak,
            connected: dto.connected
        )
    }

    /// Collier connu seulement par son code (détail indisponible) : mêmes valeurs par défaut que l'activation.
    static func collar(serial: String) -> Collar {
        Collar(code: serial, model: "Pawrise Collar", batteryPercent: 100, signal: .strong, connected: true)
    }

    static func dog(from dto: PetDTO, collar: CollarDTO? = nil, samples: [Data], calendar: Calendar = .current) throws -> Dog {
        guard let id = UUID(uuidString: dto.id) else {
            throw APIError.decoding("Identifiant de chien invalide : \(dto.id)")
        }
        let birthDate = dto.birthDate.flatMap { calendarDateFormatter(calendar: calendar).date(from: $0) }
        return Dog(
            id: id,
            name: dto.name,
            breed: dto.breeds?.first?.name ?? DogCatalog.unknownBreedLabel,
            birthDate: birthDate.map { calendar.startOfDay(for: $0) },
            sex: Dog.Sex(rawValue: dto.sex),
            weightKg: dto.weightKg,
            heightCm: dto.heightCm.map { Int($0.rounded()) },
            medicalNotes: dto.medicalNotes,
            photo: photo(url: dto.photoUrl, sample: dto.photoSample, samples: samples),
            collar: collar.map(Self.collar(from:)) ?? dto.collarSerial.map(Self.collar(serial:))
        )
    }

    static func createRequest(for dog: Dog, breedCodes: [String], calendar: Calendar = .current) -> CreatePetRequest {
        CreatePetRequest(
            name: dog.name.trimmingCharacters(in: .whitespacesAndNewlines),
            birthDate: dog.birthDate.map { calendarDateFormatter(calendar: calendar).string(from: $0) },
            sex: wireSex(dog.sex),
            breedCodes: breedCodes,
            weightKg: dog.weightKg,
            heightCm: dog.heightCm.map(Double.init),
            medicalNotes: dog.medicalNotes.flatMap { $0.isEmpty ? nil : $0 }
        )
    }

    /// Champs du profil qui ont changé (`breedCodes` : nil si la race n'a pas changé).
    static func updateRequest(from old: Dog, to new: Dog, breedCodes: [String]?, calendar: Calendar = .current) -> UpdatePetRequest {
        let formatter = calendarDateFormatter(calendar: calendar)
        var request = UpdatePetRequest()
        if new.name != old.name { request.name = new.name.trimmingCharacters(in: .whitespacesAndNewlines) }
        if new.birthDate != old.birthDate {
            request.birthDate = new.birthDate.map { .set(formatter.string(from: $0)) } ?? .clear
        }
        if new.sex != old.sex { request.sex = wireSex(new.sex) }
        if new.breed != old.breed { request.breedCodes = breedCodes ?? [] }
        if new.weightKg != old.weightKg, let weight = new.weightKg { request.weightKg = weight }
        if new.heightCm != old.heightCm { request.heightCm = new.heightCm.map { .set(Double($0)) } ?? .clear }
        if new.medicalNotes != old.medicalNotes {
            request.medicalNotes = new.medicalNotes.flatMap { $0.isEmpty ? nil : PatchValue.set($0) } ?? .clear
        }
        return request
    }

    /// Erreurs de `PUT /pets/{petId}/collar` traduites dans celles de l'écran d'activation.
    static func collarError(_ error: Error) -> Error {
        guard let api = error as? APIError else { return error }
        if api.code == "COLLAR_ALREADY_ASSIGNED" { return CollarActivationError.alreadyPaired }
        if api.fieldErrors.contains(where: { ["UNKNOWN_COLLAR", "INVALID_COLLAR_CODE"].contains($0.code) }) {
            return CollarActivationError.invalidCode
        }
        return error
    }
}
