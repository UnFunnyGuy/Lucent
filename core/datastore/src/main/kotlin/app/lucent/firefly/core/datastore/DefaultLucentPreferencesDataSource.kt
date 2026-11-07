package app.lucent.firefly.core.datastore

import androidx.datastore.core.DataStore
import app.lucent.firefly.core.datastore.model.LucentPreferences
import app.lucent.firefly.core.datastore.model.ServerConnection
import app.lucent.firefly.core.datastore.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class DefaultLucentPreferencesDataSource(
    private val dataStore: DataStore<LucentPreferences>,
) : LucentPreferencesDataSource {

    override val preferences: Flow<LucentPreferences>
        get() = dataStore.data

    override val connection: Flow<ServerConnection?>
        get() = dataStore.data.map { it.connection }.distinctUntilChanged()

    override val themeMode: Flow<ThemeMode>
        get() = dataStore.data.map { it.themeMode }.distinctUntilChanged()

    override val isDynamicColorEnabled: Flow<Boolean>
        get() = dataStore.data.map { it.useDynamicColor }.distinctUntilChanged()

    override val isBiometricEnabled: Flow<Boolean>
        get() = dataStore.data.map { it.biometricAuthEnabled }.distinctUntilChanged()

    override suspend fun saveConnection(connection: ServerConnection) {
        dataStore.updateData { it.copy(connection = connection) }
    }

    override suspend fun clearToken() {
        dataStore.updateData { current ->
            current.copy(connection = current.connection?.copy(encryptedPat = ""))
        }
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.updateData { it.copy(themeMode = themeMode) }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.updateData { it.copy(useDynamicColor = enabled) }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy(biometricAuthEnabled = enabled) }
    }

    override suspend fun clearAll() {
        dataStore.updateData { LucentPreferences() }
    }
}
