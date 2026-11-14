package app.lucent.firefly.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.tink.AeadSerializer
import app.lucent.firefly.core.common.network.ApplicationScope
import app.lucent.firefly.core.common.network.IoDispatcher
import app.lucent.firefly.core.datastore.DefaultLucentPreferencesDataSource
import app.lucent.firefly.core.datastore.LucentPreferencesDataSource
import app.lucent.firefly.core.datastore.model.LucentPreferences
import app.lucent.firefly.core.datastore.serializer.LucentPreferencesSerializer
import app.lucent.firefly.core.datastore.serializer.TinkAeadProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import java.io.File

@Module
@Configuration
@ComponentScan("app.lucent.firefly.core.datastore")
class DataStoreModule {
    /**
     * Provides the encrypted `DataStore<LucentPreferences>` singleton.
     *
     * - File location: `<filesDir>/datastore/lucent_prefs.pb`
     * - Encryption: AES-256-GCM via Android Keystore (Tink)
     * - Corruption recovery: [ReplaceFileCorruptionHandler] resets to [LucentPreferences] defaults
     *   instead of crashing (e.g. on key loss or file tampering).
     *
     * The IO dispatcher is passed to the DataStore scope so that all internal
     * DataStore reads/writes are dispatched off the main thread automatically.
     */
    @Singleton
    fun provideDataStore(
        context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @ApplicationScope appScope: CoroutineScope,
        tinkAeadProvider: TinkAeadProvider,
    ): DataStore<LucentPreferences> {
        val aead = tinkAeadProvider.getAead()
        val encryptedSerializer =
            AeadSerializer(
                aead = aead,
                wrappedSerializer = LucentPreferencesSerializer,
            )
        return DataStoreFactory.create(
            serializer = encryptedSerializer,
            corruptionHandler =
                ReplaceFileCorruptionHandler {
                    LucentPreferences()
                },
            scope = CoroutineScope(appScope.coroutineContext + ioDispatcher),
            produceFile = {
                File(context.filesDir, "datastore/lucent_prefs.pb")
            },
        )
    }

    /**
     * Binds [DefaultLucentPreferencesDataSource] as the implementation of
     * [LucentPreferencesDataSource] for the entire application lifecycle.
     *
     * No ioDispatcher is needed here — DataStore handles its own IO dispatch
     * internally via the scope provided to [provideDataStore].
     */
    @Singleton
    fun provideLucentPreferencesDataSource(dataStore: DataStore<LucentPreferences>): LucentPreferencesDataSource = DefaultLucentPreferencesDataSource(dataStore)
}
