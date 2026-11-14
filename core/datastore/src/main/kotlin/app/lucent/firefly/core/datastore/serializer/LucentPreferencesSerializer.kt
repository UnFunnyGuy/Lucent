package app.lucent.firefly.core.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import app.lucent.firefly.core.datastore.model.LucentPreferences
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * Underlying [Serializer] for [LucentPreferences], using `kotlinx.serialization` JSON.
 *
 * This serializer handles the plaintext read/write contract. In production it is wrapped
 * by `AeadSerializer` from `androidx.datastore:datastore-tink` which transparently encrypts
 * all bytes before writing to disk and decrypts them before passing to [readFrom].
 *
 * Corruption (malformed JSON after decryption) is surfaced as a [CorruptionException],
 * which is then caught by the [androidx.datastore.core.handlers.ReplaceFileCorruptionHandler]
 * registered in [app.lucent.firefly.core.datastore.di.DataStoreModule].
 */
object LucentPreferencesSerializer : Serializer<LucentPreferences> {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override val defaultValue: LucentPreferences = LucentPreferences()

    override suspend fun readFrom(input: InputStream): LucentPreferences {
        return try {
            val bytes = input.readBytes()
            if (bytes.isEmpty()) return defaultValue
            json.decodeFromString<LucentPreferences>(bytes.decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot decode LucentPreferences from DataStore.", e)
        }
    }

    override suspend fun writeTo(
        t: LucentPreferences,
        output: OutputStream,
    ) {
        output.write(
            json.encodeToString(t).encodeToByteArray(),
        )
    }
}
