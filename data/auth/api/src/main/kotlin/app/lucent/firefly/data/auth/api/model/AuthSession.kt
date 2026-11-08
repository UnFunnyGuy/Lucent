package app.lucent.firefly.data.auth.api.model

/**
 * The signed-in Firefly III account.
 *
 * @property token The decrypted Personal Access Token for making authenticated requests.
 */
data class AuthSession(
    val serverConfig: ServerConfig,
    val user: AuthUser,
    val token: String,
)
