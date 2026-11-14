package app.lucent.firefly.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.lucent.firefly.core.database.model.SyncStatus

@Entity(
    tableName = "transaction_splits",
    foreignKeys = [
        ForeignKey(
            entity = TransactionGroupEntity::class,
            parentColumns = ["localId"],
            childColumns = ["transactionGroupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["transactionGroupId"]),
        Index(value = ["date"]),
        Index(value = ["sourceId"]),
        Index(value = ["destinationId"]),
        Index(value = ["syncStatus"]),
    ],
)
data class TransactionSplitEntity(
    @PrimaryKey val localSplitId: String,
    val transactionGroupId: String,
    val serverSplitId: String?,
    val type: String,
    val date: Long,
    val amount: String,
    val currencyCode: String,
    val currencySymbol: String,
    val currencyDecimalPlaces: Int,
    val description: String,
    val sourceId: String?,
    val sourceName: String?,
    val destinationId: String?,
    val destinationName: String?,
    val categoryId: String?,
    val categoryName: String?,
    val budgetId: String?,
    val budgetName: String?,
    val notes: String?,
    val tags: List<String>,
    val syncStatus: SyncStatus,
    val order: Int = 0,
)
