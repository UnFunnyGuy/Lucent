package app.lucent.firefly.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import org.koin.core.annotation.Singleton
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Android Keystore implementation of [Crypto] using AES-256-GCM authenticated encryption.
 */
@Singleton
class KeyStoreCrypto : Crypto {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher =
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            }
        return cipher.iv + cipher.doFinal(plaintext)
    }

    override fun decrypt(encrypted: ByteArray): ByteArray {
        require(encrypted.size >= GCM_IV_SIZE_BYTES) { "Invalid encrypted payload size" }
        val iv = encrypted.copyOfRange(0, GCM_IV_SIZE_BYTES)
        val ciphertext = encrypted.copyOfRange(GCM_IV_SIZE_BYTES, encrypted.size)

        val cipher =
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))
            }
        return cipher.doFinal(ciphertext)
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun encryptString(plaintext: String): String {
        val encrypted = encrypt(plaintext.encodeToByteArray())
        return Base64.encode(encrypted)
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun decryptString(encryptedBase64: String): String {
        val encrypted = Base64.decode(encryptedBase64)
        return decrypt(encrypted).decodeToString()
    }

    override fun encryptCatching(plaintext: ByteArray): Result<ByteArray> =
        try {
            Result.success(encrypt(plaintext))
        } catch (e: Exception) {
            Result.error(AppError.Common(e.message ?: "Encryption failed", e))
        }

    override fun decryptCatching(encrypted: ByteArray): Result<ByteArray> =
        try {
            Result.success(decrypt(encrypted))
        } catch (e: Exception) {
            Result.error(AppError.Common(e.message ?: "Decryption failed", e))
        }

    override fun encryptStringCatching(plaintext: String): Result<String> =
        try {
            Result.success(encryptString(plaintext))
        } catch (e: Exception) {
            Result.error(AppError.Common(e.message ?: "String encryption failed", e))
        }

    override fun decryptStringCatching(encryptedBase64: String): Result<String> =
        try {
            Result.success(decryptString(encryptedBase64))
        } catch (e: Exception) {
            Result.error(AppError.Common(e.message ?: "String decryption failed", e))
        }

    private fun getOrCreateSecretKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val spec =
            KeyGenParameterSpec
                .Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "lucent_master_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_SIZE_BYTES = 12
        const val GCM_TAG_SIZE_BITS = 128
    }
}
