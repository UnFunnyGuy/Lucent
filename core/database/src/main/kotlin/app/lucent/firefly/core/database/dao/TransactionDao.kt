package app.lucent.firefly.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import app.lucent.firefly.core.database.entity.TransactionGroupEntity
import app.lucent.firefly.core.database.entity.TransactionSplitEntity
import app.lucent.firefly.core.database.model.SyncStatus
import app.lucent.firefly.core.database.relation.TransactionWithSplits
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Transaction
    @Query(
        """
        SELECT * FROM transaction_groups
        WHERE NOT EXISTS (
            SELECT 1 FROM transaction_splits
            WHERE transaction_splits.transactionGroupId = transaction_groups.localId
            AND transaction_splits.syncStatus = 'PENDING_DELETE'
        )
        ORDER BY (
            SELECT MAX(date) FROM transaction_splits
            WHERE transaction_splits.transactionGroupId = transaction_groups.localId
        ) DESC, createdAt DESC
        """
    )
    fun pagingSource(): PagingSource<Int, TransactionWithSplits>

    @Transaction
    @Query(
        """
        SELECT * FROM transaction_groups
        WHERE NOT EXISTS (
            SELECT 1 FROM transaction_splits
            WHERE transaction_splits.transactionGroupId = transaction_groups.localId
            AND transaction_splits.syncStatus = 'PENDING_DELETE'
        )
        ORDER BY (
            SELECT MAX(date) FROM transaction_splits
            WHERE transaction_splits.transactionGroupId = transaction_groups.localId
        ) DESC, createdAt DESC
        """
    )
    fun observeTransactions(): Flow<List<TransactionWithSplits>>

    @Query("SELECT * FROM transaction_splits WHERE syncStatus != 'SYNCED'")
    fun observeUnsyncedSplits(): Flow<List<TransactionSplitEntity>>

    @Query(
        """
        SELECT * FROM transaction_splits
        WHERE (sourceId IN (:accountIds) OR destinationId IN (:accountIds))
        AND syncStatus != 'PENDING_DELETE'
        ORDER BY date DESC
        """
    )
    fun observeSplitsForAccount(accountIds: List<String>): Flow<List<TransactionSplitEntity>>

    @Transaction
    @Query("SELECT * FROM transaction_groups WHERE localId = :localGroupId")
    suspend fun getTransactionWithSplits(localGroupId: String): TransactionWithSplits?

    @Query("SELECT * FROM transaction_groups WHERE serverId = :serverId")
    suspend fun getGroupByServerId(serverId: String): TransactionGroupEntity?

    @Query("SELECT * FROM transaction_splits WHERE localSplitId = :localSplitId")
    suspend fun getSplitByLocalId(localSplitId: String): TransactionSplitEntity?

    // Upsert, not REPLACE: REPLACE deletes the row first, which would cascade to its splits.
    @Upsert
    suspend fun insertGroup(group: TransactionGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<TransactionSplitEntity>)

    @Query("UPDATE transaction_splits SET serverSplitId = :serverSplitId, syncStatus = :status WHERE localSplitId = :localSplitId")
    suspend fun updateSplitSyncStatus(localSplitId: String, serverSplitId: String?, status: SyncStatus)

    @Query("UPDATE transaction_groups SET serverId = :serverId WHERE localId = :localId")
    suspend fun updateGroupServerId(localId: String, serverId: String?)

    /** Records the server copy a local group now mirrors; [serverUpdatedAt] is the base for conflict checks. */
    @Query("UPDATE transaction_groups SET serverId = :serverId, updatedAt = :serverUpdatedAt WHERE localId = :localId")
    suspend fun linkGroup(localId: String, serverId: String, serverUpdatedAt: Long)

    @Transaction
    @Query(
        """
        SELECT * FROM transaction_groups
        WHERE localId IN (
            SELECT transactionGroupId FROM transaction_splits
            WHERE date BETWEEN :startMillis AND :endMillis
        )
        """
    )
    suspend fun getGroupsInDateRange(startMillis: Long, endMillis: Long): List<TransactionWithSplits>

    @Query("DELETE FROM transaction_groups WHERE localId = :localGroupId")
    suspend fun deleteGroup(localGroupId: String)

    @Query("DELETE FROM transaction_splits WHERE transactionGroupId = :localGroupId")
    suspend fun deleteSplitsByGroupId(localGroupId: String)

    @Transaction
    suspend fun deleteGroupWithSplits(localGroupId: String) {
        deleteSplitsByGroupId(localGroupId)
        deleteGroup(localGroupId)
    }

}
