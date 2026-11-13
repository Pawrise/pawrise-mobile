package com.pawrise.feature.chat.assistant

import com.pawrise.feature.chat.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Un tour de conversation : événements `status` puis `response` (ou une [AssistantException]). */
interface AssistantService {
    fun streamTurn(request: AssistantTurnRequest): Flow<AssistantEvent>
}

/** Réglages de l'assistant (Gradle : `pawrise.assistantBaseUrl`, `pawrise.assistantPetRef`). */
data class AssistantConfig(
    val baseUrl: String,
    /** `pet_ref` envoyé en développement, tant que la Core API n'existe pas (vide = identifiant du chien). */
    val petRefOverride: String?,
) {
    fun petRef(petId: String): String = petRefOverride ?: petId
}

/**
 * Appelle `POST /v1/turns/stream` (Server-Sent Events), comme l'app iOS (`AssistantClient.swift`).
 * En production, la même route sera exposée derrière la passerelle avec le JWT (ajouté par le client de l'app).
 */
class AssistantClient @Inject constructor(
    client: OkHttpClient,
    private val config: AssistantConfig,
) : AssistantService {

    // Les réponses arrivent en flux : délai de lecture large, pas de délai global.
    private val client = client.newBuilder().readTimeout(60, TimeUnit.SECONDS).build()

    override fun streamTurn(request: AssistantTurnRequest): Flow<AssistantEvent> = callbackFlow {
        val call = client.newCall(
            Request.Builder()
                .url(config.baseUrl.toHttpUrl().newBuilder().addPathSegments("v1/turns/stream").build())
                .header("Accept", "text/event-stream")
                .post(AssistantJson.encodeToString(AssistantTurnRequest.serializer(), request).toRequestBody(JSON))
                .build(),
        )
        launch(Dispatchers.IO) {
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) throw AssistantException.Unreachable()
                    val source = response.body.source()
                    val parser = SseParser()
                    while (true) {
                        val line = source.readUtf8Line() ?: break
                        parser.consume(line)?.let { message -> decode(message)?.let { send(it) } }
                    }
                    parser.flush()?.let { message -> decode(message)?.let { send(it) } }
                }
                close()
            } catch (error: AssistantException) {
                close(error)
            } catch (error: IOException) {
                close(if (call.isCanceled()) null else AssistantException.Unreachable())
            }
        }
        awaitClose { call.cancel() }
    }

    @Serializable
    private data class Envelope(
        val type: String,
        val text: String? = null,
        val message: String? = null,
        val response: AssistantResponse? = null,
    )

    companion object {
        private val JSON = "application/json".toMediaType()

        /** Message SSE → événement ; `null` pour un ping, un commentaire ou un type inconnu. */
        fun decode(message: SseParser.Message): AssistantEvent? {
            val envelope = runCatching { AssistantJson.decodeFromString(Envelope.serializer(), message.data) }.getOrNull()
                ?: return null
            return when (envelope.type) {
                "status" -> envelope.text?.let { AssistantEvent.Status(it) }
                "response" -> AssistantEvent.Response(envelope.response ?: throw AssistantException.BadResponse())
                "error" -> throw AssistantException.Server(envelope.message ?: "Le service est momentanément indisponible.")
                else -> null
            }
        }
    }
}

/** Analyseur Server-Sent Events minimal (lignes `event:` / `data:`, message terminé par une ligne vide). */
class SseParser {
    data class Message(val event: String?, val data: String)

    private var event: String? = null
    private val dataLines = mutableListOf<String>()

    /** Renvoie un message complet quand [rawLine] est la ligne vide qui le termine. */
    fun consume(rawLine: String): Message? {
        val line = rawLine.removeSuffix("\r")
        if (line.isEmpty()) return flush()
        if (line.startsWith(":")) return null
        val colon = line.indexOf(':')
        val field = if (colon < 0) line else line.substring(0, colon)
        val value = if (colon < 0) "" else line.substring(colon + 1).removePrefix(" ")
        when (field) {
            "event" -> event = value
            "data" -> dataLines += value
        }
        return null
    }

    fun flush(): Message? {
        val message = if (dataLines.isEmpty()) null else Message(event, dataLines.joinToString("\n"))
        event = null
        dataLines.clear()
        return message
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantBindings {
    @Binds
    abstract fun bindAssistantService(client: AssistantClient): AssistantService
}

@Module
@InstallIn(SingletonComponent::class)
object AssistantConfigModule {
    @Provides
    @Singleton
    fun provideAssistantConfig(): AssistantConfig = AssistantConfig(
        baseUrl = BuildConfig.ASSISTANT_BASE_URL,
        petRefOverride = BuildConfig.ASSISTANT_PET_REF.ifBlank { null },
    )
}
