package app.lucent.firefly.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.lucent.firefly.core.database.converter.LucentTypeConverters
import app.lucent.firefly.core.database.dao.AccountDao
import app.lucent.firefly.core.database.dao.BudgetDao
import app.lucent.firefly.core.database.dao.CategoryDao
import app.lucent.firefly.core.database.dao.RemoteKeyDao
import app.lucent.firefly.core.database.dao.SyncMetadataDao
import app.lucent.firefly.core.database.dao.SyncQueueDao
import app.lucent.firefly.core.database.dao.TransactionDao
import app.lucent.firefly.core.database.entity.AccountEntity
import app.lucent.firefly.core.database.entity.BudgetEntity
import app.lucent.firefly.core.database.entity.CategoryEntity
import app.lucent.firefly.core.database.entity.RemoteKeyEntity
import app.lucent.firefly.core.database.entity.SyncMetadataEntity
import app.lucent.firefly.core.database.entity.SyncQueueEntity
import app.lucent.firefly.core.database.entity.TransactionGroupEntity
import app.lucent.firefly.core.database.entity.TransactionSplitEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionGroupEntity::class,
        TransactionSplitEntity::class,
        SyncQueueEntity::class,
        RemoteKeyEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        SyncMetadataEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(LucentTypeConverters::class)
abstract class LucentDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun remoteKeyDao(): RemoteKeyDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun syncMetadataDao(): SyncMetadataDao
}
