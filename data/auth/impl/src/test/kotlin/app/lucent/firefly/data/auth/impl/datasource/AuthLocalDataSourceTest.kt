package app.lucent.firefly.data.auth.impl.datasource

import app.cash.turbine.test
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.datastore.LucentPreferencesDataSource
import app.lucent.firefly.core.datastore.model.LucentPreferences
import app.lucent.firefly.core.datastore.model.ServerConnection
import app.lucent.firefly.core.datastore.model.ThemeMode
import app.lucent.firefly.core.security.Crypto
import app.lucent.firefly.data.auth.api.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthLocalDataSourceTest {
    private class FakeCrypto : Crypto {
        override fun encrypt(plaintext: ByteArray): ByteArray = plaintext

        override fun decrypt(encrypted: ByteArray): ByteArray = encrypted

        override fun encryptString(plaintext: String): String = "enc_$plaintext"

        override fun decryptString(encryptedBase64: String): String = encryptedBase64.removePrefix("enc_")

        override fun encryptCatching(plaintext: ByteArray): Result<ByteArray> = Result.success(plaintext)

        override fun decryptCatching(encrypted: ByteArray): Result<ByteArray> = Result.success(encrypted)

        override fun encryptStringCatching(plaintext: String): Result<String> = Result.success(encryptString(plaintext))

        override fun decryptStringCatching(encryptedBase64: String): Result<String> = Result.success(decryptString(encryptedBase64))
    }

    private class FakePreferencesDataSource : LucentPreferencesDataSource {
        private val _prefs = MutableStateFlow(LucentPreferences())

        override val preferences: Flow<LucentPreferences> = _prefs
        override val connection: Flow<ServerConnection?> = _prefs.map { it.connection }
        override val themeMode: Flow<ThemeMode> = _prefs.map { it.themeMode }
        override val isDynamicColorEnabled: Flow<Boolean> = _prefs.map { it.useDynamicColor }
        override val isBiometricEnabled: Flow<Boolean> = _prefs.map { it.biometricAuthEnabled }

        override suspend fun saveConnection(connection: ServerConnection) {
            _prefs.value = _prefs.value.copy(connection = connection)
        }

        override suspend fun clearToken() {
            _prefs.value = _prefs.value.copy(connection = _prefs.value.connection?.copy(encryptedPat = ""))
        }

        override suspend fun updateThemeMode(themeMode: ThemeMode) {
            _prefs.value = _prefs.value.copy(themeMode = themeMode)
        }

        override suspend fun setDynamicColor(enabled: Boolean) {
            _prefs.value = _prefs.value.copy(useDynamicColor = enabled)
        }

        override suspend fun setBiometricEnabled(enabled: Boolean) {
            _prefs.value = _prefs.value.copy(biometricAuthEnabled = enabled)
        }

        override suspend fun clearAll() {
            _prefs.value = LucentPreferences()
        }
    }

    @Test
    fun saveSession_storesEncryptedTokenAndEmitsSession() =
        runTest {
            val prefs = FakePreferencesDataSource()
            val localDataSource = DefaultAuthLocalDataSource(prefs, FakeCrypto())

            localDataSource.activeSession.test {
                assertNull(awaitItem())

                localDataSource.saveSession(
                    baseUrl = "https://firefly.test",
                    token = "secret-token",
                    user = AuthUser("1", "user@test.org", "owner"),
                    allowSelfSignedCert = true,
                )

                val session = awaitItem()
                assertEquals("https://firefly.test", session?.serverConfig?.baseUrl)
                assertTrue(session?.serverConfig?.allowSelfSignedCert == true)
                assertEquals("secret-token", session?.token)
                assertEquals("1", session?.user?.id)
                assertEquals("user@test.org", session?.user?.email)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals("enc_secret-token", prefs.connection.first()?.encryptedPat)
        }

    @Test
    fun clearActiveSession_signsOutButRemembersTheAccount() =
        runTest {
            val localDataSource = DefaultAuthLocalDataSource(FakePreferencesDataSource(), FakeCrypto())
            localDataSource.saveSession(
                baseUrl = "https://firefly.test",
                token = "secret-token",
                user = AuthUser("1", "user@test.org", "owner"),
                allowSelfSignedCert = false,
            )

            localDataSource.activeSession.test {
                assertTrue(awaitItem() != null)
                localDataSource.clearActiveSession()
                assertNull(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(localDataSource.isCurrentAccount("https://firefly.test", "1"))
            assertFalse(localDataSource.isCurrentAccount("https://firefly.test", "2"))
            assertFalse(localDataSource.isCurrentAccount("https://other.test", "1"))
        }
}
