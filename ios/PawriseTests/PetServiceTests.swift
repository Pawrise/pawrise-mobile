import Foundation
import Testing
@testable import Pawrise

/// Faux serveur : intercepte les requêtes de l'URLSession de test et répond selon `handler`.
final class StubURLProtocol: URLProtocol, @unchecked Sendable {
    struct Recorded: Sendable {
        let method: String
        let url: URL
        let headers: [String: String]
        let body: Data
    }

    typealias Handler = @Sendable (Recorded) -> (status: Int, body: String, contentType: String)

    private static let lock = NSLock()
    nonisolated(unsafe) private static var handler: Handler?
    nonisolated(unsafe) private static var recorded: [Recorded] = []

    static func start(_ handler: @escaping Handler) {
        lock.withLock {
            self.handler = handler
            recorded = []
        }
    }

    static var requests: [Recorded] { lock.withLock { recorded } }

    static func session() -> URLSession {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubURLProtocol.self]
        return URLSession(configuration: configuration)
    }

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        var body = request.httpBody ?? Data()
        if body.isEmpty, let stream = request.httpBodyStream {
            stream.open()
            var buffer = [UInt8](repeating: 0, count: 16_384)
            while stream.hasBytesAvailable {
                let read = stream.read(&buffer, maxLength: buffer.count)
                if read <= 0 { break }
                body.append(buffer, count: read)
            }
            stream.close()
        }
        let entry = Recorded(
            method: request.httpMethod ?? "GET",
            url: request.url!,
            headers: request.allHTTPHeaderFields ?? [:],
            body: body
        )
        let handler = Self.lock.withLock {
            Self.recorded.append(entry)
            return Self.handler
        }
        let reply = handler?(entry) ?? (status: 404, body: "", contentType: "text/plain")
        let response = HTTPURLResponse(
            url: entry.url, statusCode: reply.status, httpVersion: "HTTP/1.1",
            headerFields: ["Content-Type": reply.contentType]
        )!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data(reply.body.utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}

/// Réponses du contrat (exemples d'openapi.yaml).
enum CoreFixtures {
    static let petID = "3f6c2a1e-8b4d-4c1a-9f2e-1d2c3b4a5e6f"
    static let samples = [Data("sample-1".utf8), Data("sample-2".utf8), Data("sample-3".utf8), Data("sample-4".utf8)]

    static func pet(
        id: String = petID, name: String = "Nala", photoUrl: String? = "https://cdn.pawrise.example/pets/3f6c2a1e.jpg",
        photoSample: String? = nil, sex: String = "female", collar: String? = "PW-C28A4F"
    ) -> String {
        func json(_ value: String?) -> String { value.map { "\"\($0)\"" } ?? "null" }
        return """
        {"id":"\(id)","name":"\(name)","photoUrl":\(json(photoUrl)),"photoSample":\(json(photoSample)),\
        "birthDate":"2022-04-12","sex":"\(sex)","breeds":[{"code":"golden_retriever","name":"Golden Retriever"}],\
        "weightKg":27.4,"heightCm":56,"medicalNotes":null,"myRole":"chief","collarSerial":\(json(collar))}
        """
    }

    static let collar = #"{"serialNumber":"PW-C28A4F","model":"C2","batteryPct":72,"connected":true,"lastSeenAt":"2026-10-30T09:12:04Z","assignedAt":"2026-09-02T18:40:00Z"}"#

    static func problem(_ status: Int, _ code: String, field: String? = nil, fieldCode: String? = nil) -> String {
        if let field, let fieldCode {
            return #"{"status":\#(status),"code":"\#(code)","title":"x","errors":[{"field":"\#(field)","code":"\#(fieldCode)"}]}"#
        }
        return #"{"status":\#(status),"code":"\#(code)","title":"x"}"#
    }

