import SwiftUI

/// Paramètres : compte, chiens, scénario de données simulées, déconnexion.
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
            List {
                if let user = session.user {
                    Section {
                        LabeledContent("Nom", value: user.fullName)
                        LabeledContent("E-mail", value: user.email)
                        if let phone = user.phoneNumber {
                            LabeledContent("Téléphone", value: phone)
                        }
                        if !user.emailVerified {
                            Button {
                                resendVerification()
                            } label: {
                                HStack {
                                    Label("Renvoyer le lien de confirmation", systemImage: "envelope.badge")
                                    Spacer()
                                    if isSendingVerification { ProgressView() }
                                }
                            }
                            .disabled(isSendingVerification)
                        }
                    } header: {
                        Text("Compte")
                    } footer: {
                        if !user.emailVerified {
                            Text(verificationMessage ?? "Adresse e-mail pas encore confirmée : ouvrez le lien reçu par e-mail.")
                        }
                    }
                }

                Section("Mes chiens") {
                    ForEach(dogs.dogs) { dog in
                        HStack(spacing: 12) {
                            DogAvatar(photo: dog.photo, size: 44)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(dog.name)
                                    .font(Font.Pawrise.headline)
                                    .foregroundStyle(Color.Pawrise.textPrimary)
                                if !dog.subtitle.isEmpty {
                                    Text(dog.subtitle)
                                        .font(Font.Pawrise.subheadline)
                                        .foregroundStyle(Color.Pawrise.textMuted)
                                }
                            }
                        }
                        .padding(.vertical, 2)
                        .accessibilityElement(children: .combine)
                    }
                }

                Section {
                    Picker("Scénario", selection: $environment.healthScenario) {
                        ForEach(HealthScenario.allCases) { scenario in
                            Text(scenario.label).tag(scenario)
                        }
                    }
                    .pickerStyle(.segmented)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
                } header: {
                    Text("Données simulées")
                } footer: {
                    Text("Tant que le collier n'envoie pas encore de vraies mesures, l'onglet Pawrise affiche des données de démonstration. Choisissez « Anomalie » pour voir comment une mesure inhabituelle est signalée.")
                }

                Section {
                    Button(role: .destructive) {
                        isLoggingOut = true
                        Task { await environment.logout() }
                    } label: {
                        HStack {
                            Text("Se déconnecter")
                            Spacer()
                            if isLoggingOut { ProgressView() }
                        }
                    }
                    .disabled(isLoggingOut)
                } footer: {
                    Text(Self.versionLabel)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 12)
                }
            }
            .font(Font.Pawrise.body)
            .scrollContentBackground(.hidden)
            .background(Color.Pawrise.page.ignoresSafeArea())
            .navigationTitle("Paramètres")
            .task { await session.refreshUser() }
            .refreshable { await session.refreshUser() }
        }
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

    private static var versionLabel: String {
        let info = Bundle.main.infoDictionary
        let version = info?["CFBundleShortVersionString"] as? String ?? "—"
        let build = info?["CFBundleVersion"] as? String ?? "—"
        return "Pawrise \(version) (\(build))"
    }
}
