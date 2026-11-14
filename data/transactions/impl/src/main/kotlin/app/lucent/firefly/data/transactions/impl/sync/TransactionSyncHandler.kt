package app.lucent.firefly.data.transactions.impl.sync

import androidx.room.withTransaction
import app.lucent.firefly.core.common.network.IoDispatcher
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.LucentDatabase
import app.lucent.firefly.core.database.dao.SyncQueueDao
import app.lucent.firefly.core.database.dao.TransactionDao
import app.lucent.firefly.core.database.model.SyncStatus
import app.lucent.firefly.core.database.relation.TransactionWithSplits
import app.lucent.firefly.data.sync.api.handler.SyncEntityHandler
import app.lucent.firefly.data.sync.api.handler.SyncEntry
import app.lucent.firefly.data.sync.api.handler.SyncOperation
import app.lucent.firefly.data.sync.api.handler.SyncTargetType
import app.lucent.firefly.data.transactions.impl.datasource.TransactionRemoteDataSource
import app.lucent.firefly.data.transactions.impl.dto.TransactionReadDto
import app.lucent.firefly.data.transactions.impl.mapper.parseIsoInstant
import app.lucent.firefly.data.transactions.impl.mapper.toStoreDto
import app.lucent.firefly.data.transactions.impl.mapper.toUpdateDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Singleton

/**
 * Pushes queued transaction changes. Requests are built from the current local rows. Firefly has
 * no ETag, so updates and deletes compare the server's `updated_at` with the version the local
 * change was based on and report a conflict instead of overwriting someone else's edit.
 */
