package app.lucent.firefly.feature.onboarding.impl.presentation.viewmodel

import app.cash.turbine.test
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.model.AuthUser
import app.lucent.firefly.data.auth.api.model.ServerConfig
import app.lucent.firefly.data.auth.api.model.ServerInfo
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.auth.api.usecase.AuthenticateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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

class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAuthRepository : AuthRepository {
        var authResult: Result<AuthSession> = Result.success(
            AuthSession(
                serverConfig = ServerConfig("https://firefly.example.com"),
                user = AuthUser("2", "test@example.com", "owner"),
                token = "token",
            ),
        )

        override val activeSession: Flow<AuthSession?> = emptyFlow()

        override suspend fun validateServer(baseUrl: String, token: String?, allowSelfSignedCert: Boolean): Result<ServerInfo> =
            Result.success(ServerInfo(version = "6.7.8", apiVersion = "6.7.8"))

        override suspend fun authenticate(
            baseUrl: String,
            token: String,
            allowSelfSignedCert: Boolean,
        ): Result<AuthSession> = authResult

        override suspend fun logout(): Result<Unit> = Result.success(Unit)
    }

    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = OnboardingViewModel(
            authenticateUseCase = AuthenticateUseCase(fakeRepository),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasEmptyDefaults() {
        val state = viewModel.uiState.value
        assertEquals("", state.serverUrl)
        assertEquals("", state.token)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun serverUrlChanged_updatesState() {
        viewModel.onAction(OnboardingUiAction.UpdateServerUrl("https://my-firefly.org"))
        assertEquals("https://my-firefly.org", viewModel.uiState.value.serverUrl)
    }

    @Test
    fun selfSignedCertToggled_updatesState() {
        viewModel.onAction(OnboardingUiAction.UpdateAllowSelfSignedCert(true))
        assertTrue(viewModel.uiState.value.allowSelfSignedCert)

        viewModel.onAction(OnboardingUiAction.UpdateAllowSelfSignedCert(false))
        assertFalse(viewModel.uiState.value.allowSelfSignedCert)
    }

    @Test
    fun clearError_removesErrorMessage() {
        viewModel.onAction(OnboardingUiAction.Connect)
        assertTrue(viewModel.uiState.value.errorMessage != null)

        viewModel.onAction(OnboardingUiAction.ClearError)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun connectClicked_success_emitsNavigateToDashboard() = runTest {
        viewModel.onAction(OnboardingUiAction.UpdateServerUrl("https://my-firefly.org"))
        viewModel.onAction(OnboardingUiAction.UpdateToken("valid-token"))

        viewModel.uiEvent.test {
            viewModel.onAction(OnboardingUiAction.Connect)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(OnboardingUiEvent.NavigateToDashboard, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun connectClicked_emptyUrl_setsErrorMessage() {
        viewModel.onAction(OnboardingUiAction.UpdateServerUrl(""))
        viewModel.onAction(OnboardingUiAction.UpdateToken("valid-token"))
        viewModel.onAction(OnboardingUiAction.Connect)

        val state = viewModel.uiState.value
        assertEquals("Please enter a valid server URL.", state.errorMessage)
    }

    @Test
    fun connectClicked_emptyToken_setsErrorMessage() {
        viewModel.onAction(OnboardingUiAction.UpdateServerUrl("https://my-firefly.org"))
        viewModel.onAction(OnboardingUiAction.UpdateToken(""))

        viewModel.onAction(OnboardingUiAction.Connect)

        val state = viewModel.uiState.value
        assertEquals("Please enter your Personal Access Token (PAT).", state.errorMessage)
    }

    @Test
    fun connectClicked_serverError_setsErrorMessage() = runTest {
        fakeRepository.authResult = Result.error(AppError.ApiKeyError)
        viewModel.onAction(OnboardingUiAction.UpdateServerUrl("https://my-firefly.org"))
        viewModel.onAction(OnboardingUiAction.UpdateToken("bad-token"))

        viewModel.onAction(OnboardingUiAction.Connect)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.errorMessage?.isNotBlank() == true)
    }
}
