package app.lucent.firefly.data.sync.impl.worker

import android.content.Context
import androidx.work.WorkerParameters
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.dao.SyncMetadataDao
import app.lucent.firefly.data.accounts.api.repository.AccountRepository
import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.model.AuthUser
import app.lucent.firefly.data.auth.api.model.ServerConfig
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.sync.impl.processor.DrainResult
import app.lucent.firefly.data.sync.impl.processor.SyncQueueProcessor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import androidx.work.ListenableWorker.Result as WorkResult

class SyncQueueWorkerTest {
    private val processor: SyncQueueProcessor = mockk()
    private val authRepository: AuthRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val syncMetadataDao: SyncMetadataDao = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val activeSessionFlow = MutableStateFlow<AuthSession?>(null)
    private lateinit var worker: SyncQueueWorker

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
        worker =
            SyncQueueWorker(
                appContext = mockk<Context>(relaxed = true),
                workerParams = mockk<WorkerParameters>(relaxed = true),
                processor = processor,
                authRepository = authRepository,
                accountRepository = accountRepository,
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
    fun refreshesAccountsAfterPushingChanges() =
        runTest(testDispatcher) {
            activeSessionFlow.value = session
            coEvery { processor.drain() } returns DrainResult(pushedAny = true, shouldRetry = false)

            assertEquals(WorkResult.success(), worker.doWork())
            coVerify { accountRepository.refreshAccounts() }
            coVerify { syncMetadataDao.markSynced(any(), deep = false) }
        }

    @Test
    fun authenticationFailureDoesNotRetryOrRecordASuccessfulSync() =
        runTest(testDispatcher) {
            activeSessionFlow.value = session
            coEvery { processor.drain() } returns
                DrainResult(pushedAny = false, shouldRetry = false, authenticationFailed = true)

            assertEquals(WorkResult.success(), worker.doWork())
            coVerify(exactly = 0) { syncMetadataDao.markSynced(any(), any()) }
        }

    @Test
    fun retriesWhenProcessorAsksForRetry() =
        runTest(testDispatcher) {
            activeSessionFlow.value = session
            coEvery { processor.drain() } returns DrainResult(pushedAny = false, shouldRetry = true)

            assertEquals(WorkResult.retry(), worker.doWork())
            coVerify(exactly = 0) { accountRepository.refreshAccounts() }
        }
}
