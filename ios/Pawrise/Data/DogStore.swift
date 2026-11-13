import Foundation
import Observation

/// Persistance locale des chiens d'un compte (fichier JSON), utilisée en mode mock.
/// En mode remote, les chiens viennent de l'API-CORE (`PetService`).
protocol DogRepository: Sendable {
    func loadDogs(ownerID: UUID) async throws -> [Dog]
    func saveDogs(_ dogs: [Dog], ownerID: UUID) async throws
    func deleteDogs(ownerID: UUID) async throws
}

struct LocalDogRepository: DogRepository {
    private var directory: URL {
        URL.applicationSupportDirectory.appending(path: "dogs", directoryHint: .isDirectory)
    }

    func loadDogs(ownerID: UUID) async throws -> [Dog] {
        let file = directory.appending(path: "\(ownerID.uuidString).json")
        guard FileManager.default.fileExists(atPath: file.path()) else { return [] }
        return try JSONDecoder().decode([Dog].self, from: Data(contentsOf: file))
    }

    func saveDogs(_ dogs: [Dog], ownerID: UUID) async throws {
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let file = directory.appending(path: "\(ownerID.uuidString).json")
        try JSONEncoder().encode(dogs).write(to: file, options: [.atomic, .completeFileProtection])
    }

    func deleteDogs(ownerID: UUID) async throws {
        let file = directory.appending(path: "\(ownerID.uuidString).json")
        guard FileManager.default.fileExists(atPath: file.path()) else { return }
        try FileManager.default.removeItem(at: file)
    }
}

/// Chiens du compte connecté et chien sélectionné (bouton ⇄ de l'en-tête Santé).
/// - mode mock : fichier local (`DogRepository`), comme avant ;
/// - mode remote : API-CORE (`PetService`), pour retrouver le même chien et la même photo sur Android et dans le chatbot.
@MainActor
@Observable
final class DogStore {
    enum Backend {
        case local(DogRepository)
        case remote(PetService)
    }

    private(set) var dogs: [Dog] = []
    private(set) var isLoaded = false
    var selectedDogID: Dog.ID?

    /// Appelé quand le refresh de session échoue pendant un appel à l'API-CORE.
    @ObservationIgnored var onSessionExpired: (@MainActor () async -> Void)?

    private let backend: Backend
    @ObservationIgnored private let samplePhotos: () -> [Data]
    @ObservationIgnored private let retryDelay: (Int) -> Duration
    private var ownerID: UUID?

    /// Chien créé sur l'API-CORE à l'activation du collier, pas encore ajouté à la liste (clé : id du brouillon).
    private struct CreatedDraft {
        var pet: PetDTO
        var collar: CollarDTO?
        var dog: Dog
    }

    @ObservationIgnored private var createdDrafts: [Dog.ID: CreatedDraft] = [:]
    /// Codes de race déjà résolus (nom affiché → `breed_code`).
    @ObservationIgnored private var breedCodes: [String: String] = [:]

    convenience init(repository: DogRepository) {
        self.init(backend: .local(repository))
    }

    init(
        backend: Backend,
        samplePhotos: @escaping () -> [Data] = { SampleDogPhotos.all },
        retryDelay: @escaping (Int) -> Duration = { attempt in .seconds(min(30, 1 << min(attempt, 5))) }
    ) {
        self.backend = backend
        self.samplePhotos = samplePhotos
        self.retryDelay = retryDelay
    }

    var isRemote: Bool {
        if case .remote = backend { return true }
        return false
    }

    var selectedDog: Dog? {
        dogs.first { $0.id == selectedDogID } ?? dogs.first
    }

    func load(for user: AuthUser) async {
        guard ownerID != user.id || !isLoaded else { return }
        ownerID = user.id
        switch backend {
        case let .local(repository):
            var loaded = (try? await repository.loadDogs(ownerID: user.id)) ?? []
            // Le compte de démo arrive avec Nala, comme dans le Figma.
            if loaded.isEmpty, user.email == MockAuthService.demoEmail {
                loaded = [DogCatalog.nala]
                try? await repository.saveDogs(loaded, ownerID: user.id)
            }
            finishLoading(loaded)
        case let .remote(service):
            // Hors ligne ou serveur indisponible : l'écran de lancement reste affiché et l'app réessaie,
            // plutôt que de proposer l'ajout d'un premier chien à un compte qui en a peut-être déjà.
            var attempt = 0
            while !Task.isCancelled, ownerID == user.id {
                do {
                    let loaded = try await fetchDogs(service)
                    guard ownerID == user.id else { return }
                    finishLoading(loaded)
                    return
                } catch APIError.sessionExpired {
                    await onSessionExpired?()
                    return
                } catch {
                    try? await Task.sleep(for: retryDelay(attempt))
                    attempt += 1
                }
            }
        }
    }

