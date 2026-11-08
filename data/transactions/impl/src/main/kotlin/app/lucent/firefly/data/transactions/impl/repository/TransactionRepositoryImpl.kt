package app.lucent.firefly.data.transactions.impl.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import app.lucent.firefly.core.common.network.IoDispatcher
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.LucentDatabase
import app.lucent.firefly.core.database.dao.RemoteKeyDao
import app.lucent.firefly.core.database.dao.SyncQueueDao
import app.lucent.firefly.core.database.dao.TransactionDao
import app.lucent.firefly.core.database.model.EnqueueResult
import app.lucent.firefly.core.database.model.SyncAction
import app.lucent.firefly.core.database.model.SyncEntityType
import app.lucent.firefly.core.database.model.SyncStatus
import app.lucent.firefly.core.database.paging.LucentRemoteMediator
import app.lucent.firefly.core.database.paging.RemotePage
import app.lucent.firefly.core.database.relation.TransactionWithSplits
import app.lucent.firefly.core.network.model.hasNextPage
import app.lucent.firefly.data.sync.api.manager.SyncManager
import app.lucent.firefly.data.transactions.api.model.Transaction
import app.lucent.firefly.data.transactions.api.model.TransactionDraft
import app.lucent.firefly.data.transactions.api.repository.TransactionRepository
import app.lucent.firefly.data.transactions.impl.datasource.TransactionRemoteDataSource
import app.lucent.firefly.data.transactions.impl.dto.TransactionReadDto
import app.lucent.firefly.data.transactions.impl.mapper.toDomain
import app.lucent.firefly.data.transactions.impl.mapper.toGroupEntity
import app.lucent.firefly.data.transactions.impl.mapper.toSplitEntity
import app.lucent.firefly.data.transactions.impl.sync.RemoteTransactionWriter
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Singleton

@OptIn(ExperimentalPagingApi::class)
@Singleton
class TransactionRepositoryImpl(
    private val database: LucentDatabase,
    private val transactionDao: TransactionDao,
    private val remoteKeyDao: RemoteKeyDao,
    private val syncQueueDao: SyncQueueDao,
    private val remoteDataSource: TransactionRemoteDataSource,
    private val remoteWriter: RemoteTransactionWriter,
    private val syncManager: SyncManager,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactionDao.observeTransactions()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun pagedTransactions(): Flow<PagingData<Transaction>> =
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            remoteMediator = LucentRemoteMediator<TransactionWithSplits, TransactionReadDto>(
                database = database,
                remoteKeyDao = remoteKeyDao,
                listKey = TRANSACTIONS_LIST_KEY,
                fetchPage = { page ->
                    when (val result = remoteDataSource.getTransactionPage(page)) {
                        is Result.Success -> Result.Success(RemotePage(result.data.data, result.data.meta.hasNextPage))
                        is Result.Error -> result
                    }
                },
                persistPage = { groups -> groups.forEach { remoteWriter.upsert(it) } },
            ),
            pagingSourceFactory = { transactionDao.pagingSource() },
        ).flow.map { page -> page.map { it.toDomain() } }

    override suspend fun getTransaction(id: String): Result<Transaction> = withContext(ioDispatcher) {
        val groupWithSplits = transactionDao.getTransactionWithSplits(id)
        if (groupWithSplits != null) {
            Result.Success(groupWithSplits.toDomain())
        } else {
            Result.Error(AppError.Common("Transaction not found with ID: $id"))
        }
    }

    override suspend fun createTransaction(draft: TransactionDraft): Result<String> = withContext(ioDispatcher) {
        validate(draft)?.let { return@withContext Result.Error(it) }

        val groupId = UUID.randomUUID().toString()
        database.withTransaction {
            transactionDao.insertGroup(draft.toGroupEntity(groupId = groupId))
            transactionDao.insertSplits(
                listOf(
                    draft.toSplitEntity(splitId = UUID.randomUUID().toString(), groupId = groupId),
                ),
            )
            syncQueueDao.enqueueChange(
                entityType = SyncEntityType.TRANSACTION,
                localEntityId = groupId,
                serverEntityId = null,
                action = SyncAction.CREATE,
                baseUpdatedAt = null,
            )
        }
        syncManager.requestSync(expedited = true)
        Result.Success(groupId)
    }

    override suspend fun updateTransaction(id: String, draft: TransactionDraft): Result<Unit> = withContext(ioDispatcher) {
        validate(draft)?.let { return@withContext Result.Error(it) }

        // The read and the write share one transaction: a sync run may link or complete this
        // transaction at any moment, and an edit built from a stale copy would be lost.
        val result = database.withTransaction {
            val transaction = transactionDao.getTransactionWithSplits(id)
                ?: return@withTransaction Result.Error(AppError.Common("Transaction not found: $id"))
            val split = transaction.splits.singleOrNull()
                ?: return@withTransaction Result.Error(AppError.Common("Editing split transactions is not supported yet"))
            if (split.syncStatus == SyncStatus.PENDING_DELETE) {
                return@withTransaction Result.Error(AppError.Common("This transaction is being deleted"))
            }

            val serverId = transaction.group.serverId
            val updatedSplit = draft.toSplitEntity(splitId = split.localSplitId, groupId = id)
                .copy(
                    serverSplitId = split.serverSplitId,
                    order = split.order,
                    syncStatus = if (serverId == null) SyncStatus.PENDING_CREATE else SyncStatus.PENDING_UPDATE,
                )
            transactionDao.insertSplits(listOf(updatedSplit))
            syncQueueDao.enqueueChange(
                entityType = SyncEntityType.TRANSACTION,
                localEntityId = id,
                serverEntityId = serverId,
                // Not on the server yet: the queued CREATE picks up the edit.
                action = if (serverId == null) SyncAction.CREATE else SyncAction.UPDATE,
                baseUpdatedAt = serverId?.let { transaction.group.updatedAt },
            )
            Result.Success(Unit)
        }
        if (result is Result.Success) syncManager.requestSync(expedited = true)
        result
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> = withContext(ioDispatcher) {
        val outcome = database.withTransaction {
            val transaction = transactionDao.getTransactionWithSplits(id)
                ?: return@withTransaction Result.Error(AppError.Common("Transaction not found: $id"))

            val enqueued = syncQueueDao.enqueueChange(
                entityType = SyncEntityType.TRANSACTION,
                localEntityId = id,
                serverEntityId = transaction.group.serverId,
                action = SyncAction.DELETE,
                baseUpdatedAt = transaction.group.serverId?.let { transaction.group.updatedAt },
            )
            if (enqueued == EnqueueResult.CANCELLED_CREATE) {
                transactionDao.deleteGroupWithSplits(id)
            } else {
                transaction.splits.forEach { split ->
                    transactionDao.updateSplitSyncStatus(split.localSplitId, split.serverSplitId, SyncStatus.PENDING_DELETE)
                }
            }
            Result.Success(enqueued)
        }
        when (outcome) {
            is Result.Error -> outcome
            is Result.Success -> {
                if (outcome.data != EnqueueResult.CANCELLED_CREATE) syncManager.requestSync(expedited = true)
                Result.Success(Unit)
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val TRANSACTIONS_LIST_KEY = "transactions"
    }

    private fun validate(draft: TransactionDraft): AppError? = when {
        draft.amount.amount.signum() <= 0 -> AppError.Common("The amount must be more than zero.")
        draft.description.isBlank() -> AppError.Common("A description is required.")
        else -> null
    }
}