@Singleton
class TransactionSyncHandler(
    private val database: LucentDatabase,
    private val transactionDao: TransactionDao,
    private val syncQueueDao: SyncQueueDao,
    private val remoteDataSource: TransactionRemoteDataSource,
    private val remoteWriter: RemoteTransactionWriter,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SyncEntityHandler {
    override val targetType: SyncTargetType = SyncTargetType.TRANSACTION

    override suspend fun processSync(entry: SyncEntry): Result<Unit> =
        withContext(ioDispatcher) {
            when (entry.operation) {
                SyncOperation.CREATE -> handleCreate(entry)
                SyncOperation.UPDATE -> handleUpdate(entry)
                SyncOperation.DELETE -> handleDelete(entry)
            }
        }

    override suspend fun discard(entry: SyncEntry): Result<Unit> =
        withContext(ioDispatcher) {
            when (entry.operation) {
                SyncOperation.CREATE -> {
                    // An earlier attempt may have reached the server; don't leave that copy behind.
                    if (entry.previousAttempts > 0) {
                        when (val existing = findRemoteByExternalId(entry.localId)) {
                            is Result.Success ->
                                existing.data?.let { remote ->
                                    val deleted = remoteDataSource.deleteTransaction(remote.id)
                                    if (deleted is Result.Error && !deleted.error.isNotFound()) return@withContext deleted
                                }
                            is Result.Error -> return@withContext existing
                        }
                    }
                    complete(entry) { transactionDao.deleteGroupWithSplits(entry.localId) }
                }
                SyncOperation.UPDATE -> {
                    val serverId =
                        transactionDao.getTransactionWithSplits(entry.localId)?.group?.serverId ?: entry.serverId
                            ?: return@withContext complete(entry)
                    when (val remote = remoteDataSource.getTransaction(serverId)) {
                        is Result.Success ->
                            complete(entry) {
                                remoteWriter.upsert(remote.data.data, overwriteLocalChanges = true)
                            }
                        is Result.Error ->
                            if (remote.error.isNotFound()) {
                                complete(entry) { transactionDao.deleteGroupWithSplits(entry.localId) }
                            } else {
                                remote
                            }
                    }
                }
                SyncOperation.DELETE ->
                    complete(entry) {
                        transactionDao
                            .getTransactionWithSplits(entry.localId)
                            ?.splits
                            ?.filter { it.syncStatus == SyncStatus.PENDING_DELETE }
                            ?.forEach { transactionDao.updateSplitSyncStatus(it.localSplitId, it.serverSplitId, SyncStatus.SYNCED) }
                    }
            }
        }

    private suspend fun handleCreate(entry: SyncEntry): Result<Unit> {
        val local = transactionDao.getTransactionWithSplits(entry.localId)
        if (local == null || local.group.serverId != null) {
            // Deleted locally, or linked by an earlier run that died before removing the queue row.
            return complete(entry) { local?.let { markSynced(it, SyncStatus.PENDING_CREATE) } }
        }

        // An earlier attempt may have reached the server even though no response was recorded.
        if (entry.previousAttempts > 0) {
            when (val existing = findRemoteByExternalId(entry.localId)) {
                is Result.Success -> existing.data?.let { return linkCreated(entry, it) }
                is Result.Error -> return existing
            }
        }

        return when (val response = remoteDataSource.postTransaction(local.toStoreDto(externalId = entry.localId))) {
            is Result.Success -> linkCreated(entry, response.data.data)
            is Result.Error -> {
                if (response.error.isUncertainDelivery()) {
                    when (val existing = findRemoteByExternalId(entry.localId)) {
                        is Result.Success -> existing.data?.let { linkCreated(entry, it) } ?: response
                        is Result.Error -> response
                    }
                } else {
                    response
                }
            }
        }
    }

    private suspend fun handleUpdate(entry: SyncEntry): Result<Unit> {
        val local = transactionDao.getTransactionWithSplits(entry.localId) ?: return complete(entry)
        val serverId =
            local.group.serverId ?: entry.serverId
                ?: return Result.Error(AppError.Common("Transaction ${entry.localId} has no server copy to update"))

        when (val state = serverState(serverId, entry.baseUpdatedAt)) {
            ServerState.Unchanged -> Unit
            ServerState.Changed -> return Result.Error(AppError.Conflict("Changed on the server after it was edited here"))
            ServerState.Missing -> return Result.Error(AppError.Conflict("Deleted on the server after it was edited here"))
            is ServerState.Unknown -> return Result.Error(state.error)
        }

        return when (val response = remoteDataSource.updateTransaction(serverId, local.toUpdateDto())) {
            is Result.Success -> {
                val remote = response.data.data
                complete(entry) {
                    transactionDao.linkGroup(entry.localId, remote.id, parseIsoInstant(remote.attributes.updatedAt))
                    // Re-read: the user may have edited again while the request was in flight.
                    transactionDao.getTransactionWithSplits(entry.localId)?.let { current ->
                        current.splits.withIndex().forEach { (index, split) ->
                            if (split.syncStatus == SyncStatus.PENDING_UPDATE) {
                                transactionDao.updateSplitSyncStatus(
                                    split.localSplitId,
                                    remote.attributes.transactions
                                        .getOrNull(index)
                                        ?.transactionJournalId ?: split.serverSplitId,
                                    SyncStatus.SYNCED,
                                )
                            }
                        }
                    }
                }
            }
            is Result.Error -> response
        }
    }

    private suspend fun handleDelete(entry: SyncEntry): Result<Unit> {
        val serverId = transactionDao.getTransactionWithSplits(entry.localId)?.group?.serverId ?: entry.serverId
        if (serverId != null) {
            when (val state = serverState(serverId, entry.baseUpdatedAt)) {
                ServerState.Unchanged -> Unit
                ServerState.Changed ->
                    return Result.Error(AppError.Conflict("Changed on the server after it was deleted here"))
                ServerState.Missing -> return complete(entry) { transactionDao.deleteGroupWithSplits(entry.localId) }
                is ServerState.Unknown -> return Result.Error(state.error)
            }
            val result = remoteDataSource.deleteTransaction(serverId)
            if (result is Result.Error && !result.error.isNotFound()) return result
        }
        return complete(entry) { transactionDao.deleteGroupWithSplits(entry.localId) }
    }

    private sealed interface ServerState {
        data object Unchanged : ServerState

        data object Changed : ServerState

        data object Missing : ServerState

        data class Unknown(
            val error: AppError,
        ) : ServerState
    }

    /** Compares the server copy with [baseUpdatedAt]; no base means the user chose to overwrite. */
    private suspend fun serverState(
        serverId: String,
        baseUpdatedAt: Long?,
    ): ServerState {
        if (baseUpdatedAt == null) return ServerState.Unchanged
        return when (val remote = remoteDataSource.getTransaction(serverId)) {
            is Result.Success ->
                if (parseIsoInstant(remote.data.data.attributes.updatedAt) > baseUpdatedAt) {
                    ServerState.Changed
                } else {
                    ServerState.Unchanged
                }
            is Result.Error -> if (remote.error.isNotFound()) ServerState.Missing else ServerState.Unknown(remote.error)
        }
    }

    private suspend fun findRemoteByExternalId(localId: String): Result<TransactionReadDto?> =
        when (val search = remoteDataSource.findTransactionsByExternalId(localId)) {
            is Result.Success ->
                Result.Success(
                    search.data.data.firstOrNull { group ->
                        group.attributes.transactions.any { it.externalId == localId }
                    },
                )
            is Result.Error -> search
        }

    private suspend fun linkCreated(
        entry: SyncEntry,
        remoteGroup: TransactionReadDto,
    ): Result<Unit> {
        val remoteSplits = remoteGroup.attributes.transactions
        val linked =
            database.withTransaction {
                val existing = transactionDao.getTransactionWithSplits(entry.localId) ?: return@withTransaction false
                transactionDao.linkGroup(entry.localId, remoteGroup.id, parseIsoInstant(remoteGroup.attributes.updatedAt))
                for ((index, split) in existing.splits.withIndex()) {
                    val status = if (split.syncStatus == SyncStatus.PENDING_CREATE) SyncStatus.SYNCED else split.syncStatus
                    transactionDao.updateSplitSyncStatus(
                        localSplitId = split.localSplitId,
                        serverSplitId = remoteSplits.getOrNull(index)?.transactionJournalId,
                        status = status,
                    )
                }
                syncQueueDao.deleteById(entry.queueId)
                true
            }
        if (linked) return Result.Success(Unit)

        // Deleted locally while the create was in flight: remove the server copy too.
        return when (val result = remoteDataSource.deleteTransaction(remoteGroup.id)) {
            is Result.Error -> if (result.error.isNotFound()) complete(entry) else result
            is Result.Success -> complete(entry)
        }
    }

    private suspend fun markSynced(
        transaction: TransactionWithSplits,
        from: SyncStatus,
    ) {
        transaction.splits
            .filter { it.syncStatus == from }
            .forEach { transactionDao.updateSplitSyncStatus(it.localSplitId, it.serverSplitId, SyncStatus.SYNCED) }
    }

    /** Applies the local side of a finished operation and removes its queue row atomically. */
    private suspend fun complete(
        entry: SyncEntry,
        localChanges: suspend () -> Unit = {},
    ): Result<Unit> {
        database.withTransaction {
            localChanges()
            syncQueueDao.deleteById(entry.queueId)
        }
        return Result.Success(Unit)
    }

    private fun AppError.isNotFound(): Boolean = this is AppError.ApiError && code == 404

    private fun AppError.isUncertainDelivery(): Boolean =
        this == AppError.Network.TIMEOUT ||
            this == AppError.Network.SERVER_ERROR ||
            this == AppError.Network.SERIALIZATION ||
            this == AppError.Network.UNKNOWN
}
