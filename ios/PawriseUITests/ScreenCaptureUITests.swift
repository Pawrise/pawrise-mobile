import XCTest

/// Captures écran par écran, nommées comme les cadres du Figma « Pawrise-App » (A01 = US-A écran 01…).
/// À lancer en mode simulé sur un simulateur 390 × 844 (même taille que la maquette) :
///   xcodebuild test … PAWRISE_AUTH_MODE=mock -only-testing:PawriseUITests/ScreenCaptureUITests
/// Les images sont jointes au .xcresult (xcresulttool export attachments).
final class ScreenCaptureUITests: PawriseUITestCase {
    /// H03 : la réponse de l'assistant est retenue 6 s pour capturer l'indicateur de saisie.
    override var extraLaunchArguments: [String] { ["-PawriseAssistantMinimumDelay", "6"] }

    /// US-J : connexion.
    func testCaptureLoginScreen() {
        snapshot("A01")
        tap(button: "Se connecter")
        XCTAssertTrue(app.staticTexts["Connexion"].waitForExistence(timeout: 5))
        type(app.textFields.firstMatch, "alex@example.com")
        type(app.secureTextFields.firstMatch, "chien-26")
        dismissKeyboard()
        // iOS exclut le contenu des champs sécurisés des captures d’écran : le champ paraît vide sur l’image,
        // mais les 8 caractères sont bien saisis (vérifié ici).
        XCTAssertEqual((app.secureTextFields.firstMatch.value as? String)?.count, 8)
        snapshot("J02")
    }

    /// US-A : inscription.
    func testCaptureSignupScreen() {
        tap(button: "Créer un compte")
        XCTAssertTrue(app.staticTexts["Quelques informations, puis on vous montre Pawrise."].waitForExistence(timeout: 5))
        let fields = app.textFields
        type(fields.element(boundBy: 0), "Alex")
        type(fields.element(boundBy: 1), "Martin")
        type(fields.element(boundBy: 2), "alex@example.com")
        type(app.secureTextFields.element(boundBy: 0), "chien-26")
        dismissKeyboard()
        // La saisie a fait défiler le formulaire : revenir en haut comme la maquette.
        app.swipeDown()
        settle()
        snapshot("A02")
    }

    /// US-A : découverte, profil du chien, collier, première homepage.
    func testCaptureOnboardingScreens() {
        signUp()
        for (index, label) in ["A03", "A04", "A05", "A06", "A07"].enumerated() {
            settle()
            snapshot(label)
            tap(button: index == 4 ? "Connecter le collier" : "Suivant")
        }

        XCTAssertTrue(app.staticTexts["Ajoutez une photo de votre chien"].waitForExistence(timeout: 5))
        // Maquette A08 : la première photo d’exemple est choisie.
        tap(button: "Photo d’exemple 1")
        settle()
        snapshot("A08")
        tap(button: "Suivant")
        type(app.textFields.firstMatch, "Nala")
        dismissKeyboard()
        snapshot("A09")
        tap(button: "Suivant")
        // Maquette A10 : 12 / 04 / 2023.
        type(app.textFields.firstMatch, "12042023")
        dismissKeyboard()
        settle()
        snapshot("A10")
        tap(button: "Suivant")
        tap(button: "Femelle")
        settle()
        snapshot("A11")
        tap(button: "Suivant")
        type(app.textFields.firstMatch, "Golden")
        // Maquette A12 : la suggestion « Golden Retriever » est choisie, clavier fermé.
        tap(button: "Golden Retriever")
        dismissKeyboard()
        settle()
        snapshot("A12")
        tap(button: "Suivant")
        type(app.textFields.firstMatch, "28")
        dismissKeyboard()
        snapshot("A13")
        tap(button: "Suivant")
        type(app.textFields.firstMatch, "55")
        dismissKeyboard()
        snapshot("A14")
        tap(button: "Suivant")
        settle()
        snapshot("A15")
        tap(button: "Suivant")
        XCTAssertTrue(app.staticTexts["Le profil de Nala"].waitForExistence(timeout: 5))
        settle()
        snapshot("A16")
        tap(button: "Continuer")

        XCTAssertTrue(app.staticTexts["Connectons le collier de Nala"].waitForExistence(timeout: 5))
        settle()
        snapshot("A17")
        tap(button: "Scanner le QR code")
        settle(1.5)
        snapshot("A18")
        tap(button: "Saisir le code manuellement")
        type(app.textFields.firstMatch, "PW-ABC123")
        // Fermer le clavier avant d’activer : la capture A19 ne doit pas saisir son animation.
        dismissKeyboard()
        tap(button: "Activer le collier")
        // Laisser la transition se terminer, sans dépasser l’activation simulée (1,5 s).
        settle(0.4)
        snapshot("A19")
        XCTAssertTrue(app.staticTexts["Le collier de Nala est activé"].waitForExistence(timeout: 10))
        settle()
        snapshot("A20")
        tap(button: "Voir le tableau de bord de Nala")
        XCTAssertTrue(app.staticTexts["INDICATEURS"].waitForExistence(timeout: 5))
        settle()
        snapshot("A21")
    }

