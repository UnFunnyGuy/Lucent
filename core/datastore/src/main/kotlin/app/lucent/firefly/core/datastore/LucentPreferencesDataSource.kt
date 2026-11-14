package app.lucent.firefly.core.datastore

import app.lucent.firefly.core.datastore.model.LucentPreferences
import app.lucent.firefly.core.datastore.model.ServerConnection
import app.lucent.firefly.core.datastore.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface LucentPreferencesDataSource {
    val preferences: Flow<LucentPreferences>
    val connection: Flow<ServerConnection?>
    val themeMode: Flow<ThemeMode>
    val isDynamicColorEnabled: Flow<Boolean>
    val isBiometricEnabled: Flow<Boolean>

    suspend fun saveConnection(connection: ServerConnection)

    /** Signs out but remembers the server and user, so signing in again as them keeps local data. */
    suspend fun clearToken()

    suspend fun updateThemeMode(themeMode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setBiometricEnabled(enabled: Boolean)

    suspend fun clearAll()
}
