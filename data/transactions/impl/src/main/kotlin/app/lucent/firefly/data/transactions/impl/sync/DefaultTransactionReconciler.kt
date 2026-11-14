package app.lucent.firefly.data.transactions.impl.sync

import androidx.room.withTransaction
import app.lucent.firefly.core.common.network.IoDispatcher
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.LucentDatabase
import app.lucent.firefly.core.database.dao.SyncQueueDao
import app.lucent.firefly.core.database.dao.TransactionDao
import app.lucent.firefly.core.database.model.SyncEntityType
import app.lucent.firefly.core.database.model.SyncStatus
import app.lucent.firefly.core.network.model.hasNextPage
import app.lucent.firefly.data.transactions.api.repository.TransactionReconciler
import app.lucent.firefly.data.transactions.impl.datasource.TransactionRemoteDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Singleton
import java.time.Clock
import java.time.LocalDate

@Singleton
class DefaultTransactionReconciler(
    private val database: LucentDatabase,
    private val transactionDao: TransactionDao,
    private val syncQueueDao: SyncQueueDao,
    private val remoteDataSource: TransactionRemoteDataSource,
    private val remoteWriter: RemoteTransactionWriter,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TransactionReconciler {
    override suspend fun syncRecentTransactions(): Result<Unit> {
        val today = LocalDate.now(clock)
        return reconcileWindow(start = today.minusMonths(1).withDayOfMonth(1), end = today)
    }

    override suspend fun reconcileTransactions(monthsBack: Int): Result<Unit> {
        val today = LocalDate.now(clock)
        return reconcileWindow(start = today.minusMonths(monthsBack.toLong()).withDayOfMonth(1), end = today)
    }

    /**
     * Makes the local copy of [start]..[end] match the server: upserts every server transaction in
     * the window and removes synced local ones the server no longer has. Rows with pending local
     * work are never touched.
     *
     * Each page is written in its own short database transaction so a long reconcile does not hold
     * the write lock against new local edits; upserts are idempotent, so stopping part-way is safe.
     */
    private suspend fun reconcileWindow(
        start: LocalDate,
        end: LocalDate,
    ): Result<Unit> =
        withContext(ioDispatcher) {
            val remoteIds = mutableSetOf<String>()
            var page = 1
            while (true) {
                when (val result = remoteDataSource.getTransactions(start = start, end = end, page = page)) {
                    is Result.Success -> {
                        val groups = result.data.data
                        database.withTransaction { groups.forEach { remoteWriter.upsert(it) } }
                        groups.mapTo(remoteIds) { it.id }
                        if (!result.data.meta.hasNextPage) break
                        page++
                    }
                    is Result.Error -> return@withContext result
                }
            }

            // The server filters by its own time zone; only prune well inside the window so a
            // transaction near the edges is never deleted and re-added on alternate runs.
            val zone = clock.zone
            val pruneFrom =
                start
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
            val pruneUntil = end.atStartOfDay(zone).toInstant().toEpochMilli() - 1
            if (pruneFrom <= pruneUntil) {
                database.withTransaction {
                    transactionDao
                        .getGroupsInDateRange(pruneFrom, pruneUntil)
                        .filter { local ->
                            val serverId = local.group.serverId
                            serverId != null &&
                                serverId !in remoteIds &&
                                local.splits.all { it.syncStatus == SyncStatus.SYNCED } &&
                                syncQueueDao.getForEntity(SyncEntityType.TRANSACTION, local.group.localId).isEmpty()
                        }.forEach { transactionDao.deleteGroup(it.group.localId) }
                }
            }
            Result.Success(Unit)
        }
}
