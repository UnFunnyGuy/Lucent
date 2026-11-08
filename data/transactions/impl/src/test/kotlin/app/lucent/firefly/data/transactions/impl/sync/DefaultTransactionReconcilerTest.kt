package app.lucent.firefly.data.transactions.impl.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.LucentDatabase
import app.lucent.firefly.core.database.dao.SyncQueueDao
import app.lucent.firefly.core.database.dao.TransactionDao
import app.lucent.firefly.core.database.entity.TransactionGroupEntity
import app.lucent.firefly.core.database.entity.TransactionSplitEntity
import app.lucent.firefly.core.database.model.SyncAction
import app.lucent.firefly.core.database.model.SyncEntityType
import app.lucent.firefly.core.database.model.SyncStatus
import app.lucent.firefly.core.network.model.PageMetaDto
import app.lucent.firefly.core.network.model.PaginationDto
import app.lucent.firefly.data.transactions.impl.datasource.TransactionRemoteDataSource
import app.lucent.firefly.data.transactions.impl.dto.TransactionArrayDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionAttributesDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionReadDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionSplitDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class DefaultTransactionReconcilerTest {

    private lateinit var database: LucentDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var syncQueueDao: SyncQueueDao

    private val remoteDataSource: TransactionRemoteDataSource = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var reconciler: DefaultTransactionReconciler

    // The recent window for this clock is 2026-02-01 .. 2026-03-15.
    private val now = Instant.parse("2026-03-15T12:00:00Z")

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LucentDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = database.transactionDao()
        syncQueueDao = database.syncQueueDao()

        reconciler = DefaultTransactionReconciler(
            database = database,
            transactionDao = transactionDao,
            syncQueueDao = syncQueueDao,
            remoteDataSource = remoteDataSource,
            remoteWriter = RemoteTransactionWriter(transactionDao, syncQueueDao),
            clock = Clock.fixed(now, ZoneOffset.UTC),
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun recentSyncFetchesEveryPageOfTheWindowWithoutDuplicates() = runTest(testDispatcher) {
        val start = LocalDate.of(2026, 2, 1)
        val end = LocalDate.of(2026, 3, 15)
        coEvery { remoteDataSource.getTransactions(start, end, 1) } returns Result.Success(
            TransactionArrayDto(data = listOf(remoteGroup("srv-1", "j-1", "5.00")), meta = page(1, 2)),
        )
        coEvery { remoteDataSource.getTransactions(start, end, 2) } returns Result.Success(
            TransactionArrayDto(data = listOf(remoteGroup("srv-2", "j-2", "8.00")), meta = page(2, 2)),
        )

        assertTrue(reconciler.syncRecentTransactions() is Result.Success)
        assertTrue(reconciler.syncRecentTransactions() is Result.Success)

        val serverIds = transactionDao.getGroupsInDateRange(0L, Long.MAX_VALUE).map { it.group.serverId }
        assertEquals(setOf("srv-1", "srv-2"), serverIds.toSet())
        assertEquals(2, serverIds.size)
    }

    @Test
    fun recentSyncLinksLocallyCreatedTransactionByExternalId() = runTest(testDispatcher) {
        insertSyncedGroup(localId = "g-local", serverId = null, splitLocalId = "s-local", journalId = null, updatedAt = 0L)
        coEvery { remoteDataSource.getTransactions(any(), any(), any()) } returns Result.Success(
            TransactionArrayDto(data = listOf(remoteGroup("srv-9", "j-9", "5.00", externalId = "g-local"))),
        )

        reconciler.syncRecentTransactions()

        assertEquals("srv-9", transactionDao.getTransactionWithSplits("g-local")!!.group.serverId)
    }

    @Test
    fun recentSyncRemovesTransactionsDeletedOnTheServer() = runTest(testDispatcher) {
        insertSyncedGroup(localId = "g-gone", serverId = "srv-gone", splitLocalId = "s-gone", journalId = "j-gone", updatedAt = 0L)
        coEvery { remoteDataSource.getTransactions(any(), any(), any()) } returns
            Result.Success(TransactionArrayDto(data = emptyList()))

        reconciler.syncRecentTransactions()

        assertNull(transactionDao.getTransactionWithSplits("g-gone"))
        assertNull(transactionDao.getSplitByLocalId("s-gone"))
    }

    @Test
    fun recentSyncKeepsTransactionsWithPendingLocalChanges() = runTest(testDispatcher) {
        insertSyncedGroup(localId = "g-1", serverId = "srv-1", splitLocalId = "s-1", journalId = "j-1", updatedAt = 42L)
        transactionDao.updateSplitSyncStatus("s-1", "j-1", SyncStatus.PENDING_DELETE)
        syncQueueDao.enqueueChange(SyncEntityType.TRANSACTION, "g-1", "srv-1", SyncAction.DELETE, baseUpdatedAt = 42L)
        coEvery { remoteDataSource.getTransactions(any(), any(), any()) } returns Result.Success(
            TransactionArrayDto(data = listOf(remoteGroup("srv-1", "j-1", "99.00"))),
        )

        reconciler.syncRecentTransactions()

        val stored = transactionDao.getTransactionWithSplits("g-1")!!
        assertEquals(SyncStatus.PENDING_DELETE, stored.splits.single().syncStatus)
        assertEquals("5.00", stored.splits.single().amount)
    }

    @Test
    fun recentSyncFailureLeavesLocalDataUntouched() = runTest(testDispatcher) {
        insertSyncedGroup(localId = "g-1", serverId = "srv-1", splitLocalId = "s-1", journalId = "j-1", updatedAt = 0L)
        coEvery { remoteDataSource.getTransactions(any(), any(), any()) } returns Result.Error(AppError.Network.TIMEOUT)

        assertTrue(reconciler.syncRecentTransactions() is Result.Error)
        assertEquals("srv-1", transactionDao.getTransactionWithSplits("g-1")!!.group.serverId)
    }

    @Test
    fun deepReconcileRequestsTheLongerWindow() = runTest(testDispatcher) {
        coEvery { remoteDataSource.getTransactions(any(), any(), any()) } returns
            Result.Success(TransactionArrayDto(data = emptyList()))

        reconciler.reconcileTransactions(monthsBack = 12)

        coVerify { remoteDataSource.getTransactions(LocalDate.of(2025, 3, 1), LocalDate.of(2026, 3, 15), 1) }
    }

    private suspend fun insertSyncedGroup(
        localId: String,
        serverId: String?,
        splitLocalId: String,
        journalId: String?,
        updatedAt: Long,
    ) {
        transactionDao.insertGroup(
            TransactionGroupEntity(
                localId = localId,
                serverId = serverId,
                groupTitle = null,
                createdAt = updatedAt,
                updatedAt = updatedAt,
            ),
        )
        transactionDao.insertSplits(
            listOf(
                TransactionSplitEntity(
                    localSplitId = splitLocalId,
                    transactionGroupId = localId,
                    serverSplitId = journalId,
                    type = "withdrawal",
                    // Inside the recent window, away from its edges.
                    date = Instant.parse("2026-03-01T10:00:00Z").toEpochMilli(),
                    amount = "5.00",
                    currencyCode = "EUR",
                    currencySymbol = "€",
                    currencyDecimalPlaces = 2,
                    description = "Snack",
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
                    syncStatus = SyncStatus.SYNCED,
                ),
            ),
        )
    }

    private fun remoteGroup(id: String, journalId: String, amount: String, externalId: String? = null) = TransactionReadDto(
        id = id,
        attributes = TransactionAttributesDto(
            updatedAt = "2026-03-01T10:00:00+00:00",
            transactions = listOf(
                TransactionSplitDto(
                    transactionJournalId = journalId,
                    type = "withdrawal",
                    date = "2026-03-01T10:00:00+00:00",
                    amount = amount,
                    description = "Snack",
                    externalId = externalId,
                ),
            ),
        ),
    )

    private fun page(current: Int, total: Int) =
        PageMetaDto(PaginationDto(total = total, count = 1, perPage = 1, currentPage = current, totalPages = total))
}
