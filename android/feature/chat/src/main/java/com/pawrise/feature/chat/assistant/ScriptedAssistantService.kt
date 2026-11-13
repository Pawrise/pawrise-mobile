package com.pawrise.feature.chat.assistant

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Faux assistant scriptable (tests, captures d'écran, démonstration hors ligne) : répond par [reply],
 * après [gate] s'il est posé (points de saisie visibles), ou échoue avec [failure].
 */
class ScriptedAssistantService(
    var reply: (AssistantTurnRequest) -> String = ::maquetteReply,
) : AssistantService {

    var gate: CompletableDeferred<Unit>? = null
    var failure: AssistantException? = null
    var status: String? = null
    val requests = mutableListOf<AssistantTurnRequest>()

    override fun streamTurn(request: AssistantTurnRequest): Flow<AssistantEvent> = flow {
        requests += request
        status?.let { emit(AssistantEvent.Status(it)) }
        gate?.await()
        failure?.let { throw it }
        emit(
            AssistantEvent.Response(
                AssistantResponse(
                    responseText = reply(request),
                    metadata = AssistantResponse.Metadata(threadId = request.threadId, turnId = request.turnId),
                ),
            ),
        )
    }

    companion object {
        /** Réponses de la maquette (US-H 04 et 06) pour le chien de démo. */
        fun maquetteReply(request: AssistantTurnRequest): String {
            val question = request.userMessage.lowercase()
            return when {
                "sommeil" in question -> "Nala — 11 h de sommeil."
                "activité" in question -> "Nala — 94 min actives."
                else -> "Nala va bien aujourd'hui : tous ses indicateurs sont dans ses valeurs habituelles."
            }
        }
    }
}
