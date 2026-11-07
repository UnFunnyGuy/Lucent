package app.lucent.firefly.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import app.lucent.firefly.core.database.dao.AccountDao
import app.lucent.firefly.core.database.entity.AccountEntity
import app.lucent.firefly.core.database.model.SyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountDaoTest {

    private lateinit var database: LucentDatabase
    private lateinit var accountDao: AccountDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LucentDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountDao = database.accountDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun observeAccountsOrdersByOrderThenName() = runTest {
        accountDao.insertOrReplace(
            listOf(
                createAccount(id = "b", name = "Savings", order = 2),
                createAccount(id = "a", name = "Checking", order = 1),
            ),
        )

        accountDao.observeAccounts().test {
            assertEquals(listOf("a", "b"), awaitItem().map { it.localId })
        }
    }

    @Test
    fun observeActiveAccountsExcludesArchived() = runTest {
        val activeAccount = createAccount(id = "active", active = true)
        val archivedAccount = createAccount(id = "archived", active = false)

        accountDao.insertOrReplace(listOf(activeAccount, archivedAccount))

        accountDao.observeActiveAccounts().test {
            val accounts = awaitItem()
            assertEquals(1, accounts.size)
            assertEquals("active", accounts[0].localId)
        }
    }

    private fun createAccount(
        id: String,
        name: String = "Test Account",
        order: Int = 0,
        active: Boolean = true,
        status: SyncStatus = SyncStatus.SYNCED,
    ) = AccountEntity(
        localId = id,
        serverId = null,
        name = name,
        type = "asset",
        currentBalance = "100.00",
        currencyId = "1",
        currencyCode = "EUR",
        currencySymbol = "€",
        currencyDecimalPlaces = 2,
        active = active,
        order = order,
        syncStatus = status,
        updatedAt = System.currentTimeMillis()
    )
}
