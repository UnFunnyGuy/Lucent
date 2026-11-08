package app.lucent.firefly.data.auth.api.model

/**
 * Authenticated user profile information returned by Firefly III `/v1/about/user`.
 *
 * @property id The Firefly III user ID (e.g. "2").
 * @property email The email address of the authenticated user.
 * @property role User role on the instance (e.g. "owner", "demo").
 * @property isBlocked Whether the user account has been disabled on the server.
 */
data class AuthUser(
    val id: String,
    val email: String,
    val role: String,
    val isBlocked: Boolean = false,
)
