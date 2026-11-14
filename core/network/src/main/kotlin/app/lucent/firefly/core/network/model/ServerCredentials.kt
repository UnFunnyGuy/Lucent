package app.lucent.firefly.core.network.model

data class ServerCredentials(
    val baseUrl: String,
    val token: String,
    val allowSelfSignedCert: Boolean = false,
) {
    override fun toString(): String = "ServerCredentials(baseUrl=$baseUrl, token=***, allowSelfSignedCert=$allowSelfSignedCert)"
}
