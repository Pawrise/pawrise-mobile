import Foundation

struct Endpoint: Sendable {
    enum Method: String, Sendable { case get = "GET", post = "POST", put = "PUT", patch = "PATCH", delete = "DELETE" }

    /// Corps déjà encodé (multipart de la photo du chien, par exemple).
    struct RawBody: Sendable {
        let data: Data
        let contentType: String
    }

    var method: Method
    var path: String
    var body: (any Encodable & Sendable)?
    var requiresAuth: Bool
    var query: [URLQueryItem] = []
    var rawBody: RawBody?

    static func get(_ path: String, auth: Bool = true) -> Endpoint {
        Endpoint(method: .get, path: path, body: nil, requiresAuth: auth)
    }

    static func post(_ path: String, body: (any Encodable & Sendable)? = nil, auth: Bool = true) -> Endpoint {
        Endpoint(method: .post, path: path, body: body, requiresAuth: auth)
    }

    static func put(_ path: String, body: any Encodable & Sendable) -> Endpoint {
        Endpoint(method: .put, path: path, body: body, requiresAuth: true)
    }

    static func patch(_ path: String, body: any Encodable & Sendable) -> Endpoint {
        Endpoint(method: .patch, path: path, body: body, requiresAuth: true)
    }

    static func delete(_ path: String) -> Endpoint {
        Endpoint(method: .delete, path: path, body: nil, requiresAuth: true)
    }
}

/// Client HTTP JSON des services Pawrise (JSON en camelCase, convention 3 de pawrise-doc).
/// - ajoute `Authorization: Bearer` pour les routes protégées ;
/// - sur 401, rafraîchit la session une fois (via TokenStore) puis rejoue la requête ;
/// - décode les erreurs RFC 9457 en `APIError.problem`.
final class APIClient: Sendable {
    let baseURL: URL
    private let session: URLSession
    private let tokens: TokenStore

    static let decoder: JSONDecoder = makeDecoder(keys: .useDefaultKeys)
    static let encoder: JSONEncoder = makeEncoder(keys: .useDefaultKeys)

    /// Dates ISO 8601 en UTC, avec ou sans millisecondes (convention 1).
    static func makeDecoder(keys: JSONDecoder.KeyDecodingStrategy) -> JSONDecoder {
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = keys
        decoder.dateDecodingStrategy = .custom { decoder in
            let string = try decoder.singleValueContainer().decode(String.self)
            let withFraction = ISO8601DateFormatter()
            withFraction.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            if let date = withFraction.date(from: string) ?? ISO8601DateFormatter().date(from: string) {
                return date
            }
            throw DecodingError.dataCorrupted(.init(codingPath: decoder.codingPath, debugDescription: "Date ISO 8601 invalide : \(string)"))
        }
        return decoder
    }

    static func makeEncoder(keys: JSONEncoder.KeyEncodingStrategy) -> JSONEncoder {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = keys
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }

    init(baseURL: URL, tokens: TokenStore, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.tokens = tokens
        self.session = session
    }

    func send<Response: Decodable>(_ endpoint: Endpoint, as type: Response.Type = Response.self) async throws -> Response {
        let data = try await perform(endpoint, isRetry: false)
        do {
            return try Self.decoder.decode(Response.self, from: data)
        } catch {
            throw APIError.decoding(String(describing: error))
        }
    }

    func sendWithoutResponse(_ endpoint: Endpoint) async throws {
        _ = try await perform(endpoint, isRetry: false)
    }

    private func perform(_ endpoint: Endpoint, isRetry: Bool) async throws -> Data {
        var url = baseURL.appending(path: endpoint.path)
        if !endpoint.query.isEmpty { url.append(queryItems: endpoint.query) }
        var request = URLRequest(url: url)
        request.httpMethod = endpoint.method.rawValue
        request.timeoutInterval = 15
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        var usedAccessToken: String?
        if endpoint.requiresAuth, let pair = await tokens.tokens {
            usedAccessToken = pair.accessToken
            request.setValue("Bearer \(pair.accessToken)", forHTTPHeaderField: "Authorization")
        }
        if let raw = endpoint.rawBody {
            request.setValue(raw.contentType, forHTTPHeaderField: "Content-Type")
            request.httpBody = raw.data
        } else if let body = endpoint.body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try Self.encoder.encode(body)
        }

        let data: Data, response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch let error as URLError {
            throw APIError.network(error.code)
        }
        guard let http = response as? HTTPURLResponse else { throw APIError.unexpectedStatus(0) }

        if http.statusCode == 401, endpoint.requiresAuth, !isRetry {
            _ = try await tokens.refreshedTokens(replacing: usedAccessToken)
            return try await perform(endpoint, isRetry: true)
        }
        guard (200..<300).contains(http.statusCode) else {
            if let problem = try? Self.decoder.decode(ProblemDetails.self, from: data) {
                throw APIError.problem(status: http.statusCode, problem)
            }
            throw APIError.unexpectedStatus(http.statusCode)
        }
        return data
    }
}
