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

/// Bienvenue (maquette A01) : bloc centré dans les 680 premiers points de l’écran, boutons à 680 et 748,
/// marges latérales 24. Positions absolues : la maquette ne met rien sous la barre d’état.
struct WelcomeView: View {
    let onLogin: () -> Void
    let onSignup: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 20) {
                PawriseMark()
                Text("Pawrise")
                    .font(Font.Pawrise.display(32, relativeTo: .largeTitle))
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .figmaLineHeight(1.35, size: 32, family: .display, relativeTo: .largeTitle)
                    .accessibilityAddTraits(.isHeader)
                Text("Le collier connecté qui suit la santé et la position de votre chien.")
                    .font(Font.Pawrise.body)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .multilineTextAlignment(.center)
                    .figmaLineHeight(1.35, size: 15)
                    .frame(maxWidth: 288)
            }
            .frame(maxWidth: .infinity)
            .frame(maxHeight: 680)

            VStack(spacing: 12) {
                Button("Se connecter", action: onLogin)
                    .buttonStyle(.pawrisePrimary)
                Button("Créer un compte", action: onSignup)
                    .buttonStyle(.pawriseSecondary)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 40)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
        .ignoresSafeArea(.container, edges: [.top, .bottom])
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
    @State private var form = LoginForm()
    @State private var isSubmitting = false
    @State private var errorMessage: String?
    @FocusState private var focus: Field?

    private enum Field { case email, password }

    /// « Se connecter » reste grisé tant que l'e-mail n'est pas valide ou que le mot de passe est vide.
    private var canSubmit: Bool { form.isValid && !isSubmitting }

    var body: some View {
        ScrollView {
            // Maquette J02 : marges 24, écart 16, contenu à 48 pt du haut de l’écran (1 pt sous la zone sûre).
            VStack(alignment: .leading, spacing: 16) {
                Button { dismiss() } label: {
                    HStack(spacing: 6) {
                        Image("Onboarding/loginback").resizable().frame(width: 18, height: 18)
                        Text("Retour")
                            .font(Font.Pawrise.link)
                            .foregroundStyle(Color.Pawrise.primary)
                            .figmaLineHeight(1.35, size: 14, relativeTo: .subheadline)
                    }
                    .contentShape(Rectangle().inset(by: -12))
                }
                .buttonStyle(.plain)

                AuthTitle(title: "Connexion", subtitle: "Retrouvez vos chiens et leur suivi.", subtitleSize: 14)

                LabeledField(label: "E-mail") {
                    // `verbatim` : une adresse dans un `LocalizedStringKey` devient un lien (texte bleu).
                    TextField("", text: $form.email, prompt: Text(verbatim: "vous@exemple.fr"))
                        .credentialContentType(.username)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .focused($focus, equals: .email)
                        .submitLabel(.next)
                        .onSubmit { focus = .password }
                }
                LabeledField(label: "Mot de passe") {
                    PasswordInput(text: $form.password, contentType: .password, focus: $focus, focusValue: .password)
                        .submitLabel(.go)
                        .onSubmit(submit)
                }

                Button("Mot de passe oublié ?") { onForgotPassword(form.email) }
                    .buttonStyle(.pawriseCompactLink)
                    .frame(maxWidth: .infinity, alignment: .trailing)

                if let errorMessage { ErrorBanner(message: errorMessage) }

                Button("Se connecter", action: submit)
                    .buttonStyle(.pawrisePrimary(loading: isSubmitting))
                    .disabled(!canSubmit)
                    .accessibilityHint(form.isValid ? "" : "Saisissez une adresse e-mail valide et votre mot de passe.")

                Button("Créer un compte", action: onSignupInstead)
                    .buttonStyle(.pawriseCompactLink)
                    .frame(maxWidth: .infinity)
            }
            .padding(.horizontal, 24)
            .padding(.top, 1)
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
                try await session.login(email: form.email, password: form.password)
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
    @State private var form = SignupForm()
    /// Champs déjà quittés par l'utilisateur : leur erreur s'affiche dès lors (pas avant).
    @State private var touched: Set<SignupForm.Field> = []
    @State private var isSubmitting = false
    @State private var errorMessage: String?
    /// Erreurs par champ renvoyées par le serveur (VALIDATION_FAILED), affichées sous le champ.
    @State private var serverErrors: [String: String] = [:]
    @FocusState private var focus: SignupForm.Field?

    private func error(_ field: SignupForm.Field, server key: String) -> String? {
        touched.contains(field) ? (form.error(for: field) ?? serverErrors[key]) : serverErrors[key]
    }

    var body: some View {
        VStack(spacing: 0) {
            // Maquette A02 : en-tête « chevron seul » (rangée de 44 pt sous la zone sûre), puis contenu à 26 pt,
            // marges 20, écart 24 entre la question et le formulaire, 12 entre les champs.
            FlowHeader(onBack: { dismiss() })
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    AuthTitle(
                        title: "Créer un compte",
                        subtitle: "Quelques informations, puis on vous montre Pawrise.",
                        subtitleSize: 13.5
                    )

                    VStack(alignment: .leading, spacing: 12) {
                        HStack(alignment: .top, spacing: 12) {
                            LabeledField(label: "Prénom", error: error(.firstName, server: "firstName"), fieldFont: Self.fieldFont, borderColor: Color.Pawrise.borderStrong) {
                                TextField("", text: $form.firstName, prompt: Text("Alex"))
                                    .textContentType(.givenName)
                                    .focused($focus, equals: .firstName)
                                    .submitLabel(.next)
                                    .onSubmit { focus = .lastName }
                            }
                            LabeledField(label: "Nom", error: error(.lastName, server: "lastName"), fieldFont: Self.fieldFont, borderColor: Color.Pawrise.borderStrong) {
                                TextField("", text: $form.lastName, prompt: Text("Martin"))
                                    .textContentType(.familyName)
                                    .focused($focus, equals: .lastName)
                                    .submitLabel(.next)
                                    .onSubmit { focus = .email }
                            }
                        }
                        LabeledField(label: "E-mail", error: error(.email, server: "email"), fieldFont: Self.fieldFont, borderColor: Color.Pawrise.borderStrong) {
                            // `verbatim` : une adresse dans un `LocalizedStringKey` devient un lien (texte bleu).
                            TextField("", text: $form.email, prompt: Text(verbatim: "alex@exemple.fr"))
                                .credentialContentType(.emailAddress)
                                .keyboardType(.emailAddress)
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled()
                                .focused($focus, equals: .email)
                                .submitLabel(.next)
                                .onSubmit { focus = .phone }
                        }
                        LabeledField(label: "Téléphone", error: error(.phone, server: "phoneNumber"), fieldFont: Self.fieldFont, borderColor: Color.Pawrise.borderStrong) {
                            TextField("", text: $form.phone, prompt: Text(verbatim: PhoneInput.placeholder))
                                .textContentType(.telephoneNumber)
                                .keyboardType(.phonePad)
                                .focused($focus, equals: .phone)
                                .accessibilityHint("Facultatif")
                                // Mise en forme pendant la saisie (PhoneNumberKit, région FR par défaut).
                                .onChange(of: form.phone) { _, value in
                                    let formatted = PhoneInput.formatWhileTyping(value)
                                    if formatted != value { form.phone = formatted }
                                }
                        }
                        LabeledField(label: "Mot de passe", error: error(.password, server: "password"), fieldFont: Self.fieldFont, borderColor: Color.Pawrise.borderStrong) {
                            PasswordInput(
                                text: $form.password, prompt: "8 caractères minimum", contentType: .newPassword,
                                focus: $focus, focusValue: .password
                            )
                            .submitLabel(.go)
                            .onSubmit(submit)
                        }

                        TermsCheckbox(isOn: $form.acceptsTerms, error: error(.terms, server: "acceptedTermsVersion"))
                            .onChange(of: form.acceptsTerms) { touched.insert(.terms) }

                        if let errorMessage { ErrorBanner(message: errorMessage) }

                        // Grisé tant que le formulaire est incomplet ou invalide (même style que « Suivant »).
                        Button("Continuer", action: submit)
                            .buttonStyle(.pawrisePrimary(loading: isSubmitting))
                            .disabled(!form.isValid || isSubmitting)
                            .accessibilityHint(form.isValid ? "" : "Remplissez les champs obligatoires et acceptez les conditions pour continuer.")

                        Button("J’ai déjà un compte", action: onLoginInstead)
                            .buttonStyle(.pawriseCompactLink)
                            .frame(maxWidth: .infinity)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 26)
                .padding(.bottom, 24)
            }
            .scrollDismissesKeyboard(.interactively)
            .scrollIndicators(.hidden)
        }
        .onChange(of: focus) { previous, _ in
            if let previous { touched.insert(previous) }
        }
        .pawrisePage()
        .toolbar(.hidden, for: .navigationBar)
    }

    /// Saisie des champs d’inscription : Inter 17 (maquette A02).
    private static let fieldFont = Font.Pawrise.inter(17)

    private func submit() {
        guard form.isValid, !isSubmitting else {
            // Validation au clavier (« Aller ») sur un formulaire incomplet : on montre toutes les erreurs.
            touched = Set(SignupForm.Field.allCases)
            return
        }
        serverErrors = [:]
        focus = nil
        isSubmitting = true
        errorMessage = nil
        let form = form
        Task {
            defer { isSubmitting = false }
            do {
                try await session.register(
                    firstName: form.firstName, lastName: form.lastName, email: form.email,
                    phoneNumber: form.e164Phone, password: form.password
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
/// Style aligné sur la maquette : case 22 pt rayon 6 bordure #C1D4E0, texte Inter 13,5 secondaire.
private struct TermsCheckbox: View {
    @Binding var isOn: Bool
    var error: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Button {
                isOn.toggle()
            } label: {
                HStack(alignment: .top, spacing: 12) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 6)
                            .fill(isOn ? Color.Pawrise.primary : Color.Pawrise.card)
                        RoundedRectangle(cornerRadius: 6)
                            .strokeBorder(isOn ? Color.Pawrise.primary : (error == nil ? Color.Pawrise.borderStrong : Color.Pawrise.alert), lineWidth: 1)
                        if isOn {
                            PawriseIcon(.check, size: 14, lineWidth: 3.4)
                                .foregroundStyle(Color.Pawrise.onPrimary)
                        }
                    }
                    .frame(width: 22, height: 22)
                    Text("J’accepte les conditions d’utilisation et la politique de confidentialité de Pawrise.")
                        .font(Font.Pawrise.inter(13.5, relativeTo: .subheadline))
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .multilineTextAlignment(.leading)
                        .figmaLineHeight(1.35, size: 13.5, relativeTo: .subheadline)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("J’accepte les conditions d’utilisation et la politique de confidentialité")
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
                    TextField("", text: $email, prompt: Text(verbatim: "vous@exemple.fr"))
                        .credentialContentType(.username)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .focused($focus, equals: .email)
                        .disabled(codeSent)
                }

                if codeSent {
                    LabeledField(label: "Code à 6 chiffres") {
                        TextField("", text: $code, prompt: Text(verbatim: "123456"))
                            .textContentType(.oneTimeCode)
                            .keyboardType(.numberPad)
                            .focused($focus, equals: .code)
                            .onChange(of: code) { _, value in code = String(value.filter(\.isNumber).prefix(6)) }
                    }
                    LabeledField(label: "Nouveau mot de passe") {
                        PasswordInput(
                            text: $newPassword, prompt: "8 caractères minimum", contentType: .newPassword,
                            focus: $focus, focusValue: .password
                        )
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

/// Titre d’écran Bricolage 700 26 (135 %) + sous-titre Inter 400 (135 %), écart 8 (maquette « Question »).
private struct AuthTitle: View {
    let title: String
    let subtitle: String
    var subtitleSize: CGFloat = 14

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(Font.Pawrise.largeTitle)
                .foregroundStyle(Color.Pawrise.textPrimary)
                .figmaLineHeight(1.35, size: 26, family: .display, relativeTo: .largeTitle)
                .accessibilityAddTraits(.isHeader)
            Text(subtitle)
                .font(Font.Pawrise.inter(subtitleSize, relativeTo: .subheadline))
                // Figma rend Inter 13,5 environ 2 % plus serré que iOS (mesuré).
                .tracking(subtitleSize == 13.5 ? -0.12 : 0)
                .foregroundStyle(Color.Pawrise.textMuted)
                .figmaLineHeight(1.35, size: subtitleSize, relativeTo: .subheadline)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

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
