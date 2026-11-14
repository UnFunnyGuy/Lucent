package app.lucent.firefly.data.auth.impl.datasource

import app.lucent.firefly.core.datastore.LucentPreferencesDataSource
import app.lucent.firefly.core.datastore.model.ServerConnection
import app.lucent.firefly.core.security.Crypto
import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.model.AuthUser
import app.lucent.firefly.data.auth.api.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Singleton

@Singleton
class DefaultAuthLocalDataSource(
    private val preferencesDataSource: LucentPreferencesDataSource,
    private val crypto: Crypto,
) : AuthLocalDataSource {
    override val activeSession: Flow<AuthSession?> =
        preferencesDataSource.connection.map { connection ->
            if (connection == null || connection.encryptedPat.isBlank()) return@map null
            val token = runCatching { crypto.decryptString(connection.encryptedPat) }.getOrDefault("")
            if (token.isBlank()) return@map null
            AuthSession(
                serverConfig =
                    ServerConfig(
                        baseUrl = connection.baseUrl,
                        apiVersion = connection.apiVersion,
                        allowSelfSignedCert = connection.allowSelfSignedCert,
                    ),
                user =
                    AuthUser(
                        id = connection.userId.orEmpty(),
                        email = connection.userEmail.orEmpty(),
                        role = connection.userRole ?: "user",
                    ),
                token = token,
            )
        }

    override suspend fun saveSession(
        baseUrl: String,
        token: String,
        user: AuthUser,
        allowSelfSignedCert: Boolean,
    ) {
        preferencesDataSource.saveConnection(
            ServerConnection(
                baseUrl = baseUrl,
                encryptedPat = crypto.encryptString(token),
                allowSelfSignedCert = allowSelfSignedCert,
                userId = user.id,
                userEmail = user.email,
                userRole = user.role,
            ),
        )
    }

    override suspend fun isCurrentAccount(
        baseUrl: String,
        userId: String,
    ): Boolean {
        val connection = preferencesDataSource.connection.first() ?: return false
        return connection.baseUrl == baseUrl && connection.userId == userId
    }

    override suspend fun clearActiveSession() {
        preferencesDataSource.clearToken()
    }
}
