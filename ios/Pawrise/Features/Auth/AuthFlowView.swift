import SwiftUI

/// Accueil → Connexion / Créer un compte (Figma 01, 02, 02 Connexion).
struct AuthFlowView: View {
    enum Route: Hashable { case login, signup, forgotPassword(email: String) }

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
                    LoginView(
                        onSignupInstead: { path = [.signup] },
                        onForgotPassword: { email in path.append(.forgotPassword(email: email)) }
                    )
                case .signup:
                    SignupView(onLoginInstead: { path = [.login] })
                case let .forgotPassword(email):
                    ForgotPasswordView(initialEmail: email)
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
    let onForgotPassword: (String) -> Void

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

                Button("Mot de passe oublié ?") { onForgotPassword(email) }
                    .buttonStyle(.pawriseLink)
                    .frame(maxWidth: .infinity, alignment: .trailing)

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
    @State private var firstName = ""
    @State private var lastName = ""
    @State private var email = ""
    @State private var phone = ""
    @State private var password = ""
    @State private var acceptsTerms = false
    @State private var isSubmitting = false
    @State private var showErrors = false
    @State private var errorMessage: String?
    /// Erreurs par champ renvoyées par le serveur (VALIDATION_FAILED), affichées sous le champ.
    @State private var serverErrors: [String: String] = [:]
    @FocusState private var focus: Field?

    private enum Field { case firstName, lastName, email, phone, password }

    private var firstNameError: String? {
        firstName.trimmingCharacters(in: .whitespaces).isEmpty ? "Indiquez votre prénom." : nil
    }

    private var lastNameError: String? {
        lastName.trimmingCharacters(in: .whitespaces).isEmpty ? "Indiquez votre nom." : nil
    }

    private var emailError: String? {
        CredentialRules.isValidEmail(email) ? nil : "Saisissez une adresse e-mail valide, par exemple alex@exemple.fr."
    }

    private var phoneError: String? {
        let trimmed = phone.trimmingCharacters(in: .whitespaces)
        return trimmed.isEmpty || CredentialRules.e164Phone(trimmed) != nil
            ? nil : "Saisissez un numéro valide, par exemple 06 12 34 56 78."
    }

    private var passwordError: String? {
        CredentialRules.isValidPassword(password)
            ? nil
            : "Le mot de passe doit faire au moins \(CredentialRules.passwordMinLength) caractères."
    }

    private var termsError: String? {
        acceptsTerms ? nil : "Acceptez les conditions d'utilisation pour continuer."
    }

    private func error(_ local: String?, server field: String) -> String? {
        showErrors ? (local ?? serverErrors[field]) : serverErrors[field]
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                AuthHeader(
                    title: "Créer un compte",
                    subtitle: "Quelques informations, puis on vous montre Pawrise.",
                    onBack: { dismiss() }
                )

                HStack(alignment: .top, spacing: 12) {
                    LabeledField(label: "Prénom", error: error(firstNameError, server: "firstName")) {
                        TextField("", text: $firstName, prompt: Text("Alex"))
                            .textContentType(.givenName)
                            .focused($focus, equals: .firstName)
                            .submitLabel(.next)
                            .onSubmit { focus = .lastName }
                    }
                    LabeledField(label: "Nom", error: error(lastNameError, server: "lastName")) {
                        TextField("", text: $lastName, prompt: Text("Martin"))
                            .textContentType(.familyName)
                            .focused($focus, equals: .lastName)
                            .submitLabel(.next)
                            .onSubmit { focus = .email }
                    }
                }
                LabeledField(label: "E-mail", error: error(emailError, server: "email")) {
                    TextField("", text: $email, prompt: Text("alex@exemple.fr"))
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .focused($focus, equals: .email)
                        .submitLabel(.next)
                        .onSubmit { focus = .phone }
                }
                LabeledField(label: "Téléphone (facultatif)", error: error(phoneError, server: "phoneNumber")) {
                    TextField("", text: $phone, prompt: Text("06 12 34 56 78"))
                        .textContentType(.telephoneNumber)
                        .keyboardType(.phonePad)
                        .focused($focus, equals: .phone)
                }
                LabeledField(label: "Mot de passe", error: error(passwordError, server: "password")) {
                    SecureField("", text: $password, prompt: Text("8 caractères minimum"))
                        .textContentType(.newPassword)
                        .focused($focus, equals: .password)
                        .submitLabel(.go)
                        .onSubmit(submit)
                }

                TermsCheckbox(isOn: $acceptsTerms, error: error(termsError, server: "acceptedTermsVersion"))

                if let errorMessage { ErrorBanner(message: errorMessage) }

                Button("Continuer", action: submit)
                    .buttonStyle(.pawrisePrimary(loading: isSubmitting))
                    .disabled(isSubmitting)

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
        serverErrors = [:]
        let localErrors = [firstNameError, lastNameError, emailError, phoneError, passwordError, termsError]
        guard localErrors.allSatisfy({ $0 == nil }), !isSubmitting else { return }
        focus = nil
        isSubmitting = true
        errorMessage = nil
        let trimmedPhone = phone.trimmingCharacters(in: .whitespaces)
        Task {
            defer { isSubmitting = false }
            do {
                try await session.register(
                    firstName: firstName, lastName: lastName, email: email,
                    phoneNumber: trimmedPhone.isEmpty ? nil : CredentialRules.e164Phone(trimmedPhone),
                    password: password
                )
            } catch let error as APIError where !error.fieldErrors.isEmpty {
                for field in error.fieldErrors {
                    serverErrors[field.field] = APIError.fieldMessage(field: field.field, code: field.code)
                }
                if serverErrors.isEmpty { errorMessage = error.localizedDescription }
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}

/// Case à cocher des CGU (RG11) : obligatoire, enregistrée côté serveur avec la version acceptée.
private struct TermsCheckbox: View {
    @Binding var isOn: Bool
    var error: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Button {
                isOn.toggle()
            } label: {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: isOn ? "checkmark.square.fill" : "square")
                        .font(.system(size: 22))
                        .foregroundStyle(isOn ? Color.Pawrise.primary : (error == nil ? Color.Pawrise.borderStrong : Color.Pawrise.alert))
                    Text("J'accepte les conditions d'utilisation et la politique de confidentialité de Pawrise.")
                        .font(Font.Pawrise.subheadline)
                        .foregroundStyle(Color.Pawrise.textPrimary)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("J'accepte les conditions d'utilisation et la politique de confidentialité")
            .accessibilityValue(isOn ? "Coché" : "Non coché")
            .accessibilityAddTraits(.isToggle)
            .accessibilityIdentifier("terms-checkbox")

            if let error {
                Text(error)
                    .font(Font.Pawrise.caption)
                    .foregroundStyle(Color.Pawrise.alert)
            }
        }
    }
}

// MARK: - Mot de passe oublié

/// Deux étapes : l'e-mail, puis le code à 6 chiffres reçu et le nouveau mot de passe. Connecte directement à la fin.
struct ForgotPasswordView: View {
    let initialEmail: String

