import Foundation
import Testing
@testable import Pawrise

@Suite("Saisie : e-mail, mot de passe, code collier")
struct InputRulesTests {
    @Test(arguments: ["alex@example.com", "  Alex@Example.COM ", "a.b+c@sub.domain.fr"])
    func acceptsValidEmails(_ email: String) {
        #expect(CredentialRules.isValidEmail(email))
    }

    @Test(arguments: ["", "alex", "alex@", "@example.com", "alex@example", "alex @example.com", "alex@.com"])
    func rejectsInvalidEmails(_ email: String) {
        #expect(!CredentialRules.isValidEmail(email))
    }

    @Test func normalizesEmail() {
        #expect(CredentialRules.normalizedEmail("  Alex@Example.COM\n") == "alex@example.com")
    }

    @Test func passwordLengthFollowsContract() {
        #expect(!CredentialRules.isValidPassword("1234567"))
        #expect(CredentialRules.isValidPassword("12345678"))
        #expect(CredentialRules.isValidPassword(String(repeating: "a", count: 128)))
        #expect(!CredentialRules.isValidPassword(String(repeating: "a", count: 129)))
        // Compté en caractères, pas en octets
        #expect(CredentialRules.isValidPassword("éàçùéàçù"))
    }

    @Test func phoneNumbersBecomeE164() {
        #expect(CredentialRules.e164Phone("06 12 34 56 78") == "+33612345678")
        #expect(CredentialRules.e164Phone("06.12.34.56.78") == "+33612345678")
        #expect(CredentialRules.e164Phone("+33 6 12 34 56 78") == "+33612345678")
        #expect(CredentialRules.e164Phone("0033612345678") == "+33612345678")
        #expect(CredentialRules.e164Phone("0612") == nil)
        #expect(CredentialRules.e164Phone("+0612345678") == nil)
    }

    @Test func resetCodeIsSixDigits() {
        #expect(CredentialRules.isValidResetCode("012345"))
        #expect(!CredentialRules.isValidResetCode("12345"))
        #expect(!CredentialRules.isValidResetCode("12345a"))
        #expect(!CredentialRules.isValidResetCode("١٢٣٤٥٦")) // chiffres non ASCII
    }

    @Test(arguments: ["PW-ABC123", "pw-abc123", " PW - ABC 123 "])
    func acceptsCollarCodes(_ code: String) {
        #expect(Collar.isValidCode(code))
    }

    @Test(arguments: ["PW-ABC12", "PW-ABC1234", "XX-ABC123", "PW_ABC123", "PW-ABC12É"])
    func rejectsCollarCodes(_ code: String) {
        #expect(!Collar.isValidCode(code))
    }
}

@Suite("Server-Sent Events de l'assistant")
struct SSETests {
    private func parse(_ raw: String) -> [SSEParser.Message] {
        var parser = SSEParser()
        var messages: [SSEParser.Message] = []
        for line in raw.components(separatedBy: "\n") {
            if let message = parser.consume(line) { messages.append(message) }
        }
        if let last = parser.flush() { messages.append(last) }
        return messages
    }

    @Test func splitsMessagesOnBlankLines() {
        let raw = """
        event: status
        data: {"type":"status","text":"Je lis votre message…"}

        : ping - 2026-11-06

        event: response
        data: {"type":"response"}

        """
        let messages = parse(raw)
        #expect(messages.count == 2)
        #expect(messages[0].event == "status")
        #expect(messages[0].data.contains("Je lis votre message"))
        #expect(messages[1].event == "response")
    }

    @Test func joinsMultilineData() {
        let messages = parse("data: ligne 1\ndata: ligne 2\n\n")
        #expect(messages == [SSEParser.Message(event: nil, data: "ligne 1\nligne 2")])
    }

