import SwiftUI

/// S25 Détruire le compte (US-35, US-36) : on liste ce qui va disparaître, puis une confirmation finale.
/// Appelle `DELETE /v1/me` ; en cas de succès l'app revient à l'accueil, déconnectée.
struct DeleteAccountView: View {
    @Environment(AppEnvironment.self) private var environment
    @Environment(SessionStore.self) private var session
    @Environment(DogStore.self) private var dogs

    @State private var isConfirming = false
    @State private var isDeleting = false
    @State private var errorMessage: String?

    var body: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: 12) {
                    PawriseIcon(.alert, size: 32)
                        .foregroundStyle(Color.Pawrise.alert)
                    Text("Cette action est définitive")
                        .font(Font.Pawrise.title2)
                        .foregroundStyle(Color.Pawrise.textPrimary)
                    Text("Votre compte ne pourra pas être récupéré. Pour utiliser Pawrise à nouveau, il faudra en créer un autre.")
                        .foregroundStyle(Color.Pawrise.textMuted)
                }
                .padding(.vertical, 8)
            }

            Section("Ce qui sera supprimé") {
                consequence("person.crop.circle.badge.xmark", "Votre compte : nom, e-mail, téléphone, mot de passe")
                consequence("iphone.slash", "Vos connexions sur tous vos appareils")
                consequence("hand.raised", "Vos choix de confidentialité et votre acceptation des CGU")
                if dogs.dogs.isEmpty {
                    consequence("pawprint", "Les profils de chiens enregistrés sur ce téléphone")
                } else if dogs.dogs.count == 1 {
                    consequence("pawprint", "Le profil de \(Self.names(dogs.dogs.map(\.name))) enregistré sur ce téléphone")
                } else {
                    consequence("pawprint", "Les profils de \(Self.names(dogs.dogs.map(\.name))) enregistrés sur ce téléphone")
                }
            }

            if let email = session.user?.email {
                Section {
                    LabeledContent("Compte", value: email)
                }
            }

            Section {
                Button(role: .destructive) {
                    isConfirming = true
                } label: {
                    HStack {
                        Text("Supprimer définitivement mon compte")
                        Spacer()
                        if isDeleting { ProgressView() }
                    }
                }
                .disabled(isDeleting)
                .accessibilityIdentifier("delete-account-button")
            } footer: {
                if let errorMessage {
                    Text(errorMessage).foregroundStyle(Color.Pawrise.alert)
                }
            }
        }
        .font(Font.Pawrise.body)
        .scrollContentBackground(.hidden)
        .background(Color.Pawrise.page.ignoresSafeArea())
        .navigationTitle("Supprimer le compte")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(isDeleting)
        .confirmationDialog("Supprimer votre compte ?", isPresented: $isConfirming, titleVisibility: .visible) {
            Button("Supprimer définitivement", role: .destructive) { delete() }
            Button("Annuler", role: .cancel) {}
        } message: {
            Text("Vous ne pourrez pas revenir en arrière.")
        }
    }

    private func consequence(_ symbol: String, _ text: String) -> some View {
        Label {
            Text(text).foregroundStyle(Color.Pawrise.textPrimary)
        } icon: {
            Image(systemName: symbol).foregroundStyle(Color.Pawrise.alert)
        }
    }

    private func delete() {
        isDeleting = true
        errorMessage = nil
        Task {
            do {
                try await environment.deleteAccount()
                // La session est effacée : RootView revient à l'accueil.
            } catch {
                errorMessage = error.localizedDescription
                isDeleting = false
            }
        }
    }

    /// « Nala », « Nala et Oslo », « Nala, Oslo et Rex ».
    static func names(_ names: [String]) -> String {
        guard let last = names.last else { return "" }
        return names.count == 1 ? last : names.dropLast().joined(separator: ", ") + " et " + last
    }
}
