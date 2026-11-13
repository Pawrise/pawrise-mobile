package com.pawrise.feature.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil3.SingletonImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.pawrise.core.testing.FakePetRepository
import com.pawrise.feature.home.ui.DogAvatar
import java.util.Base64
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Photo du chien posée sur le serveur (`photoUrl` http(s), ex. `http://localhost:3000/media/pets/….jpg`) :
 * Coil doit la télécharger (module réseau coil-network-okhttp), sinon l'avatar reste vide.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DogPhotoUrlTest {

    @get:Rule val compose = createComposeRule()

    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.enqueue(
            MockResponse.Builder()
                .addHeader("Content-Type", "image/png")
                .body(Buffer().write(PNG_1X1))
                .build(),
        )
        server.start()
    }

    @After
    fun tearDown() = server.close()

    private val photoUrl get() = server.url("/media/pets/2f6c1f9e-0000-4000-8000-000000000001.jpg").toString()

    @Test
    fun `le chargeur d'images telecharge et decode une photo servie en http`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val request = ImageRequest.Builder(context).data(photoUrl).allowHardware(false).build()

        val result = runBlocking { SingletonImageLoader.get(context).execute(request) }

        assertTrue("échec : ${(result as? ErrorResult)?.throwable}", result is SuccessResult)
        assertEquals("/media/pets/2f6c1f9e-0000-4000-8000-000000000001.jpg", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `l'avatar du chien demande sa photo au serveur`() {
        val pet = FakePetRepository.NALA.copy(photoUrl = photoUrl)
        compose.setContent { DogAvatar(pet, size = 64.dp) }

        compose.waitUntil(timeoutMillis = 5_000) { server.requestCount > 0 }
        assertEquals("/media/pets/2f6c1f9e-0000-4000-8000-000000000001.jpg", server.takeRequest().url.encodedPath)
    }

    private companion object {
        /** PNG 1 × 1 transparent. */
        val PNG_1X1: ByteArray = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
        )
    }
}
