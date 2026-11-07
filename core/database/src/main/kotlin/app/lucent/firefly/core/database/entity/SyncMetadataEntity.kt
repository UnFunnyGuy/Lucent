package app.lucent.firefly.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table: sync bookkeeping for the connected server. */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastSuccessfulSyncAt: Long? = null,
    val lastDeepSyncAt: Long? = null,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
