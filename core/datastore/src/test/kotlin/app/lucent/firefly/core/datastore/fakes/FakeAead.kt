package app.lucent.firefly.core.datastore.fakes

import com.google.crypto.tink.Aead

/**
 * In-memory [Aead] implementation for JVM unit tests.
 *
 * Uses a simple XOR cipher with a fixed byte (0xAB.toByte()) so that tests can verify
 * encryption/decryption round-trips and corruption recovery completely offline on standard
 * JVM without hardware Android Keystore dependencies.
 *
 * **IMPORTANT:** This is intentionally weak and is for testing purposes only.
 * It must never be used in production code.
 */
internal class FakeAead : Aead {
    private val xorKey: Byte = 0xAB.toByte()

    override fun encrypt(
        plaintext: ByteArray,
        associatedData: ByteArray?,
    ): ByteArray = ByteArray(plaintext.size) { i -> (plaintext[i].toInt() xor xorKey.toInt()).toByte() }

    override fun decrypt(
        ciphertext: ByteArray,
        associatedData: ByteArray?,
    ): ByteArray =
        // XOR is its own inverse
        ByteArray(ciphertext.size) { i -> (ciphertext[i].toInt() xor xorKey.toInt()).toByte() }
}
