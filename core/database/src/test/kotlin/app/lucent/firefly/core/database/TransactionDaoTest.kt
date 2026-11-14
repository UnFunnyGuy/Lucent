package app.lucent.firefly.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import app.lucent.firefly.core.database.dao.TransactionDao
import app.lucent.firefly.core.database.entity.TransactionGroupEntity
import app.lucent.firefly.core.database.entity.TransactionSplitEntity
import app.lucent.firefly.core.database.model.SyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransactionDaoTest {
    private lateinit var database: LucentDatabase
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, LucentDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        transactionDao = database.transactionDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun observeTransactionsRetrievesGroupsWithSplits() =
        runTest {
            val group =
                TransactionGroupEntity(
                    localId = "grp-1",
                    serverId = null,
                    groupTitle = "Coffee & Snack",
                )
            val split1 = createSplit(splitId = "split-1", groupId = "grp-1", amount = "4.50")
            val split2 = createSplit(splitId = "split-2", groupId = "grp-1", amount = "3.00")

            transactionDao.insertGroup(group)
            transactionDao.insertSplits(listOf(split1, split2))

            transactionDao.observeTransactions().test {
                val transactions = awaitItem()
                assertEquals(1, transactions.size)
                assertEquals("grp-1", transactions[0].group.localId)
                assertEquals(2, transactions[0].splits.size)
            }
        }

    @Test
    fun observeSplitsForAccountFiltersCorrectly() =
        runTest {
            val group = TransactionGroupEntity(localId = "grp-1", serverId = null, groupTitle = null)
            val splitAccountA =
                createSplit(splitId = "s1", groupId = "grp-1", sourceId = "acc-10", destinationId = "acc-20")
            val splitAccountB =
                createSplit(splitId = "s2", groupId = "grp-1", sourceId = "acc-30", destinationId = "acc-40")

            transactionDao.insertGroup(group)
            transactionDao.insertSplits(listOf(splitAccountA, splitAccountB))

            transactionDao.observeSplitsForAccount(listOf("acc-10")).test {
                val splits = awaitItem()
                assertEquals(1, splits.size)
                assertEquals("s1", splits[0].localSplitId)
            }
        }

    private fun createSplit(
        splitId: String,
        groupId: String,
        amount: String = "10.00",
        sourceId: String? = "acc-1",
        destinationId: String? = "acc-2",
        status: SyncStatus = SyncStatus.SYNCED,
    ) = TransactionSplitEntity(
        localSplitId = splitId,
        transactionGroupId = groupId,
        serverSplitId = null,
        type = "withdrawal",
        date = System.currentTimeMillis(),
        amount = amount,
        currencyCode = "EUR",
        currencySymbol = "€",
        currencyDecimalPlaces = 2,
        description = "Test split",
        sourceId = sourceId,
        sourceName = "Checking",
        destinationId = destinationId,
        destinationName = "Supermarket",
        categoryId = "cat-1",
        categoryName = "Groceries",
        budgetId = null,
        budgetName = null,
        notes = null,
        tags = listOf("food", "weekly"),
        syncStatus = status,
        order = 0,
    )
}
