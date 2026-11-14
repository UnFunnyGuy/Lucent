package app.lucent.firefly.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "remote_keys",
    primaryKeys = ["entityType", "entityId"],
)
data class RemoteKeyEntity(
    val entityType: String,
    val entityId: String,
    val prevPage: Int?,
    val nextPage: Int?,
    val lastUpdated: Long = System.currentTimeMillis(),
)
