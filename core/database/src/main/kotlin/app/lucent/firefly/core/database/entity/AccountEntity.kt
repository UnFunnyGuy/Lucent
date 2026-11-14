package app.lucent.firefly.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.lucent.firefly.core.database.model.SyncStatus

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["serverId"], unique = true),
        Index(value = ["type", "active"]),
    ],
)
data class AccountEntity(
    @PrimaryKey val localId: String,
    val serverId: String?,
    val name: String,
    val type: String,
    val currentBalance: String,
    val currencyId: String?,
    val currencyCode: String,
    val currencySymbol: String,
    val currencyDecimalPlaces: Int,
    val active: Boolean,
    val order: Int?,
    @ColumnInfo(defaultValue = "1")
    val includeNetWorth: Boolean = true,
    val primaryCurrencyBalance: String? = null,
    val primaryCurrencyCode: String? = null,
    val primaryCurrencySymbol: String? = null,
    val primaryCurrencyDecimalPlaces: Int? = null,
    val syncStatus: SyncStatus,
    val updatedAt: Long = System.currentTimeMillis(),
)
