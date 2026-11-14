package app.lucent.firefly.core.common

import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.common.result.safeTry
import app.lucent.firefly.core.common.result.safeTryFlat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class SafeTryTest {
    @Test
    fun safeTryReturnsSuccessOnSuccessfulExecution() =
        runTest {
            val result = safeTry { "success data" }

            assertTrue(result is Result.Success)
            assertEquals("success data", (result as Result.Success).data)
        }

    @Test
    fun safeTryFlatReturnsDirectResult() =
        runTest {
            val result = safeTryFlat { Result.success("flat data") }

            assertTrue(result is Result.Success)
            assertEquals("flat data", (result as Result.Success).data)
        }

    @Test
    fun safeTryMapsSocketTimeoutToTimeoutError() =
        runTest {
            val result: Result<String> = safeTry { throw SocketTimeoutException("Read timed out") }

            assertTrue(result is Result.Error)
            assertEquals(AppError.Network.TIMEOUT, (result as Result.Error).error)
        }

    @Test
    fun safeTryMapsUnknownHostToNoInternetError() =
        runTest {
            val result: Result<String> = safeTry { throw UnknownHostException("Unable to resolve host") }

            assertTrue(result is Result.Error)
            assertEquals(AppError.Network.NO_INTERNET, (result as Result.Error).error)
        }

    @Test
    fun safeTryMapsSSLExceptionToSslUntrusted() =
        runTest {
            val result: Result<String> = safeTry { throw SSLException("Cert path untrusted") }

            assertTrue(result is Result.Error)
            assertEquals(AppError.Network.SSL_UNTRUSTED, (result as Result.Error).error)
        }

    @Test
    fun safeTryMapsGenericIOExceptionToUnknownNetwork() =
        runTest {
            val result: Result<String> = safeTry { throw IOException("Connection reset") }

            assertTrue(result is Result.Error)
            assertEquals(AppError.Network.UNKNOWN, (result as Result.Error).error)
        }

    @Test
    fun safeTryMapsGeneralExceptionToCommonError() =
        runTest {
            val result: Result<String> = safeTry { throw IllegalStateException("Database locked") }

            assertTrue(result is Result.Error)
            val error = (result as Result.Error).error
            assertTrue(error is AppError.Common)
            assertEquals("Database locked", error.errorMessage)
        }

    @Test
    fun safeTryRethrowsCancellationException() =
        runTest {
            try {
                safeTry<String> { throw CancellationException("Coroutine cancelled") }
                fail("Expected CancellationException to be rethrown")
            } catch (e: CancellationException) {
                assertEquals("Coroutine cancelled", e.message)
            }
        }
}
