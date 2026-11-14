package app.lucent.firefly.feature.dashboard.impl.presentation.viewmodel

import app.lucent.firefly.core.common.base.UIAction

sealed interface DashboardUiAction : UIAction {
    data object Refresh : DashboardUiAction

    data object OpenQuickAdd : DashboardUiAction

    data object Logout : DashboardUiAction

    data class OpenTransaction(
        val transactionId: String,
    ) : DashboardUiAction

    data class OpenAccount(
        val accountId: String,
    ) : DashboardUiAction

    data object DismissError : DashboardUiAction

    data object ConfirmLogout : DashboardUiAction

    data object CancelLogout : DashboardUiAction

    data class RetryChange(
        val changeId: Long,
    ) : DashboardUiAction

    data class KeepLocalChange(
        val changeId: Long,
    ) : DashboardUiAction

    data class DiscardChange(
        val changeId: Long,
    ) : DashboardUiAction
}
