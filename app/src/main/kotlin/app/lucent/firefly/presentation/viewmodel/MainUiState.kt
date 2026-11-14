package app.lucent.firefly.presentation.viewmodel

import app.lucent.firefly.core.common.base.UIState

data class MainUiState(
    val isSessionResolved: Boolean = false,
) : UIState
