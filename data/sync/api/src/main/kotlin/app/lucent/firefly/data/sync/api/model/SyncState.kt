package app.lucent.firefly.data.sync.api.model

import app.lucent.firefly.data.sync.api.handler.SyncOperation
import app.lucent.firefly.data.sync.api.handler.SyncTargetType

data class SyncState(
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val failedChanges: List<FailedChange> = emptyList(),
    val lastSuccessfulSyncAt: Long? = null,
    /** The server rejected the stored token; syncing is paused until the user signs in again. */
    val requiresReauthentication: Boolean = false,
)

/** A local change the server rejected; the user decides whether to retry or discard it. */
data class FailedChange(
    val id: Long,
    val target: SyncTargetType,
    val operation: SyncOperation,
    val localId: String,
    val message: String,
    /** The server copy changed in the meantime; retrying with overwrite replaces it. */
    val isConflict: Boolean,
)