    @Test func decodesStatusResponseAndError() throws {
        let status = try AssistantClient.decode(.init(event: "status", data: #"{"type":"status","text":"Je vérifie la réponse…"}"#))
        #expect(status == .status("Je vérifie la réponse…"))

        let json = """
        {"type":"response","response":{"schema_version":"1","response_text":"Contactez un vétérinaire dès maintenant.",
        "citations":[{"source_id":"telemetry","snippet":"Activité -35 % sur 6 jours","url":null}],
        "escalation":{"trigger":true,"urgency":"high","reason":"R-ESC-01"},
        "suggested_actions":[{"label":"Contacter un vétérinaire","action_id":"open_vet_handoff"}],
        "metadata":{"thread_id":"t","turn_id":"1","path":["finalize"],"latency_ms":12,"prompt_version":"v1","template_id":"SR-URG-01"}}}
        """
        guard case let .response(response)? = try AssistantClient.decode(.init(event: "response", data: json)) else {
            Issue.record("réponse attendue")
            return
        }
        #expect(response.isUrgent)
        #expect(response.citations.first?.sourceId == "telemetry")
        #expect(response.suggestedActions.first?.actionId == "open_vet_handoff")
        #expect(response.metadata.templateId == "SR-URG-01")

        #expect(throws: AssistantError.server("Le service est momentanément indisponible.")) {
            try AssistantClient.decode(.init(event: "error", data: #"{"type":"error","message":"Le service est momentanément indisponible."}"#))
        }
    }

    @Test func encodesTurnRequestInSnakeCase() throws {
        let request = AssistantTurnRequest(
            threadId: "t1", turnId: "u1", petRef: "pet_demo_nala", userMessage: "Et son sommeil ?",
            history: [AssistantTurn(role: .owner, content: "Bonjour")], alertContext: nil
        )
        let json = try #require(String(data: AssistantClient.encoder.encode(request), encoding: .utf8))
        #expect(json.contains(#""pet_ref":"pet_demo_nala""#))
        #expect(json.contains(#""user_message":"Et son sommeil ?""#))
        #expect(json.contains(#""thread_id":"t1""#))
    }
}

@Suite("Contrat d'auth : JSON camelCase et erreurs (pawrise-doc / pawrise-auth)")
struct AuthContractTests {
    @Test func decodesAuthSessionFromPawriseAuth() throws {
        // Réponse réelle de POST /v1/auth/register (pawrise-auth).
        let json = """
        {"accessToken":"eyJ.a.b","refreshToken":"prt_x","user":{"id":"9403c4db-d6c2-4856-82e4-ab0cac2b54c9",
        "firstName":"Camille","lastName":"Martin","email":"camille@example.com","phoneNumber":null,
        "registeredAt":"2026-11-06T15:20:22.687Z","emailVerified":false}}
        """
        let session = try APIClient.decoder.decode(AuthResponse.self, from: Data(json.utf8))
        #expect(session.tokens == TokenPair(accessToken: "eyJ.a.b", refreshToken: "prt_x"))
        #expect(session.user.fullName == "Camille Martin")
        #expect(session.user.phoneNumber == nil)
        #expect(session.isNewUser == nil)
    }

    @Test func encodesRegisterRequestInCamelCase() throws {
        let request = RegisterRequest(
            firstName: "Camille", lastName: "Martin", email: "camille@example.com",
            phoneNumber: "+33612345678", password: "chien-oslo-2026", acceptedTermsVersion: LegalTerms.version
        )
        let json = try #require(String(data: APIClient.encoder.encode(request), encoding: .utf8))
        #expect(json.contains(#""firstName":"Camille""#))
        #expect(json.contains(#""acceptedTermsVersion":"2026-10""#))
        #expect(json.contains(#""phoneNumber":"+33612345678""#))
    }

    @Test func encodesPreferencesPatchWithOnlyChangedField() throws {
        let json = try JSONSerialization.jsonObject(with: APIClient.encoder.encode(PreferencesUpdate(healthNotifications: false))) as? [String: Any]
        #expect(json?.count == 1)
        #expect(json?["healthNotifications"] as? Bool == false)
    }

    @Test func explainsChiefTransferOnAccountDeletion() {
        #expect(APIError.problem(409, "CHIEF_TRANSFER_REQUIRED").localizedDescription.contains("Transférez"))
        #expect(DeleteAccountView.names(["Nala", "Oslo", "Rex"]) == "Nala, Oslo et Rex")
    }

    @Test func mapsValidationErrorsToFieldMessages() throws {
        let json = #"{"status":400,"code":"VALIDATION_FAILED","title":"Validation failed","errors":[{"field":"password","code":"PASSWORD_BREACHED"},{"field":"email","code":"INVALID_EMAIL"}]}"#
        let problem = try APIClient.decoder.decode(ProblemDetails.self, from: Data(json.utf8))
        let error = APIError.problem(status: 400, problem)
        #expect(error.fieldErrors.count == 2)
        #expect(error.localizedDescription.contains("fuite de données"))
        #expect(APIError.problem(409, "EMAIL_ALREADY_USED").localizedDescription.contains("existe déjà"))
        #expect(APIError.problem(429, "TOO_MANY_ATTEMPTS").localizedDescription.contains("15 minutes"))
    }
}

@Suite("Authentification simulée (mêmes règles que pawrise-auth)")
struct MockAuthServiceTests {
    private let auth = MockAuthService(latency: .zero)

    private func register(
        _ email: String, password: String = "un-mot-de-passe-solide", terms: String = LegalTerms.version
    ) async throws -> AuthResponse {
        try await auth.register(RegisterRequest(
            firstName: "Alex", lastName: "Martin", email: email, phoneNumber: nil,
            password: password, acceptedTermsVersion: terms
        ))
    }

    private func code(of operation: () async throws -> Void) async -> String? {
        do { try await operation(); return nil } catch let error as APIError { return error.code } catch { return "?" }
    }

    @Test func registerThenLogin() async throws {
        let created = try await register("alex@example.com")
        #expect(created.user.fullName == "Alex Martin")
        #expect(!created.user.emailVerified)
        let logged = try await auth.login(LoginRequest(email: "alex@example.com", password: "un-mot-de-passe-solide"))
        #expect(logged.user.id == created.user.id)
    }

    @Test func duplicateEmailIsRejected() async throws {
        _ = try await register("alex@example.com")
        #expect(await code { _ = try await register("ALEX@example.com") } == "EMAIL_ALREADY_USED")
    }

    @Test func validationReportsEveryField() async {
        do {
            _ = try await register("pas-un-email", password: "12345678", terms: "")
            Issue.record("inscription invalide acceptée")
        } catch let error as APIError {
            #expect(error.code == "VALIDATION_FAILED")
            #expect(Set(error.fieldErrors.map(\.field)) == ["email", "password", "acceptedTermsVersion"])
            #expect(error.fieldErrors.first { $0.field == "password" }?.code == "PASSWORD_TOO_COMMON")
        } catch {
            Issue.record("erreur inattendue \(error)")
        }
    }

    @Test func locksAfterFiveFailures() async throws {
        for _ in 0..<5 {
            #expect(await code { _ = try await auth.login(LoginRequest(email: MockAuthService.demoEmail, password: "mauvais")) } == "INVALID_CREDENTIALS")
        }
        #expect(await code {
            _ = try await auth.login(LoginRequest(email: MockAuthService.demoEmail, password: MockAuthService.demoPassword))
        } == "TOO_MANY_ATTEMPTS")
    }

    @Test func refreshTokenRotates() async throws {
        let session = try await auth.login(LoginRequest(email: MockAuthService.demoEmail, password: MockAuthService.demoPassword))
        let rotated = try await auth.refresh(refreshToken: session.tokens.refreshToken)
        #expect(rotated.refreshToken != session.tokens.refreshToken)
        #expect(await code { _ = try await auth.refresh(refreshToken: session.tokens.refreshToken) } == "INVALID_REFRESH_TOKEN")
    }

    @Test func passwordResetWithCode() async throws {
        let email = MockAuthService.demoEmail
        try await auth.requestPasswordReset(ForgotPasswordRequest(email: email))
        #expect(await code {
            try await auth.resetPassword(ResetPasswordRequest(email: email, code: "000000", newPassword: "nouveau-chien-2026"))
        } == "INVALID_OR_EXPIRED_TOKEN")
        try await auth.resetPassword(ResetPasswordRequest(email: email, code: MockAuthService.resetCode, newPassword: "nouveau-chien-2026"))
        _ = try await auth.login(LoginRequest(email: email, password: "nouveau-chien-2026"))
        #expect(await code { _ = try await auth.login(LoginRequest(email: email, password: MockAuthService.demoPassword)) } == "INVALID_CREDENTIALS")
    }

    @Test func preferencesChangeOneFieldAtATime() async throws {
        _ = try await register("alex@example.com")
        #expect(try await auth.preferences() == .defaults)
        let updated = try await auth.updatePreferences(PreferencesUpdate(anonymizedDataSharing: true))
        #expect(updated == Preferences(deviceLocationSharing: true, healthNotifications: true, anonymizedDataSharing: true))
        #expect(await code { _ = try await auth.updatePreferences(PreferencesUpdate()) } == "VALIDATION_FAILED")
    }

    @Test func deletedAccountIsGone() async throws {
        let session = try await register("alex@example.com")
        try await auth.deleteAccount()
        #expect(await code { _ = try await auth.refresh(refreshToken: session.tokens.refreshToken) } == "INVALID_REFRESH_TOKEN")
        #expect(await code {
            _ = try await auth.login(LoginRequest(email: "alex@example.com", password: "un-mot-de-passe-solide"))
        } == "INVALID_CREDENTIALS")
        _ = try await register("alex@example.com") // l'adresse est de nouveau libre
    }
}

