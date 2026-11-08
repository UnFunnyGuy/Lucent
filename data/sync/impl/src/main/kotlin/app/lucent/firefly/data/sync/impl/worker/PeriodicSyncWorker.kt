package app.lucent.firefly.data.sync.impl.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.lucent.firefly.core.common.network.IoDispatcher
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.dao.SyncMetadataDao
import app.lucent.firefly.data.accounts.api.repository.AccountRepository
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.sync.impl.processor.SyncQueueProcessor
import app.lucent.firefly.data.transactions.api.repository.TransactionReconciler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinWorker
import java.util.concurrent.TimeUnit
import androidx.work.ListenableWorker.Result as WorkResult

/**
 * Full sync: push local changes, then pull. Firefly has no change feed, so the pull reconciles the
 * recent date window every run and a longer window once a week to catch backdated edits.
 */
@KoinWorker
class PeriodicSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val processor: SyncQueueProcessor,
    private val accountRepository: AccountRepository,
    private val transactionReconciler: TransactionReconciler,
    private val authRepository: AuthRepository,
    private val syncMetadataDao: SyncMetadataDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): WorkResult = withContext(ioDispatcher) {
        authRepository.activeSession.firstOrNull() ?: return@withContext WorkResult.success()

        // Push first so the pulled data and balances already include local changes.
        val push = processor.drain()
        if (push.authenticationFailed) return@withContext WorkResult.success()

        val now = System.currentTimeMillis()
        val lastDeep = syncMetadataDao.get()?.lastDeepSyncAt
        val deep = lastDeep == null || now - lastDeep >= DEEP_RECONCILE_INTERVAL_MILLIS

        val pulls = listOf(
            accountRepository.refreshAccounts(),
            if (deep) {
                transactionReconciler.reconcileTransactions(monthsBack = DEEP_RECONCILE_MONTHS)
            } else {
                transactionReconciler.syncRecentTransactions()
            },
        )
        val pullFailed = pulls.any { it is Result.Error }

        if (!push.shouldRetry && !pullFailed) {
            syncMetadataDao.markSynced(at = now, deep = deep)
        }
        if (push.shouldRetry || pullFailed) WorkResult.retry() else WorkResult.success()
    }

    private companion object {
        val DEEP_RECONCILE_INTERVAL_MILLIS = TimeUnit.DAYS.toMillis(7)
        const val DEEP_RECONCILE_MONTHS = 12
    }
}
