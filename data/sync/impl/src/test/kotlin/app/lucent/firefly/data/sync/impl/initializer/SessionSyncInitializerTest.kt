package app.lucent.firefly.data.sync.impl.initializer

import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.model.AuthUser
import app.lucent.firefly.data.auth.api.model.ServerConfig
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.sync.api.manager.SyncManager
import app.lucent.firefly.data.sync.api.model.SyncState
import app.lucent.firefly.data.sync.impl.processor.SyncStatusStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionSyncInitializerTest {
    private class RecordingSyncManager : SyncManager {
        val calls = mutableListOf<String>()
        override val syncState: StateFlow<SyncState> = MutableStateFlow(SyncState())

        override fun requestSync(expedited: Boolean) {
            calls += "request"
        }

        override fun schedulePeriodicSync(intervalMinutes: Long) {
            calls += "schedule"
        }

        override fun cancelSync() {
            calls += "cancel"
        }

        override suspend fun retryFailedChange(
            id: Long,
            overwriteServer: Boolean,
        ) = Unit

        override suspend fun discardFailedChange(id: Long): Result<Unit> = Result.Success(Unit)
    }

    private val session =
        AuthSession(
            serverConfig = ServerConfig("https://demo.firefly.org"),
            user = AuthUser("2", "u@example.com", "owner"),
            token = "pat",
        )

    @Test
    fun schedulesWhileSignedInAndCancelsOnSignOut() =
        runTest(UnconfinedTestDispatcher()) {
            val sessionFlow = MutableStateFlow<AuthSession?>(session)
            val authRepository = mockk<AuthRepository> { every { activeSession } returns sessionFlow }
            val syncManager = RecordingSyncManager()
            val statusStore = SyncStatusStore()
            val initializer = SessionSyncInitializer(authRepository, syncManager, statusStore, backgroundScope)

            initializer.initialize()
            initializer.initialize()
            sessionFlow.value = session.copy(user = session.user.copy(email = "renamed@example.com"))
            sessionFlow.value = null

            assertEquals(listOf("schedule", "request", "cancel"), syncManager.calls)
        }

    @Test
    fun newTokenClearsTheReauthenticationFlag() =
        runTest(UnconfinedTestDispatcher()) {
            val sessionFlow = MutableStateFlow<AuthSession?>(session)
            val authRepository = mockk<AuthRepository> { every { activeSession } returns sessionFlow }
            val statusStore = SyncStatusStore()
            SessionSyncInitializer(authRepository, RecordingSyncManager(), statusStore, backgroundScope).initialize()

            statusStore.markReauthenticationRequired()
            sessionFlow.value = session.copy(token = "new-token")

            assertEquals(false, statusStore.requiresReauthentication.value)
        }
}
