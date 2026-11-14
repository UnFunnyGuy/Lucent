package app.lucent.firefly.data.auth.api.repository

import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.model.ServerInfo
import kotlinx.coroutines.flow.Flow

/**
 * Public domain contract for server discovery, authentication, and session management.
 */
interface AuthRepository {
    /**
     * Continuous reactive stream of the currently active authentication session.
     * Emits `null` if the app is unauthenticated or credentials have been cleared.
     */
    val activeSession: Flow<AuthSession?>

    /**
     * Probes [baseUrl] by querying `/api/v1/about` to check server reachability and version.
     *
     * @param baseUrl Target server URL (e.g. "https://firefly.example.com").
     * @param token Optional Personal Access Token; required by Firefly III v6+ for `/api/v1/about`.
     * @param allowSelfSignedCert Whether to trust custom homelab TLS certificates.
     */
    suspend fun validateServer(
        baseUrl: String,
        token: String? = null,
        allowSelfSignedCert: Boolean = false,
    ): Result<ServerInfo>

    /**
     * Verifies [token] against the Firefly III `/api/v1/about/user` endpoint.
     *
     * On success the token is stored encrypted and [activeSession] emits the new session. Local
     * data is kept when the same user signs in to the same server again and erased otherwise.
     *
     * @param baseUrl Verified base URL of the instance.
     * @param token Personal Access Token (PAT).
     * @param allowSelfSignedCert Whether to trust custom homelab TLS certificates.
     */
    suspend fun authenticate(
        baseUrl: String,
        token: String,
        allowSelfSignedCert: Boolean = false,
    ): Result<AuthSession>

    /**
     * Terminates the active session by clearing sensitive credentials from disk.
     */
    suspend fun logout(): Result<Unit>
}
