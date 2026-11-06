import Foundation

// Contrat de pawrise-assistant (src/pawrise_assistant/domain/models.py, schema_version "1").
// L'assistant ne garde rien : l'app envoie l'historique complet à chaque tour.

struct AssistantTurn: Codable, Sendable, Equatable {
    enum Role: String, Codable, Sendable { case owner, assistant }
    let role: Role
    let content: String
}

struct AssistantAlertContext: Codable, Sendable, Equatable {
    enum Kind: String, Codable, Sendable { case activityDrop = "activity_drop", nightHeartRate = "night_heart_rate", geofenceExit = "geofence_exit", other }
    enum Level: String, Codable, Sendable { case info, vigilance, action }
    let alertId: String
    let kind: Kind
    let level: Level
    let summary: String
    var persistedDays: Int = 0
    var deltaPct: Double?
}

struct AssistantTurnRequest: Encodable, Sendable {
    let threadId: String
    let turnId: String
    let petRef: String
    let userMessage: String
    let history: [AssistantTurn]
    let alertContext: AssistantAlertContext?
}

struct AssistantResponse: Decodable, Sendable, Equatable {
    struct Citation: Decodable, Sendable, Equatable {
        let sourceId: String
        let snippet: String
        let url: String?
    }

    struct Escalation: Decodable, Sendable, Equatable {
        enum Urgency: String, Decodable, Sendable { case low, medium, high }
        let trigger: Bool
        let urgency: Urgency?
        let reason: String?
    }

    struct SuggestedAction: Decodable, Sendable, Equatable {
        let label: String
        let actionId: String
    }

    struct Metadata: Decodable, Sendable, Equatable {
        let threadId: String
        let turnId: String
        let latencyMs: Int
        let templateId: String?
    }

    let schemaVersion: String
    let responseText: String
    let citations: [Citation]
    let escalation: Escalation
    let suggestedActions: [SuggestedAction]
    let metadata: Metadata

    var isUrgent: Bool { escalation.trigger && escalation.urgency == .high }
}

enum AssistantEvent: Sendable, Equatable {
    /// Étape en cours (« Je regarde les données du collier… »), à afficher à la place des points de saisie.
    case status(String)
    /// Réponse vérifiée par les garde-fous. Toujours le dernier événement d'un tour réussi.
    case response(AssistantResponse)
}

enum AssistantError: LocalizedError, Equatable {
    case server(String)
    case unreachable
    case badResponse

    var errorDescription: String? {
        switch self {
        case let .server(message): message
        case .unreachable: "L'assistant Pawrise est injoignable pour le moment. Réessayez dans un instant."
        case .badResponse: "L'assistant a renvoyé une réponse illisible. Réessayez."
        }
    }
}

protocol AssistantService: Sendable {
    func streamTurn(_ request: AssistantTurnRequest) -> AsyncThrowingStream<AssistantEvent, Error>
}

/// Appelle `POST /v1/turns/stream` (Server-Sent Events).
/// En production, la même route sera exposée derrière KrakenD (et pawrise-dialog) avec le JWT.
final class AssistantClient: AssistantService {
    private let baseURL: URL
    private let session: URLSession
    private let tokens: TokenStore?

    /// pawrise-assistant (Python) parle en snake_case, contrairement à l'API d'auth et à l'API-CORE.
    static let decoder = APIClient.makeDecoder(keys: .convertFromSnakeCase)
    static let encoder = APIClient.makeEncoder(keys: .convertToSnakeCase)

    init(baseURL: URL, tokens: TokenStore? = nil, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.tokens = tokens
        self.session = session
    }

    func streamTurn(_ request: AssistantTurnRequest) -> AsyncThrowingStream<AssistantEvent, Error> {
        AsyncThrowingStream { continuation in
            let task = Task {
                do {
                    var urlRequest = URLRequest(url: baseURL.appending(path: "v1/turns/stream"))
                    urlRequest.httpMethod = "POST"
                    urlRequest.timeoutInterval = 60
                    urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
                    urlRequest.setValue("text/event-stream", forHTTPHeaderField: "Accept")
                    if let token = await tokens?.tokens?.accessToken {
                        urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                    }
                    urlRequest.httpBody = try Self.encoder.encode(request)

                    let (bytes, response) = try await session.bytes(for: urlRequest)
                    guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                        throw AssistantError.unreachable
                    }
                    // Découpage manuel : `bytes.lines` saute les lignes vides, qui terminent les messages SSE.
                    var parser = SSEParser()
                    var buffer = Data()
                    for try await byte in bytes {
                        guard byte == UInt8(ascii: "\n") else { buffer.append(byte); continue }
                        if buffer.last == UInt8(ascii: "\r") { buffer.removeLast() }
                        let line = String(decoding: buffer, as: UTF8.self)
                        buffer.removeAll(keepingCapacity: true)
                        if let message = parser.consume(line), let event = try Self.decode(message) {
                            continuation.yield(event)
                        }
                    }
                    if let message = parser.flush(), let event = try Self.decode(message) {
                        continuation.yield(event)
                    }
                    continuation.finish()
                } catch is CancellationError {
                    continuation.finish()
                } catch let error as AssistantError {
                    continuation.finish(throwing: error)
                } catch is URLError {
                    continuation.finish(throwing: AssistantError.unreachable)
                } catch {
                    continuation.finish(throwing: error)
                }
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    private struct Envelope: Decodable {
        let type: String
        let text: String?
        let message: String?
        let response: AssistantResponse?
    }

    static func decode(_ message: SSEParser.Message) throws -> AssistantEvent? {
        guard let data = message.data.data(using: .utf8),
              let envelope = try? Self.decoder.decode(Envelope.self, from: data) else {
            return nil // ping ou commentaire
        }
        switch envelope.type {
        case "status":
            return envelope.text.map(AssistantEvent.status)
        case "response":
            guard let response = envelope.response else { throw AssistantError.badResponse }
            return .response(response)
        case "error":
            throw AssistantError.server(envelope.message ?? "Le service est momentanément indisponible.")
        default:
            return nil
        }
    }
}

/// Analyseur Server-Sent Events minimal (lignes `event:` / `data:`, message terminé par une ligne vide).
struct SSEParser {
    struct Message: Equatable {
        var event: String?
        var data: String
    }

    private var event: String?
    private var dataLines: [String] = []

    /// Renvoie un message complet quand `line` est la ligne vide qui le termine.
    mutating func consume(_ line: String) -> Message? {
        if line.isEmpty { return flush() }
        if line.hasPrefix(":") { return nil }
        let (field, value) = Self.split(line)
        switch field {
        case "event": event = value
        case "data": dataLines.append(value)
        default: break
        }
        return nil
    }

    mutating func flush() -> Message? {
        defer { event = nil; dataLines = [] }
        guard !dataLines.isEmpty else { return nil }
        return Message(event: event, data: dataLines.joined(separator: "\n"))
    }

    private static func split(_ line: String) -> (String, String) {
        guard let colon = line.firstIndex(of: ":") else { return (line, "") }
        var value = line[line.index(after: colon)...]
        if value.hasPrefix(" ") { value = value.dropFirst() }
        return (String(line[..<colon]), String(value))
    }
}
