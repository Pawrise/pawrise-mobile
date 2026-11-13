import XCTest

/// Parcours de bout en bout dans le simulateur (mode Debug : auth et collier simulés).
/// Chaque étape clé est jointe en capture d'écran au rapport de test (.xcresult).
final class OnboardingJourneyUITests: PawriseUITestCase {
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

        // Sans les CGU, l'inscription est impossible (RG11) : « Continuer » reste grisé, et décocher la case affiche
        // le message.
        let continueButton = app.buttons["Continuer"]
        XCTAssertFalse(continueButton.isEnabled)
        let terms = app.descendants(matching: .any).matching(identifier: "terms-checkbox").firstMatch
        terms.tap()
        XCTAssertTrue(continueButton.isEnabled)
        terms.tap()
        XCTAssertTrue(app.staticTexts["Acceptez les conditions d’utilisation pour continuer."].waitForExistence(timeout: 3))
        XCTAssertFalse(continueButton.isEnabled)
        terms.tap()
        tap(button: "Continuer")

        // Découverte (5 étapes)
        XCTAssertTrue(app.staticTexts["Pawrise veille sur votre chien"].waitForExistence(timeout: 5))
        dismissSavePasswordPrompt(within: 3)
        snapshot("02-decouverte")
        for _ in 0..<4 { tap(button: "Suivant") }
        tap(button: "Connecter le collier")

        createNalaWithCollar(snapshots: true)

