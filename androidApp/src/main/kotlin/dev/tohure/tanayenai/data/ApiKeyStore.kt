package dev.tohure.tanayenai.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the Gemini API key securely using the Android Keystore.
 *
 * A hardware-backed AES-256-GCM key is generated in the Keystore (never leaves secure hardware).
 * The encrypted value (IV + ciphertext) is stored in plain SharedPreferences — the raw bytes
 * are meaningless without the Keystore key.
 */
class ApiKeyStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("tanayen_prefs", Context.MODE_PRIVATE)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val spec =
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

        return KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
            .apply { init(spec) }
            .generateKey()
    }

    fun saveApiKey(key: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(key.trim().toByteArray(Charsets.UTF_8))

        prefs
            .edit()
            .putString(PREF_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .putString(PREF_ENC, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .apply()
    }

    fun getApiKey(): String? {
        val ivB64 = prefs.getString(PREF_IV, null) ?: return null
        val encB64 = prefs.getString(PREF_ENC, null) ?: return null
        return try {
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            val ciphertext = Base64.decode(encB64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8).takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    fun clearApiKey() {
        prefs
            .edit()
            .remove(PREF_IV)
            .remove(PREF_ENC)
            .apply()
    }

    companion object {
        private const val PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "tanayen_gemini_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val PREF_IV = "gemini_iv"
        private const val PREF_ENC = "gemini_enc"
    }
}
