import XCTest

/// Base des tests d'interface : lance l'app depuis un état vierge et fournit les gestes communs.
@MainActor
class PawriseUITestCase: XCTestCase {
    var app: XCUIApplication!

    /// Arguments supplémentaires passés à l'app (ex. scénario de données).
    var extraLaunchArguments: [String] { [] }

    override func setUp() async throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-PawriseUITestReset"] + extraLaunchArguments
        app.launch()
        // Attendre la fin de la transition « lancement → accueil » avant de toucher quoi que ce soit.
        XCTAssertTrue(app.staticTexts["Le collier connecté qui suit la santé et la position de votre chien."].waitForExistence(timeout: 10))
        try await Task.sleep(for: .milliseconds(500))
    }

    /// Profil du chien (9 étapes) puis collier, jusqu'au tableau de bord de Nala.
    func createNalaWithCollar(snapshots: Bool) {
        XCTAssertTrue(app.staticTexts["Ajoutez une photo de votre chien"].waitForExistence(timeout: 5))
        if snapshots { snapshot("03-profil-photo") }
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
        if snapshots { snapshot("04-profil-recapitulatif") }
        tap(button: "Continuer")

        XCTAssertTrue(app.staticTexts["Connectons le collier de Nala"].waitForExistence(timeout: 5))
        if snapshots { snapshot("05-collier") }
        tap(button: "Saisir le code manuellement")
        type(app.textFields.firstMatch, "PW-ABC123")
        tap(button: "Activer le collier")
        XCTAssertTrue(app.staticTexts["Le collier de Nala est activé"].waitForExistence(timeout: 10))
        if snapshots { snapshot("06-collier-active") }
        tap(button: "Voir le tableau de bord de Nala")
    }

    /// Fenêtres « mot de passe » d'iOS (mot de passe robuste, enregistrer dans le trousseau) : elles recouvrent l'app.
    var passwordSheetButtons: [XCUIElement] {
        // Pas le bouton ✕ « Fermer » : l'app en a un aussi (profil du chien).
        ["Plus tard", "Not Now", "Choisir mon propre mot de passe", "Choose My Own Password"].map { app.buttons[$0] }
    }

    /// Ferme une fenêtre « mot de passe » d'iOS si elle est affichée (sans attendre).
    @discardableResult
    func dismissPasswordSheetIfShown() -> Bool {
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
    func dismissSavePasswordPrompt(within seconds: TimeInterval = 4) {
        let deadline = Date().addingTimeInterval(seconds)
        while Date() < deadline {
            if dismissPasswordSheetIfShown() { return }
            RunLoop.current.run(until: Date().addingTimeInterval(0.25))
        }
    }

    func tap(button label: String, file: StaticString = #filePath, line: UInt = #line) {
        dismissPasswordSheetIfShown()
        let button = app.buttons[label].firstMatch
        XCTAssertTrue(button.waitForExistence(timeout: 8), "Bouton introuvable : \(label)", file: file, line: line)
        button.tap()
    }

    func type(_ element: XCUIElement, _ text: String, file: StaticString = #filePath, line: UInt = #line) {
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

    func snapshot(_ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
