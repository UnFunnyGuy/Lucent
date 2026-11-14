package app.lucent.firefly.core.common.result

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Safely executes [block], preserving coroutine cancellation while catching errors and mapping them
 * to a typed [Result].
 */
suspend inline fun <T> safeTry(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        currentCoroutineContext().ensureActive()
        Result.Error(e.toAppError())
    }

/**
 * Executes a suspend block that already produces a [Result], catching unexpected exceptions and
 * preserving coroutine cancellation.
 */
suspend inline fun <T> safeTryFlat(crossinline block: suspend () -> Result<T>): Result<T> =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        currentCoroutineContext().ensureActive()
        Result.Error(e.toAppError())
    }

/** Converts a [Throwable] into an [AppError]. */
fun Throwable.toAppError(): AppError =
    when (this) {
        is SocketTimeoutException -> AppError.Network.TIMEOUT
        is UnknownHostException -> AppError.Network.NO_INTERNET
        is SSLException -> AppError.Network.SSL_UNTRUSTED
        is IOException -> AppError.Network.UNKNOWN
        else -> AppError.Common(message ?: "Unexpected error occurred", this)
    }
