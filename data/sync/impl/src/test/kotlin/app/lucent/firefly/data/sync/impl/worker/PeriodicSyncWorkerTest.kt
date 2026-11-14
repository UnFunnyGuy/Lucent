package app.lucent.firefly.data.sync.impl.worker

import android.content.Context
import androidx.work.WorkerParameters
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.dao.SyncMetadataDao
import app.lucent.firefly.core.database.entity.SyncMetadataEntity
import app.lucent.firefly.data.accounts.api.repository.AccountRepository
import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.model.AuthUser
import app.lucent.firefly.data.auth.api.model.ServerConfig
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.sync.impl.processor.DrainResult
import app.lucent.firefly.data.sync.impl.processor.SyncQueueProcessor
import app.lucent.firefly.data.transactions.api.repository.TransactionReconciler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import androidx.work.ListenableWorker.Result as WorkResult

class PeriodicSyncWorkerTest {
    private val accountRepository: AccountRepository = mockk()
    private val transactionReconciler: TransactionReconciler = mockk()
    private val authRepository: AuthRepository = mockk()
    private val processor: SyncQueueProcessor = mockk()
    private val syncMetadataDao: SyncMetadataDao = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val activeSessionFlow = MutableStateFlow<AuthSession?>(null)
    private lateinit var worker: PeriodicSyncWorker

    private val session =
        AuthSession(
            serverConfig = ServerConfig(baseUrl = "https://demo.firefly.org"),
            user = AuthUser(id = "user-1", email = "test@example.com", role = "owner"),
            token = "pat",
        )

    @Before
    fun setup() {
        every { authRepository.activeSession } returns activeSessionFlow
        coEvery { accountRepository.refreshAccounts() } returns Result.Success(emptyList())
        coEvery { transactionReconciler.syncRecentTransactions() } returns Result.Success(Unit)
        coEvery { transactionReconciler.reconcileTransactions(any()) } returns Result.Success(Unit)
        coEvery { processor.drain() } returns DrainResult(pushedAny = false, shouldRetry = false)
        worker =
            PeriodicSyncWorker(
                appContext = mockk<Context>(relaxed = true),
                workerParams = mockk<WorkerParameters>(relaxed = true),
                processor = processor,
                accountRepository = accountRepository,
                transactionReconciler = transactionReconciler,
                authRepository = authRepository,
                syncMetadataDao = syncMetadataDao,
                ioDispatcher = testDispatcher,
            )
    }

    @Test
    fun succeedsWithoutWorkWhenSignedOut() =
        runTest(testDispatcher) {
            assertEquals(WorkResult.success(), worker.doWork())
            coVerify(exactly = 0) { processor.drain() }
        }

    @Test
    fun pushesThenPullsTheRecentWindow() =
        runTest(testDispatcher) {
            activeSessionFlow.value = session
            coEvery { syncMetadataDao.get() } returns
                SyncMetadataEntity(lastDeepSyncAt = System.currentTimeMillis())

            assertEquals(WorkResult.success(), worker.doWork())

            coVerifyOrder {
                processor.drain()
                accountRepository.refreshAccounts()
                transactionReconciler.syncRecentTransactions()
            }
            coVerify(exactly = 0) { transactionReconciler.reconcileTransactions(any()) }
            coVerify { syncMetadataDao.markSynced(any(), deep = false) }
        }

    @Test
    fun runsDeepReconcileWhenItIsDue() =
        runTest(testDispatcher) {
            activeSessionFlow.value = session
            coEvery { syncMetadataDao.get() } returns null

            worker.doWork()

            coVerify { transactionReconciler.reconcileTransactions(12) }
            coVerify { syncMetadataDao.markSynced(any(), deep = true) }
        }

    @Test
    fun retriesWhenPullFails() =
        runTest(testDispatcher) {
            activeSessionFlow.value = session
            coEvery { syncMetadataDao.get() } returns null
            coEvery { transactionReconciler.reconcileTransactions(any()) } returns Result.Error(AppError.Network.TIMEOUT)

            assertEquals(WorkResult.retry(), worker.doWork())
            coVerify(exactly = 0) { syncMetadataDao.markSynced(any(), any()) }
        }

    @Test
    fun stopsWithoutPullingWhenTheTokenIsRejected() =
        runTest(testDispatcher) {
            activeSessionFlow.value = session
            coEvery { processor.drain() } returns
                DrainResult(pushedAny = false, shouldRetry = false, authenticationFailed = true)

            assertEquals(WorkResult.success(), worker.doWork())
            coVerify(exactly = 0) { accountRepository.refreshAccounts() }
        }
}
