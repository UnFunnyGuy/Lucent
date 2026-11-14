package app.lucent.firefly.feature.dashboard.impl.presentation.viewmodel

import androidx.paging.PagingData
import app.lucent.firefly.core.common.model.Currency
import app.lucent.firefly.core.common.model.Money
import app.lucent.firefly.core.common.network.ConnectivityHandler
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.accounts.api.model.Account
import app.lucent.firefly.data.accounts.api.model.AccountType
import app.lucent.firefly.data.accounts.api.repository.AccountRepository
import app.lucent.firefly.data.accounts.api.usecase.ObserveAccountsUseCase
import app.lucent.firefly.data.accounts.api.usecase.RefreshAccountsUseCase
import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.model.ServerInfo
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.sync.api.handler.SyncOperation
import app.lucent.firefly.data.sync.api.handler.SyncTargetType
import app.lucent.firefly.data.sync.api.manager.SyncManager
import app.lucent.firefly.data.sync.api.model.FailedChange
import app.lucent.firefly.data.sync.api.model.SyncState
import app.lucent.firefly.data.transactions.api.model.Transaction
import app.lucent.firefly.data.transactions.api.model.TransactionDraft
import app.lucent.firefly.data.transactions.api.model.TransactionSplit
import app.lucent.firefly.data.transactions.api.model.TransactionType
import app.lucent.firefly.data.transactions.api.repository.TransactionReconciler
import app.lucent.firefly.data.transactions.api.repository.TransactionRepository
import app.lucent.firefly.data.transactions.api.usecase.ObservePagedTransactionsUseCase
import app.lucent.firefly.data.transactions.api.usecase.ObserveTransactionsUseCase
import app.lucent.firefly.data.transactions.api.usecase.RefreshTransactionsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val accountsFlow = MutableStateFlow<List<Account>>(emptyList())
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    private val sessionFlow = MutableStateFlow<AuthSession?>(null)
    private val isConnectedFlow = MutableStateFlow(true)
    private val syncStateFlow = MutableStateFlow(SyncState())

    private var requestedExpeditedSync = false
    private var loggedOut = false

    private val accountRepository =
        object : AccountRepository {
            override fun observeAccounts(): Flow<List<Account>> = accountsFlow

            override fun observeActiveAccounts(): Flow<List<Account>> = accountsFlow

            override fun observeAccount(id: String): Flow<Account?> = flowOf(null)

            override suspend fun getAccount(id: String): Result<Account> = Result.Error(AppError.Common("Not mocked"))

            override suspend fun refreshAccounts(): Result<List<Account>> = Result.Success(emptyList())
        }

    private val transactionRepository =
        object : TransactionRepository {
            override fun observeTransactions(): Flow<List<Transaction>> = transactionsFlow

            override suspend fun getTransaction(id: String): Result<Transaction> = Result.Error(AppError.Common("Not mocked"))

            override suspend fun createTransaction(draft: TransactionDraft): Result<String> = Result.Success("tx-1")

            override suspend fun deleteTransaction(id: String): Result<Unit> = Result.Success(Unit)

            override suspend fun updateTransaction(
                id: String,
                draft: TransactionDraft,
            ): Result<Unit> = Result.Success(Unit)

            override fun pagedTransactions(): Flow<PagingData<Transaction>> = flowOf(PagingData.empty())
        }

    private val transactionReconciler =
        object : TransactionReconciler {
            override suspend fun syncRecentTransactions(): Result<Unit> = Result.Success(Unit)

            override suspend fun reconcileTransactions(monthsBack: Int): Result<Unit> = Result.Success(Unit)
        }

    private val authRepository =
        object : AuthRepository {
            override val activeSession: Flow<AuthSession?> = sessionFlow

            override suspend fun validateServer(
                baseUrl: String,
                token: String?,
                allowSelfSignedCert: Boolean,
            ): Result<ServerInfo> = Result.Error(AppError.Common("Not mocked"))

            override suspend fun authenticate(
                baseUrl: String,
                token: String,
                allowSelfSignedCert: Boolean,
            ): Result<AuthSession> = Result.Error(AppError.Common("Not mocked"))

            override suspend fun logout(): Result<Unit> {
                loggedOut = true
                return Result.Success(Unit)
            }
        }

    private val connectivityHandler =
        object : ConnectivityHandler {
            override val isConnected = isConnectedFlow.asStateFlow()

            override fun isCurrentlyConnected(): Boolean = isConnectedFlow.value

            override fun hasValidatedInternet(): Boolean = isConnectedFlow.value
        }

    private val retriedChanges = mutableListOf<Pair<Long, Boolean>>()

    private val syncManager =
        object : SyncManager {
            override val syncState = syncStateFlow.asStateFlow()

            override fun requestSync(expedited: Boolean) {
                requestedExpeditedSync = expedited
            }

            override fun schedulePeriodicSync(intervalMinutes: Long) {}

            override fun cancelSync() {}

            override suspend fun retryFailedChange(
                id: Long,
                overwriteServer: Boolean,
            ) {
                retriedChanges += id to overwriteServer
            }

            override suspend fun discardFailedChange(id: Long): Result<Unit> = Result.Success(Unit)
        }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        DashboardViewModel(
            observeAccountsUseCase = ObserveAccountsUseCase(accountRepository),
            observeTransactionsUseCase = ObserveTransactionsUseCase(transactionRepository),
            observePagedTransactionsUseCase = ObservePagedTransactionsUseCase(transactionRepository),
            refreshAccountsUseCase = RefreshAccountsUseCase(accountRepository),
            refreshTransactionsUseCase = RefreshTransactionsUseCase(transactionReconciler),
            syncManager = syncManager,
            authRepository = authRepository,
            connectivityHandler = connectivityHandler,
        )

    @Test
    fun `initial state collects accounts and calculates net worth`() =
        runTest(testDispatcher) {
            val currency = Currency(code = "EUR", symbol = "€")
            val accounts =
                listOf(
                    Account(
                        id = "acc1",
                        serverId = "1",
                        name = "Checking",
                        type = AccountType.ASSET,
                        balance = Money.of("100.00", currency),
                        pendingDelta = Money.of("-10.00", currency),
                        currency = currency,
                        active = true,
                        order = 1,
                        updatedAt = 1000L,
                    ),
                    Account(
                        id = "acc2",
                        serverId = "2",
                        name = "Savings",
                        type = AccountType.ASSET,
                        balance = Money.of("200.00", currency),
                        pendingDelta = Money.zero(currency),
                        currency = currency,
                        active = true,
                        order = 2,
                        updatedAt = 1000L,
                    ),
                )
            accountsFlow.value = accounts

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2, state.accounts.size)
            assertEquals(Money.of("290.00", currency), state.netWorth)
        }

    @Test
    fun `refresh action triggers expedited sync`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAction(DashboardUiAction.Refresh)
            advanceUntilIdle()

            assertTrue(requestedExpeditedSync)
        }

    @Test
    fun `logout action calls authRepository and emits NavigateToLogin`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAction(DashboardUiAction.Logout)
            advanceUntilIdle()

            assertTrue(loggedOut)
        }

    @Test
    fun `logout with unsent changes asks for confirmation first`() =
        runTest(testDispatcher) {
            syncStateFlow.value = SyncState(pendingCount = 2)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAction(DashboardUiAction.Logout)
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.logoutConfirmationPendingCount)
            assertEquals(false, loggedOut)

            viewModel.onAction(DashboardUiAction.ConfirmLogout)
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.logoutConfirmationPendingCount)
            assertEquals(true, loggedOut)
        }

    @Test
    fun `failed changes are exposed and can be retried with overwrite`() =
        runTest(testDispatcher) {
            val conflict =
                FailedChange(
                    id = 5,
                    target = SyncTargetType.TRANSACTION,
                    operation = SyncOperation.UPDATE,
                    localId = "g-1",
                    message = "Changed on the server",
                    isConflict = true,
                )
            syncStateFlow.value = SyncState(failedChanges = listOf(conflict))
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(listOf(conflict), viewModel.uiState.value.failedChanges)

            viewModel.onAction(DashboardUiAction.KeepLocalChange(5))
            advanceUntilIdle()

            assertEquals(listOf(5L to true), retriedChanges)
        }

    @Test
    fun `month summary adds this month's income and spending in the net worth currency`() =
        runTest(testDispatcher) {
            val currency = Currency(code = "EUR", symbol = "€")
            accountsFlow.value =
                listOf(
                    Account(
                        id = "acc1",
                        serverId = "1",
                        name = "Checking",
                        type = AccountType.ASSET,
                        balance = Money.of("100.00", currency),
                        pendingDelta = Money.zero(currency),
                        currency = currency,
                        active = true,
                        order = 1,
                        updatedAt = 1000L,
                    ),
                )
            val now = System.currentTimeMillis()
            transactionsFlow.value =
                listOf(
                    transaction("t1", TransactionType.DEPOSIT, "1000.00", currency, now),
                    transaction("t2", TransactionType.WITHDRAWAL, "40.00", currency, now),
                    transaction("t3", TransactionType.TRANSFER, "500.00", currency, now),
                    transaction("t4", TransactionType.WITHDRAWAL, "99.00", currency, now - 400L * 24 * 60 * 60 * 1000),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(Money.of("1000.00", currency), viewModel.uiState.value.monthIncome)
            assertEquals(Money.of("40.00", currency), viewModel.uiState.value.monthSpending)
        }

    private fun transaction(
        id: String,
        type: TransactionType,
        amount: String,
        currency: Currency,
        date: Long,
    ) = Transaction(
        id = id,
        serverId = null,
        title = null,
        splits =
            listOf(
                TransactionSplit(
                    splitId = "$id-s",
                    serverSplitId = null,
                    groupId = id,
                    type = type,
                    date = date,
                    amount = Money.of(amount, currency),
                    description = id,
                    sourceId = null,
                    sourceName = null,
                    destinationId = null,
                    destinationName = null,
                    categoryId = null,
                    categoryName = null,
                    budgetId = null,
                    budgetName = null,
                    notes = null,
                    tags = emptyList(),
                    isPendingSync = false,
                ),
            ),
        createdAt = date,
        updatedAt = date,
    )
}