    /// US-B : homepage et détail de chaque indicateur, scénario normal.
    func testCaptureHealthScreens() {
        captureHealth(prefix: "B")
    }

    /// US-H : Chat IA, H02 à H06 (nécessite pawrise-assistant sur 127.0.0.1:8100 pour les réponses).
    func testCaptureChatScreens() {
        logInDemo()
        dismissSavePasswordPrompt(within: 6)
        tap(button: "Chat IA")
        let suggestion = app.buttons["Et son sommeil ?"]
        XCTAssertTrue(suggestion.waitForExistence(timeout: 8))
        settle()
        snapshot("H02")
        suggestion.tap()
        snapshot("H03")
        waitForAssistantAnswer(count: 1)
        settle(1)
        snapshot("H04")
        let input = app.textFields.firstMatch.exists ? app.textFields.firstMatch : app.textViews.firstMatch
        type(input, "Et son activité ?")
        // Maquette sans clavier : on le ferme en touchant le fil, le brouillon reste dans le champ.
        app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH 'Bonjour'")).firstMatch.tap()
        settle(0.8)
        snapshot("H05")
        // H06 : question envoyée, seconde réponse.
        app.buttons["Envoyer"].tap()
        waitForAssistantAnswer(count: 2)
        // Maquette sans clavier : toucher le fil le ferme.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.2)).tap()
        settle(1)
        snapshot("H06")
    }

    /// Attend que le fil contienne `count` réponses de l'assistant (textes longs hors accueil et états).
    private func waitForAssistantAnswer(count: Int, timeout: TimeInterval = 40) {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            let typing = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH 'Je '")).count > 0
                || app.otherElements["Pawrise prépare sa réponse"].exists
            if !typing, answerCount() >= count { return }
            settle(0.5)
        }
    }

    private func answerCount() -> Int {
        app.staticTexts.allElementsBoundByIndex.filter { element in
            let label = element.label
            return label.count > 12 && !label.hasPrefix("Bonjour") && !label.hasPrefix("Je ")
                && !label.contains("Assistant de") && !label.hasPrefix("Vous") && label != "Contacter un vétérinaire"
                && label != "Et son sommeil ?" && label != "Et son activité ?"
        }.count
    }

    /// Paramètres (référence de style : US-I « Paramètres · partage »).
    func testCaptureSettingsScreen() {
        logInDemo()
        dismissSavePasswordPrompt(within: 6)
        tap(button: "Paramètres")
        settle(1.5)
        snapshot("I02")
        // Bas de page : Légal et actions du compte.
        app.swipeUp()
        settle(1)
        snapshot("I02L")
    }
}

