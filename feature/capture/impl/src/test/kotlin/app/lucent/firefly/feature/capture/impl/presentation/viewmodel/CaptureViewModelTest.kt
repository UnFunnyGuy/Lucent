package app.lucent.firefly.feature.capture.impl.presentation.viewmodel

import androidx.paging.PagingData
import app.cash.turbine.test
import app.lucent.firefly.core.common.model.Currency
import app.lucent.firefly.core.common.model.Money
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.accounts.api.model.Account
import app.lucent.firefly.data.accounts.api.model.AccountType
import app.lucent.firefly.data.accounts.api.repository.AccountRepository
import app.lucent.firefly.data.accounts.api.usecase.ObserveAccountsUseCase
import app.lucent.firefly.data.transactions.api.model.Transaction
import app.lucent.firefly.data.transactions.api.model.TransactionDraft
import app.lucent.firefly.data.transactions.api.model.TransactionType
import app.lucent.firefly.data.transactions.api.repository.TransactionRepository
import app.lucent.firefly.data.transactions.api.usecase.CreateTransactionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val eur = Currency(code = "EUR", symbol = "€")
    private val checking = account("acc-1", "1", "Checking", AccountType.ASSET, order = 1)
    private val savings = account("acc-2", "2", "Savings", AccountType.ASSET, order = 2)
    private val creditCard = account("acc-3", "3", "Credit card", AccountType.LIABILITY, order = 3)
    private val groceries = account("acc-4", "4", "Groceries", AccountType.EXPENSE, order = 4)

    private val accountsFlow = MutableStateFlow(listOf(checking, savings, creditCard, groceries))
    private var createdDraft: TransactionDraft? = null
    private var createResult: Result<String> = Result.Success("tx-1")

    private val accountRepository = object : AccountRepository {
        override fun observeAccounts(): Flow<List<Account>> = accountsFlow
        override fun observeActiveAccounts(): Flow<List<Account>> = accountsFlow
        override fun observeAccount(id: String): Flow<Account?> = flowOf(null)
        override suspend fun getAccount(id: String): Result<Account> = Result.Error(AppError.Common("Not mocked"))
        override suspend fun refreshAccounts(): Result<List<Account>> = Result.Success(emptyList())
    }

    private val transactionRepository = object : TransactionRepository {
        override fun observeTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
        override fun pagedTransactions(): Flow<PagingData<Transaction>> = flowOf(PagingData.empty())
        override suspend fun getTransaction(id: String): Result<Transaction> = Result.Error(AppError.Common("Not mocked"))
        override suspend fun createTransaction(draft: TransactionDraft): Result<String> {
            createdDraft = draft
            return createResult
        }
        override suspend fun updateTransaction(id: String, draft: TransactionDraft): Result<Unit> = Result.Success(Unit)
        override suspend fun deleteTransaction(id: String): Result<Unit> = Result.Success(Unit)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = CaptureViewModel(
        observeAccountsUseCase = ObserveAccountsUseCase(accountRepository),
        createTransactionUseCase = CreateTransactionUseCase(transactionRepository),
    )

    @Test
    fun `keypad builds an amount with at most two decimals`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        "12.345".forEach { viewModel.onAction(CaptureUiAction.EnterDigit(it)) }
        assertEquals("12.34", viewModel.uiState.value.amountInput)

        viewModel.onAction(CaptureUiAction.DeleteDigit)
        assertEquals("12.3", viewModel.uiState.value.amountInput)

        viewModel.onAction(CaptureUiAction.ClearAmount)
        assertEquals("0", viewModel.uiState.value.amountInput)
    }

    @Test
    fun `expense defaults to the first own account and offers no counterparty`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TransactionType.WITHDRAWAL, state.type)
        assertEquals("acc-1", state.sourceAccountId)
        assertNull(state.destinationAccountId)
        assertFalse(state.needsDestination)
        assertEquals(listOf("acc-1", "acc-2", "acc-3"), state.sourceOptions.map { it.id })
    }

    @Test
    fun `switching to income keeps the chosen account as the receiving account`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onAction(CaptureUiAction.SelectSourceAccount("acc-2"))

        viewModel.onAction(CaptureUiAction.SelectType(TransactionType.DEPOSIT))

        val state = viewModel.uiState.value
        assertNull(state.sourceAccountId)
        assertEquals("acc-2", state.destinationAccountId)
    }

    @Test
    fun `transfer uses two different asset accounts and swaps when they would collide`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(CaptureUiAction.SelectType(TransactionType.TRANSFER))
        assertEquals("acc-1", viewModel.uiState.value.sourceAccountId)
        assertEquals("acc-2", viewModel.uiState.value.destinationAccountId)
        assertEquals(listOf("acc-1", "acc-2"), viewModel.uiState.value.sourceOptions.map { it.id })

        viewModel.onAction(CaptureUiAction.SelectSourceAccount("acc-2"))

        assertEquals("acc-2", viewModel.uiState.value.sourceAccountId)
        assertEquals("acc-1", viewModel.uiState.value.destinationAccountId)
    }

    @Test
    fun `save is disabled until amount and description are set`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onAction(CaptureUiAction.EnterDigit('5'))
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onAction(CaptureUiAction.UpdateDescription("Coffee"))
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `expense draft sends only the source account and dismisses`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiEvent.test {
            "25".forEach { viewModel.onAction(CaptureUiAction.EnterDigit(it)) }
            viewModel.onAction(CaptureUiAction.UpdateDescription("  Dinner "))
            viewModel.onAction(CaptureUiAction.Save)
            advanceUntilIdle()

            assertEquals(CaptureUiEvent.Dismiss, awaitItem())
        }
        val draft = createdDraft!!
        assertEquals(TransactionType.WITHDRAWAL, draft.type)
        assertEquals("Dinner", draft.description)
        assertEquals(Money.of("25", eur), draft.amount)
        assertEquals("1", draft.sourceId)
        assertNull(draft.destinationId)
        assertNull(draft.destinationName)
    }

    @Test
    fun `income draft sends only the destination account`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(CaptureUiAction.SelectType(TransactionType.DEPOSIT))
        viewModel.onAction(CaptureUiAction.EnterDigit('9'))
        viewModel.onAction(CaptureUiAction.UpdateDescription("Salary"))
        viewModel.onAction(CaptureUiAction.Save)
        advanceUntilIdle()

        assertNull(createdDraft?.sourceId)
        assertNull(createdDraft?.sourceName)
        assertEquals("1", createdDraft?.destinationId)
    }

    @Test
    fun `transfer draft sends both accounts and the chosen date`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val lastWeek = LocalDate.now().minusDays(7)

        viewModel.onAction(CaptureUiAction.SelectType(TransactionType.TRANSFER))
        viewModel.onAction(CaptureUiAction.EnterDigit('3'))
        viewModel.onAction(CaptureUiAction.UpdateDescription("Savings"))
        viewModel.onAction(CaptureUiAction.SelectDate(lastWeek))
        viewModel.onAction(CaptureUiAction.Save)
        advanceUntilIdle()

        assertEquals("1", createdDraft?.sourceId)
        assertEquals("2", createdDraft?.destinationId)
        val savedDay = java.time.Instant.ofEpochMilli(createdDraft!!.date).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        assertEquals(lastWeek, savedDay)
    }

    @Test
    fun `failed save keeps the sheet open with the error`() = runTest(testDispatcher) {
        createResult = Result.Error(AppError.Common("A description is required."))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(CaptureUiAction.EnterDigit('1'))
        viewModel.onAction(CaptureUiAction.UpdateDescription("x"))
        viewModel.onAction(CaptureUiAction.Save)
        advanceUntilIdle()

        assertEquals("A description is required.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    private fun account(id: String, serverId: String, name: String, type: AccountType, order: Int) = Account(
        id = id,
        serverId = serverId,
        name = name,
        type = type,
        balance = Money.of("500.00", eur),
        pendingDelta = Money.zero(eur),
        currency = eur,
        active = true,
        order = order,
        updatedAt = 0L,
    )
}
