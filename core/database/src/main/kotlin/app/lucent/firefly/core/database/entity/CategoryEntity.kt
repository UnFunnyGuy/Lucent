package app.lucent.firefly.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["serverId"], unique = true),
    ],
)
data class CategoryEntity(
    @PrimaryKey val localId: String,
    val serverId: String?,
    val name: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
