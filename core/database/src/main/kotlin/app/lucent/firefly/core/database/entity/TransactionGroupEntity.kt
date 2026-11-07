package app.lucent.firefly.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_groups",
    indices = [
        Index(value = ["serverId"], unique = true),
        Index(value = ["createdAt"]),
    ]
)
data class TransactionGroupEntity(
    @PrimaryKey val localId: String,
    val serverId: String?,
    val groupTitle: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