@Suite("Session : refresh unique")
struct TokenStoreTests {
    actor Counter {
        var value = 0
        func increment() -> Int { value += 1; return value }
    }

    @Test func concurrentRefreshesShareOneRequest() async throws {
        let store = TokenStore(keychain: KeychainStore(service: "fr.pawrise.tests.\(UUID().uuidString)"))
        await store.save(TokenPair(accessToken: "old", refreshToken: "r0"))
        let counter = Counter()
        await store.setRefresher { _ in
            let n = await counter.increment()
            try await Task.sleep(for: .milliseconds(50))
            return TokenPair(accessToken: "new\(n)", refreshToken: "r\(n)")
        }

        async let a = store.refreshedTokens(replacing: "old")
        async let b = store.refreshedTokens(replacing: "old")
        async let c = store.refreshedTokens(replacing: "old")
        let results = try await [a, b, c]

        #expect(await counter.value == 1)
        #expect(Set(results.map(\.accessToken)) == ["new1"])
        await store.clear()
    }

    @Test func failedRefreshClearsSession() async {
        let store = TokenStore(keychain: KeychainStore(service: "fr.pawrise.tests.\(UUID().uuidString)"))
        await store.save(TokenPair(accessToken: "old", refreshToken: "r0"))
        await store.setRefresher { _ in throw APIError.unexpectedStatus(401) }
        await #expect(throws: APIError.sessionExpired) { try await store.refreshedTokens(replacing: "old") }
        #expect(await store.tokens == nil)
    }
}

