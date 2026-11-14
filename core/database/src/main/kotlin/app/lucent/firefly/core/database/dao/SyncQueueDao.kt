package app.lucent.firefly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import app.lucent.firefly.core.database.entity.SyncQueueEntity
import app.lucent.firefly.core.database.model.EnqueueResult
import app.lucent.firefly.core.database.model.SyncAction
import app.lucent.firefly.core.database.model.SyncEntityType
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY id ASC")
    suspend fun getPendingQueue(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status = 'FAILED' ORDER BY id ASC")
    suspend fun getFailed(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status = 'FAILED' ORDER BY id ASC")
    fun observeFailed(): Flow<List<SyncQueueEntity>>

    /** All operations for one entity, in any status. */
    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND localEntityId = :localEntityId ORDER BY id ASC")
    suspend fun getForEntity(
        entityType: SyncEntityType,
        localEntityId: String,
    ): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun getById(id: Long): SyncQueueEntity?

    @Insert
    suspend fun enqueue(entry: SyncQueueEntity): Long

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_queue WHERE entityType = :entityType AND localEntityId = :localEntityId")
    suspend fun deleteByEntity(
        entityType: SyncEntityType,
        localEntityId: String,
    )

    /** Recorded before the request is sent, so an attempt interrupted by process death is still counted. */
    @Query("UPDATE sync_queue SET attemptCount = attemptCount + 1 WHERE id = :id")
    suspend fun markAttemptStarted(id: Long)

    @Query("UPDATE sync_queue SET lastError = :lastError WHERE id = :id")
    suspend fun recordError(
        id: Long,
        lastError: String?,
    )

    @Query("UPDATE sync_queue SET status = 'FAILED', lastError = :lastError, isConflict = :isConflict WHERE id = :id")
    suspend fun markFailed(
        id: Long,
        lastError: String,
        isConflict: Boolean = false,
    )

    @Query(
        "UPDATE sync_queue SET status = 'PENDING', attemptCount = 0, lastError = NULL, isConflict = 0 WHERE id = :id",
    )
    suspend fun resetForRetry(id: Long)

    /** Retry that overwrites server-side changes: the conflict check is skipped. */
    @Query(
        "UPDATE sync_queue SET status = 'PENDING', attemptCount = 0, lastError = NULL, isConflict = 0, baseUpdatedAt = NULL WHERE id = :id",
    )
    suspend fun resetForOverwrite(id: Long)

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun countPending(): Flow<Int>

    /**
     * Queues [action] for an entity, folding it into what is already queued so each entity has at
     * most one operation:
     * - CREATE/UPDATE + UPDATE → unchanged (the push reads the current row);
     * - CREATE + DELETE → both removed ([EnqueueResult.CANCELLED_CREATE]);
     * - UPDATE + DELETE → DELETE, keeping the UPDATE's base version.
     * A merged operation that had failed is reset to pending, since the user changed it again.
     */
    @Transaction
    suspend fun enqueueChange(
        entityType: SyncEntityType,
        localEntityId: String,
        serverEntityId: String?,
        action: SyncAction,
        baseUpdatedAt: Long?,
    ): EnqueueResult {
        val existing = getForEntity(entityType, localEntityId).lastOrNull()
        val newEntry =
            SyncQueueEntity(
                entityType = entityType,
                action = action,
                localEntityId = localEntityId,
                serverEntityId = serverEntityId,
                baseUpdatedAt = baseUpdatedAt,
            )
        if (existing == null) {
            enqueue(newEntry)
            return EnqueueResult.QUEUED
        }
        return when (action) {
            SyncAction.CREATE, SyncAction.UPDATE -> {
                check(existing.action != SyncAction.DELETE) { "Entity $localEntityId is already being deleted" }
                resetForRetry(existing.id)
                EnqueueResult.MERGED
            }
            SyncAction.DELETE ->
                when (existing.action) {
                    SyncAction.CREATE -> {
                        deleteByEntity(entityType, localEntityId)
                        EnqueueResult.CANCELLED_CREATE
                    }
                    SyncAction.UPDATE -> {
                        deleteById(existing.id)
                        enqueue(newEntry.copy(baseUpdatedAt = existing.baseUpdatedAt ?: baseUpdatedAt))
                        EnqueueResult.MERGED
                    }
                    SyncAction.DELETE -> EnqueueResult.MERGED
                }
        }
    }
}
