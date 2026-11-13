package com.pawrise.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawrise.feature.chat.assistant.AssistantConfig
import com.pawrise.feature.chat.assistant.AssistantEvent
import com.pawrise.feature.chat.assistant.AssistantException
import com.pawrise.feature.chat.assistant.AssistantResponse
import com.pawrise.feature.chat.assistant.AssistantService
import com.pawrise.feature.chat.assistant.AssistantTurn
import com.pawrise.feature.chat.assistant.AssistantTurnRequest
import com.pawrise.feature.chat.assistant.TurnRole
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Message du fil. Le message d'accueil n'en fait pas partie : il est purement local. */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: TurnRole,
    val text: String,
    /** Réponse complète de l'assistant (urgence…) ; `null` pour un message du propriétaire. */
    val response: AssistantResponse? = null,
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    /** Un tour est en cours : points de saisie affichés, envoi désactivé. */
    val isResponding: Boolean = false,
    /** Étape annoncée par l'assistant (« Je regarde les données du collier… »). */
    val statusText: String? = null,
    /** Erreur du dernier tour, affichée avec « Réessayer ». */
    val failure: String? = null,
) {
    /** Les suggestions ne s'affichent qu'avant le premier message. */
    val showsQuickReplies: Boolean get() = messages.isEmpty() && !isResponding && failure == null
    val canSend: Boolean get() = !isResponding
}

/**
 * Conversation avec l'assistant pour un chien : flux SSE (`status` puis `response`), erreurs et relance.
 * L'assistant ne garde rien : l'historique complet part à chaque tour.
 */
class ChatConversation(
    private val petRef: String,
    private val service: AssistantService,
    private val scope: CoroutineScope,
    /** Fil stable pour toute la session de ce chien. */
    val threadId: String = UUID.randomUUID().toString(),
) {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var turn: Job? = null
    private var activeTurnId: String? = null

    /** Ajoute le message du propriétaire et lance un tour. Renvoie `false` si rien n'a été envoyé. */
    fun send(rawText: String): Boolean {
        val text = rawText.trim()
        if (text.isEmpty() || !_state.value.canSend) return false
        _state.update { it.copy(messages = it.messages + ChatMessage(role = TurnRole.Owner, text = text)) }
        startTurn(text)
        return true
    }

    /** Relance le dernier message du propriétaire resté sans réponse. */
    fun retry() {
        val last = _state.value.messages.lastOrNull()
        if (!_state.value.canSend || last?.role != TurnRole.Owner) return
        startTurn(last.text)
    }

    /** Interrompt le tour en cours (onglet quitté, changement de chien). */
    fun cancel() {
        val job = turn ?: return
        job.cancel()
        turn = null
        activeTurnId = null
        _state.update {
            it.copy(
                isResponding = false,
                statusText = null,
                failure = if (it.messages.lastOrNull()?.role == TurnRole.Owner) INTERRUPTED else it.failure,
            )
        }
    }

    private fun startTurn(userMessage: String) {
        val request = AssistantTurnRequest(
            threadId = threadId,
            turnId = UUID.randomUUID().toString(),
            petRef = petRef,
            userMessage = userMessage,
            history = history(),
        )
        _state.update { it.copy(failure = null, statusText = null, isResponding = true) }
        activeTurnId = request.turnId
        turn = scope.launch { consume(request) }
    }

    /** Historique envoyé : tours précédents uniquement (ni accueil, ni erreurs, ni message en cours). */
    private fun history(): List<AssistantTurn> {
        val previous = _state.value.messages.let { if (it.lastOrNull()?.role == TurnRole.Owner) it.dropLast(1) else it }
        return previous.map { AssistantTurn(it.role, it.text) }
    }

    private suspend fun consume(request: AssistantTurnRequest) {
        var received = false
        var error: String? = null
        try {
            service.streamTurn(request).collect { event ->
                if (activeTurnId != request.turnId) return@collect
                when (event) {
                    is AssistantEvent.Status -> _state.update { it.copy(statusText = event.text) }
                    is AssistantEvent.Response -> {
                        received = true
                        _state.update {
                            it.copy(messages = it.messages + ChatMessage(role = TurnRole.Assistant, text = event.response.responseText, response = event.response))
                        }
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: AssistantException) {
            error = failure.message
        } catch (_: Exception) {
            error = AssistantException.Unreachable().message
        }
        // Un tour annulé ou remplacé ne touche plus à l'état.
        if (activeTurnId != request.turnId) return
        if (error == null && !received) error = AssistantException.BadResponse().message
        _state.update { it.copy(failure = error, isResponding = false, statusText = null) }
        activeTurnId = null
        turn = null
    }

    companion object {
        const val INTERRUPTED = "La réponse a été interrompue."
    }
}

/** Conversations de la session, une par chien, conservées quand on change d'onglet. */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val service: AssistantService,
    private val config: AssistantConfig,
) : ViewModel() {
    private val conversations = mutableMapOf<String, ChatConversation>()

    fun conversation(petId: String): ChatConversation =
        conversations.getOrPut(petId) { ChatConversation(config.petRef(petId), service, viewModelScope) }

    fun cancelAll() {
        conversations.values.forEach { it.cancel() }
    }
}