@Suite("Données simulées")
struct SeedDataTests {
    @Test func normalAndAnomalyScenarios() async throws {
        let repository = SeedHealthRepository()
        let normal = try await repository.snapshot(for: DogCatalog.nala, scenario: .normal)
        #expect(normal.score == 87)
        #expect(normal.state == .good)
        #expect(!normal.hasAnomaly)
        #expect(normal.indicators.map(\.kind) == IndicatorKind.allCases)
        #expect(normal.indicators.first { $0.kind == .heart }?.headline == "78 bpm")

        let anomaly = try await repository.snapshot(for: DogCatalog.nala, scenario: .anomaly)
        #expect(anomaly.score == 64)
        #expect(anomaly.state == .watch)
        #expect(anomaly.hasAnomaly)
    }

    @Test func seriesCoverEveryPeriod() async throws {
        let snapshot = try await SeedHealthRepository().snapshot(for: DogCatalog.nala, scenario: .normal)
        for indicator in snapshot.indicators {
            for period in HealthPeriod.allCases {
                #expect(!(indicator.series[period] ?? []).isEmpty, "\(indicator.kind) \(period)")
            }
        }
        #expect(snapshot.indicators.first { $0.kind == .heart }?.series[.today]?.count == 18)
    }

    @Test func dogAgeLabel() {
        let calendar = Calendar(identifier: .gregorian)
        let now = calendar.date(from: DateComponents(year: 2026, month: 11, day: 6))!
        var dog = DogCatalog.nala
        #expect(dog.ageLabel(now: now, calendar: calendar) == "3 ans")
        dog.birthDate = calendar.date(from: DateComponents(year: 2026, month: 3, day: 1))
        #expect(dog.ageLabel(now: now, calendar: calendar) == "8 mois")
        dog.birthDate = calendar.date(from: DateComponents(year: 2025, month: 6, day: 1))
        #expect(dog.ageLabel(now: now, calendar: calendar) == "1 an")
    }
}
