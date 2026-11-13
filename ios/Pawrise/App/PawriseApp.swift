import SwiftUI

@main
struct PawriseApp: App {
    @State private var environment: AppEnvironment

    init() {
        #if DEBUG
        if CommandLine.arguments.contains("-PawriseUITestReset") {
            AppEnvironment.resetLocalDataForUITests()
        }
        #endif
        let environment = AppEnvironment()
        #if DEBUG
        // Captures d'écran : « -PawriseHealthScenario anomaly » affiche directement le scénario Anomalie.
        if let raw = UserDefaults.standard.string(forKey: "PawriseHealthScenario"),
           let scenario = HealthScenario(rawValue: raw) {
            environment.healthScenario = scenario
        }
        #endif
        _environment = State(initialValue: environment)
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(environment)
                .environment(environment.session)
                .environment(environment.dogs)
                .tint(Color.Pawrise.primary)
                .preferredColorScheme(.light) // le Figma n'a pas encore de thème sombre
                .onOpenURL { url in _ = SocialSignIn.handle(url: url) }
                .task { await environment.session.restore() }
        }
    }
}

/// Vrai quand l'app est lancée par les tests d'interface (Debug uniquement).
enum UITestMode {
    static let isActive: Bool = {
        #if DEBUG
        CommandLine.arguments.contains("-PawriseUITestReset")
        #else
        false
        #endif
    }()
}

extension View {
    /// Type de contenu des champs d'identifiants. Désactivé pendant les tests d'interface :
    /// les fenêtres « mot de passe robuste » et « enregistrer le mot de passe » d'iOS les rendent instables.
    func credentialContentType(_ type: UITextContentType) -> some View {
        textContentType(UITestMode.isActive ? nil : type)
    }
}
