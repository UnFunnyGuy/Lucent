package app.lucent.firefly.data.auth.impl.repository

import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.common.result.safeTry
import app.lucent.firefly.core.common.result.safeTryFlat
import app.lucent.firefly.core.database.LocalDataEraser
import app.lucent.firefly.core.network.model.ServerCredentials
import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.model.ServerConfig
import app.lucent.firefly.data.auth.api.model.ServerInfo
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.auth.impl.datasource.AuthLocalDataSource
import app.lucent.firefly.data.auth.impl.datasource.AuthRemoteDataSource
import app.lucent.firefly.data.auth.impl.mapper.toDomain
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Singleton

@Singleton
class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val localDataSource: AuthLocalDataSource,
    private val localDataEraser: LocalDataEraser,
) : AuthRepository {

    override val activeSession: Flow<AuthSession?> = localDataSource.activeSession

    override suspend fun validateServer(
        baseUrl: String,
        token: String?,
        allowSelfSignedCert: Boolean,
    ): Result<ServerInfo> = safeTryFlat {
        if (token.isNullOrBlank()) return@safeTryFlat Result.error(AppError.ApiKeyError)
        val credentials = ServerCredentials(normalizeUrl(baseUrl), token, allowSelfSignedCert)
        when (val result = remoteDataSource.getAbout(credentials)) {
            is Result.Success -> Result.success(result.data.toDomain())
            is Result.Error -> Result.error(result.error)
        }
    }

    override suspend fun authenticate(
        baseUrl: String,
        token: String,
        allowSelfSignedCert: Boolean,
    ): Result<AuthSession> = safeTryFlat {
        val cleanUrl = normalizeUrl(baseUrl)
        when (val result = remoteDataSource.getCurrentUser(ServerCredentials(cleanUrl, token, allowSelfSignedCert))) {
            is Result.Success -> {
                val user = result.data.toDomain()
                if (user.isBlocked) {
                    return@safeTryFlat Result.error(
                        AppError.Common("This account is blocked on the Firefly III server."),
                    )
                }

                // The same user on the same server keeps local data and unsent changes; anything else
                // would mix two accounts' data, so it starts from an empty database.
                if (!localDataSource.isCurrentAccount(cleanUrl, user.id)) {
                    localDataEraser.eraseAll()
                }
                localDataSource.saveSession(
                    baseUrl = cleanUrl,
                    token = token,
                    user = user,
                    allowSelfSignedCert = allowSelfSignedCert,
                )

                Result.success(
                    AuthSession(
                        serverConfig = ServerConfig(
                            baseUrl = cleanUrl,
                            allowSelfSignedCert = allowSelfSignedCert,
                        ),
                        user = user,
                        token = token,
                    ),
                )
            }
            is Result.Error -> Result.error(result.error)
        }
    }

    override suspend fun logout(): Result<Unit> = safeTry {
        localDataSource.clearActiveSession()
    }

    private fun normalizeUrl(url: String): String {
        var clean = url.trim().trimEnd('/')
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        if (clean.endsWith("/api/v1")) {
            clean = clean.removeSuffix("/api/v1").trimEnd('/')
        } else if (clean.endsWith("/api")) {
            clean = clean.removeSuffix("/api").trimEnd('/')
        }
        return clean
    }
}
