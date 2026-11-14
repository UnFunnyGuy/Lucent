package app.lucent.firefly.data.auth.impl.repository

import app.cash.turbine.test
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.LocalDataEraser
import app.lucent.firefly.core.network.model.ServerCredentials
import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.model.AuthUser
import app.lucent.firefly.data.auth.api.model.ServerConfig
import app.lucent.firefly.data.auth.impl.datasource.AuthLocalDataSource
import app.lucent.firefly.data.auth.impl.datasource.AuthRemoteDataSource
import app.lucent.firefly.data.auth.impl.dto.SystemInfoDataDto
import app.lucent.firefly.data.auth.impl.dto.SystemInfoDto
import app.lucent.firefly.data.auth.impl.dto.UserAttributesDto
import app.lucent.firefly.data.auth.impl.dto.UserReadDto
import app.lucent.firefly.data.auth.impl.dto.UserSingleDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {
    private class FakeRemoteDataSource : AuthRemoteDataSource {
        var aboutResult: Result<SystemInfoDto> =
            Result.success(
                SystemInfoDto(
                    data =
                        SystemInfoDataDto(
                            version = "6.7.8",
                            apiVersion = "6.7.8",
                            phpVersion = "8.1.0",
                            os = "Linux",
                        ),
                ),
            )

        var userResult: Result<UserSingleDto> =
            Result.success(
                UserSingleDto(
                    data =
                        UserReadDto(
                            id = "2",
                            attributes =
                                UserAttributesDto(
                                    email = "test@example.com",
                                    blocked = false,
                                    role = "owner",
                                ),
                        ),
                ),
            )

        override suspend fun getAbout(credentials: ServerCredentials): Result<SystemInfoDto> = aboutResult

        override suspend fun getCurrentUser(credentials: ServerCredentials): Result<UserSingleDto> = userResult
    }

    private class FakeLocalDataSource : AuthLocalDataSource {
        private val _session = MutableStateFlow<AuthSession?>(null)
        override val activeSession: Flow<AuthSession?> = _session
        private var storedAccount: Pair<String, String>? = null

        override suspend fun saveSession(
            baseUrl: String,
            token: String,
            user: AuthUser,
            allowSelfSignedCert: Boolean,
        ) {
            storedAccount = baseUrl to user.id
            _session.value =
                AuthSession(
                    serverConfig = ServerConfig(baseUrl, allowSelfSignedCert = allowSelfSignedCert),
                    user = user,
                    token = token,
                )
        }

        override suspend fun isCurrentAccount(
            baseUrl: String,
            userId: String,
        ): Boolean = storedAccount == (baseUrl to userId)

        override suspend fun clearActiveSession() {
            _session.value = null
        }
    }

    private class CountingEraser : LocalDataEraser {
        var erasures = 0

        override suspend fun eraseAll() {
            erasures++
        }
    }

    private fun repository(
        remote: FakeRemoteDataSource = FakeRemoteDataSource(),
        local: FakeLocalDataSource = FakeLocalDataSource(),
        eraser: CountingEraser = CountingEraser(),
    ) = AuthRepositoryImpl(remote, local, eraser)

    @Test
    fun validateServer_returnsSuccess_withParsedServerInfo() =
        runTest {
            val result = repository().validateServer("https://demo.firefly-iii.org", token = "pat")

            val info = (result as Result.Success).data
            assertEquals("6.7.8", info.version)
            assertEquals("Linux", info.os)
        }

    @Test
    fun authenticate_savesSessionAndEmitsActiveSession() =
        runTest {
            val repository = repository()

            repository.activeSession.test {
                assertEquals(null, awaitItem())

                val result = repository.authenticate(baseUrl = "https://demo.firefly-iii.org", token = "valid-token")
                assertTrue(result is Result.Success)

                val session = awaitItem()
                assertEquals("test@example.com", session?.user?.email)
                assertEquals("valid-token", session?.token)

                repository.logout()
                assertEquals(null, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun signingBackInAsTheSameUserKeepsLocalData() =
        runTest {
            val eraser = CountingEraser()
            val repository = repository(eraser = eraser)

            repository.authenticate("https://demo.firefly-iii.org", "token-1")
            val erasuresAfterFirstSignIn = eraser.erasures
            repository.logout()
            repository.authenticate("https://demo.firefly-iii.org/", "token-2")

            assertEquals(erasuresAfterFirstSignIn, eraser.erasures)
        }

    @Test
    fun signingInToAnotherServerErasesLocalData() =
        runTest {
            val eraser = CountingEraser()
            val repository = repository(eraser = eraser)
            repository.authenticate("https://demo.firefly-iii.org", "token-1")
            val before = eraser.erasures

            repository.authenticate("https://other.example.com", "token-2")

            assertEquals(before + 1, eraser.erasures)
        }

    @Test
    fun authenticate_rejectsBlockedUserWithoutTouchingLocalData() =
        runTest {
            val remote =
                FakeRemoteDataSource().apply {
                    userResult =
                        Result.success(
                            UserSingleDto(
                                data =
                                    UserReadDto(
                                        id = "3",
                                        attributes = UserAttributesDto(email = "blocked@example.com", blocked = true, role = "demo"),
                                    ),
                            ),
                        )
                }
            val eraser = CountingEraser()

            val result = repository(remote = remote, eraser = eraser).authenticate("https://demo.firefly-iii.org", "token")

            assertTrue(result is Result.Error)
            assertEquals(0, eraser.erasures)
        }
}
