package app.lucent.firefly.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import app.cash.turbine.test
import app.lucent.firefly.core.datastore.model.LucentPreferences
import app.lucent.firefly.core.datastore.model.ServerConnection
import app.lucent.firefly.core.datastore.model.ThemeMode
import app.lucent.firefly.core.datastore.serializer.LucentPreferencesSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [DefaultLucentPreferencesDataSource].
 *
 * Uses an unencrypted [LucentPreferencesSerializer] (no Tink/AeadSerializer) with an
 * in-memory temp file so tests run on pure JVM without Android Keystore dependencies.
 * All reactive Flow assertions use Turbine.
 */
class LucentPreferencesDataSourceTest {
    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<LucentPreferences>
    private lateinit var dataSource: LucentPreferencesDataSource

    private fun buildDataStoreFile(name: String): File = tmpFolder.newFile(name)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dataStore =
            DataStoreFactory.create(
                serializer = LucentPreferencesSerializer,
                scope = testScope,
                produceFile = { buildDataStoreFile("lucent_prefs_test.pb") },
            )
        dataSource =
            DefaultLucentPreferencesDataSource(
                dataStore = dataStore,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `preferences emits default values on first run`() =
        runTest {
            dataSource.preferences.test {
                assertEquals(LucentPreferences(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `connection is null before the first sign-in`() =
        runTest {
            dataSource.connection.test {
                assertNull(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `saveConnection replaces the stored connection`() =
        runTest {
            dataSource.saveConnection(testConnection("a"))
            dataSource.saveConnection(testConnection("b"))

            dataSource.connection.test {
                assertEquals(testConnection("b"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `clearToken removes only the token`() =
        runTest {
            dataSource.saveConnection(testConnection("a"))
            dataSource.updateThemeMode(ThemeMode.DARK)

            dataSource.clearToken()

            dataSource.preferences.test {
                val prefs = awaitItem()
                assertEquals(testConnection("a").copy(encryptedPat = ""), prefs.connection)
                assertEquals(ThemeMode.DARK, prefs.themeMode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `clearToken without a connection is a no-op`() =
        runTest {
            dataSource.clearToken()

            dataSource.connection.test {
                assertNull(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `updateThemeMode persists new theme`() =
        runTest {
            dataSource.updateThemeMode(ThemeMode.DARK)

            dataSource.themeMode.test {
                assertEquals(ThemeMode.DARK, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setDynamicColor false persists disabled state`() =
        runTest {
            dataSource.setDynamicColor(false)

            dataSource.isDynamicColorEnabled.test {
                assertEquals(false, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setBiometricEnabled true persists enabled state`() =
        runTest {
            dataSource.setBiometricEnabled(true)

            dataSource.isBiometricEnabled.test {
                assertEquals(true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `clearAll resets all preferences to defaults`() =
        runTest {
            dataSource.saveConnection(testConnection("a"))
            dataSource.updateThemeMode(ThemeMode.DARK)
            dataSource.setBiometricEnabled(true)

            dataSource.clearAll()

            dataSource.preferences.test {
                assertEquals(LucentPreferences(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun testConnection(id: String) =
        ServerConnection(
            baseUrl = "https://firefly-$id.local",
            encryptedPat = "encrypted-pat-$id",
            userId = "user-$id",
            userEmail = "$id@example.com",
        )
}
