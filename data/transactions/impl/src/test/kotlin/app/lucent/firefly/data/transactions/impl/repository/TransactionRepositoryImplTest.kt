package app.lucent.firefly.data.transactions.impl.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import app.lucent.firefly.core.common.model.Currency
import app.lucent.firefly.core.common.model.Money
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.LucentDatabase
import app.lucent.firefly.core.database.dao.SyncQueueDao
import app.lucent.firefly.core.database.dao.TransactionDao
import app.lucent.firefly.core.database.entity.TransactionGroupEntity
import app.lucent.firefly.core.database.entity.TransactionSplitEntity
import app.lucent.firefly.core.database.model.SyncAction
import app.lucent.firefly.core.database.model.SyncStatus
import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.model.AuthUser
import app.lucent.firefly.data.auth.api.model.ServerConfig
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.sync.api.manager.SyncManager
import app.lucent.firefly.data.transactions.api.model.TransactionDraft
import app.lucent.firefly.data.transactions.api.model.TransactionType
import app.lucent.firefly.data.transactions.impl.datasource.TransactionRemoteDataSource
import app.lucent.firefly.data.transactions.impl.sync.RemoteTransactionWriter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import java.math.BigDecimal
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class TransactionRepositoryImplTest {
    private lateinit var database: LucentDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var syncQueueDao: SyncQueueDao

    private val remoteDataSource: TransactionRemoteDataSource = mockk()
    private val authRepository: AuthRepository = mockk()
    private val syncManager: SyncManager = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val activeSessionFlow = MutableStateFlow<AuthSession?>(null)
    private lateinit var repository: TransactionRepositoryImpl

    private val eur = Currency(code = "EUR", symbol = "€", decimalPlaces = 2)

    private val testSession =
        AuthSession(
            serverConfig = ServerConfig(baseUrl = "https://demo.firefly.org"),
            user = AuthUser(id = "user-1", email = "test@example.com", role = "owner"),
            token = "secret-pat",
        )

    // The recent window for this clock is 2026-02-01 .. 2026-03-15.
    private val now = Instant.parse("2026-03-15T12:00:00Z")

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, LucentDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        transactionDao = database.transactionDao()
        syncQueueDao = database.syncQueueDao()
        every { authRepository.activeSession } returns activeSessionFlow
        activeSessionFlow.value = testSession

        repository =
            TransactionRepositoryImpl(
                database = database,
                transactionDao = transactionDao,
                remoteKeyDao = database.remoteKeyDao(),
                syncQueueDao = syncQueueDao,
                remoteDataSource = remoteDataSource,
                remoteWriter = RemoteTransactionWriter(transactionDao, syncQueueDao),
                syncManager = syncManager,
                ioDispatcher = testDispatcher,
            )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun createStoresTransactionAndQueuesOneCreate() =
        runTest(testDispatcher) {
            val groupId = (repository.createTransaction(draft("Groceries", "45.50")) as Result.Success).data

            val stored = transactionDao.getTransactionWithSplits(groupId)!!
            assertEquals("45.50", stored.splits.single().amount)
            assertEquals(SyncStatus.PENDING_CREATE, stored.splits.single().syncStatus)
            val queued = syncQueueDao.getPendingQueue().single()
            assertEquals(SyncAction.CREATE, queued.action)
            assertEquals(groupId, queued.localEntityId)
            verify { syncManager.requestSync(expedited = true) }
        }

    @Test
    fun createRejectsZeroAmount() =
        runTest(testDispatcher) {
            assertTrue(repository.createTransaction(draft("Nothing", "0")) is Result.Error)
            assertTrue(syncQueueDao.getPendingQueue().isEmpty())
        }

    @Test
    fun editBeforeUploadIsMergedIntoTheQueuedCreate() =
        runTest(testDispatcher) {
            val groupId = (repository.createTransaction(draft("Coffee", "3.00")) as Result.Success).data

            repository.updateTransaction(groupId, draft("Coffee and cake", "7.50"))

            val split = transactionDao.getTransactionWithSplits(groupId)!!.splits.single()
            assertEquals("7.50", split.amount)
            assertEquals(SyncStatus.PENDING_CREATE, split.syncStatus)
            assertEquals(listOf(SyncAction.CREATE), syncQueueDao.getPendingQueue().map { it.action })
        }

    @Test
    fun editOfSyncedTransactionQueuesUpdateWithServerBaseVersion() =
        runTest(testDispatcher) {
            insertSyncedGroup(localId = "g-1", serverId = "srv-1", splitLocalId = "s-1", journalId = "j-1", updatedAt = 42L)

            repository.updateTransaction("g-1", draft("Snack", "6.00"))

            val split = transactionDao.getSplitByLocalId("s-1")!!
            assertEquals(SyncStatus.PENDING_UPDATE, split.syncStatus)
            assertEquals("j-1", split.serverSplitId)
            val queued = syncQueueDao.getPendingQueue().single()
            assertEquals(SyncAction.UPDATE, queued.action)
            assertEquals(42L, queued.baseUpdatedAt)
        }

    @Test
    fun deleteOfUnsyncedTransactionRemovesItAndItsQueuedCreate() =
        runTest(testDispatcher) {
            val groupId = (repository.createTransaction(draft("Coffee", "12.00")) as Result.Success).data

            assertTrue(repository.deleteTransaction(groupId) is Result.Success)

            assertNull(transactionDao.getTransactionWithSplits(groupId))
            assertTrue(syncQueueDao.getPendingQueue().isEmpty())
        }

    @Test
    fun deleteAfterEditReplacesTheQueuedUpdateAndKeepsItsBase() =
        runTest(testDispatcher) {
            insertSyncedGroup(localId = "g-1", serverId = "srv-1", splitLocalId = "s-1", journalId = "j-1", updatedAt = 42L)
            repository.updateTransaction("g-1", draft("Snack", "6.00"))

            repository.deleteTransaction("g-1")

            val queued = syncQueueDao.getPendingQueue().single()
            assertEquals(SyncAction.DELETE, queued.action)
            assertEquals(42L, queued.baseUpdatedAt)
            assertEquals(SyncStatus.PENDING_DELETE, transactionDao.getSplitByLocalId("s-1")!!.syncStatus)
            repository.observeTransactions().test {
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun updateOfMissingTransactionFailsWithoutQueueing() =
        runTest(testDispatcher) {
            assertTrue(repository.updateTransaction("missing", draft("Snack", "6.00")) is Result.Error)
            assertTrue(syncQueueDao.getPendingQueue().isEmpty())
        }

    @Test
    fun updateIsRejectedWhileTheTransactionIsBeingDeleted() =
        runTest(testDispatcher) {
            insertSyncedGroup(localId = "g-1", serverId = "srv-1", splitLocalId = "s-1", journalId = "j-1", updatedAt = 42L)
            repository.deleteTransaction("g-1")

            assertTrue(repository.updateTransaction("g-1", draft("Snack", "6.00")) is Result.Error)
            assertEquals(listOf(SyncAction.DELETE), syncQueueDao.getPendingQueue().map { it.action })
        }

    @Test
    fun deleteOfMissingTransactionFailsWithoutQueueing() =
        runTest(testDispatcher) {
            assertTrue(repository.deleteTransaction("missing") is Result.Error)
            assertTrue(syncQueueDao.getPendingQueue().isEmpty())
        }

    private fun draft(
        description: String,
        amount: String,
    ) = TransactionDraft(
        type = TransactionType.WITHDRAWAL,
        amount = Money(BigDecimal(amount), eur),
        description = description,
        sourceId = "1",
        sourceName = "Checking",
        date = now.toEpochMilli(),
    )

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
}
