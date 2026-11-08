package app.lucent.firefly.data.accounts.impl.repository

import app.cash.turbine.test
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.dao.AccountDao
import app.lucent.firefly.core.database.dao.TransactionDao
import app.lucent.firefly.core.database.entity.AccountEntity
import app.lucent.firefly.core.database.model.SyncStatus
import app.lucent.firefly.core.network.model.PageMetaDto
import app.lucent.firefly.core.network.model.PaginationDto
import app.lucent.firefly.data.accounts.api.model.AccountType
import app.lucent.firefly.data.accounts.impl.datasource.AccountRemoteDataSource
import app.lucent.firefly.data.accounts.impl.dto.AccountArrayDto
import app.lucent.firefly.data.accounts.impl.dto.AccountPropertiesDto
import app.lucent.firefly.data.accounts.impl.dto.AccountReadDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountRepositoryImplTest {

    private val accountDao: AccountDao = mockk(relaxed = true)
    private val transactionDao: TransactionDao = mockk(relaxed = true)
    private val remoteDataSource: AccountRemoteDataSource = mockk()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: AccountRepositoryImpl

    @Before
    fun setup() {
        every { transactionDao.observeUnsyncedSplits() } returns flowOf(emptyList())
        repository = AccountRepositoryImpl(
            accountDao = accountDao,
            transactionDao = transactionDao,
            remoteDataSource = remoteDataSource,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun observeAccountsEmitsMappedAccounts() = runTest(testDispatcher) {

        val entity = createEntity(id = "local-1", name = "Checking", balance = "100.50")
        every { accountDao.observeAccounts() } returns flowOf(listOf(entity))

        repository.observeAccounts().test {
            val accounts = awaitItem()
            assertEquals(1, accounts.size)
            assertEquals("local-1", accounts[0].id)
            assertEquals("Checking", accounts[0].name)
            assertEquals(BigDecimal("100.50"), accounts[0].balance.amount)
            assertEquals(AccountType.ASSET, accounts[0].type)
            awaitComplete()
        }
    }

    @Test
    fun refreshAccountsFetchesAssetAndLiabilityPagesAndKeepsLocalIds() = runTest(testDispatcher) {
        coEvery { accountDao.getAccounts() } returns listOf(createEntity(localId = "local-42", serverId = "server-42"))
        coEvery { remoteDataSource.getAccounts("asset", 1) } returns Result.Success(
            AccountArrayDto(data = listOf(remoteDto("server-42", "Remote Checking")), meta = page(current = 1, total = 2)),
        )
        coEvery { remoteDataSource.getAccounts("asset", 2) } returns Result.Success(
            AccountArrayDto(data = listOf(remoteDto("server-43", "Savings")), meta = page(current = 2, total = 2)),
        )
        coEvery { remoteDataSource.getAccounts("liabilities", 1) } returns Result.Success(
            AccountArrayDto(data = listOf(remoteDto("server-50", "Mortgage", type = "liabilities")), meta = page(current = 1, total = 1)),
        )

        val accounts = (repository.refreshAccounts() as Result.Success).data

        assertEquals(listOf("server-42", "server-43", "server-50"), accounts.map { it.serverId })
        assertEquals("local-42", accounts.first().id)
        coVerify { accountDao.insertOrReplace(any<List<AccountEntity>>()) }
        coVerify(exactly = 0) { accountDao.deleteByLocalId(any()) }
    }

    @Test
    fun refreshAccountsRemovesSyncedAccountsMissingOnServer() = runTest(testDispatcher) {
        coEvery { accountDao.getAccounts() } returns listOf(
            createEntity(localId = "local-gone", serverId = "server-gone"),
        )
        coEvery { remoteDataSource.getAccounts(any(), any()) } returns Result.Success(AccountArrayDto(data = emptyList()))

        repository.refreshAccounts()

        coVerify { accountDao.deleteByLocalId("local-gone") }
    }

    @Test
    fun refreshAccountsWritesNothingWhenAPageFails() = runTest(testDispatcher) {
        coEvery { accountDao.getAccounts() } returns emptyList()
        coEvery { remoteDataSource.getAccounts("asset", 1) } returns Result.Error(AppError.Network.TIMEOUT)

        val result = repository.refreshAccounts()

        assertEquals(AppError.Network.TIMEOUT, (result as Result.Error).error)
        coVerify(exactly = 0) { accountDao.insertOrReplace(any<List<AccountEntity>>()) }
    }

    private fun remoteDto(id: String, name: String, type: String = "asset") = AccountReadDto(
        type = "accounts",
        id = id,
        attributes = AccountPropertiesDto(
            name = name,
            type = type,
            currentBalance = "750.00",
            currencyCode = "EUR",
            currencySymbol = "€",
            currencyDecimalPlaces = 2,
        ),
    )

    private fun page(current: Int, total: Int) = PageMetaDto(
        PaginationDto(total = total * 50, count = 50, perPage = 50, currentPage = current, totalPages = total),
    )

    @Test
    fun getAccountReturnsSuccessWhenFound() = runTest(testDispatcher) {
        val entity = createEntity(id = "local-acc", name = "Wallet", balance = "50.00")
        coEvery { accountDao.getAccountByLocalId("local-acc") } returns entity

        val result = repository.getAccount("local-acc")
        assertTrue(result is Result.Success)
        val account = (result as Result.Success).data
        assertEquals("Wallet", account.name)
    }

    @Test
    fun getAccountReturnsErrorWhenNotFound() = runTest(testDispatcher) {
        coEvery { accountDao.getAccountByLocalId("unknown") } returns null

        val result = repository.getAccount("unknown")
        assertTrue(result is Result.Error)
    }

    private fun createEntity(
        id: String = "local-1",
        name: String = "Checking",
        balance: String = "0.00",
        localId: String = id,
        serverId: String = "srv-1",
    ) = AccountEntity(
        localId = localId,
        serverId = serverId,
        name = name,
        type = "asset",
        currentBalance = balance,
        currencyId = "1",
        currencyCode = "EUR",
        currencySymbol = "€",
        currencyDecimalPlaces = 2,
        active = true,
        order = 1,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = System.currentTimeMillis()
    )
}
