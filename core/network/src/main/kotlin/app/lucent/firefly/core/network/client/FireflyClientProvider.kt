package app.lucent.firefly.core.network.client

import app.lucent.firefly.core.network.BuildConfig
import app.lucent.firefly.core.network.model.ServerCredentials
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Singleton

interface FireflyClientProvider {

    /** Client for the active session. Throws [MissingCredentialsException] when signed out. */
    suspend fun authenticated(): HttpClient

    /** Short-lived client for a server that is not the active session yet. The caller closes it. */
    fun forServer(credentials: ServerCredentials): HttpClient
}

@Singleton
class DefaultFireflyClientProvider(
    private val credentialsProvider: ServerCredentialsProvider,
    private val json: Json,
) : FireflyClientProvider {

    private val mutex = Mutex()
    private var cached: Pair<ServerCredentials, HttpClient>? = null

    override suspend fun authenticated(): HttpClient {
        val credentials = credentialsProvider.currentCredentials() ?: throw MissingCredentialsException()
        return mutex.withLock {
            cached?.takeIf { it.first == credentials }?.second
                ?: forServer(credentials).also { client ->
                    // close() completes the client job: in-flight calls finish, new calls use the new client.
                    cached?.second?.close()
                    cached = credentials to client
                }
        }
    }

    override fun forServer(credentials: ServerCredentials): HttpClient = HttpClient(OkHttp) {
        installFirefly(credentials, json, logBodies = BuildConfig.DEBUG)
    }
}
