import Foundation

/// Point d'entrée unique de l'app vers l'authentification.
/// Deux implémentations : `RemoteAuthService` (pawrise-auth) et `MockAuthService` (développement sans serveur).
protocol AuthService: Sendable {
    func register(_ request: RegisterRequest) async throws -> AuthResponse
    func login(_ request: LoginRequest) async throws -> AuthResponse
    func signInWithApple(_ request: AppleSignInRequest) async throws -> AuthResponse
    func signInWithGoogle(_ request: GoogleSignInRequest) async throws -> AuthResponse
    func refresh(refreshToken: String) async throws -> TokenPair
    func logout(refreshToken: String) async throws
    func currentUser() async throws -> AuthUser
}

final class RemoteAuthService: AuthService {
    private let client: APIClient

    init(client: APIClient) {
        self.client = client
    }

    func register(_ request: RegisterRequest) async throws -> AuthResponse {
        try await client.send(.post("v1/auth/register", body: request, auth: false))
    }

    func login(_ request: LoginRequest) async throws -> AuthResponse {
        try await client.send(.post("v1/auth/login", body: request, auth: false))
    }

    func signInWithApple(_ request: AppleSignInRequest) async throws -> AuthResponse {
        try await client.send(.post("v1/auth/apple", body: request, auth: false))
    }

    func signInWithGoogle(_ request: GoogleSignInRequest) async throws -> AuthResponse {
        try await client.send(.post("v1/auth/google", body: request, auth: false))
    }

    func refresh(refreshToken: String) async throws -> TokenPair {
        let response: RefreshResponse = try await client.send(
            .post("v1/auth/refresh", body: RefreshRequest(refreshToken: refreshToken), auth: false)
        )
        return response.tokens
    }

    func logout(refreshToken: String) async throws {
        try await client.sendWithoutResponse(.post("v1/auth/logout", body: RefreshRequest(refreshToken: refreshToken)))
    }

    func currentUser() async throws -> AuthUser {
        try await client.send(.get("v1/me"))
    }
}
