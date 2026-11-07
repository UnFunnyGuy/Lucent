package app.lucent.firefly.core.network

import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.network.client.FireflyJson
import app.lucent.firefly.core.network.client.MissingCredentialsException
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Executes a network [call] and maps the response or any thrown exception to a typed [Result].
 */
suspend inline fun <reified T> safeApiCall(
    crossinline call: suspend () -> HttpResponse,
): Result<T> = try {
    val response = call()
    if (response.status.isSuccess()) {
        Result.success(response.body<T>())
    } else {
        Result.error(mapHttpError(response.status.value, response.bodyAsText()))
    }
} catch (e: CancellationException) {
    throw e
} catch (e: HttpRequestTimeoutException) {
    Result.error(AppError.Network.TIMEOUT)
} catch (e: SocketTimeoutException) {
    Result.error(AppError.Network.TIMEOUT)
} catch (e: UnknownHostException) {
    Result.error(AppError.Network.NO_INTERNET)
} catch (e: ConnectException) {
    Result.error(AppError.Network.SERVER_UNREACHABLE)
} catch (e: SSLException) {
    Result.error(AppError.Network.SSL_UNTRUSTED)
} catch (e: MissingCredentialsException) {
    Result.error(AppError.ApiKeyError)
} catch (e: ContentConvertException) {
    Result.error(AppError.Network.SERIALIZATION)
} catch (e: SerializationException) {
    Result.error(AppError.Network.SERIALIZATION)
} catch (e: NoTransformationFoundException) {
    Result.error(AppError.Network.SERIALIZATION)
} catch (e: IOException) {
    Result.error(AppError.Network.UNKNOWN)
} catch (e: Throwable) {
    currentCoroutineContext().ensureActive()
    Result.error(AppError.Common(e.message ?: "Unexpected network error", e))
}

fun mapHttpError(statusCode: Int, body: String?): AppError = when (statusCode) {
    401 -> AppError.ApiKeyError
    in 500..599 -> AppError.Network.SERVER_ERROR
    else -> AppError.ApiError(
        message = parseFireflyErrorMessage(body) ?: "HTTP Error $statusCode",
        code = statusCode,
        errorBody = body,
    )
}

/**
 * Extracts a readable message from Firefly III error bodies:
 * `{ "message": "...", "errors": { "field": ["..."] } }` (validation errors use `errors`).
 */
fun parseFireflyErrorMessage(body: String?): String? {
    if (body.isNullOrBlank()) return null
    val root = runCatching { FireflyJson.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return null
    val fieldErrors = (root["errors"] as? JsonObject)?.values
        ?.flatMap { value -> (value as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty() }
        .orEmpty()
    if (fieldErrors.isNotEmpty()) return fieldErrors.distinct().joinToString("\n")
    return (root["message"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
}
