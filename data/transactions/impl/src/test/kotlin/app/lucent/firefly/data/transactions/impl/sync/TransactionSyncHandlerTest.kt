package app.lucent.firefly.data.transactions.impl.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.LucentDatabase
import app.lucent.firefly.core.database.dao.SyncQueueDao
import app.lucent.firefly.core.database.dao.TransactionDao
import app.lucent.firefly.core.database.entity.SyncQueueEntity
import app.lucent.firefly.core.database.entity.TransactionGroupEntity
import app.lucent.firefly.core.database.entity.TransactionSplitEntity
import app.lucent.firefly.core.database.model.SyncAction
import app.lucent.firefly.core.database.model.SyncEntityType
import app.lucent.firefly.core.database.model.SyncStatus
import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.model.AuthUser
import app.lucent.firefly.data.auth.api.model.ServerConfig
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.sync.api.handler.SyncEntry
import app.lucent.firefly.data.sync.api.handler.SyncOperation
import app.lucent.firefly.data.transactions.impl.datasource.TransactionRemoteDataSource
import app.lucent.firefly.data.transactions.impl.dto.TransactionArrayDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionAttributesDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionReadDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionSingleDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionSplitDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionStoreDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionUpdateDto
import app.lucent.firefly.data.transactions.impl.mapper.parseIsoInstant
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransactionSyncHandlerTest {

    private lateinit var database: LucentDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var syncQueueDao: SyncQueueDao

    private val remoteDataSource: TransactionRemoteDataSource = mockk()
    private val authRepository: AuthRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private val activeSessionFlow = MutableStateFlow<AuthSession?>(null)
    private lateinit var handler: TransactionSyncHandler

    private val testSession = AuthSession(
        serverConfig = ServerConfig(baseUrl = "https://demo.firefly.org"),
        user = AuthUser(id = "user-1", email = "test@example.com", role = "owner"),
        token = "secret-pat",
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LucentDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = database.transactionDao()
        syncQueueDao = database.syncQueueDao()
        every { authRepository.activeSession } returns activeSessionFlow
        activeSessionFlow.value = testSession

        handler = TransactionSyncHandler(
            database = database,
            transactionDao = transactionDao,
            syncQueueDao = syncQueueDao,
            remoteDataSource = remoteDataSource,
            remoteWriter = RemoteTransactionWriter(transactionDao, syncQueueDao),
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun createPostsLinksSplitsAndRemovesQueueRowTogether() = runTest(testDispatcher) {
        insertPendingCreate("g-1", "s-1")
        val entry = enqueue("g-1", SyncAction.CREATE)
        coEvery { remoteDataSource.postTransaction(any()) } returns
            Result.Success(TransactionSingleDto(data = remoteGroup("srv-1", "j-1", externalId = "g-1")))

        val result = handler.processSync(entry)

        assertTrue(result is Result.Success)
        val stored = transactionDao.getTransactionWithSplits("g-1")!!
        assertEquals("srv-1", stored.group.serverId)
        assertEquals("j-1", stored.splits.single().serverSplitId)
        assertEquals(SyncStatus.SYNCED, stored.splits.single().syncStatus)
        assertTrue(syncQueueDao.getPendingQueue().isEmpty())
    }

    @Test
    fun createAlreadyLinkedByAnEarlierRunCompletesWithoutNetwork() = runTest(testDispatcher) {
        insertPendingCreate("g-1", "s-1")
        transactionDao.updateGroupServerId("g-1", "srv-1")
        val entry = enqueue("g-1", SyncAction.CREATE)

        val result = handler.processSync(entry)

        assertTrue(result is Result.Success)
        coVerify(exactly = 0) { remoteDataSource.postTransaction(any()) }
        coVerify(exactly = 0) { remoteDataSource.findTransactionsByExternalId(any()) }
        assertEquals(SyncStatus.SYNCED, transactionDao.getSplitByLocalId("s-1")!!.syncStatus)
        assertTrue(syncQueueDao.getPendingQueue().isEmpty())
    }

    @Test
    fun retriedCreateLinksExistingServerCopyInsteadOfPostingAgain() = runTest(testDispatcher) {
        insertPendingCreate("g-1", "s-1")
        val entry = enqueue("g-1", SyncAction.CREATE).copy(previousAttempts = 1)
        coEvery { remoteDataSource.findTransactionsByExternalId("g-1") } returns
            Result.Success(TransactionArrayDto(data = listOf(remoteGroup("srv-1", "j-1", externalId = "g-1"))))

        val result = handler.processSync(entry)

        assertTrue(result is Result.Success)
        coVerify(exactly = 0) { remoteDataSource.postTransaction(any()) }
        assertEquals("srv-1", transactionDao.getTransactionWithSplits("g-1")!!.group.serverId)
        assertTrue(syncQueueDao.getPendingQueue().isEmpty())
    }

    @Test
    fun uncertainPostFailureIsResolvedBySearchingExternalId() = runTest(testDispatcher) {
        insertPendingCreate("g-1", "s-1")
        val entry = enqueue("g-1", SyncAction.CREATE)
        coEvery { remoteDataSource.postTransaction(any()) } returns Result.Error(AppError.Network.TIMEOUT)
        coEvery { remoteDataSource.findTransactionsByExternalId("g-1") } returns
            Result.Success(TransactionArrayDto(data = listOf(remoteGroup("srv-1", "j-1", externalId = "g-1"))))

        val result = handler.processSync(entry)

        assertTrue(result is Result.Success)
        assertEquals("srv-1", transactionDao.getTransactionWithSplits("g-1")!!.group.serverId)
    }

    @Test
    fun failedCreateKeepsQueueRow() = runTest(testDispatcher) {
        insertPendingCreate("g-1", "s-1")
        val entry = enqueue("g-1", SyncAction.CREATE)
        coEvery { remoteDataSource.postTransaction(any()) } returns Result.Error(AppError.Network.TIMEOUT)
        coEvery { remoteDataSource.findTransactionsByExternalId("g-1") } returns
            Result.Success(TransactionArrayDto(data = emptyList()))

        val result = handler.processSync(entry)

        assertEquals(AppError.Network.TIMEOUT, (result as Result.Error).error)
        assertEquals(1, syncQueueDao.getPendingQueue().size)
        assertEquals(SyncStatus.PENDING_CREATE, transactionDao.getSplitByLocalId("s-1")!!.syncStatus)
    }

    @Test
    fun createThatLandsAfterLocalDeleteRemovesServerCopy() = runTest(testDispatcher) {
        insertPendingCreate("g-1", "s-1")
        val entry = enqueue("g-1", SyncAction.CREATE)
        coEvery { remoteDataSource.postTransaction(any()) } coAnswers {
            transactionDao.deleteGroupWithSplits("g-1")
            Result.Success(TransactionSingleDto(data = remoteGroup("srv-1", "j-1", externalId = "g-1")))
        }
        coEvery { remoteDataSource.deleteTransaction("srv-1") } returns Result.Success(Unit)

        val result = handler.processSync(entry)

        assertTrue(result is Result.Success)
        coVerify { remoteDataSource.deleteTransaction("srv-1") }
        assertTrue(syncQueueDao.getPendingQueue().isEmpty())
    }

    @Test
    fun deleteUsesServerIdAssignedAfterEnqueueAndRemovesEverything() = runTest(testDispatcher) {
        insertPendingCreate("g-1", "s-1")
        transactionDao.updateGroupServerId("g-1", "srv-late")
        val entry = enqueue("g-1", SyncAction.DELETE)
        coEvery { remoteDataSource.deleteTransaction("srv-late") } returns Result.Success(Unit)

        val result = handler.processSync(entry)

        assertTrue(result is Result.Success)
        assertNull(transactionDao.getTransactionWithSplits("g-1"))
        assertNull(transactionDao.getSplitByLocalId("s-1"))
        assertTrue(syncQueueDao.getPendingQueue().isEmpty())
    }

    @Test
    fun deleteFailureKeepsLocalRowsAndQueueRow() = runTest(testDispatcher) {
        insertPendingCreate("g-1", "s-1")
        transactionDao.updateGroupServerId("g-1", "srv-1")
        val entry = enqueue("g-1", SyncAction.DELETE)
        coEvery { remoteDataSource.deleteTransaction("srv-1") } returns Result.Error(AppError.Network.NO_INTERNET)

        val result = handler.processSync(entry)

        assertTrue(result is Result.Error)
        assertEquals("srv-1", transactionDao.getTransactionWithSplits("g-1")!!.group.serverId)
        assertEquals(1, syncQueueDao.getPendingQueue().size)
    }

    @Test
    fun createSendsCurrentLocalRowsWithExternalId() = runTest(testDispatcher) {
        insertPendingCreate("g-1", "s-1")
        val entry = enqueue("g-1", SyncAction.CREATE)
        val sent = slot<TransactionStoreDto>()
        coEvery { remoteDataSource.postTransaction(capture(sent)) } returns
            Result.Success(TransactionSingleDto(data = remoteGroup("srv-1", "j-1", externalId = "g-1")))

        handler.processSync(entry)

        val split = sent.captured.transactions.single()
        assertEquals("25.00", split.amount)
        assertEquals("EUR", split.currencyCode)
        assertEquals("g-1", split.externalId)
        assertEquals(parseIsoInstant("2026-11-01T20:00:00+00:00"), transactionDao.getTransactionWithSplits("g-1")!!.group.updatedAt)
    }

    @Test
    fun updateWithUnchangedServerCopyIsSentAndMarkedSynced() = runTest(testDispatcher) {
        insertLinkedGroup("g-1", "s-1", serverUpdatedAt = BASE)
        val entry = enqueue("g-1", SyncAction.UPDATE, baseUpdatedAt = BASE)
        coEvery { remoteDataSource.getTransaction("srv-1") } returns
            Result.Success(TransactionSingleDto(data = remoteGroup("srv-1", "j-1", null, updatedAt = BASE_ISO)))
        val sent = slot<TransactionUpdateDto>()
        coEvery { remoteDataSource.updateTransaction("srv-1", capture(sent)) } returns
            Result.Success(TransactionSingleDto(data = remoteGroup("srv-1", "j-1", null, updatedAt = LATER_ISO)))

        val result = handler.processSync(entry)

        assertTrue(result is Result.Success)
        assertEquals("j-1", sent.captured.transactions.single().transactionJournalId)
        val stored = transactionDao.getTransactionWithSplits("g-1")!!
        assertEquals(SyncStatus.SYNCED, stored.splits.single().syncStatus)
        assertEquals(parseIsoInstant(LATER_ISO), stored.group.updatedAt)
        assertTrue(syncQueueDao.getPendingQueue().isEmpty())
    }

    @Test
    fun updateReportsConflictWhenServerChangedSinceTheBase() = runTest(testDispatcher) {
        insertLinkedGroup("g-1", "s-1", serverUpdatedAt = BASE)
        val entry = enqueue("g-1", SyncAction.UPDATE, baseUpdatedAt = BASE)
        coEvery { remoteDataSource.getTransaction("srv-1") } returns
            Result.Success(TransactionSingleDto(data = remoteGroup("srv-1", "j-1", null, updatedAt = LATER_ISO)))

        val result = handler.processSync(entry)

        assertTrue((result as Result.Error).error is AppError.Conflict)
        coVerify(exactly = 0) { remoteDataSource.updateTransaction(any(), any()) }
        assertEquals(1, syncQueueDao.getPendingQueue().size)
    }

    @Test
    fun updateReportsConflictWhenServerCopyWasDeleted() = runTest(testDispatcher) {
        insertLinkedGroup("g-1", "s-1", serverUpdatedAt = BASE)
        val entry = enqueue("g-1", SyncAction.UPDATE, baseUpdatedAt = BASE)
        coEvery { remoteDataSource.getTransaction("srv-1") } returns Result.Error(AppError.ApiError("Not found", code = 404))

        val result = handler.processSync(entry)

        assertTrue((result as Result.Error).error is AppError.Conflict)
    }

    @Test
    fun updateWithoutBaseOverwritesTheServerWithoutChecking() = runTest(testDispatcher) {
        insertLinkedGroup("g-1", "s-1", serverUpdatedAt = BASE)
        val entry = enqueue("g-1", SyncAction.UPDATE, baseUpdatedAt = null)
        coEvery { remoteDataSource.updateTransaction("srv-1", any()) } returns
            Result.Success(TransactionSingleDto(data = remoteGroup("srv-1", "j-1", null, updatedAt = LATER_ISO)))

        assertTrue(handler.processSync(entry) is Result.Success)
        coVerify(exactly = 0) { remoteDataSource.getTransaction(any()) }
    }

    @Test
    fun deleteReportsConflictWhenServerChangedSinceTheBase() = runTest(testDispatcher) {
        insertLinkedGroup("g-1", "s-1", serverUpdatedAt = BASE)
        val entry = enqueue("g-1", SyncAction.DELETE, baseUpdatedAt = BASE)
        coEvery { remoteDataSource.getTransaction("srv-1") } returns
            Result.Success(TransactionSingleDto(data = remoteGroup("srv-1", "j-1", null, updatedAt = LATER_ISO)))

        val result = handler.processSync(entry)

        assertTrue((result as Result.Error).error is AppError.Conflict)
        coVerify(exactly = 0) { remoteDataSource.deleteTransaction(any()) }
    }

    @Test
    fun discardingAFailedUpdateRestoresTheServerVersion() = runTest(testDispatcher) {
        insertLinkedGroup("g-1", "s-1", serverUpdatedAt = BASE, status = SyncStatus.PENDING_UPDATE)
        val entry = enqueue("g-1", SyncAction.UPDATE, baseUpdatedAt = BASE)
        coEvery { remoteDataSource.getTransaction("srv-1") } returns Result.Success(
            TransactionSingleDto(data = remoteGroup("srv-1", "j-1", null, updatedAt = LATER_ISO, amount = "99.00")),
        )

        assertTrue(handler.discard(entry) is Result.Success)

        val split = transactionDao.getTransactionWithSplits("g-1")!!.splits.single()
        assertEquals("99.00", split.amount)
        assertEquals(SyncStatus.SYNCED, split.syncStatus)
        assertTrue(syncQueueDao.getPendingQueue().isEmpty())
    }

    @Test
    fun discardingAFailedCreateRemovesTheLocalTransaction() = runTest(testDispatcher) {
        insertPendingCreate("g-1", "s-1")
        val entry = enqueue("g-1", SyncAction.CREATE)

        assertTrue(handler.discard(entry) is Result.Success)

        assertNull(transactionDao.getTransactionWithSplits("g-1"))
        coVerify(exactly = 0) { remoteDataSource.findTransactionsByExternalId(any()) }
    }

    @Test
    fun discardingAFailedDeleteRestoresTheTransaction() = runTest(testDispatcher) {
        insertLinkedGroup("g-1", "s-1", serverUpdatedAt = BASE, status = SyncStatus.PENDING_DELETE)
        val entry = enqueue("g-1", SyncAction.DELETE, baseUpdatedAt = BASE)

        assertTrue(handler.discard(entry) is Result.Success)

        assertEquals(SyncStatus.SYNCED, transactionDao.getSplitByLocalId("s-1")!!.syncStatus)
        assertTrue(syncQueueDao.getPendingQueue().isEmpty())
    }

    private suspend fun insertLinkedGroup(
        groupId: String,
        splitId: String,
        serverUpdatedAt: Long,
        status: SyncStatus = SyncStatus.PENDING_UPDATE,
    ) {
        insertPendingCreate(groupId, splitId)
        transactionDao.linkGroup(groupId, "srv-1", serverUpdatedAt)
        transactionDao.updateSplitSyncStatus(splitId, "j-1", status)
    }

    private suspend fun enqueue(localId: String, action: SyncAction, baseUpdatedAt: Long? = null): SyncEntry {
        val id = syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.TRANSACTION,
                action = action,
                localEntityId = localId,
                serverEntityId = null,
                baseUpdatedAt = baseUpdatedAt,
            ),
        )
        return SyncEntry(
            queueId = id,
            operation = when (action) {
                SyncAction.CREATE -> SyncOperation.CREATE
                SyncAction.UPDATE -> SyncOperation.UPDATE
                SyncAction.DELETE -> SyncOperation.DELETE
            },
            localId = localId,
            serverId = null,
            baseUpdatedAt = baseUpdatedAt,
            previousAttempts = 0,
        )
    }

    private suspend fun insertPendingCreate(groupId: String, splitId: String) {
        transactionDao.insertGroup(
            TransactionGroupEntity(localId = groupId, serverId = null, groupTitle = null),
        )
        transactionDao.insertSplits(
            listOf(
                TransactionSplitEntity(
                    localSplitId = splitId,
                    transactionGroupId = groupId,
                    serverSplitId = null,
                    type = "withdrawal",
                    date = 1000L,
                    amount = "25.00",
                    currencyCode = "EUR",
                    currencySymbol = "€",
                    currencyDecimalPlaces = 2,
                    description = "Pizza",
                    sourceId = "1",
                    sourceName = "Checking",
                    destinationId = null,
                    destinationName = null,
                    categoryId = null,
                    categoryName = null,
                    budgetId = null,
                    budgetName = null,
                    notes = null,
                    tags = emptyList(),
                    syncStatus = SyncStatus.PENDING_CREATE,
                ),
            ),
        )
    }

    private fun remoteGroup(
        id: String,
        journalId: String,
        externalId: String?,
        updatedAt: String = "2026-11-01T20:00:00+00:00",
        amount: String = "25.00",
    ) = TransactionReadDto(
        id = id,
        attributes = TransactionAttributesDto(
            updatedAt = updatedAt,
            transactions = listOf(
                TransactionSplitDto(
                    transactionJournalId = journalId,
                    type = "withdrawal",
                    date = "2026-11-01T20:00:00+00:00",
                    amount = amount,
                    description = "Pizza",
                    externalId = externalId,
                ),
            ),
        ),
    )

    private companion object {
        const val BASE_ISO = "2026-11-01T20:00:00+00:00"
        const val LATER_ISO = "2026-11-02T08:00:00+00:00"
        val BASE = parseIsoInstant(BASE_ISO)
    }
}
