package app.lucent.firefly.core.datastore.model

import kotlinx.serialization.Serializable

/** Everything Lucent persists in DataStore, written atomically. */
@Serializable
data class LucentPreferences(
    val connection: ServerConnection? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val biometricAuthEnabled: Boolean = false,
)
