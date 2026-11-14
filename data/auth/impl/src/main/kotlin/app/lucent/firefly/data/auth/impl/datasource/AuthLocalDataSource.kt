package app.lucent.firefly.data.auth.impl.datasource

import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthLocalDataSource {
    /** The signed-in session, or `null` when signed out. */
    val activeSession: Flow<AuthSession?>

    /** Encrypts [token] and stores the connection, replacing any previous one. */
    suspend fun saveSession(
        baseUrl: String,
        token: String,
        user: AuthUser,
        allowSelfSignedCert: Boolean,
    )

    /** Whether the stored connection (signed in or not) belongs to [userId] on [baseUrl]. */
    suspend fun isCurrentAccount(
        baseUrl: String,
        userId: String,
    ): Boolean

    /** Signs out, keeping the server and user so the same account can sign back in to its data. */
    suspend fun clearActiveSession()
}
