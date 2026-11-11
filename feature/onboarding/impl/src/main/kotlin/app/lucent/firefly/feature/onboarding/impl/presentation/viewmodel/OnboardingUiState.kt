package app.lucent.firefly.feature.onboarding.impl.presentation.viewmodel

import app.lucent.firefly.core.common.base.UIState

data class OnboardingUiState(
    val serverUrl: String = "",
    val token: String = "",
    val allowSelfSignedCert: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : UIState
