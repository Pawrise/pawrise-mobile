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
    func requestPasswordReset(_ request: ForgotPasswordRequest) async throws
    func resetPassword(_ request: ResetPasswordRequest) async throws
    func resendVerificationEmail() async throws
}

/// Client de pawrise-auth (routes `/v1/auth/*` et `/v1/me`).
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
        try await client.send(.post("v1/auth/refresh", body: RefreshRequest(refreshToken: refreshToken), auth: false))
    }

    func logout(refreshToken: String) async throws {
        try await client.sendWithoutResponse(.post("v1/auth/logout", body: RefreshRequest(refreshToken: refreshToken)))
    }

    func currentUser() async throws -> AuthUser {
        try await client.send(.get("v1/me"))
    }

    func requestPasswordReset(_ request: ForgotPasswordRequest) async throws {
        try await client.sendWithoutResponse(.post("v1/auth/forgot-password", body: request, auth: false))
    }

    func resetPassword(_ request: ResetPasswordRequest) async throws {
        try await client.sendWithoutResponse(.post("v1/auth/reset-password", body: request, auth: false))
    }

    func resendVerificationEmail() async throws {
        try await client.sendWithoutResponse(.post("v1/auth/resend-verification"))
    }
}
