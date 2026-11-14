package app.lucent.firefly.feature.onboarding.impl.presentation.viewmodel

import app.lucent.firefly.core.common.base.BaseViewModel
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.auth.api.usecase.AuthenticateUseCase
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class OnboardingViewModel(
    private val authenticateUseCase: AuthenticateUseCase,
) : BaseViewModel<OnboardingUiState, OnboardingUiEvent, OnboardingUiAction>() {
    override fun initialState(): OnboardingUiState = OnboardingUiState()

    override fun onActionEvent(action: OnboardingUiAction) {
        when (action) {
            is OnboardingUiAction.UpdateServerUrl -> {
                update { copy(serverUrl = action.url, errorMessage = null) }
            }
            is OnboardingUiAction.UpdateToken -> {
                update { copy(token = action.token, errorMessage = null) }
            }
            is OnboardingUiAction.UpdateAllowSelfSignedCert -> {
                update { copy(allowSelfSignedCert = action.enabled, errorMessage = null) }
            }
            OnboardingUiAction.Connect -> handleConnect()
            OnboardingUiAction.ClearError -> update { copy(errorMessage = null) }
        }
    }

    private fun handleConnect() {
        val url = currentState.serverUrl.trim()
        val token = currentState.token.trim()

        if (url.isBlank()) {
            update { copy(errorMessage = "Please enter a valid server URL.") }
            return
        }
        if (token.isBlank()) {
            update { copy(errorMessage = "Please enter your Personal Access Token (PAT).") }
            return
        }

        viewmodelLaunch {
            update { copy(isLoading = true, errorMessage = null) }
            val result =
                authenticateUseCase.perform(
                    AuthenticateUseCase.Params(
                        baseUrl = url,
                        token = token,
                        allowSelfSignedCert = currentState.allowSelfSignedCert,
                    ),
                )
            when (result) {
                is Result.Success -> {
                    update { copy(isLoading = false, errorMessage = null) }
                    sendEvent(OnboardingUiEvent.NavigateToDashboard)
                }
                is Result.Error -> {
                    update {
                        copy(
                            isLoading = false,
                            errorMessage = result.error.errorMessage,
                        )
                    }
                }
            }
        }
    }
}
