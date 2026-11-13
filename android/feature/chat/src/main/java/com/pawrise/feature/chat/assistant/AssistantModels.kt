package com.pawrise.feature.chat.assistant

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Contrat de pawrise-assistant (src/pawrise_assistant/domain/models.py, schema_version "1"), JSON en snake_case.
// L'assistant ne garde rien : l'app envoie l'historique complet à chaque tour.

@Serializable
enum class TurnRole {
    @SerialName("owner") Owner,
    @SerialName("assistant") Assistant,
}

@Serializable
data class AssistantTurn(val role: TurnRole, val content: String)

@Serializable
data class AssistantAlertContext(
    @SerialName("alert_id") val alertId: String,
    val kind: String,
    val level: String,
    val summary: String,
    @SerialName("persisted_days") val persistedDays: Int = 0,
    @SerialName("delta_pct") val deltaPct: Double? = null,
)

@Serializable
data class AssistantTurnRequest(
    @SerialName("thread_id") val threadId: String,
    @SerialName("turn_id") val turnId: String,
    @SerialName("pet_ref") val petRef: String,
    @SerialName("user_message") val userMessage: String,
    val history: List<AssistantTurn>,
    @SerialName("alert_context") val alertContext: AssistantAlertContext? = null,
)

@Serializable
data class AssistantResponse(
    @SerialName("schema_version") val schemaVersion: String = "1",
    @SerialName("response_text") val responseText: String,
    val citations: List<Citation> = emptyList(),
    val escalation: Escalation = Escalation(trigger = false),
    @SerialName("suggested_actions") val suggestedActions: List<SuggestedAction> = emptyList(),
    val metadata: Metadata? = null,
) {
    @Serializable
    data class Citation(@SerialName("source_id") val sourceId: String, val snippet: String, val url: String? = null)

    @Serializable
    data class Escalation(val trigger: Boolean, val urgency: String? = null, val reason: String? = null)

    @Serializable
    data class SuggestedAction(val label: String, @SerialName("action_id") val actionId: String)

    @Serializable
    data class Metadata(
        @SerialName("thread_id") val threadId: String,
        @SerialName("turn_id") val turnId: String,
        @SerialName("latency_ms") val latencyMs: Int = 0,
        @SerialName("template_id") val templateId: String? = null,
    )

    val isUrgent: Boolean get() = escalation.trigger && escalation.urgency == "high"
}

sealed interface AssistantEvent {
    /** Étape en cours (« Je regarde les données du collier… »), annoncée pendant les points de saisie. */
    data class Status(val text: String) : AssistantEvent

    /** Réponse vérifiée par les garde-fous. Toujours le dernier événement d'un tour réussi. */
    data class Response(val response: AssistantResponse) : AssistantEvent
}

sealed class AssistantException(message: String) : Exception(message) {
    class Server(message: String) : AssistantException(message)
    class Unreachable : AssistantException("L'assistant Pawrise est injoignable pour le moment. Réessayez dans un instant.")
    class BadResponse : AssistantException("L'assistant a renvoyé une réponse illisible. Réessayez.")
}

/** Sérialisation de pawrise-assistant (Python) : champs inconnus ignorés, `null` omis. */
val AssistantJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}
