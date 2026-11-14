package app.lucent.firefly.feature.dashboard.impl.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import app.lucent.firefly.core.common.base.BaseViewModel
import app.lucent.firefly.core.common.model.Money
import app.lucent.firefly.core.common.network.ConnectivityHandler
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.accounts.api.model.calculateNetWorth
import app.lucent.firefly.data.accounts.api.usecase.ObserveAccountsUseCase
import app.lucent.firefly.data.accounts.api.usecase.RefreshAccountsUseCase
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.sync.api.manager.SyncManager
import app.lucent.firefly.data.transactions.api.model.Transaction
import app.lucent.firefly.data.transactions.api.model.TransactionType
import app.lucent.firefly.data.transactions.api.usecase.ObservePagedTransactionsUseCase
import app.lucent.firefly.data.transactions.api.usecase.ObserveTransactionsUseCase
import app.lucent.firefly.data.transactions.api.usecase.RefreshTransactionsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.KoinViewModel
import java.net.URI
import java.time.YearMonth

@KoinViewModel
class DashboardViewModel(
    private val observeAccountsUseCase: ObserveAccountsUseCase,
    private val observeTransactionsUseCase: ObserveTransactionsUseCase,
    observePagedTransactionsUseCase: ObservePagedTransactionsUseCase,
    private val refreshAccountsUseCase: RefreshAccountsUseCase,
    private val refreshTransactionsUseCase: RefreshTransactionsUseCase,
    private val syncManager: SyncManager,
    private val authRepository: AuthRepository,
    private val connectivityHandler: ConnectivityHandler,
) : BaseViewModel<DashboardUiState, DashboardUiEvent, DashboardUiAction>() {
    /**
     * Paged history, read from Room and paged in from the server while scrolling. Kept out of the
     * UI state because Paging owns its own loading state and must be collected by the list.
     */
    val transactions: Flow<PagingData<TransactionListItem>> =
        observePagedTransactionsUseCase
            .performStreaming()
            .map { page ->
                page
                    .map { TransactionListItem.Entry(it) }
                    .insertSeparators<TransactionListItem.Entry, TransactionListItem> { before, after ->
                        val day = after?.date ?: return@insertSeparators null
                        if (before?.date == day) null else TransactionListItem.DayHeader(day)
                    }
            }.cachedIn(viewModelScope)

    override fun initialState(): DashboardUiState = DashboardUiState()

    init {
        observeSession()
        observeConnectivity()
        observeSync()
        observeBalances()
        refresh()
    }

    override fun onActionEvent(action: DashboardUiAction) {
        when (action) {
            DashboardUiAction.Refresh -> refresh()
            DashboardUiAction.OpenQuickAdd -> sendEvent(DashboardUiEvent.NavigateToQuickAdd)
            DashboardUiAction.Logout -> requestLogout()
            DashboardUiAction.ConfirmLogout -> logout()
            DashboardUiAction.CancelLogout -> update { copy(logoutConfirmationPendingCount = null) }
            is DashboardUiAction.RetryChange -> viewmodelLaunch { syncManager.retryFailedChange(action.changeId) }
            is DashboardUiAction.KeepLocalChange ->
                viewmodelLaunch { syncManager.retryFailedChange(action.changeId, overwriteServer = true) }
            is DashboardUiAction.DiscardChange -> discardChange(action.changeId)
            is DashboardUiAction.OpenTransaction ->
                sendEvent(DashboardUiEvent.NavigateToTransactionDetail(action.transactionId))
            is DashboardUiAction.OpenAccount -> sendEvent(DashboardUiEvent.NavigateToAccountDetail(action.accountId))
            DashboardUiAction.DismissError -> update { copy(errorMessage = null) }
        }
    }

    private fun observeSession() {
        viewmodelLaunch {
            authRepository.activeSession.collect { session ->
                update {
                    copy(
                        serverHost =
                            session
                                ?.serverConfig
                                ?.baseUrl
                                ?.let(::hostOf)
                                .orEmpty(),
                    )
                }
            }
        }
    }

    private fun observeConnectivity() {
        viewmodelLaunch {
            connectivityHandler.isConnected.collect { connected -> update { copy(isOnline = connected) } }
        }
    }

    private fun observeSync() {
        viewmodelLaunch {
            syncManager.syncState.collect { sync ->
                update {
                    copy(
                        isSyncing = sync.isSyncing,
                        pendingChangesCount = sync.pendingCount,
                        failedChanges = sync.failedChanges,
                        requiresReauthentication = sync.requiresReauthentication,
                    )
                }
            }
        }
    }

    private fun observeBalances() {
        viewmodelLaunch {
            combine(
                observeAccountsUseCase.performStreaming(),
                observeTransactionsUseCase.performStreaming(),
            ) { accounts, transactions -> accounts to transactions }
                .collect { (accounts, transactions) ->
                    val netWorth = calculateNetWorth(accounts) ?: Money.zero(currentState.netWorth.currency)
                    val month = YearMonth.now()
                    val thisMonth = transactions.filter { YearMonth.from(it.localDate()) == month }
                    update {
                        copy(
                            accounts = accounts.filter { it.active },
                            netWorth = netWorth,
                            monthIncome = thisMonth.total(TransactionType.DEPOSIT, netWorth),
                            monthSpending = thisMonth.total(TransactionType.WITHDRAWAL, netWorth),
                        )
                    }
                }
        }
    }

    private fun refresh() {
        syncManager.requestSync(expedited = true)
        viewmodelLaunch {
            update { copy(isRefreshing = true, errorMessage = null) }
            val accountsResult = refreshAccountsUseCase.perform()
            val transactionsResult = refreshTransactionsUseCase.perform()
            val error =
                (accountsResult as? Result.Error)?.error?.errorMessage
                    ?: (transactionsResult as? Result.Error)?.error?.errorMessage
            update { copy(isRefreshing = false, errorMessage = error) }
        }
    }

    private fun requestLogout() {
        val unsent = currentState.pendingChangesCount + currentState.failedChanges.size
        if (unsent > 0) {
            update { copy(logoutConfirmationPendingCount = unsent) }
        } else {
            logout()
        }
    }

    /** Unsent changes stay queued for this profile and upload when the same user signs in again. */
    private fun logout() {
        update { copy(logoutConfirmationPendingCount = null) }
        viewmodelLaunch {
            authRepository.logout()
            sendEvent(DashboardUiEvent.NavigateToLogin)
        }
    }

    private fun discardChange(changeId: Long) {
        viewmodelLaunch {
            val result = syncManager.discardFailedChange(changeId)
            if (result is Result.Error) update { copy(errorMessage = result.error.errorMessage) }
        }
    }

    /** Sum of one transaction type in the net-worth currency; other currencies are left out. */
    private fun List<Transaction>.total(
        type: TransactionType,
        reference: Money,
    ): Money =
        flatMap { it.splits }
            .filter { it.type == type && it.amount.currency.code == reference.currency.code }
            .fold(Money.zero(reference.currency)) { sum, split -> Money(sum.amount + split.amount.amount.abs(), sum.currency) }

    private fun hostOf(baseUrl: String): String = runCatching { URI(baseUrl).host }.getOrNull() ?: baseUrl
}