        // Santé
        XCTAssertTrue(app.staticTexts["INDICATEURS"].waitForExistence(timeout: 5))
        snapshot("07-sante")
        let heartTile = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Rythme cardiaque'")).firstMatch
        XCTAssertTrue(heartTile.waitForExistence(timeout: 5))
        heartTile.tap()
        XCTAssertTrue(app.staticTexts["78 bpm"].waitForExistence(timeout: 5))
        snapshot("08-detail-rythme")
    }

    /// Inscription : « Continuer » grisé tant que le formulaire est incomplet ou invalide, erreurs affichées une fois
    /// le champ quitté, téléphone mis en forme pendant la saisie, œil pour afficher le mot de passe.
    func testSignupFormValidationPhoneAndPasswordVisibility() {
        tap(button: "Créer un compte")
        XCTAssertTrue(app.staticTexts["Quelques informations, puis on vous montre Pawrise."].waitForExistence(timeout: 5))
        let continueButton = app.buttons["Continuer"]
        XCTAssertFalse(continueButton.isEnabled)
        // Aucune erreur avant d'avoir touché les champs.
        XCTAssertFalse(app.staticTexts["Indiquez votre prénom."].exists)

        let fields = app.textFields
        type(fields.element(boundBy: 0), "Alex")
        type(fields.element(boundBy: 1), "Martin")
        type(fields.element(boundBy: 2), "alex@example")
        // E-mail incomplet quitté : message affiché, bouton toujours grisé.
        fields.element(boundBy: 3).tap()
        XCTAssertTrue(app.staticTexts["Saisissez une adresse e-mail valide, par exemple alex@exemple.fr."].waitForExistence(timeout: 3))
        XCTAssertFalse(continueButton.isEnabled)
        type(fields.element(boundBy: 2), ".com")
        XCTAssertFalse(app.staticTexts["Saisissez une adresse e-mail valide, par exemple alex@exemple.fr."].exists)

        // Téléphone mis en forme pendant la saisie (région FR).
        type(fields.element(boundBy: 3), "0612345678")
        XCTAssertEqual(fields.element(boundBy: 3).value as? String, "06 12 34 56 78")

        // Mot de passe : l'œil l'affiche puis le masque.
        type(app.secureTextFields.element(boundBy: 0), "chien-26")
        dismissPasswordSheetIfShown()
        let show = app.buttons["Afficher le mot de passe"]
        XCTAssertTrue(show.exists)
        show.tap()
        XCTAssertTrue(app.buttons["Masquer le mot de passe"].waitForExistence(timeout: 3))
        XCTAssertEqual(fields.element(boundBy: 4).value as? String, "chien-26")
        XCTAssertEqual(app.secureTextFields.count, 0)
        app.buttons["Masquer le mot de passe"].tap()
        XCTAssertEqual(app.secureTextFields.count, 1)
        XCTAssertEqual((app.secureTextFields.element(boundBy: 0).value as? String)?.count, 8)

        XCTAssertFalse(continueButton.isEnabled) // CGU non cochées
        let terms = app.descendants(matching: .any).matching(identifier: "terms-checkbox").firstMatch
        for _ in 0..<3 where !terms.isHittable { app.swipeUp() }
        terms.tap()
        XCTAssertTrue(continueButton.isEnabled)
        snapshot("11-inscription-valide")
    }

    /// Connexion : « Se connecter » grisé sans e-mail valide ni mot de passe ; œil sur le mot de passe.
    func testLoginButtonDisabledUntilValidAndPasswordVisibility() {
        tap(button: "Se connecter")
        XCTAssertTrue(app.staticTexts["Connexion"].waitForExistence(timeout: 5))
        let login = app.buttons.matching(NSPredicate(format: "label == 'Se connecter'")).firstMatch
        XCTAssertFalse(login.isEnabled)
        type(app.textFields.firstMatch, "alex@example")
        type(app.secureTextFields.firstMatch, "chien-26")
        XCTAssertFalse(login.isEnabled) // e-mail invalide
        type(app.textFields.firstMatch, ".com")
        XCTAssertTrue(login.isEnabled)

        app.buttons["Afficher le mot de passe"].tap()
        XCTAssertEqual(app.secureTextFields.count, 0)
        XCTAssertTrue(app.buttons["Masquer le mot de passe"].exists)
        app.buttons["Masquer le mot de passe"].tap()
        XCTAssertEqual(app.secureTextFields.count, 1)
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
        dismissSavePasswordPrompt(within: 2)
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

    /// Paramètres → Confidentialité (un interrupteur) → Supprimer le compte → retour à l'accueil, compte inutilisable.
    func testPrivacyToggleThenDeleteAccount() {
        let email = "suppr.\(Int.random(in: 1000...9999))@example.com"
        tap(button: "Créer un compte")
        let fields = app.textFields
        type(fields.element(boundBy: 0), "Alex")
        type(fields.element(boundBy: 1), "Martin")
        type(fields.element(boundBy: 2), email)
        type(app.secureTextFields.element(boundBy: 0), "un-mot-de-passe-solide")
        app.descendants(matching: .any).matching(identifier: "terms-checkbox").firstMatch.tap()
        tap(button: "Continuer")
        XCTAssertTrue(app.staticTexts["Pawrise veille sur votre chien"].waitForExistence(timeout: 8))
        dismissSavePasswordPrompt(within: 3)

        // Les onglets n'apparaissent qu'une fois un chien ajouté.
        for _ in 0..<4 { tap(button: "Suivant") }
        tap(button: "Connecter le collier")
        createNalaWithCollar(snapshots: false)
        XCTAssertTrue(app.staticTexts["INDICATEURS"].waitForExistence(timeout: 5))
        tap(button: "Paramètres")

        let anonymized = app.switches["privacy-anonymized-data"]
        XCTAssertTrue(anonymized.waitForExistence(timeout: 8))
        XCTAssertEqual(anonymized.value as? String, "0")
        // Le libellé occupe toute la ligne : on touche l'interrupteur lui-même, à droite.
        anonymized.coordinate(withNormalizedOffset: CGVector(dx: 0.93, dy: 0.5)).tap()
        let enabled = NSPredicate(format: "value == '1' AND isEnabled == true")
        expectation(for: enabled, evaluatedWith: anonymized)
        waitForExpectations(timeout: 5)
        snapshot("11-confidentialite")

        let openDelete = app.buttons["open-delete-account"]
        for _ in 0..<6 where !openDelete.isHittable { app.swipeUp() }
        openDelete.tap()
        XCTAssertTrue(app.staticTexts["Cette action est définitive"].waitForExistence(timeout: 5))
        snapshot("12-supprimer-compte")
        let deleteButton = app.buttons["delete-account-button"]
        for _ in 0..<4 where !(deleteButton.exists && deleteButton.isHittable) { app.swipeUp() }
        deleteButton.tap()
        let confirm = app.buttons["Supprimer définitivement"]
        XCTAssertTrue(confirm.waitForExistence(timeout: 3))
        confirm.tap()

        XCTAssertTrue(app.staticTexts["Le collier connecté qui suit la santé et la position de votre chien."].waitForExistence(timeout: 10))
        tap(button: "Se connecter")
        type(app.textFields.firstMatch, email)
        type(app.secureTextFields.firstMatch, "un-mot-de-passe-solide")
        app.buttons.matching(NSPredicate(format: "label == 'Se connecter'")).firstMatch.tap()
        XCTAssertTrue(app.staticTexts["E-mail ou mot de passe incorrect."].waitForExistence(timeout: 8))
    }
}
