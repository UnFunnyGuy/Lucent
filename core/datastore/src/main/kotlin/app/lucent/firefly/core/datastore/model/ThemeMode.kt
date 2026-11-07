@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package app.lucent.firefly.core.datastore.model

import kotlinx.serialization.Serializable

/**
 * App-wide theme preference.
 */
@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
