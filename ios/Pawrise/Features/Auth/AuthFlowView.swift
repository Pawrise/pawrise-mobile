import SwiftUI

/// Accueil → Connexion / Créer un compte (Figma 01, 02, 02 Connexion).
struct AuthFlowView: View {
    enum Route: Hashable { case login, signup }

    @State private var path: [Route] = []

    var body: some View {
        NavigationStack(path: $path) {
            WelcomeView(
                onLogin: { path.append(.login) },
                onSignup: { path.append(.signup) }
            )
            .navigationDestination(for: Route.self) { route in
                switch route {
                case .login:
                    LoginView(onSignupInstead: { path = [.signup] })
                case .signup:
                    SignupView(onLoginInstead: { path = [.login] })
                }
            }
        }
    }
}

struct WelcomeView: View {
    let onLogin: () -> Void
    let onSignup: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Spacer()
            VStack(spacing: 20) {
                PawriseMark()
                Text("Pawrise")
                    .font(Font.Pawrise.display(34))
                    .foregroundStyle(Color.Pawrise.textPrimary)
                Text("Le collier connecté qui suit la santé et la position de votre chien.")
                    .font(Font.Pawrise.body)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
            }
            Spacer()
            VStack(spacing: 12) {
                Button("Se connecter", action: onLogin)
                    .buttonStyle(.pawrisePrimary)
                Button("Créer un compte", action: onSignup)
                    .buttonStyle(.pawriseSecondary)
            }
        }
        .padding(.horizontal, Spacing.page)
        .padding(.bottom, 16)
        .pawrisePage()
        .toolbar(.hidden, for: .navigationBar)
    }
}

// MARK: - Connexion

struct LoginView: View {
    let onSignupInstead: () -> Void

    @Environment(SessionStore.self) private var session
    @Environment(\.dismiss) private var dismiss
    @State private var email = ""
    @State private var password = ""
    @State private var isSubmitting = false
    @State private var errorMessage: String?
    @FocusState private var focus: Field?

    private enum Field { case email, password }

    private var canSubmit: Bool { !email.isEmpty && !password.isEmpty && !isSubmitting }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                AuthHeader(title: "Connexion", subtitle: "Retrouvez vos chiens et leur suivi.", onBack: { dismiss() })

                LabeledField(label: "E-mail") {
                    TextField("", text: $email, prompt: Text("vous@exemple.fr"))
                        .textContentType(.username)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .focused($focus, equals: .email)
                        .submitLabel(.next)
                        .onSubmit { focus = .password }
                }
                LabeledField(label: "Mot de passe") {
                    SecureField("", text: $password)
                        .textContentType(.password)
                        .focused($focus, equals: .password)
                        .submitLabel(.go)
                        .onSubmit(submit)
                }

                if let errorMessage { ErrorBanner(message: errorMessage) }

                Button("Se connecter", action: submit)
                    .buttonStyle(.pawrisePrimary(loading: isSubmitting))
                    .disabled(!canSubmit)

                Button("Créer un compte", action: onSignupInstead)
                    .buttonStyle(.pawriseLink)
                    .frame(maxWidth: .infinity)

