package app.lucent.firefly.core.datastore.serializer

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import org.koin.core.annotation.Factory

/**
 * Constructs and provides a Tink [Aead] primitive backed by the Android Keystore.
 *
 * The keyset is stored in `SharedPreferences` (file: `lucent_keyset_prefs`, key: `lucent_datastore_keyset`).
 * The master wrapping key lives inside the Android Keystore under the alias
 * `android-keystore://lucent_datastore_master_key` and uses AES-256-GCM.
 *
 * This class is safe to share as a singleton — [getAead] is idempotent and
 * the resulting [Aead] instance is thread-safe.
 */
@Factory
class TinkAeadProvider(
    private val context: Context,
) {

    /**
     * Returns an [Aead] primitive ready for use with `AeadSerializer`.
     *
     * @throws com.google.crypto.tink.GeneralSecurityException if Tink initialization fails.
     * @throws java.io.IOException if the keyset cannot be stored/retrieved from SharedPreferences.
     */
    fun getAead(): Aead {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(
                context,
                /* keysetName = */ "lucent_datastore_keyset",
                /* prefFileName = */ "lucent_keyset_prefs",
            )
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://lucent_datastore_master_key")
            .build()
            .keysetHandle

        return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }
}