/// Parcours et gestes partagés par les deux classes de capture.
extension PawriseUITestCase {
    func captureHealth(prefix: String) {
        logInDemo()
        dismissSavePasswordPrompt(within: 6)
        settle(1)
        snapshot("\(prefix)01")
        let indicators = ["Rythme cardiaque", "Fibrillation atriale", "Activité", "Sommeil", "Température", "Batterie du collier"]
        for (index, name) in indicators.enumerated() {
            let tile = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", name)).firstMatch
            dismissPasswordSheetIfShown()
            for _ in 0..<4 where !(tile.exists && tile.isHittable) { app.swipeUp() }
            tile.tap()
            settle(1.2)
            snapshot(String(format: "%@%02d", prefix, index + 2))
            // Fermer le panneau (✕), puis attendre le retour de la homepage.
            let close = app.buttons["Fermer"].firstMatch
            if close.exists, close.isHittable { close.tap() } else { app.swipeDown(velocity: .fast) }
            let home = app.staticTexts["INDICATEURS"]
            for _ in 0..<3 where !home.isHittable {
                settle(0.8)
                if !home.isHittable { app.swipeDown(velocity: .fast) }
            }
        }
    }

    func logInDemo() {
        tap(button: "Se connecter")
        XCTAssertTrue(app.staticTexts["Connexion"].waitForExistence(timeout: 5))
        type(app.textFields.firstMatch, "demo@pawrise.fr")
        type(app.secureTextFields.firstMatch, "pawrise2026")
        app.buttons.matching(NSPredicate(format: "label == 'Se connecter'")).firstMatch.tap()
        XCTAssertTrue(app.staticTexts["INDICATEURS"].waitForExistence(timeout: 8))
        dismissSavePasswordPrompt(within: 3)
    }

    func signUp() {
        tap(button: "Créer un compte")
        let fields = app.textFields
        type(fields.element(boundBy: 0), "Alex")
        type(fields.element(boundBy: 1), "Martin")
        type(fields.element(boundBy: 2), "alex.\(Int.random(in: 1000...9999))@example.com")
        type(app.secureTextFields.element(boundBy: 0), "un-mot-de-passe-solide")
        dismissPasswordSheetIfShown()
        let terms = app.descendants(matching: .any).matching(identifier: "terms-checkbox").firstMatch
        for _ in 0..<3 where !terms.isHittable { app.swipeUp() }
        terms.tap()
        tap(button: "Continuer")
        if !app.staticTexts["Pawrise veille sur votre chien"].waitForExistence(timeout: 8) { snapshot("ECHEC-inscription") }
        XCTAssertTrue(app.staticTexts["Pawrise veille sur votre chien"].exists)
        dismissSavePasswordPrompt(within: 3)
    }

    func settle(_ seconds: TimeInterval = 0.8) {
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
    }

    /// Ferme le clavier en touchant un texte neutre (sous-titre ou question de l'écran), jamais un bouton.
    func dismissKeyboard() {
        guard app.keyboards.count > 0 else { return }
        let neutral = app.staticTexts.allElementsBoundByIndex.first { text in
            text.isHittable && text.frame.minY > 60 && text.label.count > 12 && !text.label.contains("Retour")
        }
        if let neutral { neutral.tap() } else { app.keyboards.buttons.allElementsBoundByIndex.last?.tap() }
        settle(0.6)
        // Pavé numérique (pas de touche Retour) : un glissement vers le bas sur le contenu ferme le clavier.
        if app.keyboards.count > 0, let neutral {
            neutral.swipeDown()
            settle(0.6)
        }
    }
}

/// Même homepage et mêmes indicateurs, scénario Anomalie (US-B · ALERTE).
final class AlertScreenCaptureUITests: PawriseUITestCase {
    override var extraLaunchArguments: [String] { ["-PawriseHealthScenario", "anomaly"] }

    func testCaptureAlertHealthScreens() {
        captureHealth(prefix: "BA")
    }
}
