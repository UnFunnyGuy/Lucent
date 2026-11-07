package app.lucent.firefly.core.datastore

import app.lucent.firefly.core.datastore.fakes.FakeAead
import app.lucent.firefly.core.datastore.model.LucentPreferences
import app.lucent.firefly.core.datastore.serializer.LucentPreferencesSerializer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Unit tests for [LucentPreferencesSerializer].
 *
 * Verifies JSON round-trips (defaults + full state), FakeAead encrypt/decrypt
 * round-trips, and resilience to corrupted input bytes.
 */
class LucentPreferencesSerializerTest {

    private val serializer = LucentPreferencesSerializer

    // ─── defaultValue ──────────────────────────────────────────────────────────

    @Test
    fun `defaultValue matches factory defaults`() {
        val defaults = LucentPreferences()
        assertEquals(defaults, serializer.defaultValue)
    }

    // ─── Serialization round-trip ──────────────────────────────────────────────

    @Test
    fun `writeTo then readFrom returns equal object for defaults`() = runTest {
        val original = LucentPreferences()
        val outputStream = ByteArrayOutputStream()
        serializer.writeTo(original, outputStream)

        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val restored = serializer.readFrom(inputStream)

        assertEquals(original, restored)
    }

    @Test
    fun `writeTo then readFrom returns equal object for full state`() = runTest {
        val original = LucentPreferences(
            connection = app.lucent.firefly.core.datastore.model.ServerConnection(
                baseUrl = "https://firefly.local",
                encryptedPat = "encrypted-token-abc",
                allowSelfSignedCert = true,
                userId = "2",
                userEmail = "user@example.com",
                userRole = "owner",
            ),
            themeMode = app.lucent.firefly.core.datastore.model.ThemeMode.DARK,
            useDynamicColor = false,
            biometricAuthEnabled = true,
        )
        val outputStream = ByteArrayOutputStream()
        serializer.writeTo(original, outputStream)

        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val restored = serializer.readFrom(inputStream)

        assertEquals(original, restored)
    }

    // ─── Empty input ───────────────────────────────────────────────────────────

    @Test
    fun `readFrom empty stream returns defaultValue`() = runTest {
        val inputStream = ByteArrayInputStream(ByteArray(0))
        val result = serializer.readFrom(inputStream)
        assertEquals(serializer.defaultValue, result)
    }

    // ─── Corruption handling ───────────────────────────────────────────────────

    @Test
    fun `readFrom corrupted bytes throws CorruptionException`() = runTest {
        val corruptBytes = "not-valid-json-!@#$%^&*()".toByteArray()
        val inputStream = ByteArrayInputStream(corruptBytes)

        val exception = runCatching { serializer.readFrom(inputStream) }.exceptionOrNull()
        assertTrue(
            "Expected CorruptionException but got ${exception?.javaClass}",
            exception is androidx.datastore.core.CorruptionException,
        )
    }

    // ─── FakeAead round-trip ───────────────────────────────────────────────────

    @Test
    fun `FakeAead encrypt then decrypt returns original bytes`() {
        val fakeAead = FakeAead()
        val plaintext = "Hello, Lucent!".toByteArray()
        val encrypted = fakeAead.encrypt(plaintext, null)
        val decrypted = fakeAead.decrypt(encrypted, null)
        assertTrue(plaintext.contentEquals(decrypted))
    }

    @Test
    fun `FakeAead encrypted bytes differ from plaintext`() {
        val fakeAead = FakeAead()
        val plaintext = "SensitiveToken123".toByteArray()
        val encrypted = fakeAead.encrypt(plaintext, null)
        assertTrue(!plaintext.contentEquals(encrypted))
    }
}
