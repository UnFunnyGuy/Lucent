package app.lucent.firefly.data.auth.impl.session

import app.lucent.firefly.core.common.network.ApplicationScope
import app.lucent.firefly.core.network.client.ServerCredentialsProvider
import app.lucent.firefly.core.network.model.ServerCredentials
import app.lucent.firefly.data.auth.impl.datasource.AuthLocalDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Singleton

/**
 * Keeps the decrypted credentials of the active profile in memory so requests do not hit
 * DataStore and the Keystore every time. [currentCredentials] waits for the first load instead
 * of reporting "signed out" while DataStore is still being read.
 */
@Singleton
class DefaultServerCredentialsProvider(
    localDataSource: AuthLocalDataSource,
    @param:ApplicationScope scope: CoroutineScope,
) : ServerCredentialsProvider {
    private val state: StateFlow<CredentialsState> =
        localDataSource.activeSession
            .map { session ->
                CredentialsState.Loaded(
                    session?.let {
                        ServerCredentials(
                            baseUrl = it.serverConfig.baseUrl,
                            token = it.token,
                            allowSelfSignedCert = it.serverConfig.allowSelfSignedCert,
                        )
                    },
                )
            }.stateIn(scope, SharingStarted.Eagerly, CredentialsState.Loading)

    override suspend fun currentCredentials(): ServerCredentials? = state.filterIsInstance<CredentialsState.Loaded>().first().credentials

    private sealed interface CredentialsState {
        data object Loading : CredentialsState

        data class Loaded(
            val credentials: ServerCredentials?,
        ) : CredentialsState
    }
}
