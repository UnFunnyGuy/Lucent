package app.lucent.firefly.feature.onboarding.impl.presentation.viewmodel

import app.lucent.firefly.core.common.base.UIEvent

sealed interface OnboardingUiEvent : UIEvent {
    data object NavigateToDashboard : OnboardingUiEvent
    data class ShowSnackbar(val message: String) : OnboardingUiEvent
}
