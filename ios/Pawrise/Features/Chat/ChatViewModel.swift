import Foundation
import Observation

/// Message affiché dans le fil. Le message d'accueil n'en fait pas partie : il est purement local.
struct ChatMessage: Identifiable, Equatable, Sendable {
    enum Role: Sendable { case owner, assistant }

    let id: UUID
    let role: Role
    let text: String
    /// Réponse complète de l'assistant (sources, urgence, actions) ; `nil` pour un message du propriétaire.
    let response: AssistantResponse?

    init(id: UUID = UUID(), role: Role, text: String, response: AssistantResponse? = nil) {
        self.id = id
        self.role = role
        self.text = text
        self.response = response
    }
}

/// Conversation avec l'assistant pour un chien. Gère le flux SSE (`status` puis `response`),
/// les erreurs et la relance, indépendamment de la vue pour pouvoir être testée avec un faux `AssistantService`.
@MainActor
@Observable
final class ChatViewModel {
    /// Fil stable pour toute la session de ce chien.
    let threadId: String
    private(set) var messages: [ChatMessage] = []
    /// Un tour est en cours : points de saisie affichés, envoi désactivé.
    private(set) var isResponding = false
    /// Étape annoncée par l'assistant (« Je regarde les données du collier… »).
    private(set) var statusText: String?
    /// Erreur du dernier tour, affichée avec « Réessayer ».
    private(set) var failure: String?

    static let interruptedMessage = "La réponse a été interrompue."

    /// Délai minimal avant d'afficher une réponse, en secondes (argument de lancement
    /// `-PawriseAssistantMinimumDelay`, utilisé par les captures d'écran ; 0 en usage normal).
    static var minimumResponseDelay: Double {
        UserDefaults.standard.double(forKey: "PawriseAssistantMinimumDelay")
    }

    private let service: any AssistantService
    private let petRef: String
    @ObservationIgnored private var turnTask: Task<Void, Never>?
    @ObservationIgnored private var activeTurnID: String?

    init(petRef: String, service: any AssistantService, threadId: String = UUID().uuidString) {
        self.petRef = petRef
        self.service = service
        self.threadId = threadId
    }

    /// Les suggestions ne s'affichent qu'avant le premier message.
    var showsQuickReplies: Bool { messages.isEmpty && !isResponding && failure == nil }

    var canSend: Bool { !isResponding }

    /// Ajoute le message du propriétaire et lance un tour. Renvoie `false` si rien n'a été envoyé.
    @discardableResult
    func send(_ rawText: String) -> Bool {
        let text = rawText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, canSend else { return false }
        messages.append(ChatMessage(role: .owner, text: text))
        startTurn(userMessage: text)
        return true
    }

    /// Relance le dernier message du propriétaire resté sans réponse.
    func retry() {
        guard canSend, let last = messages.last, last.role == .owner else { return }
        startTurn(userMessage: last.text)
    }

    /// Interrompt le tour en cours (vue quittée, changement de chien).
    func cancel() {
        guard let task = turnTask else { return }
        task.cancel()
        turnTask = nil
        activeTurnID = nil
        isResponding = false
        statusText = nil
        if messages.last?.role == .owner {
            failure = Self.interruptedMessage
        }
    }

    /// Attend la fin du tour en cours (tests).
    func waitForCurrentTurn() async {
        await turnTask?.value
    }

    // MARK: Tour

    private func startTurn(userMessage: String) {
        let request = AssistantTurnRequest(
            threadId: threadId,
            turnId: UUID().uuidString.lowercased(),
            petRef: petRef,
            userMessage: userMessage,
            history: history(excludingPendingMessage: true),
            alertContext: nil
        )
        failure = nil
        statusText = nil
        isResponding = true
        activeTurnID = request.turnId
        turnTask = Task { [weak self] in
            await self?.consume(request)
        }
    }

    /// Historique envoyé à l'assistant : tours précédents uniquement (ni accueil, ni erreurs, ni message en cours).
    private func history(excludingPendingMessage: Bool) -> [AssistantTurn] {
        var previous = messages
        if excludingPendingMessage, previous.last?.role == .owner { previous.removeLast() }
        return previous.map { message in
            AssistantTurn(role: message.role == .owner ? .owner : .assistant, content: message.text)
        }
    }

    private func consume(_ request: AssistantTurnRequest) async {
        var receivedResponse = false
        var errorText: String?
        let started = ContinuousClock.now
        do {
            for try await event in service.streamTurn(request) {
                guard activeTurnID == request.turnId else { return }
                switch event {
                case let .status(text):
                    statusText = text
                case let .response(response):
                    // Captures d'écran : laisse l'indicateur de saisie visible un minimum de temps (0 par défaut).
                    let hold = Duration.seconds(Self.minimumResponseDelay) - (ContinuousClock.now - started)
                    if hold > .zero { try await Task.sleep(for: hold) }
                    guard activeTurnID == request.turnId else { return }
                    messages.append(ChatMessage(role: .assistant, text: response.responseText, response: response))
                    receivedResponse = true
                }
            }
        } catch is CancellationError {
            return
        } catch let error as LocalizedError {
            errorText = error.errorDescription ?? AssistantError.unreachable.localizedDescription
        } catch {
            errorText = AssistantError.unreachable.localizedDescription
        }

        // Un tour annulé ou remplacé ne touche plus à l'état.
        guard activeTurnID == request.turnId, !Task.isCancelled else { return }
        if errorText == nil, !receivedResponse {
            errorText = AssistantError.badResponse.localizedDescription
        }
        failure = errorText
        isResponding = false
        statusText = nil
        activeTurnID = nil
        turnTask = nil
    }
}

/// Conversations de la session, une par chien, conservées quand on change d'onglet.
@MainActor
@Observable
final class ChatConversations {
    @ObservationIgnored private var byDog: [Dog.ID: ChatViewModel] = [:]

    func conversation(for dog: Dog, environment: AppEnvironment) -> ChatViewModel {
        if let existing = byDog[dog.id] { return existing }
        let model = ChatViewModel(petRef: environment.assistantPetRef(for: dog), service: environment.assistant)
        byDog[dog.id] = model
        return model
    }

    func cancelAll() {
        byDog.values.forEach { $0.cancel() }
    }
}
