package app.lucent.firefly.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import app.lucent.firefly.core.database.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE id = 0")
    fun observe(): Flow<SyncMetadataEntity?>

    @Query("SELECT * FROM sync_metadata WHERE id = 0")
    suspend fun get(): SyncMetadataEntity?

    @Upsert
    suspend fun upsert(metadata: SyncMetadataEntity)

    @Transaction
    suspend fun markSynced(at: Long, deep: Boolean) {
        val current = get() ?: SyncMetadataEntity()
        upsert(
            current.copy(
                lastSuccessfulSyncAt = at,
                lastDeepSyncAt = if (deep) at else current.lastDeepSyncAt,
            ),
        )
    }
}
