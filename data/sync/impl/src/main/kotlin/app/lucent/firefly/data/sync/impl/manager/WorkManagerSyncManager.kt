package app.lucent.firefly.data.sync.impl.manager

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.lucent.firefly.core.common.network.ApplicationScope
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.dao.SyncMetadataDao
import app.lucent.firefly.core.database.dao.SyncQueueDao
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.sync.api.handler.SyncEntityHandler
import app.lucent.firefly.data.sync.api.manager.SyncManager
import app.lucent.firefly.data.sync.api.model.FailedChange
import app.lucent.firefly.data.sync.api.model.SyncState
import app.lucent.firefly.data.sync.impl.processor.SyncStatusStore
import app.lucent.firefly.data.sync.impl.processor.toEntry
import app.lucent.firefly.data.sync.impl.processor.toOperation
import app.lucent.firefly.data.sync.impl.processor.toTargetType
import app.lucent.firefly.data.sync.impl.worker.PeriodicSyncWorker
import app.lucent.firefly.data.sync.impl.worker.SyncQueueWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Singleton
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class WorkManagerSyncManager(
    private val context: Context,
    @param:ApplicationScope private val scope: CoroutineScope,
    private val authRepository: AuthRepository,
    private val syncQueueDao: SyncQueueDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val statusStore: SyncStatusStore,
    private val handlers: List<SyncEntityHandler>,
) : SyncManager {
    private val workManager by lazy { WorkManager.getInstance(context) }

    private val isRunning: Flow<Boolean> =
        combine(
            workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME_EXPEDITED_SYNC),
            workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME_PERIODIC_SYNC),
        ) { queueWork, periodicWork ->
            (queueWork + periodicWork).any { it.state == WorkInfo.State.RUNNING }
        }

    override val syncState: StateFlow<SyncState> =
        authRepository.activeSession
            .map { it != null }
            .distinctUntilChanged()
            .flatMapLatest { signedIn ->
                if (!signedIn) {
                    flowOf(SyncState())
                } else {
                    combine(
                        syncQueueDao.countPending(),
                        syncQueueDao.observeFailed(),
                        syncMetadataDao.observe(),
                        isRunning,
                        statusStore.requiresReauthentication,
                    ) { pending, failed, metadata, running, reauth ->
                        SyncState(
                            isSyncing = running,
                            pendingCount = pending,
                            failedChanges =
                                failed.map { entry ->
                                    FailedChange(
                                        id = entry.id,
                                        target = entry.entityType.toTargetType(),
                                        operation = entry.action.toOperation(),
                                        localId = entry.localEntityId,
                                        message = entry.lastError.orEmpty(),
                                        isConflict = entry.isConflict,
                                    )
                                },
                            lastSuccessfulSyncAt = metadata?.lastSuccessfulSyncAt,
                            requiresReauthentication = reauth,
                        )
                    }
                }
            }.stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SyncState(),
            )

    override fun requestSync(expedited: Boolean) {
        val builder =
            OneTimeWorkRequestBuilder<SyncQueueWorker>()
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)

        if (expedited) {
            builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }

        workManager.enqueueUniqueWork(
            WORK_NAME_EXPEDITED_SYNC,
            // KEEP: a running worker re-reads the queue until it is empty, so new entries are picked up
            // without waiting behind a retry backoff; periodic sync covers anything left over.
            ExistingWorkPolicy.KEEP,
            builder.build(),
        )
    }

    override fun schedulePeriodicSync(intervalMinutes: Long) {
        val periodicRequest =
            PeriodicWorkRequestBuilder<PeriodicSyncWorker>(intervalMinutes, TimeUnit.MINUTES)
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
                .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest,
        )
    }

    override fun cancelSync() {
        workManager.cancelUniqueWork(WORK_NAME_EXPEDITED_SYNC)
        workManager.cancelUniqueWork(WORK_NAME_PERIODIC_SYNC)
    }

    override suspend fun retryFailedChange(
        id: Long,
        overwriteServer: Boolean,
    ) {
        if (overwriteServer) syncQueueDao.resetForOverwrite(id) else syncQueueDao.resetForRetry(id)
        requestSync(expedited = true)
    }

    override suspend fun discardFailedChange(id: Long): Result<Unit> {
        val entry = syncQueueDao.getById(id) ?: return Result.Success(Unit)
        val handler =
            handlers.firstOrNull { it.targetType == entry.entityType.toTargetType() }
                ?: return Result.Success(Unit).also { syncQueueDao.deleteById(id) }
        return handler.discard(entry.toEntry())
    }

    private fun networkConstraints() =
        Constraints
            .Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    companion object {
        const val WORK_NAME_EXPEDITED_SYNC = "lucent_sync_queue"
        const val WORK_NAME_PERIODIC_SYNC = "lucent_periodic_sync"
        private const val BACKOFF_DELAY_SECONDS = 30L
    }
}
