import AuthenticationServices
import SwiftUI

/// « ou » + Se connecter avec Apple + Continuer avec Google.
/// Apple est obligatoire sur iOS dès qu'une connexion Google est proposée (App Store Guideline 4.8).
struct SocialSignInSection: View {
    @Binding var errorMessage: String?

    @Environment(AppEnvironment.self) private var environment
    @Environment(SessionStore.self) private var session
    @State private var appleNonce = ""
    @State private var isWorking = false

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 12) {
                line
                Text("ou").font(Font.Pawrise.footnote).foregroundStyle(Color.Pawrise.textMuted)
                line
            }

            SignInWithAppleButton(.continue) { request in
                appleNonce = environment.social.prepareAppleRequest(request)
            } onCompletion: { result in
                run {
                    let request = try environment.social.appleRequest(from: result, nonce: appleNonce)
                    try await session.signInWithApple(request)
                }
            }
            .signInWithAppleButtonStyle(.black)
            .frame(height: 56)
            .clipShape(.rect(cornerRadius: Radius.control))

            Button {
                run {
                    let request = try await environment.social.googleRequest(clientID: environment.config.googleClientID)
                    try await session.signInWithGoogle(request)
                }
            } label: {
                HStack(spacing: 10) {
                    Text("G")
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .foregroundStyle(Color(hex: 0x4285F4))
                    Text("Continuer avec Google")
                }
            }
            .buttonStyle(GoogleButtonStyle())

            // RG11 : le bouton Apple / Google vaut acceptation des CGU (version envoyée au serveur).
            Text("En continuant avec Apple ou Google, vous acceptez les conditions d'utilisation et la politique de confidentialité de Pawrise.")
                .font(Font.Pawrise.caption)
                .foregroundStyle(Color.Pawrise.textMuted)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
        }
        .disabled(isWorking)
        .padding(.top, 8)
    }

    private var line: some View {
        Rectangle().fill(Color.Pawrise.border).frame(height: 1)
    }

    private func run(_ operation: @escaping () async throws -> Void) {
        isWorking = true
        errorMessage = nil
        Task {
            defer { isWorking = false }
            do {
                try await operation()
            } catch SocialSignIn.Failure.cancelled {
                // l'utilisateur a fermé la feuille : rien à afficher
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}

private struct GoogleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(Font.Pawrise.button)
            .foregroundStyle(Color(hex: 0x1F1F1F))
            .frame(maxWidth: .infinity, minHeight: 56)
            .background(Color.white, in: .rect(cornerRadius: Radius.control))
            .overlay(RoundedRectangle(cornerRadius: Radius.control).strokeBorder(Color(hex: 0x747775), lineWidth: 1))
            .opacity(configuration.isPressed ? 0.7 : 1)
    }
}