    private func finishLoading(_ loaded: [Dog]) {
        dogs = loaded
        selectedDogID = loaded.first?.id
        isLoaded = true
    }

    func add(_ dog: Dog) async throws {
        guard let ownerID else { return }
        switch backend {
        case let .local(repository):
            dogs.append(dog)
            selectedDogID = dog.id
            try await repository.saveDogs(dogs, ownerID: ownerID)
        case let .remote(service):
            try await reportingSessionExpiry {
                let added = try await self.addRemote(dog, service: service)
                self.dogs.append(added)
                self.selectedDogID = added.id
            }
        }
    }

    func update(_ dog: Dog) async throws {
        guard let ownerID, let index = dogs.firstIndex(where: { $0.id == dog.id }) else { return }
        switch backend {
        case let .local(repository):
            dogs[index] = dog
            try await repository.saveDogs(dogs, ownerID: ownerID)
        case let .remote(service):
            try await reportingSessionExpiry {
                let updated = try await self.updateRemote(from: self.dogs[index], to: dog, service: service)
                if let current = self.dogs.firstIndex(where: { $0.id == dog.id }) {
                    self.dogs[current] = updated
                }
            }
        }
    }

    /// Mode remote : crée le chien du brouillon (une seule fois) puis lui associe le collier.
    /// Les erreurs du contrat sont traduites en `CollarActivationError` pour l'écran d'activation.
    func registerCollar(code: String, draft: Dog) async throws -> Collar {
        guard case let .remote(service) = backend else {
            throw CollarActivationError.unreachable
        }
        return try await reportingSessionExpiry {
            var created = try await self.ensureCreated(draft, service: service)
            let collar: CollarDTO
            do {
                collar = try await service.setCollar(petID: created.pet.id, serialNumber: code)
            } catch {
                throw PetMapping.collarError(error)
            }
            created.collar = collar
            self.createdDrafts[draft.id] = created
            return PetMapping.collar(from: collar)
        }
    }

    func selectNextDog() {
        guard dogs.count > 1, let current = selectedDog, let index = dogs.firstIndex(of: current) else { return }
        selectedDogID = dogs[(index + 1) % dogs.count].id
    }

    /// Données simulées (Paramètres) : en mode remote, le même scénario est envoyé à l'API-CORE
    /// pour chaque chien du compte (`PUT /v1/dev/pets/{petId}/scenario`), afin que le chatbot le voie aussi.
    func syncHealthScenario(_ scenario: HealthScenario) async throws {
        guard case let .remote(service) = backend else { return }
        let ids = dogs.map { PetMapping.petID($0.id) }
        try await reportingSessionExpiry {
            try await withThrowingTaskGroup(of: Void.self) { group in
                for id in ids {
                    group.addTask { try await service.setHealthScenario(petID: id, scenario) }
                }
                try await group.waitForAll()
            }
        }
    }

    /// Compte supprimé. Mode mock : efface aussi les chiens enregistrés sur cet appareil.
    /// Mode remote : rien n'est stocké sur l'appareil, le serveur supprime les chiens avec le compte.
    func eraseAll(ownerID: UUID) async {
        if case let .local(repository) = backend {
            try? await repository.deleteDogs(ownerID: ownerID)
        }
        reset()
    }

    func reset() {
        dogs = []
        selectedDogID = nil
        ownerID = nil
        isLoaded = false
        createdDrafts = [:]
    }

    // MARK: API-CORE

    private func reportingSessionExpiry<T>(_ operation: () async throws -> T) async throws -> T {
        do {
            return try await operation()
        } catch APIError.sessionExpired {
            await onSessionExpired?()
            throw APIError.sessionExpired
        }
    }

    private func fetchDogs(_ service: PetService) async throws -> [Dog] {
        let pets = try await service.listPets()
        // Batterie, modèle et connexion du collier : `GET /pets/{petId}/collar` (le chien ne porte que le code).
        let collars = await withTaskGroup(of: (String, CollarDTO?).self) { group in
            for pet in pets where pet.collarSerial != nil {
                group.addTask { (pet.id, try? await service.collar(petID: pet.id)) }
            }
            var result: [String: CollarDTO] = [:]
            for await (id, collar) in group {
                if let collar { result[id] = collar }
            }
            return result
        }
        let samples = samplePhotos()
        return try pets.map { try PetMapping.dog(from: $0, collar: collars[$0.id], samples: samples) }
    }

