package app.lucent.firefly.data.sync.api.handler

import app.lucent.firefly.core.common.result.Result

enum class SyncTargetType {
    TRANSACTION,
    ACCOUNT,
    CATEGORY,
    BUDGET
}

enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}

/**
 * One pending operation from the sync queue. It carries intent only: handlers build the request
 * from the entity's current local state.
 *
 * @property baseUpdatedAt server `updated_at` the local change was based on; null skips the
 * conflict check (the user chose to overwrite the server copy).
 * @property previousAttempts attempts started before this one. A value above zero means an
 * earlier request may have reached the server even if no response was recorded.
 */
data class SyncEntry(
    val queueId: Long,
    val operation: SyncOperation,
    val localId: String,
    val serverId: String?,
    val baseUpdatedAt: Long?,
    val previousAttempts: Int,
)

interface SyncEntityHandler {
    val targetType: SyncTargetType

    /**
     * Pushes [entry] to the server. On success the handler must delete the queue row
     * ([SyncEntry.queueId]) in the same database transaction as its local changes, so a crash can
     * never leave an applied change still queued.
     */
    suspend fun processSync(entry: SyncEntry): Result<Unit>

    /**
     * Abandons a failed [entry]: restores the local entity to the server's state (or removes it if
     * it never reached the server) and deletes the queue row in the same transaction.
     */
    suspend fun discard(entry: SyncEntry): Result<Unit>
}
