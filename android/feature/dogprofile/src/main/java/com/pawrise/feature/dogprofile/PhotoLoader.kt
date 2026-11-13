package com.pawrise.feature.dogprofile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import com.pawrise.core.pets.PhotoFile
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Photo choisie dans la galerie → fichier prêt pour `POST /pets/{id}/photo` (JPEG ≤ 1,5 Mo). */
fun interface PhotoLoader {
    /** `null` si l'image est illisible. */
    suspend fun load(uri: String): PhotoFile?
}

/**
 * Réduit la photo (1280 px max, JPEG) au lieu de refuser les photos de téléphone, souvent > 1,5 Mo.
 * Baisse la qualité jusqu'à passer sous la limite du contrat.
 */
class AndroidPhotoLoader @Inject constructor(@ApplicationContext private val context: Context) : PhotoLoader {

    override suspend fun load(uri: String): PhotoFile? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val parsed = uri.toUri()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val bitmap = resolver.openInputStream(parsed)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            var quality = 85
            var bytes: ByteArray
            do {
                bytes = ByteArrayOutputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    out.toByteArray()
                }
                quality -= 15
            } while (bytes.size > MAX_BYTES && quality > 20)
            bitmap.recycle()
            if (bytes.size > MAX_BYTES) null else PhotoFile(bytes, "image/jpeg")
        }.getOrNull()
    }

    private companion object {
        const val MAX_SIDE = 1280
        const val MAX_BYTES = 1_500_000
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PhotoLoaderModule {
    @Binds
    abstract fun photoLoader(impl: AndroidPhotoLoader): PhotoLoader
}
