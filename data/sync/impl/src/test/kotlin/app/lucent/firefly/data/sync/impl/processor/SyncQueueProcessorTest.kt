package app.lucent.firefly.data.sync.impl.processor

import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.dao.SyncQueueDao
import app.lucent.firefly.core.database.entity.SyncQueueEntity
import app.lucent.firefly.core.database.model.SyncAction
import app.lucent.firefly.core.database.model.SyncEntityType
import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.model.AuthUser
import app.lucent.firefly.data.auth.api.model.ServerConfig
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.sync.api.handler.SyncEntityHandler
import app.lucent.firefly.data.sync.api.handler.SyncEntry
import app.lucent.firefly.data.sync.api.handler.SyncOperation
import app.lucent.firefly.data.sync.api.handler.SyncTargetType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncQueueProcessorTest {
    private val syncQueueDao: SyncQueueDao = mockk(relaxed = true)
    private val handler: SyncEntityHandler = mockk()
    private val authRepository: AuthRepository = mockk()
    private val statusStore = SyncStatusStore()
    private val session =
        MutableStateFlow<AuthSession?>(
            AuthSession(
                serverConfig = ServerConfig("https://demo.firefly.org"),
                user = AuthUser("2", "u@example.com", "owner"),
                token = "pat",
            ),
        )
    private lateinit var processor: SyncQueueProcessor

    @Before
    fun setup() {
        every { handler.targetType } returns SyncTargetType.TRANSACTION
        every { authRepository.activeSession } returns session
        coEvery { syncQueueDao.getFailed() } returns emptyList()
        processor = SyncQueueProcessor(syncQueueDao, authRepository, statusStore, listOf(handler))
    }

    @Test
    fun emptyQueueReportsNothingPushed() =
        runTest {
            coEvery { syncQueueDao.getPendingQueue() } returns emptyList()

            assertEquals(DrainResult(pushedAny = false, shouldRetry = false), processor.drain())
        }

    @Test
    fun recordsAttemptBeforeCallingHandlerAndPassesTheEntry() =
        runTest {
            val item = item(id = 7, localId = "tx-1").copy(attemptCount = 2, baseUpdatedAt = 42L)
            coEvery { syncQueueDao.getPendingQueue() } returnsMany listOf(listOf(item), emptyList())
            coEvery { handler.processSync(any()) } returns Result.Success(Unit)

            assertEquals(DrainResult(pushedAny = true, shouldRetry = false), processor.drain())
            coVerifyOrder {
                syncQueueDao.markAttemptStarted(7)
                handler.processSync(
                    SyncEntry(
                        queueId = 7,
                        operation = SyncOperation.CREATE,
                        localId = "tx-1",
                        serverId = null,
                        baseUpdatedAt = 42L,
                        previousAttempts = 2,
                    ),
                )
            }
            coVerify(exactly = 0) { syncQueueDao.deleteById(any()) }
        }

    @Test
    fun rejectedChangeIsMarkedFailedAndBlocksOnlyItsEntity() =
        runTest {
            val create = item(id = 1, localId = "tx-1")
            val other = item(id = 2, localId = "tx-2")
            coEvery { syncQueueDao.getPendingQueue() } returns listOf(create, other)
            coEvery { handler.processSync(match { it.localId == "tx-1" }) } returns
                Result.Error(AppError.ApiError("The amount must be more than zero.", code = 422))
            coEvery { handler.processSync(match { it.localId == "tx-2" }) } returns Result.Success(Unit)

            val result = processor.drain()

            assertEquals(DrainResult(pushedAny = true, shouldRetry = false), result)
            coVerify { syncQueueDao.markFailed(1, "The amount must be more than zero.", false) }
        }

    @Test
    fun conflictIsMarkedFailedAsConflict() =
        runTest {
            coEvery { syncQueueDao.getPendingQueue() } returns listOf(item(id = 1, localId = "tx-1"))
            coEvery { handler.processSync(any()) } returns Result.Error(AppError.Conflict("Changed on the server"))

            processor.drain()

            coVerify { syncQueueDao.markFailed(1, "Changed on the server", true) }
        }

    @Test
    fun transientErrorStopsTheRunAndAsksForRetry() =
        runTest {
            coEvery { syncQueueDao.getPendingQueue() } returns listOf(item(id = 1, localId = "tx-1"), item(id = 2, localId = "tx-2"))
            coEvery { handler.processSync(any()) } returns Result.Error(AppError.Network.TIMEOUT)

            assertEquals(DrainResult(pushedAny = false, shouldRetry = true), processor.drain())
            coVerify(exactly = 1) { handler.processSync(any()) }
            coVerify { syncQueueDao.recordError(1, "TIMEOUT") }
            coVerify(exactly = 0) { syncQueueDao.markFailed(any(), any(), any()) }
        }

    @Test
    fun transientErrorAfterTooManyAttemptsIsMarkedFailed() =
        runTest {
            coEvery { syncQueueDao.getPendingQueue() } returns listOf(item(id = 1, localId = "tx-1").copy(attemptCount = 9))
            coEvery { handler.processSync(any()) } returns Result.Error(AppError.Network.SERVER_ERROR)

            assertFalse(processor.drain().shouldRetry)
            coVerify { syncQueueDao.markFailed(1, "SERVER_ERROR", false) }
        }

    @Test
    fun rejectedTokenPausesSyncAndFlagsReauthentication() =
        runTest {
            coEvery { syncQueueDao.getPendingQueue() } returns listOf(item(id = 1, localId = "tx-1"), item(id = 2, localId = "tx-2"))
            coEvery { handler.processSync(any()) } returns Result.Error(AppError.ApiKeyError)

            val result = processor.drain()

            assertTrue(result.authenticationFailed)
            assertFalse(result.shouldRetry)
            assertTrue(statusStore.requiresReauthentication.value)
            coVerify(exactly = 1) { handler.processSync(any()) }
            coVerify(exactly = 0) { syncQueueDao.markFailed(any(), any(), any()) }
        }

    @Test
    fun entitiesWithAFailedChangeAreSkipped() =
        runTest {
            coEvery { syncQueueDao.getFailed() } returns listOf(item(id = 1, localId = "tx-1").copy(action = SyncAction.UPDATE))
            coEvery { syncQueueDao.getPendingQueue() } returns listOf(item(id = 2, localId = "tx-1", action = SyncAction.DELETE))

            processor.drain()

            coVerify(exactly = 0) { handler.processSync(any()) }
        }

    @Test
    fun stopsWhenSignedOut() =
        runTest {
            coEvery { syncQueueDao.getPendingQueue() } returns listOf(item(id = 1, localId = "tx-1"))
            session.value = null

            processor.drain()

            coVerify(exactly = 0) { handler.processSync(any()) }
        }

    @Test
    fun picksUpItemsEnqueuedDuringRun() =
        runTest {
            coEvery { syncQueueDao.getPendingQueue() } returnsMany
                listOf(
                    listOf(item(id = 1, localId = "tx-1")),
                    listOf(item(id = 2, localId = "tx-2")),
                    emptyList(),
                )
            coEvery { handler.processSync(any()) } returns Result.Success(Unit)

            processor.drain()

            coVerify { handler.processSync(match { it.queueId == 1L }) }
            coVerify { handler.processSync(match { it.queueId == 2L }) }
        }

    private fun item(
        id: Long,
        localId: String,
        action: SyncAction = SyncAction.CREATE,
    ) = SyncQueueEntity(
        id = id,
        entityType = SyncEntityType.TRANSACTION,
        action = action,
        localEntityId = localId,
        serverEntityId = null,
    )
}
