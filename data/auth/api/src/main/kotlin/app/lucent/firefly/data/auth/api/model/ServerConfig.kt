package app.lucent.firefly.data.auth.api.model

/**
 * Server connection configuration for Firefly III.
 *
 * @property baseUrl The root URL of the instance (e.g. `https://firefly.example.com`).
 * @property apiVersion Firefly III API version (defaults to "v1").
 * @property allowSelfSignedCert Whether custom/self-signed homelab TLS certificates are trusted.
 */
data class ServerConfig(
    val baseUrl: String,
    val apiVersion: String = "v1",
    val allowSelfSignedCert: Boolean = false,
)
