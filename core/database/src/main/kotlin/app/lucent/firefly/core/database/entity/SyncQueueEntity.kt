package app.lucent.firefly.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.lucent.firefly.core.database.model.SyncAction
import app.lucent.firefly.core.database.model.SyncEntityType
import app.lucent.firefly.core.database.model.SyncQueueStatus

/**
 * One pending change. It records intent only; the request is built from the entity's current
 * row when it is pushed, so later edits are picked up and app updates never meet stale payloads.
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["entityType", "localEntityId"]),
    ],
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: SyncEntityType,
    val action: SyncAction,
    val localEntityId: String,
    val serverEntityId: String?,
    val status: SyncQueueStatus = SyncQueueStatus.PENDING,
    /** Server `updated_at` the change was based on; null skips the conflict check. */
    val baseUpdatedAt: Long? = null,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    /** The server copy changed since [baseUpdatedAt]; retrying can overwrite it. */
    val isConflict: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
