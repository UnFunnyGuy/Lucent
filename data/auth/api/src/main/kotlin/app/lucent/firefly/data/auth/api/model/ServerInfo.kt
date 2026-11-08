package app.lucent.firefly.data.auth.api.model

/**
 * System and version information returned by Firefly III `/v1/about`.
 *
 * @property version Software version of the Firefly III server (e.g. "6.7.8").
 * @property apiVersion API version supported by the server.
 * @property phpVersion PHP runtime version on the server.
 * @property os Operating system of the server host.
 */
data class ServerInfo(
    val version: String,
    val apiVersion: String,
    val phpVersion: String = "",
    val os: String = "",
)
