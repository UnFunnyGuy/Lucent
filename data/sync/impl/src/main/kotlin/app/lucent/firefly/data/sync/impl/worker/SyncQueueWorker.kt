package app.lucent.firefly.data.sync.impl.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import app.lucent.firefly.core.common.network.IoDispatcher
import app.lucent.firefly.core.database.dao.SyncMetadataDao
import app.lucent.firefly.data.accounts.api.repository.AccountRepository
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.sync.impl.processor.SyncQueueProcessor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinWorker
import androidx.work.ListenableWorker.Result as WorkResult

@KoinWorker
class SyncQueueWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val processor: SyncQueueProcessor,
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
    private val syncMetadataDao: SyncMetadataDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): WorkResult = withContext(ioDispatcher) {
        authRepository.activeSession.firstOrNull() ?: return@withContext WorkResult.success()

        val result = processor.drain()
        if (result.pushedAny) {
            // Server balances only change once pushed mutations land; refresh so pending deltas
            // are not dropped against a stale balance.
            accountRepository.refreshAccounts()
        }
        if (!result.shouldRetry && !result.authenticationFailed) {
            syncMetadataDao.markSynced(at = System.currentTimeMillis(), deep = false)
        }
        if (result.shouldRetry) WorkResult.retry() else WorkResult.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Sync", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Syncing transactions")
            .setOngoing(true)
            .setSilent(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "lucent_sync"
        const val NOTIFICATION_ID = 4201
    }
}
