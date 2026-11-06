import Foundation
import Observation

/// Persistance des chiens d'un compte. Aujourd'hui : fichier JSON local.
/// Plus tard : Core API (`/v1/pets`), même interface.
protocol DogRepository: Sendable {
    func loadDogs(ownerID: UUID) async throws -> [Dog]
    func saveDogs(_ dogs: [Dog], ownerID: UUID) async throws
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
}

/// Chiens du compte connecté et chien sélectionné (bouton ⇄ de l'en-tête Santé).
@MainActor
@Observable
final class DogStore {
    private(set) var dogs: [Dog] = []
    private(set) var isLoaded = false
    var selectedDogID: Dog.ID?

    private let repository: DogRepository
    private var ownerID: UUID?

    init(repository: DogRepository) {
        self.repository = repository
    }

    var selectedDog: Dog? {
        dogs.first { $0.id == selectedDogID } ?? dogs.first
    }

    func load(for user: AuthUser) async {
        guard ownerID != user.id || !isLoaded else { return }
        ownerID = user.id
        var loaded = (try? await repository.loadDogs(ownerID: user.id)) ?? []
        // Le compte de démo arrive avec Nala, comme dans le Figma.
        if loaded.isEmpty, user.email == MockAuthService.demoEmail {
            loaded = [DogCatalog.nala]
            try? await repository.saveDogs(loaded, ownerID: user.id)
        }
        dogs = loaded
        selectedDogID = loaded.first?.id
        isLoaded = true
    }

    func add(_ dog: Dog) async throws {
        guard let ownerID else { return }
        dogs.append(dog)
        selectedDogID = dog.id
        try await repository.saveDogs(dogs, ownerID: ownerID)
    }

    func update(_ dog: Dog) async throws {
        guard let ownerID, let index = dogs.firstIndex(where: { $0.id == dog.id }) else { return }
        dogs[index] = dog
        try await repository.saveDogs(dogs, ownerID: ownerID)
    }

    func selectNextDog() {
        guard dogs.count > 1, let current = selectedDog, let index = dogs.firstIndex(of: current) else { return }
        selectedDogID = dogs[(index + 1) % dogs.count].id
    }

    func reset() {
        dogs = []
        selectedDogID = nil
        ownerID = nil
        isLoaded = false
    }
}