    @Environment(SessionStore.self) private var session
    @Environment(\.dismiss) private var dismiss
    @State private var email = ""
    @State private var code = ""
    @State private var newPassword = ""
    @State private var codeSent = false
    @State private var isSubmitting = false
    @State private var errorMessage: String?
    @State private var info: String?
    @FocusState private var focus: Field?

    private enum Field { case email, code, password }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                AuthHeader(
                    title: "Mot de passe oublié",
                    subtitle: codeSent
                        ? "Saisissez le code reçu à \(CredentialRules.normalizedEmail(email)) et choisissez un nouveau mot de passe."
                        : "Indiquez votre adresse : nous vous envoyons un code pour choisir un nouveau mot de passe.",
                    onBack: { dismiss() }
                )

                LabeledField(label: "E-mail") {
                    TextField("", text: $email, prompt: Text("vous@exemple.fr"))
                        .textContentType(.username)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .focused($focus, equals: .email)
                        .disabled(codeSent)
                }

                if codeSent {
                    LabeledField(label: "Code à 6 chiffres") {
                        TextField("", text: $code, prompt: Text("123456"))
                            .textContentType(.oneTimeCode)
                            .keyboardType(.numberPad)
                            .focused($focus, equals: .code)
                            .onChange(of: code) { _, value in code = String(value.filter(\.isNumber).prefix(6)) }
                    }
                    LabeledField(label: "Nouveau mot de passe") {
                        SecureField("", text: $newPassword, prompt: Text("8 caractères minimum"))
                            .textContentType(.newPassword)
                            .focused($focus, equals: .password)
                            .submitLabel(.go)
                            .onSubmit(submit)
                    }
                }

                if let info {
                    Label(info, systemImage: "envelope")
                        .font(Font.Pawrise.subheadline)
                        .foregroundStyle(Color.Pawrise.good)
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.Pawrise.goodSurface, in: .rect(cornerRadius: Radius.control))
                }
                if let errorMessage { ErrorBanner(message: errorMessage) }

                Button(codeSent ? "Changer le mot de passe" : "Recevoir un code", action: submit)
                    .buttonStyle(.pawrisePrimary(loading: isSubmitting))
                    .disabled(isSubmitting)

                if codeSent {
                    Button("Renvoyer un code") {
                        codeSent = false
                        submit()
                    }
                    .buttonStyle(.pawriseLink)
                    .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal, Spacing.page)
            .padding(.bottom, 24)
        }
        .scrollDismissesKeyboard(.interactively)
        .pawrisePage()
        .toolbar(.hidden, for: .navigationBar)
        .onAppear { if email.isEmpty { email = initialEmail } }
    }

    private func submit() {
        errorMessage = nil
        if !codeSent {
            guard CredentialRules.isValidEmail(email) else {
                errorMessage = "Saisissez une adresse e-mail valide, par exemple alex@exemple.fr."
                return
            }
        } else {
            guard CredentialRules.isValidResetCode(code) else {
                errorMessage = "Le code contient 6 chiffres."
                return
            }
            guard CredentialRules.isValidPassword(newPassword) else {
                errorMessage = "Le mot de passe doit faire au moins \(CredentialRules.passwordMinLength) caractères."
                return
            }
        }
        focus = nil
        isSubmitting = true
        Task {
            defer { isSubmitting = false }
            do {
                if codeSent {
                    try await session.resetPassword(email: email, code: code, newPassword: newPassword)
                } else {
                    try await session.requestPasswordReset(email: email)
                    codeSent = true
                    info = "Si un compte existe avec cette adresse, un code vient de lui être envoyé. Il est valable 15 minutes."
                    focus = .code
                }
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
