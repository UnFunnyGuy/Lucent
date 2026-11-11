package app.lucent.firefly.feature.onboarding.impl.presentation.viewmodel

import app.lucent.firefly.core.common.base.UIAction

sealed interface OnboardingUiAction : UIAction {
    data class UpdateServerUrl(val url: String) : OnboardingUiAction
    data class UpdateToken(val token: String) : OnboardingUiAction
    data class UpdateAllowSelfSignedCert(val enabled: Boolean) : OnboardingUiAction
    data object Connect : OnboardingUiAction
    data object ClearError : OnboardingUiAction
}
