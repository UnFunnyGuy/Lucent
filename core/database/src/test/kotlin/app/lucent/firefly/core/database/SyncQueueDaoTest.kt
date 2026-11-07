package app.lucent.firefly.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import app.lucent.firefly.core.database.dao.SyncQueueDao
import app.lucent.firefly.core.database.entity.SyncQueueEntity
import app.lucent.firefly.core.database.model.EnqueueResult
import app.lucent.firefly.core.database.model.SyncAction
import app.lucent.firefly.core.database.model.SyncEntityType
import app.lucent.firefly.core.database.model.SyncQueueStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncQueueDaoTest {

    private lateinit var database: LucentDatabase
    private lateinit var syncQueueDao: SyncQueueDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LucentDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        syncQueueDao = database.syncQueueDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun getPendingQueueReturnsEnqueueOrderEvenWithIdenticalTimestamps() = runTest {
        syncQueueDao.enqueue(createQueueEntry(localId = "uuid-1", createdAt = 1000L))
        syncQueueDao.enqueue(createQueueEntry(localId = "uuid-2", createdAt = 1000L))
        syncQueueDao.enqueue(createQueueEntry(localId = "uuid-3", createdAt = 500L))

        val queue = syncQueueDao.getPendingQueue()

        assertEquals(listOf("uuid-1", "uuid-2", "uuid-3"), queue.map { it.localEntityId })
    }

    @Test
    fun attemptIsCountedBeforeTheErrorIsRecorded() = runTest {
        val id = syncQueueDao.enqueue(createQueueEntry(localId = "uuid-1"))

        syncQueueDao.markAttemptStarted(id)
        syncQueueDao.recordError(id, "TIMEOUT")

        val entry = syncQueueDao.getPendingQueue().single()
        assertEquals(1, entry.attemptCount)
        assertEquals("TIMEOUT", entry.lastError)
    }

    @Test
    fun updateAfterCreateIsMergedIntoTheCreate() = runTest {
        syncQueueDao.enqueueChange(SyncEntityType.TRANSACTION, "tx", null, SyncAction.CREATE, null)

        val result = syncQueueDao.enqueueChange(SyncEntityType.TRANSACTION, "tx", null, SyncAction.UPDATE, null)

        assertEquals(EnqueueResult.MERGED, result)
        assertEquals(listOf(SyncAction.CREATE), syncQueueDao.getPendingQueue().map { it.action })
    }

    @Test
    fun deleteAfterCreateCancelsBoth() = runTest {
        syncQueueDao.enqueueChange(SyncEntityType.TRANSACTION, "tx", null, SyncAction.CREATE, null)

        val result = syncQueueDao.enqueueChange(SyncEntityType.TRANSACTION, "tx", null, SyncAction.DELETE, null)

        assertEquals(EnqueueResult.CANCELLED_CREATE, result)
        assertTrue(syncQueueDao.getPendingQueue().isEmpty())
    }

    @Test
    fun deleteAfterUpdateReplacesItAndKeepsTheFirstBaseVersion() = runTest {
        syncQueueDao.enqueueChange(SyncEntityType.TRANSACTION, "tx", "srv", SyncAction.UPDATE, 10L)
        syncQueueDao.enqueueChange(SyncEntityType.TRANSACTION, "tx", "srv", SyncAction.UPDATE, 20L)

        syncQueueDao.enqueueChange(SyncEntityType.TRANSACTION, "tx", "srv", SyncAction.DELETE, 30L)

        val queued = syncQueueDao.getPendingQueue().single()
        assertEquals(SyncAction.DELETE, queued.action)
        assertEquals(10L, queued.baseUpdatedAt)
    }

    @Test
    fun editingAFailedChangePutsItBackInTheQueue() = runTest {
        syncQueueDao.enqueueChange(SyncEntityType.TRANSACTION, "tx", "srv", SyncAction.UPDATE, 10L)
        val id = syncQueueDao.getPendingQueue().single().id
        syncQueueDao.markFailed(id, "Rejected", isConflict = false)
        assertTrue(syncQueueDao.getPendingQueue().isEmpty())

        syncQueueDao.enqueueChange(SyncEntityType.TRANSACTION, "tx", "srv", SyncAction.UPDATE, 10L)

        val queued = syncQueueDao.getPendingQueue().single()
        assertEquals(SyncQueueStatus.PENDING, queued.status)
        assertEquals(null, queued.lastError)
    }

    @Test
    fun countPendingEmitsReactiveUpdates() = runTest {
        syncQueueDao.countPending().test {
            assertEquals(0, awaitItem())

            val id = syncQueueDao.enqueue(createQueueEntry(localId = "uuid-1"))
            assertEquals(1, awaitItem())

            syncQueueDao.deleteById(id)
            assertEquals(0, awaitItem())
        }
    }

    private fun createQueueEntry(
        localId: String,
        createdAt: Long = System.currentTimeMillis()
    ) = SyncQueueEntity(
        id = 0,
        entityType = SyncEntityType.TRANSACTION,
        action = SyncAction.CREATE,
        localEntityId = localId,
        serverEntityId = null,
        attemptCount = 0,
        lastError = null,
        createdAt = createdAt
    )
}
