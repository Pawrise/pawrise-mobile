import SwiftUI

/// Paramètres : compte, confidentialité, chiens, scénario de données simulées, déconnexion, suppression du compte.
/// Langage visuel de la maquette (US-I « Paramètres · partage ») : titre Bricolage 26, titres de section
/// Inter 600 13 en capitales, cartes blanches de rayon 24 avec filet #D5E3EC, pastilles d'icône de 36 pt.
struct SettingsView: View {
    @Environment(AppEnvironment.self) private var environment
    @Environment(SessionStore.self) private var session
    @Environment(DogStore.self) private var dogs
    @State private var isLoggingOut = false
    @State private var verificationMessage: String?
    @State private var isSendingVerification = false

    var body: some View {
        @Bindable var environment = environment
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text("Paramètres")
                        .pawriseFont(.display, 26, .bold, lineHeight: 1.2, relativeTo: .largeTitle)
                        .foregroundStyle(Color.Pawrise.textPrimary)
                        .accessibilityAddTraits(.isHeader)
                        .padding(.top, 14)
                        .padding(.bottom, 20)

                    if let user = session.user {
                        // Carte de compte sans titre de section, comme la carte de profil de la maquette.
                        accountCard(user)
                            .padding(.bottom, 20)
                    }

                    SettingsSectionTitle("Confidentialité")
                    PrivacySection()
                        .padding(.bottom, 21)

                    SettingsSectionTitle("Légal")
                    LegalSection(termsURL: environment.config.termsURL, privacyURL: environment.config.privacyURL)
                        .padding(.bottom, 21)

                    SettingsSectionTitle("Mes chiens")
                    SettingsCard {
                        ForEach(Array(dogs.dogs.enumerated()), id: \.element.id) { index, dog in
                            if index > 0 { SettingsDivider() }
                            dogRow(dog)
                        }
                    }
                    .padding(.bottom, 21)

                    SettingsSectionTitle("Données simulées")
                    SettingsCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Picker("Scénario", selection: $environment.healthScenario) {
                                ForEach(HealthScenario.allCases) { scenario in
                                    Text(scenario.label).tag(scenario)
                                }
                            }
                            .pickerStyle(.segmented)
                            Text("Tant que le collier n'envoie pas encore de vraies mesures, l'onglet Pawrise affiche des données de démonstration. Choisissez « Anomalie » pour voir comment une mesure inhabituelle est signalée.")
                                .pawriseFont(.inter, 12, lineHeight: 1.35, relativeTo: .caption)
                                .foregroundStyle(Color.Pawrise.textMuted)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .padding(16)
                    }
                    .padding(.bottom, 20)

                    VStack(spacing: 8) {
                        Button {
                            isLoggingOut = true
                            Task { await environment.logout() }
                        } label: {
                            HStack(spacing: 8) {
                                Text("Se déconnecter")
                                if isLoggingOut { ProgressView() }
                            }
                        }
                        .buttonStyle(SettingsActionButtonStyle(tint: Color.Pawrise.textPrimary, border: Color.Pawrise.border))
                        .disabled(isLoggingOut)

                        NavigationLink {
                            DeleteAccountView()
                        } label: {
                            Text("Supprimer mon compte")
                        }
                        .buttonStyle(SettingsActionButtonStyle(tint: Color.Pawrise.alert, border: Color.Pawrise.alert.opacity(0.4)))
                        .disabled(isLoggingOut)
                        .accessibilityIdentifier("open-delete-account")
                    }
                    .padding(.bottom, 20)