    /// Crée le chien sur l'API-CORE, ou met à jour celui déjà créé pour ce brouillon (retour au récapitulatif).
    private func ensureCreated(_ draft: Dog, service: PetService) async throws -> CreatedDraft {
        if var created = createdDrafts[draft.id] {
            var breedCodes: [String]?
            if draft.breed != created.dog.breed { breedCodes = try await resolveBreedCodes(draft.breed, service: service) }
            let request = PetMapping.updateRequest(from: created.dog, to: draft, breedCodes: breedCodes)
            if !request.isEmpty {
                created.pet = try await service.updatePet(id: created.pet.id, request)
            }
            created.dog = draft
            createdDrafts[draft.id] = created
            return created
        }
        let codes = try await resolveBreedCodes(draft.breed, service: service)
        let pet = try await service.createPet(PetMapping.createRequest(for: draft, breedCodes: codes))
        let created = CreatedDraft(pet: pet, collar: nil, dog: draft)
        createdDrafts[draft.id] = created
        return created
    }

    private func addRemote(_ dog: Dog, service: PetService) async throws -> Dog {
        var created = try await ensureCreated(dog, service: service)
        if let code = dog.collar?.code, created.collar?.serialNumber != code {
            do {
                created.collar = try await service.setCollar(petID: created.pet.id, serialNumber: code)
            } catch {
                throw PetMapping.collarError(error)
            }
            createdDrafts[dog.id] = created
        }
        if let pet = try await sendPhoto(dog.photo, petID: created.pet.id, service: service) {
            created.pet = pet
        }
        let added = try PetMapping.dog(from: created.pet, collar: created.collar, samples: samplePhotos())
        createdDrafts[dog.id] = nil
        return added
    }

    private func updateRemote(from old: Dog, to new: Dog, service: PetService) async throws -> Dog {
        let petID = PetMapping.petID(old.id)
        var breedCodes: [String]?
        if new.breed != old.breed { breedCodes = try await resolveBreedCodes(new.breed, service: service) }
        let request = PetMapping.updateRequest(from: old, to: new, breedCodes: breedCodes)
        var pet: PetDTO?
        if !request.isEmpty {
            pet = try await service.updatePet(id: petID, request)
        }
        if new.photo != old.photo, let changed = try await sendPhoto(new.photo, petID: petID, service: service) {
            pet = changed
        }
        var collar: CollarDTO?
        if let code = new.collar?.code, code != old.collar?.code {
            do {
                collar = try await service.setCollar(petID: petID, serialNumber: code)
            } catch {
                throw PetMapping.collarError(error)
            }
        }
        guard let pet else {
            var result = new
            if let collar { result.collar = PetMapping.collar(from: collar) }
            return result
        }
        var result = try PetMapping.dog(from: pet, collar: collar, samples: samplePhotos())
        if collar == nil { result.collar = new.collar }
        return result
    }

    /// Photo d'exemple → clé `sample_N` (JSON) ; photo personnelle → multipart (déjà réduite par `DogPhotoCompressor`).
    /// Rien à envoyer pour une photo déjà hébergée (`photoUrl`) ou absente.
    private func sendPhoto(_ photo: Dog.Photo?, petID: String, service: PetService) async throws -> PetDTO? {
        guard case let .local(data) = photo else { return nil }
        if let key = PetMapping.sampleKey(for: photo, samples: samplePhotos()) {
            return try await service.setSamplePhoto(petID: petID, sample: key)
        }
        var upload = PetPhotoUpload(data: data)
        if upload == nil || data.count > PetPhotoUpload.maxBytes {
            upload = DogPhotoCompressor.jpeg(from: data).flatMap(PetPhotoUpload.init(data:))
        }
        guard let upload, upload.data.count <= PetPhotoUpload.maxBytes else {
            throw APIError.problem(413, "FILE_TOO_LARGE")
        }
        return try await service.uploadPhoto(petID: petID, upload)
    }

    /// Race saisie → codes de `GET /breeds` (race principale en premier). « Je ne sais pas » = liste vide ;
    /// un nom absent du référentiel (texte libre) est envoyé comme race inconnue, le contrat n'acceptant que des codes.
    private func resolveBreedCodes(_ breed: String, service: PetService) async throws -> [String] {
        let name = breed.trimmingCharacters(in: .whitespacesAndNewlines)
        if name.isEmpty || name == DogCatalog.unknownBreedChoice || name == DogCatalog.unknownBreedLabel { return [] }
        if name == DogCatalog.mixedBreedChoice { return [DogCatalog.mixedBreedCode] }
        let key = name.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: .current)
        if let code = breedCodes[key] { return [code] }
        let matches = try await service.listBreeds(query: name, limit: 20)
        for match in matches {
            breedCodes[match.name.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: .current)] = match.code
        }
        return breedCodes[key].map { [$0] } ?? []
    }
}
