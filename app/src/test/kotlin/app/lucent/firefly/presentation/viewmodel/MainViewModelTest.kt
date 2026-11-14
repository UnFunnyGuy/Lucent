package app.lucent.firefly.presentation.viewmodel

import androidx.navigation3.runtime.NavKey
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.navigation.Navigator
import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.model.AuthUser
import app.lucent.firefly.data.auth.api.model.ServerConfig
import app.lucent.firefly.data.auth.api.model.ServerInfo
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.auth.api.usecase.GetActiveSessionUseCase
import app.lucent.firefly.feature.capture.api.CaptureNavKey
import app.lucent.firefly.feature.dashboard.api.DashboardNavKey
import app.lucent.firefly.feature.onboarding.api.OnboardingNavKey
import app.lucent.firefly.navigation.LucentNavGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MainViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private class FakeAuthRepository : AuthRepository {
        val sessionFlow = MutableStateFlow<AuthSession?>(null)
        override val activeSession: Flow<AuthSession?> = sessionFlow

        override suspend fun validateServer(
            baseUrl: String,
            token: String?,
            allowSelfSignedCert: Boolean,
        ): Result<ServerInfo> = Result.success(ServerInfo("6.7.8", "6.7.8"))

        override suspend fun authenticate(
            baseUrl: String,
            token: String,
            allowSelfSignedCert: Boolean,
        ): Result<AuthSession> =
            Result.success(
                AuthSession(
                    serverConfig = ServerConfig(baseUrl),
                    user = AuthUser("2", "test@example.com", "owner"),
                    token = token,
                ),
            )

        override suspend fun logout(): Result<Unit> {
            sessionFlow.value = null
            return Result.success(Unit)
        }
    }

    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var navigator: Navigator
    private lateinit var viewModel: MainViewModel

    private val session =
        AuthSession(
            serverConfig = ServerConfig("https://demo.firefly.org"),
            user = AuthUser("2", "test@example.com", "owner"),
            token = "pat",
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        navigator = Navigator()
        viewModel =
            MainViewModel(
                getActiveSessionUseCase = GetActiveSessionUseCase(fakeRepository),
                navigator = navigator,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsLoadingUntilSessionIsKnown() {
        assertEquals(false, viewModel.uiState.value.isSessionResolved)
    }

    @Test
    fun noSessionOpensAuthGraph() =
        runTest {
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(true, viewModel.uiState.value.isSessionResolved)
            assertEquals(LucentNavGraph.Auth, navigator.currentGraph)
            assertEquals(listOf<NavKey>(OnboardingNavKey), navigator.backStack.toList())
        }

    @Test
    fun loginSwitchesToHomeGraph() =
        runTest {
            testDispatcher.scheduler.advanceUntilIdle()

            fakeRepository.sessionFlow.value = session
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(LucentNavGraph.Home, navigator.currentGraph)
            assertEquals(listOf<NavKey>(DashboardNavKey), navigator.backStack.toList())
        }

    @Test
    fun logoutClearsHomeBackStackAndReturnsToAuthGraph() =
        runTest {
            fakeRepository.sessionFlow.value = session
            testDispatcher.scheduler.advanceUntilIdle()
            navigator.navigate(CaptureNavKey)

            fakeRepository.logout()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(LucentNavGraph.Auth, navigator.currentGraph)
            assertEquals(listOf<NavKey>(OnboardingNavKey), navigator.backStack.toList())
        }

    @Test
    fun sessionRefreshWithinSameGraphKeepsBackStack() =
        runTest {
            fakeRepository.sessionFlow.value = session
            testDispatcher.scheduler.advanceUntilIdle()
            navigator.navigate(CaptureNavKey)

            fakeRepository.sessionFlow.value = session.copy(token = "rotated")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf(DashboardNavKey, CaptureNavKey), navigator.backStack.toList())
        }
}
