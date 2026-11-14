package app.lucent.firefly.core.network.client

import android.util.Log
import app.lucent.firefly.core.network.model.ServerCredentials
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Firefly III answers collection and item endpoints with JSON:API documents. */
val JsonApiContentType: ContentType = ContentType("application", "vnd.api+json")

val FireflyJson: Json =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

/**
 * Configures a client bound to one Firefly III server. Request bodies are sent as
 * `application/json`; responses are accepted as `application/vnd.api+json` or `application/json`
 * (ContentNegotiation advertises both in the `Accept` header).
 */
fun HttpClientConfig<*>.installFirefly(
    credentials: ServerCredentials,
    json: Json = FireflyJson,
    logBodies: Boolean = false,
) {
    expectSuccess = false

    defaultRequest {
        url(credentials.baseUrl.trimEnd('/') + "/")
        bearerAuth(credentials.token)
    }

    install(ContentNegotiation) {
        json(json, ContentType.Application.Json)
        json(json, JsonApiContentType)
    }

    install(HttpTimeout) {
        connectTimeoutMillis = 15_000L
        socketTimeoutMillis = 30_000L
        requestTimeoutMillis = 60_000L
    }

    if (logBodies) {
        install(Logging) {
            logger =
                object : Logger {
                    override fun log(message: String) {
                        Log.d("LucentNetwork", message)
                    }
                }
            level = LogLevel.BODY
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
    }
}
