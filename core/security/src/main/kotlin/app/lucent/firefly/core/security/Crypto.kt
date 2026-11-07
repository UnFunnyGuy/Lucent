package app.lucent.firefly.core.security

import app.lucent.firefly.core.common.result.Result

/**
 * Contract for encrypting and decrypting sensitive data, tokens, and payloads.
 */
interface Crypto {

    /** Encrypts raw bytes. */
    fun encrypt(plaintext: ByteArray): ByteArray

    /** Decrypts raw encrypted bytes. */
    fun decrypt(encrypted: ByteArray): ByteArray

    /** Encrypts [plaintext] returning a Base64-encoded string. */
    fun encryptString(plaintext: String): String

    /** Decrypts a Base64-encoded [encryptedBase64] string back to plaintext. */
    fun decryptString(encryptedBase64: String): String

    /** Safely encrypts raw bytes, capturing errors into a [Result]. */
    fun encryptCatching(plaintext: ByteArray): Result<ByteArray>

    /** Safely decrypts raw bytes, capturing errors into a [Result]. */
    fun decryptCatching(encrypted: ByteArray): Result<ByteArray>

    /** Safely encrypts a string, capturing errors into a [Result]. */
    fun encryptStringCatching(plaintext: String): Result<String>

    /** Safely decrypts a string, capturing errors into a [Result]. */
    fun decryptStringCatching(encryptedBase64: String): Result<String>
}
