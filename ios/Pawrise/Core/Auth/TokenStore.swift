import Foundation

/// Garde le couple de tokens (Keychain) et sérialise les refresh :
/// si plusieurs requêtes reçoivent un 401 en même temps, un seul refresh part et toutes l'attendent.
actor TokenStore {
    typealias Refresher = @Sendable (_ refreshToken: String) async throws -> TokenPair

    private let keychain: KeychainStore
    private let key = "token_pair"
    private var cached: TokenPair?
    private var refreshTask: Task<TokenPair, Error>?
    private var refresher: Refresher?

    init(keychain: KeychainStore = KeychainStore()) {
        self.keychain = keychain
        if let data = keychain.data(for: key) {
            cached = try? JSONDecoder().decode(TokenPair.self, from: data)
        }
    }

    func setRefresher(_ refresher: @escaping Refresher) {
        self.refresher = refresher
    }

    var tokens: TokenPair? { cached }

    func save(_ pair: TokenPair) {
        cached = pair
        if let data = try? JSONEncoder().encode(pair) {
            keychain.set(data, for: key)
        }
    }

    func clear() {
        cached = nil
        refreshTask?.cancel()
        refreshTask = nil
        keychain.remove(key)
    }

    /// Renvoie un access token plus récent que `staleAccessToken`, en lançant un refresh si besoin.
    func refreshedTokens(replacing staleAccessToken: String?) async throws -> TokenPair {
        if let current = cached, current.accessToken != staleAccessToken {
            return current // un autre appel a déjà rafraîchi
        }
        if let running = refreshTask {
            return try await running.value
        }
        guard let refreshToken = cached?.refreshToken, let refresher else {
            throw APIError.sessionExpired
        }
        let task = Task { try await refresher(refreshToken) }
        refreshTask = task
        defer { refreshTask = nil }
        do {
            let pair = try await task.value
            save(pair)
            return pair
        } catch {
            clear()
            throw APIError.sessionExpired
        }
    }
}