    static func tokens() async -> TokenStore {
        let store = TokenStore(keychain: KeychainStore(service: "fr.pawrise.tests.\(UUID().uuidString)"))
        await store.save(TokenPair(accessToken: "access-1", refreshToken: "refresh-1"))
        return store
    }

    static func service(tokens: TokenStore) -> RemotePetService {
        RemotePetService(client: APIClient(baseURL: URL(string: "http://localhost:3000/v1")!, tokens: tokens, session: StubURLProtocol.session()))
    }

    static func json(_ data: Data) -> [String: Any] {
        (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
    }
}

private let json = "application/json"
private let problemJSON = "application/problem+json"

/// Les suites partagent le faux serveur : elles tournent l'une après l'autre.
@Suite("API-CORE", .serialized)
struct CoreAPITests {}

extension AuthUser {
    static let testUser = AuthUser(
        id: UUID(), firstName: "Alex", lastName: "Martin", email: "alex@example.com",
        phoneNumber: nil, registeredAt: .now, emailVerified: true
    )
}

extension CoreAPITests {
@Suite("Service : chiens, photo, races, collier (contrat openapi.yaml)")
struct PetServiceTests {
    @Test func listsPetsWithBearerTokenAndMapsContract() async throws {
        StubURLProtocol.start { _ in (200, #"{"items":[\#(CoreFixtures.pet())]}"#, json) }
        let pets = try await CoreFixtures.service(tokens: await CoreFixtures.tokens()).listPets()

        let request = try #require(StubURLProtocol.requests.first)
        #expect(request.method == "GET")
        #expect(request.url.absoluteString == "http://localhost:3000/v1/pets")
        #expect(request.headers["Authorization"] == "Bearer access-1")
        #expect(request.headers["Accept"] == "application/json")

        let calendar = Calendar(identifier: .gregorian)
        let dog = try PetMapping.dog(from: try #require(pets.first), samples: CoreFixtures.samples, calendar: calendar)
        #expect(dog.id == UUID(uuidString: CoreFixtures.petID))
        #expect(dog.name == "Nala")
        #expect(dog.breed == "Golden Retriever")
        #expect(dog.sex == .female)
        #expect(dog.weightKg == 27.4)
        #expect(dog.heightCm == 56)
        #expect(dog.photo == .remote(URL(string: "https://cdn.pawrise.example/pets/3f6c2a1e.jpg")!))
        #expect(dog.collar?.code == "PW-C28A4F")
        let birth = calendar.dateComponents([.year, .month, .day], from: try #require(dog.birthDate))
        #expect(birth.year == 2022 && birth.month == 4 && birth.day == 12)
    }

    @Test func mapsSamplePhotosUnknownSexAndMissingBreed() throws {
        let dto = PetDTO(
            id: CoreFixtures.petID, name: "Oslo", photoUrl: nil, photoSample: "sample_3", birthDate: nil, sex: "unknown",
            breeds: [], weightKg: nil, heightCm: nil, medicalNotes: nil, myRole: "secondary", collarSerial: nil
        )
        let dog = try PetMapping.dog(from: dto, samples: CoreFixtures.samples)
        #expect(dog.photo == .local(CoreFixtures.samples[2]))
        #expect(dog.sex == nil)
        #expect(dog.breed == DogCatalog.unknownBreedLabel)
        #expect(dog.collar == nil)

        // Ni photoUrl ni photoSample : l'app affiche sample_1 (contrat).
        let noPhoto = PetDTO(
            id: CoreFixtures.petID, name: "Oslo", photoUrl: nil, photoSample: nil, birthDate: nil, sex: "male",
            breeds: nil, weightKg: nil, heightCm: nil, medicalNotes: nil, myRole: nil, collarSerial: nil
        )
        #expect(try PetMapping.dog(from: noPhoto, samples: CoreFixtures.samples).photo == .local(CoreFixtures.samples[0]))
        #expect(PetMapping.sampleKey(for: .local(CoreFixtures.samples[1]), samples: CoreFixtures.samples) == "sample_2")
        #expect(PetMapping.sampleKey(for: .local(Data("ma-photo".utf8)), samples: CoreFixtures.samples) == nil)
    }

    @Test func mapsCollarDetail() throws {
        let dto = try APIClient.decoder.decode(CollarDTO.self, from: Data(CoreFixtures.collar.utf8))
        let collar = PetMapping.collar(from: dto)
        #expect(collar == Collar(code: "PW-C28A4F", model: "Pawrise Collar C2", batteryPercent: 72, signal: .strong, connected: true))
    }

    @Test func createsPetInCamelCase() async throws {
        StubURLProtocol.start { _ in (201, CoreFixtures.pet(photoUrl: nil), json) }
        let calendar = Calendar(identifier: .gregorian)
        var dog = DogCatalog.nala
        dog.birthDate = calendar.date(from: DateComponents(year: 2023, month: 4, day: 12))
        dog.medicalNotes = "Allergie au poulet."
        let request = PetMapping.createRequest(for: dog, breedCodes: ["golden_retriever"], calendar: calendar)
        _ = try await CoreFixtures.service(tokens: await CoreFixtures.tokens()).createPet(request)

        let sent = try #require(StubURLProtocol.requests.first)
        #expect(sent.method == "POST")
        #expect(sent.url.path == "/v1/pets")
        #expect(sent.headers["Content-Type"] == "application/json")
        let body = CoreFixtures.json(sent.body)
        #expect(body["name"] as? String == "Nala")
        #expect(body["birthDate"] as? String == "2023-04-12")
        #expect(body["sex"] as? String == "female")
        #expect(body["breedCodes"] as? [String] == ["golden_retriever"])
        #expect(body["weightKg"] as? Double == 28)
        #expect(body["heightCm"] as? Double == 55)
        #expect(body["medicalNotes"] as? String == "Allergie au poulet.")
    }

    @Test func patchSendsOnlyChangedFieldsAndNullToClear() throws {
        var old = DogCatalog.nala
        old.medicalNotes = "Allergie"
        var new = old
        new.weightKg = 29
        new.medicalNotes = nil
        new.heightCm = nil
        let request = PetMapping.updateRequest(from: old, to: new, breedCodes: nil)
        let body = CoreFixtures.json(try APIClient.encoder.encode(request))
        #expect(Set(body.keys) == ["weightKg", "medicalNotes", "heightCm"])
        #expect(body["weightKg"] as? Double == 29)
        #expect(body["medicalNotes"] is NSNull)
        #expect(body["heightCm"] is NSNull)
        #expect(PetMapping.updateRequest(from: old, to: old, breedCodes: nil).isEmpty)
    }

    @Test func uploadsPhotoAsMultipart() async throws {
        StubURLProtocol.start { _ in (200, CoreFixtures.pet(), json) }
        let jpeg = Data([0xFF, 0xD8, 0xFF, 0xE0, 0x01, 0x02, 0x03])
        let upload = try #require(PetPhotoUpload(data: jpeg))
        _ = try await CoreFixtures.service(tokens: await CoreFixtures.tokens()).uploadPhoto(petID: CoreFixtures.petID, upload)

        let sent = try #require(StubURLProtocol.requests.first)
        #expect(sent.method == "POST")
        #expect(sent.url.path == "/v1/pets/\(CoreFixtures.petID)/photo")
        #expect(sent.headers["Authorization"] == "Bearer access-1")
        let contentType = try #require(sent.headers["Content-Type"])
        #expect(contentType.hasPrefix("multipart/form-data; boundary="))
        let boundary = String(contentType.dropFirst("multipart/form-data; boundary=".count))
        let text = String(decoding: sent.body, as: UTF8.self)
        #expect(text.hasPrefix("--\(boundary)\r\n"))
        #expect(text.contains("Content-Disposition: form-data; name=\"file\"; filename=\"photo.jpg\"\r\n"))
        #expect(text.contains("Content-Type: image/jpeg\r\n\r\n"))
        #expect(text.hasSuffix("\r\n--\(boundary)--\r\n"))
        #expect(sent.body.range(of: jpeg) != nil)

        #expect(PetPhotoUpload(data: Data([0x89, 0x50, 0x4E, 0x47, 0x0D]))?.mimeType == "image/png")
        #expect(PetPhotoUpload(data: Data("GIF89a".utf8)) == nil)
    }

    @Test func setsSamplePhotoAsJSON() async throws {
        StubURLProtocol.start { _ in (200, CoreFixtures.pet(photoUrl: nil, photoSample: "sample_2"), json) }
        let pet = try await CoreFixtures.service(tokens: await CoreFixtures.tokens()).setSamplePhoto(petID: CoreFixtures.petID, sample: "sample_2")
        let sent = try #require(StubURLProtocol.requests.first)
        #expect(sent.url.path == "/v1/pets/\(CoreFixtures.petID)/photo")
        #expect(sent.headers["Content-Type"] == "application/json")
        #expect(CoreFixtures.json(sent.body) as NSDictionary == ["sample": "sample_2"])
        #expect(pet.photoSample == "sample_2")
    }

    @Test func searchesBreedsWithQuery() async throws {
        StubURLProtocol.start { _ in (200, #"{"items":[{"code":"golden_retriever","name":"Golden Retriever"}]}"#, json) }
        let breeds = try await CoreFixtures.service(tokens: await CoreFixtures.tokens()).listBreeds(query: " Golden Rét ", limit: 20)
        let sent = try #require(StubURLProtocol.requests.first)
        let items = URLComponents(url: sent.url, resolvingAgainstBaseURL: false)?.queryItems ?? []
        #expect(sent.url.path == "/v1/breeds")
        #expect(items == [URLQueryItem(name: "q", value: "Golden Rét"), URLQueryItem(name: "limit", value: "20")])
        #expect(breeds == [BreedDTO(code: "golden_retriever", name: "Golden Retriever")])
    }

    @Test func setsCollarWithNormalizedCode() async throws {
        StubURLProtocol.start { _ in (200, CoreFixtures.collar, json) }
        let collar = try await CoreFixtures.service(tokens: await CoreFixtures.tokens()).setCollar(petID: CoreFixtures.petID, serialNumber: " pw-c28a4f ")
        let sent = try #require(StubURLProtocol.requests.first)
        #expect(sent.method == "PUT")
        #expect(sent.url.path == "/v1/pets/\(CoreFixtures.petID)/collar")
        #expect(CoreFixtures.json(sent.body) as NSDictionary == ["serialNumber": "PW-C28A4F"])
        #expect(collar.batteryPct == 72)
    }

    @Test func sendsHealthScenarioToDevRoute() async throws {
        StubURLProtocol.start { _ in (204, "", json) }
        try await CoreFixtures.service(tokens: await CoreFixtures.tokens()).setHealthScenario(petID: CoreFixtures.petID, .anomaly)
        let sent = try #require(StubURLProtocol.requests.first)
        #expect(sent.method == "PUT")
        #expect(sent.url.absoluteString == "http://localhost:3000/v1/dev/pets/\(CoreFixtures.petID)/scenario")
        #expect(CoreFixtures.json(sent.body) as NSDictionary == ["scenario": "anomaly"])
    }

    @Test func refreshesOnceOn401ThenRetries() async throws {
        StubURLProtocol.start { request in
            request.headers["Authorization"] == "Bearer access-2"
                ? (200, #"{"items":[]}"#, json)
                : (401, CoreFixtures.problem(401, "UNAUTHORIZED"), problemJSON)
        }
        let tokens = await CoreFixtures.tokens()
        await tokens.setRefresher { _ in TokenPair(accessToken: "access-2", refreshToken: "refresh-2") }
        let pets = try await CoreFixtures.service(tokens: tokens).listPets()
        #expect(pets.isEmpty)
        #expect(StubURLProtocol.requests.map { $0.headers["Authorization"] } == ["Bearer access-1", "Bearer access-2"])
    }

    @Test func expiredSessionWhenRefreshFails() async throws {
        StubURLProtocol.start { _ in (401, CoreFixtures.problem(401, "UNAUTHORIZED"), problemJSON) }
        let tokens = await CoreFixtures.tokens()
        await tokens.setRefresher { _ in throw APIError.problem(401, "INVALID_REFRESH_TOKEN") }
        await #expect(throws: APIError.sessionExpired) {
            _ = try await CoreFixtures.service(tokens: tokens).listPets()
        }
    }

    @Test func mapsProblemDetails() async throws {
        StubURLProtocol.start { _ in (404, CoreFixtures.problem(404, "PET_NOT_FOUND"), problemJSON) }
        do {
            _ = try await CoreFixtures.service(tokens: await CoreFixtures.tokens()).updatePet(id: CoreFixtures.petID, UpdatePetRequest(name: "Rex"))
            Issue.record("404 attendu")
        } catch let error as APIError {
            #expect(error.code == "PET_NOT_FOUND")
            #expect(error.localizedDescription.contains("n'existe plus"))
        }
        #expect(StubURLProtocol.requests.first?.method == "PATCH")

        let tooLarge = APIError.problem(413, "FILE_TOO_LARGE")
        #expect(tooLarge.localizedDescription.contains("trop lourde"))
        #expect(PetMapping.collarError(APIError.problem(409, "COLLAR_ALREADY_ASSIGNED")) as? CollarActivationError == .alreadyPaired)
        #expect(PetMapping.collarError(APIError.invalidField("serialNumber", "UNKNOWN_COLLAR")) as? CollarActivationError == .invalidCode)
        #expect(PetMapping.collarError(APIError.network(.timedOut)) as? APIError == .network(.timedOut))
    }
}

}

extension CoreAPITests {
@Suite("DogStore en mode remote")
@MainActor
struct RemoteDogStoreTests {
    private let user = AuthUser.testUser

    private func store(tokens: TokenStore) -> DogStore {
        DogStore(
            backend: .remote(CoreFixtures.service(tokens: tokens)),
            samplePhotos: { CoreFixtures.samples },
            retryDelay: { _ in .milliseconds(10) }
        )
    }

    @Test func loadsDogsAndCollarDetailFromAPI() async throws {
        StubURLProtocol.start { request in
            request.url.path.hasSuffix("/collar") ? (200, CoreFixtures.collar, json) : (200, #"{"items":[\#(CoreFixtures.pet())]}"#, json)
        }
        let dogs = store(tokens: await CoreFixtures.tokens())
        await dogs.load(for: user)
        #expect(dogs.isLoaded)
        #expect(dogs.dogs.map(\.name) == ["Nala"])
        #expect(dogs.selectedDog?.collar?.batteryPercent == 72)
        #expect(Set(StubURLProtocol.requests.map(\.url.path)) == ["/v1/pets", "/v1/pets/\(CoreFixtures.petID)/collar"])
    }

    @Test func retriesLoadingWhenServerIsDown() async throws {
        StubURLProtocol.start { _ in
            StubURLProtocol.requests.count < 3 ? (503, "", "text/plain") : (200, #"{"items":[]}"#, json)
        }
        let dogs = store(tokens: await CoreFixtures.tokens())
        await dogs.load(for: user)
        #expect(dogs.isLoaded)
        #expect(dogs.dogs.isEmpty)
        #expect(StubURLProtocol.requests.count == 3)
    }

    @Test func addCreatesOnceAssociatesCollarAndSendsSamplePhoto() async throws {
        StubURLProtocol.start { request in
            switch (request.method, request.url.path) {
            case ("GET", "/v1/pets"): (200, #"{"items":[]}"#, json)
            case ("GET", "/v1/breeds"): (200, #"{"items":[{"code":"beagle","name":"Beagle"}]}"#, json)
            case ("POST", "/v1/pets"): (201, CoreFixtures.pet(name: "Rex", photoUrl: nil, collar: nil), json)
            case ("PUT", _):
                CoreFixtures.json(request.body)["serialNumber"] as? String == "PW-000000"
                    ? (409, CoreFixtures.problem(409, "COLLAR_ALREADY_ASSIGNED"), problemJSON)
                    : (200, CoreFixtures.collar, json)
            case ("POST", _): (200, CoreFixtures.pet(name: "Rex", photoUrl: nil, photoSample: "sample_2"), json)
            default: (500, "", "text/plain")
            }
        }
        let dogs = store(tokens: await CoreFixtures.tokens())
        await dogs.load(for: user)

        var draft = DogCatalog.nala
        draft.id = UUID()
        draft.name = "Rex"
        draft.breed = "beagle"
        draft.collar = nil
        draft.photo = .local(CoreFixtures.samples[1])

        await #expect(throws: CollarActivationError.alreadyPaired) {
            _ = try await dogs.registerCollar(code: "PW-000000", draft: draft)
        }
        let collar = try await dogs.registerCollar(code: "PW-C28A4F", draft: draft)
        draft.collar = collar
        try await dogs.add(draft)

        let requests = StubURLProtocol.requests
        #expect(requests.filter { $0.method == "POST" && $0.url.path == "/v1/pets" }.count == 1)
        let create = try #require(requests.first { $0.method == "POST" && $0.url.path == "/v1/pets" })
        #expect(CoreFixtures.json(create.body)["breedCodes"] as? [String] == ["beagle"])
        let photo = try #require(requests.first { $0.url.path.hasSuffix("/photo") })
        #expect(CoreFixtures.json(photo.body) as NSDictionary == ["sample": "sample_2"])

        #expect(dogs.dogs.count == 1)
        let added = try #require(dogs.selectedDog)
        #expect(added.id == UUID(uuidString: CoreFixtures.petID))
        #expect(added.photo == .local(CoreFixtures.samples[1]))
        #expect(added.collar?.code == "PW-C28A4F")
    }

    @Test func scenarioIsSentForEveryDog() async throws {
        StubURLProtocol.start { request in
            switch request.method {
            case "GET" where request.url.path == "/v1/pets":
                (200, #"{"items":[\#(CoreFixtures.pet(collar: nil)),\#(CoreFixtures.pet(id: "8b0e7f3c-1a2b-4c3d-9e8f-0a1b2c3d4e5f", name: "Oslo", collar: nil))]}"#, json)
            default: (204, "", json)
            }
        }
        let dogs = store(tokens: await CoreFixtures.tokens())
        await dogs.load(for: user)
        try await dogs.syncHealthScenario(.anomaly)
        let paths = StubURLProtocol.requests.filter { $0.method == "PUT" }.map(\.url.path)
        #expect(Set(paths) == [
            "/v1/dev/pets/\(CoreFixtures.petID)/scenario",
            "/v1/dev/pets/8b0e7f3c-1a2b-4c3d-9e8f-0a1b2c3d4e5f/scenario",
        ])
    }

    @Test func expiredSessionSignsOut() async throws {
        StubURLProtocol.start { _ in (401, CoreFixtures.problem(401, "UNAUTHORIZED"), problemJSON) }
        let tokens = await CoreFixtures.tokens()
        await tokens.setRefresher { _ in throw APIError.problem(401, "INVALID_REFRESH_TOKEN") }
        let dogs = store(tokens: tokens)
        var expired = false
        dogs.onSessionExpired = { expired = true }
        await dogs.load(for: user)
        #expect(expired)
        #expect(!dogs.isLoaded)
    }
}
}
