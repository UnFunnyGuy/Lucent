package app.lucent.firefly.data.sync.api.manager

import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.sync.api.model.SyncState
import kotlinx.coroutines.flow.StateFlow

interface SyncManager {
    val syncState: StateFlow<SyncState>
    fun requestSync(expedited: Boolean = false)
    fun schedulePeriodicSync(intervalMinutes: Long = 15)
    fun cancelSync()

    /** Puts a failed change back in the queue; [overwriteServer] skips the conflict check. */
    suspend fun retryFailedChange(id: Long, overwriteServer: Boolean = false)

    /** Drops a failed change and restores the server's version locally. */
    suspend fun discardFailedChange(id: Long): Result<Unit>
}
