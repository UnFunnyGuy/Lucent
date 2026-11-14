package app.lucent.firefly.feature.dashboard.impl.presentation.viewmodel

import app.lucent.firefly.core.common.base.UIState
import app.lucent.firefly.core.common.model.Currency
import app.lucent.firefly.core.common.model.Money
import app.lucent.firefly.data.accounts.api.model.Account
import app.lucent.firefly.data.sync.api.model.FailedChange

data class DashboardUiState(
    val serverHost: String = "",
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val isRefreshing: Boolean = false,
    val netWorth: Money = Money.zero(DEFAULT_CURRENCY),
    val monthIncome: Money = Money.zero(DEFAULT_CURRENCY),
    val monthSpending: Money = Money.zero(DEFAULT_CURRENCY),
    val accounts: List<Account> = emptyList(),
    val pendingChangesCount: Int = 0,
    val failedChanges: List<FailedChange> = emptyList(),
    val requiresReauthentication: Boolean = false,
    /** Unsent change count shown in the logout confirmation; null when the dialog is hidden. */
    val logoutConfirmationPendingCount: Int? = null,
    val errorMessage: String? = null,
) : UIState {
    private companion object {
        val DEFAULT_CURRENCY = Currency(code = "EUR", symbol = "€")
    }
}
