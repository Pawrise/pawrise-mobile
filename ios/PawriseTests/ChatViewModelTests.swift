import Foundation
import Testing
@testable import Pawrise

/// Faux assistant : rejoue une liste d'événements (ou une erreur) et garde les requêtes reçues.
final class FakeAssistant: AssistantService, @unchecked Sendable {
    private let lock = NSLock()
    private var _requests: [AssistantTurnRequest] = []
    var script: [Result<AssistantEvent, AssistantError>]

    init(script: [Result<AssistantEvent, AssistantError>]) {
        self.script = script
    }

    var requests: [AssistantTurnRequest] { lock.withLock { _requests } }

    func streamTurn(_ request: AssistantTurnRequest) -> AsyncThrowingStream<AssistantEvent, Error> {
        lock.withLock { _requests.append(request) }
        let script = self.script
        return AsyncThrowingStream { continuation in
            for step in script {
                switch step {
                case let .success(event): continuation.yield(event)
                case let .failure(error): continuation.finish(throwing: error); return
                }
            }
            continuation.finish()
        }
    }
}

@MainActor
@Suite("Chat : conversation avec l'assistant")
struct ChatViewModelTests {
    static func response(_ text: String) -> AssistantResponse {
        AssistantResponse(
            schemaVersion: "1", responseText: text, citations: [],
            escalation: .init(trigger: false, urgency: nil, reason: nil), suggestedActions: [],
            metadata: .init(threadId: "t", turnId: "1", latencyMs: 10, templateId: nil)
        )
    }

    @Test func sendsMessageAndAppendsAnswer() async {
        let fake = FakeAssistant(script: [.success(.status("Je lis votre message…")), .success(.response(Self.response("Nala — 11 h de sommeil.")))])
        let chat = ChatViewModel(petRef: "pet_demo_nala", service: fake, threadId: "thread-1")

        #expect(chat.send("  Et son sommeil ?  "))
        #expect(chat.isResponding)
        await chat.waitForCurrentTurn()

        #expect(!chat.isResponding)
        #expect(chat.failure == nil)
        #expect(chat.messages.map(\.text) == ["Et son sommeil ?", "Nala — 11 h de sommeil."])
        let request = fake.requests[0]
        #expect(request.threadId == "thread-1")
        #expect(request.petRef == "pet_demo_nala")
        #expect(request.userMessage == "Et son sommeil ?")
        #expect(request.history.isEmpty)
    }

    @Test func sendsPreviousTurnsAsHistory() async {
        let fake = FakeAssistant(script: [.success(.response(Self.response("Réponse")))])
        let chat = ChatViewModel(petRef: "p", service: fake)
        chat.send("Question 1")
        await chat.waitForCurrentTurn()
        chat.send("Question 2")
        await chat.waitForCurrentTurn()

        #expect(fake.requests[1].history == [
            AssistantTurn(role: .owner, content: "Question 1"),
            AssistantTurn(role: .assistant, content: "Réponse"),
        ])
        #expect(fake.requests[0].threadId == fake.requests[1].threadId)
        #expect(fake.requests[0].turnId != fake.requests[1].turnId)
    }

    @Test func ignoresEmptyMessages() {
        let chat = ChatViewModel(petRef: "p", service: FakeAssistant(script: []))
        #expect(!chat.send("   \n"))
        #expect(chat.messages.isEmpty)
    }

    @Test func errorThenRetryDoesNotDuplicateMessage() async {
        let fake = FakeAssistant(script: [.failure(.unreachable)])
        let chat = ChatViewModel(petRef: "p", service: fake)
        chat.send("Comment va Nala ?")
        await chat.waitForCurrentTurn()
        #expect(chat.failure == AssistantError.unreachable.localizedDescription)

        fake.script = [.success(.response(Self.response("Tout va bien.")))]
        chat.retry()
        await chat.waitForCurrentTurn()

        #expect(chat.failure == nil)
        #expect(chat.messages.map(\.text) == ["Comment va Nala ?", "Tout va bien."])
        #expect(fake.requests.count == 2)
        #expect(fake.requests[1].history.isEmpty)
    }

    @Test func streamWithoutResponseIsAnError() async {
        let chat = ChatViewModel(petRef: "p", service: FakeAssistant(script: [.success(.status("…"))]))
        chat.send("Bonjour")
        await chat.waitForCurrentTurn()
        #expect(chat.failure == AssistantError.badResponse.localizedDescription)
    }
}
