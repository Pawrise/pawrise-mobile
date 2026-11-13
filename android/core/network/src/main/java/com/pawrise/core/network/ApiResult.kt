package com.pawrise.core.network

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** Résultat d'un appel d'API, sans exception à attraper côté écran. */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

sealed interface ApiError {
    /**
     * Erreur Problem Details (RFC 9457, convention 4 de pawrise-doc).
     * Décider sur [code] (stable, `MAJUSCULES_SNAKE`), jamais sur [title] ni [detail].
     */
    data class Problem(
        val status: Int,
        val code: String?,
        val title: String? = null,
        val detail: String? = null,
        /** `VALIDATION_FAILED` : une entrée par champ en défaut (`field` en camelCase). */
        val fieldErrors: List<FieldError> = emptyList(),
        /** En-tête `Retry-After` (429). */
        val retryAfterSeconds: Long? = null,
    ) : ApiError {
        fun hasFieldError(field: String, code: String? = null): Boolean =
            fieldErrors.any { it.field == field && (code == null || it.code == code) }
    }

    /** Pas de réseau, délai dépassé… : proposer de réessayer. */
    data class Network(val cause: IOException) : ApiError

    /** Réponse illisible ou erreur inattendue. */
    data class Unexpected(val cause: Throwable) : ApiError
}

@Serializable
data class FieldError(val field: String, val code: String)

/** Codes d'erreur du contrat (`pawrise-doc/contrat-interface/openapi.yaml`). Une valeur inconnue reste possible. */
object ErrorCodes {
    const val VALIDATION_FAILED = "VALIDATION_FAILED"
    const val EMAIL_ALREADY_USED = "EMAIL_ALREADY_USED"
    const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    const val TOO_MANY_ATTEMPTS = "TOO_MANY_ATTEMPTS"
    const val INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN"
    const val PET_NOT_FOUND = "PET_NOT_FOUND"
    const val FILE_TOO_LARGE = "FILE_TOO_LARGE"
    const val COLLAR_ALREADY_ASSIGNED = "COLLAR_ALREADY_ASSIGNED"
    const val UNAUTHORIZED = "UNAUTHORIZED"

    // pawrise-auth (ECARTS.md) : mot de passe oublié, suppression du compte
    const val INVALID_OR_EXPIRED_TOKEN = "INVALID_OR_EXPIRED_TOKEN"
    const val CHIEF_TRANSFER_REQUIRED = "CHIEF_TRANSFER_REQUIRED"

    // Codes par champ (errors[].code)
    const val REQUIRED = "REQUIRED"
    const val INVALID_EMAIL = "INVALID_EMAIL"
    const val INVALID_PHONE = "INVALID_PHONE"
    const val PASSWORD_TOO_SHORT = "PASSWORD_TOO_SHORT"
    const val PASSWORD_TOO_LONG = "PASSWORD_TOO_LONG"
    const val PASSWORD_TOO_COMMON = "PASSWORD_TOO_COMMON"
    const val PASSWORD_BREACHED = "PASSWORD_BREACHED"
    const val INVALID_CODE = "INVALID_CODE"
    const val TOO_LONG = "TOO_LONG"
    const val TERMS_NOT_ACCEPTED = "TERMS_NOT_ACCEPTED"
    const val BIRTH_DATE_IN_FUTURE = "BIRTH_DATE_IN_FUTURE"
    const val MUST_BE_POSITIVE = "MUST_BE_POSITIVE"
    const val UNKNOWN_BREED = "UNKNOWN_BREED"
    const val UNSUPPORTED_FORMAT = "UNSUPPORTED_FORMAT"
    const val INVALID_COLLAR_CODE = "INVALID_COLLAR_CODE"
    const val UNKNOWN_COLLAR = "UNKNOWN_COLLAR"
}

@Serializable
internal data class ProblemDto(
    val status: Int? = null,
    val code: String? = null,
    val title: String? = null,
    val detail: String? = null,
    val errors: List<FieldError> = emptyList(),
)

/** Exécute un appel Retrofit et range toute erreur dans un [ApiResult.Failure]. */
suspend fun <T> apiCall(json: Json = PawriseJson, block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    ApiResult.Failure(e.toProblem(json))
} catch (e: IOException) {
    ApiResult.Failure(ApiError.Network(e))
} catch (e: SerializationException) {
    ApiResult.Failure(ApiError.Unexpected(e))
} catch (e: IllegalArgumentException) {
    ApiResult.Failure(ApiError.Unexpected(e))
}

/** Transforme un succès en autre chose sans toucher aux échecs. */
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}

internal fun HttpException.toProblem(json: Json): ApiError.Problem {
    val response = response()
    val dto = response?.errorBody()?.string()
        ?.let { body -> runCatching { json.decodeFromString(ProblemDto.serializer(), body) }.getOrNull() }
    return ApiError.Problem(
        status = code(),
        code = dto?.code,
        title = dto?.title,
        detail = dto?.detail,
        fieldErrors = dto?.errors.orEmpty(),
        retryAfterSeconds = response?.headers()?.get("Retry-After")?.toLongOrNull(),
    )
}
