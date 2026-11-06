import SwiftUI

/// Paramètres : compte, chiens, scénario de données simulées, déconnexion.
struct SettingsView: View {
    @Environment(AppEnvironment.self) private var environment
    @Environment(SessionStore.self) private var session
    @Environment(DogStore.self) private var dogs
    @State private var isLoggingOut = false

    var body: some View {
        @Bindable var environment = environment
        NavigationStack {
            List {
                if let user = session.user {
                    Section("Compte") {
                        LabeledContent("Nom", value: user.name)
                        LabeledContent("E-mail", value: user.email)
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
        }
    }

    private static var versionLabel: String {
        let info = Bundle.main.infoDictionary
        let version = info?["CFBundleShortVersionString"] as? String ?? "—"
        let build = info?["CFBundleVersion"] as? String ?? "—"
        return "Pawrise \(version) (\(build))"
    }
}
