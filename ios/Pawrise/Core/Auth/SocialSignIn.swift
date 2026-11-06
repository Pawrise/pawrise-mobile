import AuthenticationServices
import CryptoKit
import Foundation
@preconcurrency import GoogleSignIn
import UIKit

/// Récupère les jetons d'identité Apple et Google côté appareil.
/// Le serveur (pawrise-auth) vérifie ensuite signature, audience, expiration et nonce.
@MainActor
final class SocialSignIn {
    enum Failure: LocalizedError, Equatable {
        case cancelled
        case missingToken
        case googleNotConfigured
        case noWindow

        var errorDescription: String? {
            switch self {
            case .cancelled: nil
            case .missingToken: "La connexion n'a pas renvoyé d'identifiant. Réessayez."
            case .googleNotConfigured: "La connexion Google n'est pas encore configurée dans cette version de l'app."
            case .noWindow: "Impossible d'afficher la connexion. Réessayez."
            }
        }
    }

    // MARK: Apple

    /// Prépare une demande Apple avec un nonce : Apple reçoit SHA-256(nonce), le serveur reçoit la valeur brute.
    func prepareAppleRequest(_ request: ASAuthorizationAppleIDRequest) -> String {
        let nonce = Self.randomNonce()
        request.requestedScopes = [.fullName, .email]
        request.nonce = Self.sha256(nonce)
        return nonce
    }

    /// Transforme le résultat du `SignInWithAppleButton` en requête pour pawrise-auth.
    func appleRequest(from result: Result<ASAuthorization, Error>, nonce: String) throws -> AppleSignInRequest {
        switch result {
        case let .failure(error):
            if (error as? ASAuthorizationError)?.code == .canceled { throw Failure.cancelled }
            throw error
        case let .success(authorization):
            guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
                  let tokenData = credential.identityToken,
                  let idToken = String(data: tokenData, encoding: .utf8) else {
                throw Failure.missingToken
            }
            let given = credential.fullName?.givenName?.trimmingCharacters(in: .whitespaces)
            let family = credential.fullName?.familyName?.trimmingCharacters(in: .whitespaces)
            return AppleSignInRequest(
                identityToken: idToken,
                nonce: nonce,
                firstName: given?.isEmpty == false ? given : nil,
                lastName: family?.isEmpty == false ? family : nil,
                acceptedTermsVersion: LegalTerms.version
            )
        }
    }

    // MARK: Google

    func googleRequest(clientID: String?) async throws -> GoogleSignInRequest {
        guard let clientID, !clientID.isEmpty else { throw Failure.googleNotConfigured }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        guard let presenter = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?.rootViewController else {
            throw Failure.noWindow
        }
        do {
            // GoogleSignIn 8.x ne permet pas de fixer le nonce : le contrat le rend facultatif pour Google.
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
            guard let idToken = result.user.idToken?.tokenString else { throw Failure.missingToken }
            return GoogleSignInRequest(idToken: idToken, nonce: nil, acceptedTermsVersion: LegalTerms.version)
        } catch let error as NSError where error.domain == kGIDSignInErrorDomain && error.code == GIDSignInError.canceled.rawValue {
            throw Failure.cancelled
        }
    }

    static func handle(url: URL) -> Bool {
        GIDSignIn.sharedInstance.handle(url)
    }

    // MARK: Nonce

    static func randomNonce(length: Int = 32) -> String {
        let charset = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var generator = SystemRandomNumberGenerator()
        return String((0..<length).map { _ in charset.randomElement(using: &generator)! })
    }

    static func sha256(_ input: String) -> String {
        SHA256.hash(data: Data(input.utf8)).map { String(format: "%02x", $0) }.joined()
    }
}
