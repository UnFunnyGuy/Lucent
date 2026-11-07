package app.lucent.firefly.core.network.client

import app.lucent.firefly.core.network.model.ServerCredentials

fun interface ServerCredentialsProvider {
    suspend fun currentCredentials(): ServerCredentials?
}

class MissingCredentialsException : IllegalStateException("No active Firefly III session")
