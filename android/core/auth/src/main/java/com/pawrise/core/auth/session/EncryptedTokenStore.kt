package com.pawrise.core.auth.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")
private val TOKENS_KEY = stringPreferencesKey("tokens")

/**
 * Jetons chiffrés en AES-256-GCM avec une clé de l'Android Keystore (jamais exportable),
 * puis rangés dans DataStore. La sauvegarde Android est désactivée (data_extraction_rules).
 */
@Singleton
class EncryptedTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) : TokenStore {

    private val state = MutableStateFlow<Tokens?>(null)
    override val tokens: StateFlow<Tokens?> = state.asStateFlow()

    override suspend fun load() {
        val stored = context.sessionDataStore.data.first()[TOKENS_KEY] ?: return
        // Clé Keystore perdue (restauration, réinitialisation…) → session considérée comme fermée.
        state.value = runCatching { json.decodeFromString(Tokens.serializer(), KeystoreCipher.decrypt(stored)) }
            .getOrElse { clear(); null }
    }

    override suspend fun save(tokens: Tokens) {
        val encrypted = KeystoreCipher.encrypt(json.encodeToString(Tokens.serializer(), tokens))
        context.sessionDataStore.edit { it[TOKENS_KEY] = encrypted }
        state.value = tokens
    }

    override suspend fun clear() {
        context.sessionDataStore.edit { it.remove(TOKENS_KEY) }
        state.value = null
    }
}

private object KeystoreCipher {
    private const val KEY_ALIAS = "pawrise_session"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_BITS = 128

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val payload = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, payload, 0, IV_SIZE))
        }
        return String(cipher.doFinal(payload, IV_SIZE, payload.size - IV_SIZE), Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }
}
