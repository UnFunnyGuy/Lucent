package app.lucent.firefly.core.database.di

import android.content.Context
import androidx.room.Room
import app.lucent.firefly.core.database.LucentDatabase
import app.lucent.firefly.core.database.dao.AccountDao
import app.lucent.firefly.core.database.dao.BudgetDao
import app.lucent.firefly.core.database.dao.CategoryDao
import app.lucent.firefly.core.database.dao.RemoteKeyDao
import app.lucent.firefly.core.database.dao.SyncMetadataDao
import app.lucent.firefly.core.database.dao.SyncQueueDao
import app.lucent.firefly.core.database.dao.TransactionDao
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@Configuration
@ComponentScan("app.lucent.firefly.core.database")
class DatabaseModule {

    @Singleton
    fun provideDatabase(
        context: Context
    ): LucentDatabase {
        return Room.databaseBuilder(
            context,
            LucentDatabase::class.java,
            DATABASE_NAME
        )
            // Pre-release: a schema change resets the local cache instead of migrating it.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
    }

    @Factory
    fun provideAccountDao(database: LucentDatabase): AccountDao = database.accountDao()

    @Factory
    fun provideTransactionDao(database: LucentDatabase): TransactionDao = database.transactionDao()

    @Factory
    fun provideSyncQueueDao(database: LucentDatabase): SyncQueueDao = database.syncQueueDao()

    @Factory
    fun provideRemoteKeyDao(database: LucentDatabase): RemoteKeyDao = database.remoteKeyDao()

    @Factory
    fun provideCategoryDao(database: LucentDatabase): CategoryDao = database.categoryDao()

    @Factory
    fun provideBudgetDao(database: LucentDatabase): BudgetDao = database.budgetDao()

    @Factory
    fun provideSyncMetadataDao(database: LucentDatabase): SyncMetadataDao = database.syncMetadataDao()
}

private const val DATABASE_NAME = "lucent.db"