                SocialSignInSection(errorMessage: $errorMessage)
            }
            .padding(.horizontal, Spacing.page)
            .padding(.bottom, 24)
        }
        .scrollDismissesKeyboard(.interactively)
        .pawrisePage()
        .toolbar(.hidden, for: .navigationBar)
    }

    private func submit() {
        guard canSubmit else { return }
        focus = nil
        isSubmitting = true
        errorMessage = nil
        Task {
            defer { isSubmitting = false }
            do {
                try await session.login(email: email, password: password)
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}

// MARK: - Inscription

struct SignupView: View {
    let onLoginInstead: () -> Void

    @Environment(SessionStore.self) private var session
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var email = ""
    @State private var phone = ""
    @State private var password = ""
    @State private var isSubmitting = false
    @State private var showErrors = false
    @State private var errorMessage: String?
    @FocusState private var focus: Field?

    private enum Field { case name, email, phone, password }

    private var nameError: String? {
        name.trimmingCharacters(in: .whitespaces).isEmpty ? "Indiquez votre nom." : nil
    }

    private var emailError: String? {
        CredentialRules.isValidEmail(email) ? nil : "Saisissez une adresse e-mail valide, par exemple alex@exemple.fr."
    }

    private var passwordError: String? {
        CredentialRules.isValidPassword(password)
            ? nil
            : "Le mot de passe doit faire au moins \(CredentialRules.passwordMinLength) caractères."
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                AuthHeader(
                    title: "Créer un compte",
                    subtitle: "Quelques informations, puis on vous montre Pawrise.",
                    onBack: { dismiss() }
                )

                LabeledField(label: "Nom", error: showErrors ? nameError : nil) {
                    TextField("", text: $name, prompt: Text("Alex Martin"))
                        .textContentType(.name)
                        .focused($focus, equals: .name)
                        .submitLabel(.next)
                        .onSubmit { focus = .email }
                }
                LabeledField(label: "E-mail", error: showErrors ? emailError : nil) {
                    TextField("", text: $email, prompt: Text("alex@exemple.fr"))
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .focused($focus, equals: .email)
                        .submitLabel(.next)
                        .onSubmit { focus = .phone }
                }
                LabeledField(label: "Téléphone (facultatif)") {
                    TextField("", text: $phone, prompt: Text("06 12 34 56 78"))
                        .textContentType(.telephoneNumber)
                        .keyboardType(.phonePad)
                        .focused($focus, equals: .phone)
                }
                LabeledField(label: "Mot de passe", error: showErrors ? passwordError : nil) {
                    SecureField("", text: $password, prompt: Text("8 caractères minimum"))
                        .textContentType(.newPassword)
                        .focused($focus, equals: .password)
                        .submitLabel(.go)
                        .onSubmit(submit)
                }

                if let errorMessage { ErrorBanner(message: errorMessage) }

                VStack(spacing: 10) {
                    Button("Continuer", action: submit)
                        .buttonStyle(.pawrisePrimary(loading: isSubmitting))
                        .disabled(isSubmitting)
                    Text("En continuant, vous acceptez les conditions d'utilisation et la politique de confidentialité de Pawrise.")
                        .font(Font.Pawrise.caption)
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .multilineTextAlignment(.center)
                }

                Button("J'ai déjà un compte", action: onLoginInstead)
                    .buttonStyle(.pawriseLink)
                    .frame(maxWidth: .infinity)

                SocialSignInSection(errorMessage: $errorMessage)
            }
            .padding(.horizontal, Spacing.page)
            .padding(.bottom, 24)
        }
        .scrollDismissesKeyboard(.interactively)
        .pawrisePage()
        .toolbar(.hidden, for: .navigationBar)
    }

    private func submit() {
        showErrors = true
        guard nameError == nil, emailError == nil, passwordError == nil, !isSubmitting else { return }
        focus = nil
        isSubmitting = true
        errorMessage = nil
        Task {
            defer { isSubmitting = false }
            do {
                try await session.register(name: name, email: email, phone: phone, password: password)
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}

// MARK: - Éléments partagés

private struct AuthHeader: View {
    let title: String
    let subtitle: String
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Button(action: onBack) {
                Label("Retour", systemImage: "chevron.left")
                    .font(Font.Pawrise.headline)
            }
            .foregroundStyle(Color.Pawrise.primary)
            .frame(minHeight: 44)

            VStack(alignment: .leading, spacing: 8) {
                Text(title)
                    .font(Font.Pawrise.largeTitle)
                    .foregroundStyle(Color.Pawrise.textPrimary)
                Text(subtitle)
                    .font(Font.Pawrise.callout)
                    .foregroundStyle(Color.Pawrise.textMuted)
            }
        }
        .padding(.top, 8)
    }
}
