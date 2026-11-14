package app.lucent.firefly.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["serverId"], unique = true),
    ],
)
data class BudgetEntity(
    @PrimaryKey val localId: String,
    val serverId: String?,
    val name: String,
    val active: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
)
