package com.pawrise.core.auth

import com.pawrise.core.auth.data.AuthApi
import com.pawrise.core.auth.data.RefreshApi
import com.pawrise.core.auth.session.BearerInterceptor
import com.pawrise.core.auth.session.TokenAuthenticator
import com.pawrise.core.auth.session.TokenRefresher
import com.pawrise.core.network.ApiError
import com.pawrise.core.network.ApiResult
import com.pawrise.core.network.PawriseJson
import kotlinx.coroutines.runBlocking
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/**
 * Requêtes réelles contre le mock Prism, qui les valide contre api/openapi.yaml (contrat pawrise-doc).
 * Un champ mal nommé, manquant ou en trop → Prism répond 4xx → le test échoue.
 *
 * Lancé seulement si `-Ppawrise.contractUrl=http://localhost:4010/` (CI : mock démarré par le workflow).
 */
class AuthContractTest {

    private val contractUrl: String? = System.getProperty("pawrise.contractUrl")
    private lateinit var store: InMemoryTokenStore
    private lateinit var repository: DefaultAuthRepository
    private lateinit var refresher: TokenRefresher

    @Before
    fun setUp() {
        assumeTrue("pawrise.contractUrl absent : test de contrat ignoré", contractUrl != null)
        store = InMemoryTokenStore()
        val converter = PawriseJson.asConverterFactory("application/json".toMediaType())
        val bare = OkHttpClient()
        val refreshApi = Retrofit.Builder().baseUrl(contractUrl!!).client(bare).addConverterFactory(converter).build().create<RefreshApi>()
        refresher = TokenRefresher(refreshApi, store)
        val client = bare.newBuilder()
            .dispatcher(Dispatcher())
            .addInterceptor(BearerInterceptor(store))
            .authenticator(TokenAuthenticator(refresher))
            .build()
        val api = Retrofit.Builder().baseUrl(contractUrl).client(client).addConverterFactory(converter).build().create<AuthApi>()
        repository = DefaultAuthRepository(api, store, PawriseJson)
    }

    @Test
    fun `inscription conforme au contrat ouvre une session`() = runBlocking {
        val result = repository.register(RegistrationForm("Camille", "Martin", "camille.martin@example.com", "+33612345678", "chien-oslo-2026"))

        assertSuccess(result)
        assertNotNull(store.tokens.value)
    }

    @Test
    fun `inscription sans telephone conforme au contrat`() = runBlocking {
        assertSuccess(repository.register(RegistrationForm("Camille", "Martin", "camille.martin@example.com", null, "chien-oslo-2026")))
    }

    @Test
    fun `mot de passe trop court refuse par le contrat`() = runBlocking {
        val result = repository.register(RegistrationForm("Camille", "Martin", "camille.martin@example.com", null, "court"))

        val problem = (result as ApiResult.Failure).error as ApiError.Problem
        assertTrue("statut ${problem.status}", problem.status in 400..499)
    }

    @Test
    fun `connexion, profil, refresh puis deconnexion conformes au contrat`() = runBlocking {
        assertSuccess(repository.login("camille.martin@example.com", "chien-oslo-2026"))
        assertSuccess(repository.me())

        val fresh = refresher.refresh(store.tokens.value!!.accessToken)
        assertNotNull("refresh refusé par le mock", fresh)

        repository.logout()
        assertNull(store.tokens.value)
    }

    @Test
    fun `profil sans session refuse`() = runBlocking {
        val problem = (repository.me() as ApiResult.Failure).error as ApiError.Problem
        assertEquals(401, problem.status)
    }

    private fun assertSuccess(result: ApiResult<*>) {
        assertTrue("attendu un succès, reçu $result", result is ApiResult.Success)
    }
}
