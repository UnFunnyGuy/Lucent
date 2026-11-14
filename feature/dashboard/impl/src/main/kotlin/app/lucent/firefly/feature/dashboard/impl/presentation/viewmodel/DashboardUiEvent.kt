package app.lucent.firefly.feature.dashboard.impl.presentation.viewmodel

import app.lucent.firefly.core.common.base.UIEvent

sealed interface DashboardUiEvent : UIEvent {
    data object NavigateToQuickAdd : DashboardUiEvent

    data class NavigateToTransactionDetail(
        val transactionId: String,
    ) : DashboardUiEvent

    data class NavigateToAccountDetail(
        val accountId: String,
    ) : DashboardUiEvent

    data object NavigateToLogin : DashboardUiEvent

    data class ShowSnackbar(
        val message: String,
    ) : DashboardUiEvent
}
