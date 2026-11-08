package app.lucent.firefly.data.sync.impl.initializer

import app.lucent.firefly.core.common.network.ApplicationScope
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.sync.api.manager.SyncInitializer
import app.lucent.firefly.data.sync.api.manager.SyncManager
import app.lucent.firefly.data.sync.impl.processor.SyncStatusStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import org.koin.core.annotation.Singleton

/**
 * While a profile is signed in, periodic sync is scheduled and a sync runs once at start-up
 * (uploading anything left from a previous process). Signing out cancels both.
 */
@Singleton
class SessionSyncInitializer(
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager,
    private val statusStore: SyncStatusStore,
    @param:ApplicationScope private val scope: CoroutineScope,
) : SyncInitializer {

    private val started = AtomicBoolean(false)

    override fun initialize() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            authRepository.activeSession
                .map { session -> session?.token }
                .distinctUntilChanged()
                .collect { token ->
                    // New credentials (sign-in or a replaced token) may fix an earlier 401.
                    statusStore.clearReauthenticationRequired()
                    if (token != null) {
                        syncManager.schedulePeriodicSync()
                        syncManager.requestSync(expedited = false)
                    } else {
                        syncManager.cancelSync()
                    }
                }
        }
    }
}
