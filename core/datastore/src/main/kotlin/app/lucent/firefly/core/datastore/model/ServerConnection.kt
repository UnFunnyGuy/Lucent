package app.lucent.firefly.core.datastore.model

import kotlinx.serialization.Serializable

/**
 * The Firefly III server the app is connected to.
 *
 * @property encryptedPat Personal Access Token encrypted with the Keystore key; empty after sign-out.
 * @property userId Firefly user id, used to tell whether a later sign-in is the same account.
 */
@Serializable
data class ServerConnection(
    val baseUrl: String,
    val apiVersion: String = "v1",
    val encryptedPat: String,
    val allowSelfSignedCert: Boolean = false,
    val userId: String? = null,
    val userEmail: String? = null,
    val userRole: String? = null,
)
