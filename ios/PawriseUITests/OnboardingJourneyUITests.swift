import XCTest

/// Parcours de bout en bout dans le simulateur (mode Debug : auth et collier simulés).
/// Chaque étape clé est jointe en capture d'écran au rapport de test (.xcresult).
@MainActor
final class OnboardingJourneyUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() async throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-PawriseUITestReset"]
        app.launch()
        // Attendre la fin de la transition « lancement → accueil » avant de toucher quoi que ce soit.
        XCTAssertTrue(app.staticTexts["Le collier connecté qui suit la santé et la position de votre chien."].waitForExistence(timeout: 10))
        try await Task.sleep(for: .milliseconds(500))
    }

    /// Inscription → découverte → profil du chien → collier → Santé → détail d'un indicateur.
    func testSignupThroughCollarToHealthDashboard() {
        tap(button: "Créer un compte")
        XCTAssertTrue(app.staticTexts["Quelques informations, puis on vous montre Pawrise."].waitForExistence(timeout: 5))
        snapshot("01-inscription")

        let fields = app.textFields
        type(fields.element(boundBy: 0), "Alex")
        type(fields.element(boundBy: 1), "Martin")
        type(fields.element(boundBy: 2), "alex.\(Int.random(in: 1000...9999))@example.com")
        type(app.secureTextFields.element(boundBy: 0), "un-mot-de-passe-solide")

        // Sans les CGU, l'inscription est refusée (RG11).
        tap(button: "Continuer")
        XCTAssertTrue(app.staticTexts["Acceptez les conditions d'utilisation pour continuer."].waitForExistence(timeout: 3))
        app.descendants(matching: .any).matching(identifier: "terms-checkbox").firstMatch.tap()
        tap(button: "Continuer")

        // Découverte (5 étapes)
        XCTAssertTrue(app.staticTexts["Pawrise veille sur votre chien"].waitForExistence(timeout: 5))
        dismissSavePasswordPrompt(within: 3)
        snapshot("02-decouverte")
        for _ in 0..<4 { tap(button: "Suivant") }
        tap(button: "Connecter le collier")

        // Profil du chien (9 étapes)
        XCTAssertTrue(app.staticTexts["Ajoutez une photo de votre chien"].waitForExistence(timeout: 5))
        snapshot("03-profil-photo")
        tap(button: "Suivant")
        type(app.textFields.firstMatch, "Nala")
        tap(button: "Suivant")
        tap(button: "Suivant") // date de naissance facultative
        tap(button: "Femelle")
        tap(button: "Suivant")
        type(app.textFields.firstMatch, "Golden")
        tap(button: "Golden Retriever")
        tap(button: "Suivant")
        type(app.textFields.firstMatch, "28")
        tap(button: "Suivant")
        type(app.textFields.firstMatch, "55")
        tap(button: "Suivant")
        tap(button: "Suivant") // infos santé facultatives
        XCTAssertTrue(app.staticTexts["Le profil de Nala"].waitForExistence(timeout: 5))
        snapshot("04-profil-recapitulatif")
        tap(button: "Continuer")

        // Collier
        XCTAssertTrue(app.staticTexts["Connectons le collier de Nala"].waitForExistence(timeout: 5))
        snapshot("05-collier")
        tap(button: "Saisir le code manuellement")
        type(app.textFields.firstMatch, "PW-ABC123")
        tap(button: "Activer le collier")
        XCTAssertTrue(app.staticTexts["Le collier de Nala est activé"].waitForExistence(timeout: 10))
        snapshot("06-collier-active")
        tap(button: "Voir le tableau de bord de Nala")

        // Santé
        XCTAssertTrue(app.staticTexts["INDICATEURS"].waitForExistence(timeout: 5))
        snapshot("07-sante")
        let heartTile = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Rythme cardiaque'")).firstMatch
        XCTAssertTrue(heartTile.waitForExistence(timeout: 5))
        heartTile.tap()
        XCTAssertTrue(app.staticTexts["78 bpm"].waitForExistence(timeout: 5))
        snapshot("08-detail-rythme")
    }

    /// Compte de démo → onglet Chat IA → question au vrai pawrise-assistant (doit tourner sur :8100).
    func testDemoAccountChatsWithAssistant() throws {
        tap(button: "Se connecter")
        XCTAssertTrue(app.staticTexts["Connexion"].waitForExistence(timeout: 5))
        type(app.textFields.firstMatch, "demo@pawrise.fr")
        type(app.secureTextFields.firstMatch, "pawrise2026")
        app.buttons.matching(NSPredicate(format: "label == 'Se connecter'")).firstMatch.tap()

        XCTAssertTrue(app.staticTexts["INDICATEURS"].waitForExistence(timeout: 8))
        dismissSavePasswordPrompt(within: 3)
        // La fenêtre « Enregistrer le mot de passe ? » peut arriver tard et avaler l'appui sur l'onglet.
        let suggestion = app.buttons["Et son sommeil ?"]
        for _ in 0..<3 where !suggestion.exists {
            tap(button: "Chat IA")
            if suggestion.waitForExistence(timeout: 4) { break }
            dismissSavePasswordPrompt(within: 2)
        }
        XCTAssertTrue(suggestion.waitForExistence(timeout: 5))
        snapshot("09-chat-accueil")
        suggestion.tap()

        let unreachable = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'injoignable'")).firstMatch
        if unreachable.waitForExistence(timeout: 4) {
            snapshot("10-chat-injoignable")
            throw XCTSkip("pawrise-assistant ne tourne pas sur 127.0.0.1:8100")
        }
        // La réponse de l'assistant remplace l'indicateur de saisie : on attend un texte long qui n'est
        // ni l'accueil ni un message d'état (« Je regarde… »).
        let deadline = Date().addingTimeInterval(30)
        var answered = false
        while !answered, Date() < deadline {
            answered = app.staticTexts.allElementsBoundByIndex.contains { element in
                let label = element.label
                return label.count > 20 && !label.hasPrefix("Bonjour") && !label.hasPrefix("Je ")
                    && !label.contains("Assistant de")
            }
            if !answered { Thread.sleep(forTimeInterval: 0.5) }
        }
        XCTAssertTrue(answered, "Pas de réponse de l'assistant en 30 s")
        snapshot("10-chat-reponse")
    }

    // MARK: Helpers

    /// Fenêtres « mot de passe » d'iOS (mot de passe robuste, enregistrer dans le trousseau) : elles recouvrent l'app.
    private var passwordSheetButtons: [XCUIElement] {
        // Pas le bouton ✕ « Fermer » : l'app en a un aussi (profil du chien).
        ["Plus tard", "Not Now", "Choisir mon propre mot de passe", "Choose My Own Password"].map { app.buttons[$0] }
    }

    /// Ferme une fenêtre « mot de passe » d'iOS si elle est affichée (sans attendre).
    @discardableResult
    private func dismissPasswordSheetIfShown() -> Bool {
        for button in passwordSheetButtons where button.exists && button.isHittable {
            button.tap()
            return true
        }
        // « Utiliser un mot de passe robuste ? » : seul le ✕ de cette fenêtre la ferme.
        let strongPasswordTitle = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'mot de passe robuste' OR label CONTAINS 'Strong Password'")).firstMatch
        let close = app.buttons.matching(NSPredicate(format: "identifier == 'xmark'")).firstMatch
        if strongPasswordTitle.exists, close.exists, close.isHittable {
            close.tap()
            return true
        }
        return false
    }

    /// Après une connexion ou une inscription, iOS peut proposer d'enregistrer le mot de passe un peu plus tard.
    private func dismissSavePasswordPrompt(within seconds: TimeInterval = 4) {
        let deadline = Date().addingTimeInterval(seconds)
        while Date() < deadline {
            if dismissPasswordSheetIfShown() { return }
            RunLoop.current.run(until: Date().addingTimeInterval(0.25))
        }
    }

    private func tap(button label: String, file: StaticString = #filePath, line: UInt = #line) {
        dismissPasswordSheetIfShown()
        let button = app.buttons[label].firstMatch
        XCTAssertTrue(button.waitForExistence(timeout: 8), "Bouton introuvable : \(label)", file: file, line: line)
        button.tap()
    }

    private func type(_ element: XCUIElement, _ text: String, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertTrue(element.waitForExistence(timeout: 5), "Champ introuvable", file: file, line: line)
        guard element.elementType == .secureTextField else {
            element.tap()
            element.typeText(text)
            return
        }
        // Sur un champ « nouveau mot de passe », iOS peut ouvrir une fenêtre de suggestion qui prend la saisie :
        // on la ferme et on vérifie que les caractères sont bien arrivés (affichés en points).
        for _ in 0..<3 {
            element.tap()
            RunLoop.current.run(until: Date().addingTimeInterval(1))
            if dismissPasswordSheetIfShown() {
                RunLoop.current.run(until: Date().addingTimeInterval(0.5))
                element.tap()
            }
            element.typeText(text)
            if ((element.value as? String) ?? "").count == text.count { return }
            let typed = ((element.value as? String) ?? "").count
            element.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: typed))
        }
        XCTFail("Impossible de saisir le mot de passe", file: file, line: line)
    }

    private func snapshot(_ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
