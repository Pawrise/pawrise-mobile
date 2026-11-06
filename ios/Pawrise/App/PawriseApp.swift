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
        _environment = State(initialValue: AppEnvironment())
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
