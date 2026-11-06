import SwiftUI

/// Aiguillage principal :
/// déconnecté → accueil/connexion ; connecté sans chien → découverte puis profil du chien + collier ;
/// connecté avec un chien → onglets.
struct RootView: View {
    @Environment(SessionStore.self) private var session

    var body: some View {
        Group {
            switch session.phase {
            case .restoring:
                LaunchView()
            case .signedOut:
                AuthFlowView()
                    .transition(.opacity)
            case let .signedIn(user):
                SignedInRoot(user: user)
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.25), value: session.phase)
    }
}

private struct SignedInRoot: View {
    let user: AuthUser
    @Environment(AppEnvironment.self) private var environment
    @Environment(DogStore.self) private var dogs

    var body: some View {
        Group {
            if !dogs.isLoaded {
                LaunchView()
            } else if dogs.dogs.isEmpty {
                if environment.onboarding.hasSeenLearnFlow(user.id) {
                    AddDogFlowView(
                        onFinish: {},
                        onClose: { environment.onboarding.setLearnFlowSeen(false, for: user.id) }
                    )
                } else {
                    LearnFlowView {
                        environment.onboarding.setLearnFlowSeen(true, for: user.id)
                    }
                }
            } else {
                MainTabView()
            }
        }
        .task(id: user.id) { await dogs.load(for: user) }
    }
}

struct LaunchView: View {
    var body: some View {
        VStack(spacing: 16) {
            PawriseMark()
            ProgressView()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.Pawrise.page.ignoresSafeArea())
    }
}
