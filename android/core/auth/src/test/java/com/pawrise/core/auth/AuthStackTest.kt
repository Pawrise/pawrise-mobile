package com.pawrise.core.auth

import com.pawrise.core.auth.data.AuthApi
import com.pawrise.core.auth.data.RefreshApi
import com.pawrise.core.auth.session.BearerInterceptor
import com.pawrise.core.auth.session.TokenAuthenticator
import com.pawrise.core.auth.session.TokenRefresher
import com.pawrise.core.auth.session.Tokens
import com.pawrise.core.network.ApiError
import com.pawrise.core.network.ApiResult
import com.pawrise.core.network.PawriseJson
import com.pawrise.core.network.ErrorCodes
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Dispatcher as OkHttpDispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/** Pile réseau réelle (OkHttp + Retrofit + intercepteurs) contre un faux serveur pawrise-auth. */
class AuthStackTest {

    private val server = MockWebServer()
    private val refreshCalls = AtomicInteger()
    private val requestsByPath = mutableListOf<RecordedRequest>()
    private lateinit var store: InMemoryTokenStore
    private lateinit var repository: DefaultAuthRepository

    /** Réponse de chaque route, modifiable par test. */
    private var routes: (RecordedRequest) -> MockResponse = { json(404, "{}") }

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                synchronized(requestsByPath) { requestsByPath += request }
                if (request.url.encodedPath == "/auth/refresh") refreshCalls.incrementAndGet()
                return routes(request)
            }
        }
        server.start()
    }

    @After
    fun tearDown() = server.close()

    private fun buildStack(initial: Tokens?) {
        store = InMemoryTokenStore(initial)
        val bare = OkHttpClient()
        val baseUrl = server.url("/")
        val converter = PawriseJson.asConverterFactory("application/json".toMediaType())
        val refreshApi = Retrofit.Builder().baseUrl(baseUrl).client(bare).addConverterFactory(converter).build().create<RefreshApi>()
        // Même montage que NetworkModule : Dispatcher distinct du client nu.
        val client = bare.newBuilder()
            .dispatcher(OkHttpDispatcher())
            .addInterceptor(BearerInterceptor(store))
            .authenticator(TokenAuthenticator(TokenRefresher(refreshApi, store)))
            .build()
        val authApi = Retrofit.Builder().baseUrl(baseUrl).client(client).addConverterFactory(converter).build().create<AuthApi>()
        repository = DefaultAuthRepository(authApi, store, PawriseJson)
    }

    @Test
    fun `login reussi enregistre les jetons sans envoyer de Bearer`() = runBlocking {
        buildStack(initial = null)
        routes = { json(200, AUTH_SESSION) }

        val result = repository.login(" alex@example.com ", "un-mot-de-passe")

        assertTrue(result is ApiResult.Success)
        assertEquals("Camille", (result as ApiResult.Success).value.firstName)
        assertEquals(Tokens("access-1", "refresh-1"), store.tokens.value)
        assertEquals(SessionState.SignedIn, repository.session.first())
        val sent = requestsByPath.single()
        assertNull(sent.headers["Authorization"])
        assertTrue(sent.body!!.utf8().contains("\"email\":\"alex@example.com\""))
    }

    @Test
    fun `login refuse ne declenche pas de refresh meme avec une session ouverte`() = runBlocking {
        buildStack(initial = Tokens("access-old", "refresh-old"))
        routes = { problem(401, "INVALID_CREDENTIALS") }

        val result = repository.login("alex@example.com", "faux")

        assertEquals(ErrorCodes.INVALID_CREDENTIALS, (result.failure() as ApiError.Problem).code)
        assertEquals(0, refreshCalls.get())
        assertNull(requestsByPath.single().headers["Authorization"])
    }

    @Test
    fun `401 sur une route protegee rafraichit puis rejoue la requete`() = runBlocking {
        buildStack(initial = Tokens("access-old", "refresh-old"))
        routes = { request ->
            when (request.url.encodedPath) {
                "/auth/refresh" -> json(200, tokenPair("access-new", "refresh-new"))
                "/me" -> if (request.headers["Authorization"] == "Bearer access-new") json(200, USER) else problem(401, "UNAUTHORIZED")
                else -> json(404, "{}")
            }
        }

        val result = repository.me()

        assertTrue(result is ApiResult.Success)
        assertEquals(Tokens("access-new", "refresh-new"), store.tokens.value)
        assertEquals(1, refreshCalls.get())
        val refreshBody = requestsByPath.first { it.url.encodedPath == "/auth/refresh" }.body!!.utf8()
        assertEquals("""{"refreshToken":"refresh-old"}""", refreshBody)
    }

    @Test(timeout = 10_000)
    fun `plusieurs 401 simultanes ne font qu'un seul refresh`() = runBlocking {
        buildStack(initial = Tokens("access-old", "refresh-old"))
        routes = { request ->
            when (request.url.encodedPath) {
                "/auth/refresh" -> {
                    Thread.sleep(150) // laisse les autres requêtes arriver pendant le refresh
                    json(200, tokenPair("access-new", "refresh-new"))
                }
                "/me" -> if (request.headers["Authorization"] == "Bearer access-new") json(200, USER) else problem(401, "UNAUTHORIZED")
                else -> json(404, "{}")
            }
        }

        val results = (1..5).map { async(Dispatchers.IO) { repository.me() } }.awaitAll()

        assertTrue(results.all { it is ApiResult.Success })
        assertEquals(1, refreshCalls.get())
    }

    @Test
    fun `refresh refuse ferme la session`() = runBlocking {
        buildStack(initial = Tokens("access-old", "refresh-old"))
        routes = { request ->
            when (request.url.encodedPath) {
                "/auth/refresh" -> problem(401, "INVALID_REFRESH_TOKEN")
                else -> problem(401, "UNAUTHORIZED")
            }
        }

        val result = repository.me()

        assertEquals(401, (result.failure() as ApiError.Problem).status)
        assertNull(store.tokens.value)
        assertEquals(SessionState.SignedOut, repository.session.first())
    }

    @Test
    fun `trop d'essais expose le code et Retry-After`() = runBlocking {
        buildStack(initial = null)
        routes = { problem(429, "TOO_MANY_ATTEMPTS").newBuilder().addHeader("Retry-After", "900").build() }

        val problem = repository.login("alex@example.com", "x").failure() as ApiError.Problem

        assertEquals(ErrorCodes.TOO_MANY_ATTEMPTS, problem.code)
        assertEquals(900L, problem.retryAfterSeconds)
    }

    @Test
    fun `inscription refusee expose les erreurs par champ sans ouvrir de session`() = runBlocking {
        buildStack(initial = null)
        routes = {
            json(
                400,
                """{"status":400,"code":"VALIDATION_FAILED","title":"Validation failed","errors":[{"field":"email","code":"INVALID_EMAIL"},{"field":"password","code":"PASSWORD_TOO_SHORT"}]}""",
                contentType = "application/problem+json",
            )
        }

        val problem = repository.register(FORM).failure() as ApiError.Problem

        assertEquals(ErrorCodes.VALIDATION_FAILED, problem.code)
        assertTrue(problem.hasFieldError("email", ErrorCodes.INVALID_EMAIL))
        assertTrue(problem.hasFieldError("password", ErrorCodes.PASSWORD_TOO_SHORT))
        assertNull(store.tokens.value)
    }

    @Test
    fun `inscription envoie le corps du contrat`() = runBlocking {
        buildStack(initial = null)
        routes = { json(201, AUTH_SESSION) }

        val result = repository.register(FORM)

        assertTrue(result is ApiResult.Success)
        val body = requestsByPath.single().body!!.utf8()
        assertEquals(
            """{"firstName":"Camille","lastName":"Martin","email":"camille@example.com","phoneNumber":"+33612345678","password":"chien-oslo-2026","acceptedTermsVersion":"$CURRENT_TERMS_VERSION"}""",
            body,
        )
        assertEquals(Tokens("access-1", "refresh-1"), store.tokens.value)
    }

    @Test
    fun `reponse illisible devient une erreur inattendue`() = runBlocking {
        buildStack(initial = null)
        routes = { json(200, "<html>proxy</html>") }

        assertTrue(repository.login("alex@example.com", "x").failure() is ApiError.Unexpected)
    }

    @Test
    fun `mot de passe oublie envoie l'e-mail normalise sans Bearer`() = runBlocking {
        buildStack(initial = Tokens("access-old", "refresh-old"))
        routes = { MockResponse.Builder().code(204).build() }

        val result = repository.requestPasswordReset("  Alex@Example.com ")

        assertTrue(result is ApiResult.Success)
        val sent = requestsByPath.single()
        assertEquals("/auth/forgot-password", sent.url.encodedPath)
        assertEquals("""{"email":"alex@example.com"}""", sent.body!!.utf8())
        assertNull(sent.headers["Authorization"])
    }

    @Test
    fun `reinitialisation avec le code puis connexion directe`() = runBlocking {
        buildStack(initial = null)
        routes = { request ->
            when (request.url.encodedPath) {
                "/auth/reset-password" -> MockResponse.Builder().code(204).build()
                "/auth/login" -> json(200, AUTH_SESSION)
                else -> json(404, "{}")
            }
        }

        val result = repository.resetPassword("alex@example.com", "123456", "nouveau-mot-2026")

        assertTrue(result is ApiResult.Success)
        assertEquals(
            """{"email":"alex@example.com","code":"123456","newPassword":"nouveau-mot-2026"}""",
            requestsByPath.first().body!!.utf8(),
        )
        assertEquals("""{"email":"alex@example.com","password":"nouveau-mot-2026"}""", requestsByPath[1].body!!.utf8())
        assertEquals(Tokens("access-1", "refresh-1"), store.tokens.value)
    }

    @Test
    fun `code expire n'ouvre pas de session`() = runBlocking {
        buildStack(initial = null)
        routes = { problem(400, ErrorCodes.INVALID_OR_EXPIRED_TOKEN) }

        val problem = repository.resetPassword("alex@example.com", "000000", "nouveau-mot-2026").failure() as ApiError.Problem

        assertEquals(ErrorCodes.INVALID_OR_EXPIRED_TOKEN, problem.code)
        assertEquals(1, requestsByPath.size)
        assertNull(store.tokens.value)
    }

    @Test
    fun `renvoi du lien de confirmation authentifie`() = runBlocking {
        buildStack(initial = Tokens("access-1", "refresh-1"))
        routes = { MockResponse.Builder().code(204).build() }

        assertTrue(repository.resendVerificationEmail() is ApiResult.Success)
        val sent = requestsByPath.single()
        assertEquals("/auth/resend-verification", sent.url.encodedPath)
        assertEquals("Bearer access-1", sent.headers["Authorization"])
    }

    @Test
    fun `profil expose emailVerified et alimente le compte courant`() = runBlocking {
        buildStack(initial = Tokens("access-1", "refresh-1"))
        routes = { json(200, USER.replace("}", ""","emailVerified":true}""")) }

        val user = (repository.me() as ApiResult.Success).value

        assertTrue(user.emailVerified)
        assertEquals(user, repository.user.value)
        assertEquals("Camille Martin", user.fullName)
    }

    @Test
    fun `preferences lues puis modifiees champ par champ`() = runBlocking {
        buildStack(initial = Tokens("access-1", "refresh-1"))
        routes = { request ->
            when (request.method) {
                "GET" -> json(200, """{"deviceLocationSharing":true,"healthNotifications":true,"anonymizedDataSharing":false}""")
                else -> json(200, """{"deviceLocationSharing":true,"healthNotifications":false,"anonymizedDataSharing":false}""")
            }
        }

        assertEquals(Preferences.Defaults, (repository.preferences() as ApiResult.Success).value)
        val saved = repository.updatePreferences(PreferencesUpdate(healthNotifications = false))

        assertEquals(false, (saved as ApiResult.Success).value.healthNotifications)
        val patch = requestsByPath.last()
        assertEquals("PATCH", patch.method)
        assertEquals("/me/preferences", patch.url.encodedPath)
        assertEquals("""{"healthNotifications":false}""", patch.body!!.utf8())
    }

    @Test
    fun `suppression du compte ferme la session`() = runBlocking {
        buildStack(initial = Tokens("access-1", "refresh-1"))
        routes = { MockResponse.Builder().code(204).build() }

        assertTrue(repository.deleteAccount() is ApiResult.Success)
        assertEquals("DELETE", requestsByPath.single().method)
        assertEquals("/me", requestsByPath.single().url.encodedPath)
        assertNull(store.tokens.value)
        assertEquals(SessionState.SignedOut, repository.session.first())
    }

    @Test
    fun `suppression refusee garde la session`() = runBlocking {
        buildStack(initial = Tokens("access-1", "refresh-1"))
        routes = { problem(409, ErrorCodes.CHIEF_TRANSFER_REQUIRED) }

        val problem = repository.deleteAccount().failure() as ApiError.Problem

        assertEquals(ErrorCodes.CHIEF_TRANSFER_REQUIRED, problem.code)
        assertEquals(Tokens("access-1", "refresh-1"), store.tokens.value)
    }

    private fun ApiResult<*>.failure(): ApiError = (this as ApiResult.Failure).error

    private companion object {
        val FORM = RegistrationForm(" Camille ", "Martin", "camille@example.com", "+33612345678", "chien-oslo-2026")

        const val USER = """{"id":"u1","firstName":"Camille","lastName":"Martin","email":"camille@example.com","phoneNumber":null,"registeredAt":"2026-11-06T10:12:00Z"}"""

        fun tokenPair(access: String, refresh: String) = """{"accessToken":"$access","refreshToken":"$refresh"}"""

        val AUTH_SESSION = """{"accessToken":"access-1","refreshToken":"refresh-1","user":$USER}"""

        fun json(code: Int, body: String, contentType: String = "application/json") =
            MockResponse.Builder().code(code).addHeader("Content-Type", contentType).body(body).build()

        fun problem(code: Int, problemCode: String) =
            json(code, """{"status":$code,"code":"$problemCode","title":"x"}""", "application/problem+json")
    }
}