                    Text(Self.versionLabel)
                        .pawriseFont(.inter, 12, lineHeight: 1.35, relativeTo: .caption)
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .frame(maxWidth: .infinity)
                        .padding(.bottom, 24)
                }
                .padding(.horizontal, 16)
            }
            .scrollIndicators(.hidden)
            .background(Color.Pawrise.page.ignoresSafeArea())
            // Le contenu défilant ne doit pas passer sous la barre d'état (pas de barre de navigation ici).
            .overlay(alignment: .top) {
                Color.Pawrise.page
                    .ignoresSafeArea(edges: .top)
                    .frame(height: 0)
            }
            .toolbar(.hidden, for: .navigationBar)
            .task { await session.refreshUser() }
            .refreshable { await session.refreshUser() }
        }
    }

    private func accountCard(_ user: AuthUser) -> some View {
        SettingsCard {
            HStack(spacing: 12) {
                Text(Self.initials(user.fullName))
                    .pawriseFont(.inter, 19, .bold, relativeTo: .title3)
                    .foregroundStyle(Color.Pawrise.onPrimary)
                    .frame(width: 56, height: 56)
                    .background(Color.Pawrise.primary, in: .rect(cornerRadius: 16))
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    Text(user.fullName)
                        .pawriseFont(.inter, 16, .bold, lineHeight: 1.35, relativeTo: .headline)
                        .foregroundStyle(Color.Pawrise.textPrimary)
                    Text(user.email)
                        .pawriseFont(.inter, 12.5, lineHeight: 1.35, relativeTo: .caption)
                        .foregroundStyle(Color.Pawrise.textMuted)
                    if let phone = user.phoneNumber {
                        Text(phone)
                            .pawriseFont(.inter, 12.5, lineHeight: 1.35, relativeTo: .caption)
                            .foregroundStyle(Color.Pawrise.textMuted)
                    }
                }
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 13)
            .padding(.vertical, 13)
            .accessibilityElement(children: .combine)

            if !user.emailVerified {
                SettingsDivider()
                VStack(alignment: .leading, spacing: 8) {
                    Button {
                        resendVerification()
                    } label: {
                        HStack(spacing: 12) {
                            SettingsIconTile(glyph: .send)
                            Text("Renvoyer le lien de confirmation")
                                .pawriseFont(.inter, 14, .medium, lineHeight: 1.35, relativeTo: .subheadline)
                                .foregroundStyle(Color.Pawrise.primary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            if isSendingVerification { ProgressView() }
                        }
                        .contentShape(.rect)
                    }
                    .buttonStyle(.plain)
                    .disabled(isSendingVerification)
                    Text(verificationMessage ?? "Adresse e-mail pas encore confirmée : ouvrez le lien reçu par e-mail.")
                        .pawriseFont(.inter, 12, lineHeight: 1.35, relativeTo: .caption)
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
            }
        }
    }

    private func dogRow(_ dog: Dog) -> some View {
        HStack(spacing: 12) {
            DogAvatar(photo: dog.photo, size: 44, cornerRadius: 16)
            VStack(alignment: .leading, spacing: 2) {
                Text(dog.name)
                    .pawriseFont(.inter, 15, .semibold, lineHeight: 1.35, relativeTo: .headline)
                    .foregroundStyle(Color.Pawrise.textPrimary)
                if !dog.subtitle.isEmpty {
                    Text(dog.subtitle)
                        .pawriseFont(.inter, 12, lineHeight: 1.35, relativeTo: .caption)
                        .foregroundStyle(Color.Pawrise.textMuted)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let collar = dog.collar {
                Text(collar.code)
                    .pawriseFont(.mono, 12, lineHeight: 1.35, relativeTo: .caption)
                    .foregroundStyle(Color.Pawrise.textMuted)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .accessibilityElement(children: .combine)
    }

    private func resendVerification() {
        isSendingVerification = true
        Task {
            defer { isSendingVerification = false }
            do {
                try await session.resendVerificationEmail()
                verificationMessage = "Nouveau lien envoyé. Il est valable 24 heures."
            } catch {
                verificationMessage = error.localizedDescription
            }
        }
    }

    static func initials(_ name: String) -> String {
        name.split(separator: " ").prefix(2).compactMap(\.first).map { String($0).uppercased() }.joined()
    }

    private static var versionLabel: String {
        let info = Bundle.main.infoDictionary
        let version = info?["CFBundleShortVersionString"] as? String ?? "—"
        let build = info?["CFBundleVersion"] as? String ?? "—"
        return "Pawrise \(version) (\(build))"
    }
}

// MARK: - Légal

/// Paramètres → Légal : liens vers les CGU (version acceptée à l'inscription) et la politique de confidentialité.
/// Les adresses viennent de la configuration (PAWRISE_TERMS_URL, PAWRISE_PRIVACY_URL) ; tant qu'elles ne sont pas
/// publiées, la ligne l'indique au lieu d'ouvrir une page vide.
struct LegalSection: View {
    let termsURL: URL?
    let privacyURL: URL?
    @Environment(\.openURL) private var openURL
    @State private var unpublished: String?

    var body: some View {
        SettingsCard {
            VStack(alignment: .leading, spacing: 0) {
                row("Conditions d’utilisation", url: termsURL)
                row("Politique de confidentialité", url: privacyURL)
            }
            .padding(.vertical, 8)
        }
        .alert(unpublished ?? "", isPresented: Binding(get: { unpublished != nil }, set: { if !$0 { unpublished = nil } })) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Le document sera disponible ici dès sa publication. Version des CGU acceptée : \(LegalTerms.version).")
        }
    }

    private func row(_ title: String, url: URL?) -> some View {
        Button {
            if let url { openURL(url) } else { unpublished = title }
        } label: {
            HStack(spacing: 10) {
                Text(title)
                    .pawriseFont(.inter, 14, lineHeight: 1.35, relativeTo: .subheadline)
                    .foregroundStyle(Color.Pawrise.textPrimary)
                Text("›")
                    .pawriseFont(.inter, 13, relativeTo: .footnote)
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .accessibilityHidden(true)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 13)
            .padding(.vertical, 5)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityHint(url == nil ? "Pas encore publié" : "Ouvre la page dans le navigateur")
    }
}

// MARK: - Éléments de la page Paramètres

/// Titre de section en capitales (Inter 600 12, référence I02).
struct SettingsSectionTitle: View {
    let title: String

    init(_ title: String) { self.title = title }

    var body: some View {
        Text(title.uppercased())
            .pawriseFont(.inter, 12, .semibold, lineHeight: 1.4, relativeTo: .footnote)
            .foregroundStyle(Color.Pawrise.textMuted)
            .padding(.leading, 1)
            .padding(.bottom, 19)
            .accessibilityAddTraits(.isHeader)
            .accessibilityLabel(title)
    }
}

/// Carte blanche de rayon 24 avec filet, lignes empilées.
struct SettingsCard<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            content
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.Pawrise.card, in: .rect(cornerRadius: 24))
        .overlay { RoundedRectangle(cornerRadius: 24).strokeBorder(Color.Pawrise.border, lineWidth: 1) }
    }
}

struct SettingsDivider: View {
    var body: some View {
        Rectangle()
            .fill(Color.Pawrise.border)
            .frame(height: 1)
            .padding(.leading, 64)
            .accessibilityHidden(true)
    }
}

/// Icône de ligne (18 pt, trait 2, couleur atténuée), sans pastille comme dans la maquette.
struct SettingsIconTile: View {
    let glyph: PawriseGlyph

    var body: some View {
        PawriseIcon(glyph, size: 20, lineWidth: 1.9)
            .foregroundStyle(Color.Pawrise.textMuted)
            .frame(width: 16)
    }
}

/// Bouton large blanc à filet (« Se déconnecter », « Supprimer mon compte »).
private struct SettingsActionButtonStyle: ButtonStyle {
    let tint: Color
    let border: Color
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .pawriseFont(.inter, 14, .semibold, lineHeight: 1.35, relativeTo: .subheadline)
            .foregroundStyle(tint)
            .frame(maxWidth: .infinity, minHeight: 48)
            .background(Color.Pawrise.card, in: .rect(cornerRadius: 16))
            .overlay { RoundedRectangle(cornerRadius: 16).strokeBorder(border, lineWidth: 1) }
            .opacity(isEnabled ? (configuration.isPressed ? 0.7 : 1) : 0.5)
            .contentShape(.rect(cornerRadius: 16))
    }
}

/// Interrupteur de la maquette : rail 44 × 24 (#0A5C78 activé, #C1D4E0 désactivé), pastille blanche de 20 pt.
/// L'accessibilité reste celle d'un interrupteur système (trait, valeur, identifiant).
struct PawriseSwitchStyle: ToggleStyle {
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: 12) {
            configuration.label
                .frame(maxWidth: .infinity, alignment: .leading)
            Capsule()
                .fill(configuration.isOn ? Color.Pawrise.primary : Color.Pawrise.borderStrong)
                .frame(width: 44, height: 24)
                .overlay(alignment: configuration.isOn ? .trailing : .leading) {
                    Circle()
                        .fill(Color.white)
                        .frame(width: 20, height: 20)
                        .padding(2)
                }
                .animation(.snappy(duration: 0.2), value: configuration.isOn)
        }
        .contentShape(.rect)
        .onTapGesture {
            if isEnabled { configuration.isOn.toggle() }
        }
        .accessibilityRepresentation {
            Toggle(isOn: configuration.$isOn) { configuration.label }
        }
    }
}
