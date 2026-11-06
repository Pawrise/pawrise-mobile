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
        let json = try #require(String(data: APIClient.encoder.encode(request), encoding: .utf8))
        #expect(json.contains(#""pet_ref":"pet_demo_nala""#))
        #expect(json.contains(#""user_message":"Et son sommeil ?""#))
        #expect(json.contains(#""thread_id":"t1""#))
    }
}

@Suite("Authentification simulée (contrat pawrise-auth)")
struct MockAuthServiceTests {
    private let auth = MockAuthService(latency: .zero)

    private func register(_ email: String, password: String = "un-mot-de-passe-solide") async throws -> AuthResponse {
        try await auth.register(RegisterRequest(email: email, password: password, name: "Alex Martin", phone: nil, termsAccepted: true))
    }

    @Test func registerThenLogin() async throws {
        let created = try await register("alex@example.com")
        #expect(created.isNewUser)
        #expect(created.user.role == .owner)
        let logged = try await auth.login(LoginRequest(email: "alex@example.com", password: "un-mot-de-passe-solide"))
        #expect(!logged.isNewUser)
        #expect(logged.user.id == created.user.id)
    }

    @Test func duplicateEmailIsRejected() async throws {
        _ = try await register("alex@example.com")
        await #expect(throws: APIError.self) { try await register("ALEX@example.com") }
        do { _ = try await register("alex@example.com") } catch let error as APIError {
            #expect(error.code == "email_taken")
        }
    }

    @Test func weakPasswordIsRejected() async {
        do {
            _ = try await register("alex@example.com", password: "12345678")
            Issue.record("mot de passe courant accepté")
        } catch let error as APIError {
            #expect(error.code == "weak_password")
        } catch {
            Issue.record("erreur inattendue \(error)")
        }
    }

    @Test func locksAfterFiveFailures() async throws {
        for _ in 0..<5 {
            await #expect(throws: APIError.self) {
                try await auth.login(LoginRequest(email: MockAuthService.demoEmail, password: "mauvais"))
            }
        }
        do {
            _ = try await auth.login(LoginRequest(email: MockAuthService.demoEmail, password: MockAuthService.demoPassword))
            Issue.record("le compte devait être verrouillé")
        } catch let error as APIError {
            #expect(error.code == "account_locked")
        }
    }

    @Test func refreshTokenRotates() async throws {
        let session = try await auth.login(LoginRequest(email: MockAuthService.demoEmail, password: MockAuthService.demoPassword))
        let rotated = try await auth.refresh(refreshToken: session.tokens.refreshToken)
        #expect(rotated.refreshToken != session.tokens.refreshToken)
        // L'ancien refresh token ne marche plus
        await #expect(throws: APIError.self) { try await auth.refresh(refreshToken: session.tokens.refreshToken) }
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
        await store.save(TokenPair(accessToken: "old", refreshToken: "r0", tokenType: "Bearer", expiresIn: 900))
        let counter = Counter()
        await store.setRefresher { _ in
            let n = await counter.increment()
            try await Task.sleep(for: .milliseconds(50))
            return TokenPair(accessToken: "new\(n)", refreshToken: "r\(n)", tokenType: "Bearer", expiresIn: 900)
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
        await store.save(TokenPair(accessToken: "old", refreshToken: "r0", tokenType: "Bearer", expiresIn: 900))
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
