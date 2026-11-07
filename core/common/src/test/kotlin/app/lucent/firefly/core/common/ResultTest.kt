package app.lucent.firefly.core.common

import app.cash.turbine.test
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.common.result.asResult
import app.lucent.firefly.core.common.result.flatMap
import app.lucent.firefly.core.common.result.fold
import app.lucent.firefly.core.common.result.getOrDefault
import app.lucent.firefly.core.common.result.getOrElse
import app.lucent.firefly.core.common.result.getOrNull
import app.lucent.firefly.core.common.result.isFailure
import app.lucent.firefly.core.common.result.isSuccess
import app.lucent.firefly.core.common.result.map
import app.lucent.firefly.core.common.result.mapError
import app.lucent.firefly.core.common.result.onFailure
import app.lucent.firefly.core.common.result.onSuccess
import app.lucent.firefly.core.common.result.recover
import app.lucent.firefly.core.common.result.toEmptyResult
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun successCreationAndAccessors() {
        val result: Result<String> = Result.success("test data")

        assertTrue(result.isSuccess())
        assertFalse(result.isFailure())
        assertEquals("test data", result.getOrNull())
        assertEquals("test data", result.getOrDefault("default"))
        assertEquals("test data", result.getOrElse { "default" })
    }

    @Test
    fun errorCreationAndAccessors() {
        val error = AppError.Network.NO_INTERNET
        val result: Result<String> = Result.error(error)

        assertTrue(result.isFailure())
        assertFalse(result.isSuccess())
        assertNull(result.getOrNull())
        assertEquals("default", result.getOrDefault("default"))
        assertEquals("fallback", result.getOrElse { "fallback" })
        assertEquals("fallback from error: NO_INTERNET", result.getOrElse { err -> "fallback from error: ${err.errorMessage}" })
    }

    @Test
    fun mapTransformsSuccessData() {
        val result: Result<Int> = Result.success(5)
        val mapped = result.map { it * 2 }

        assertTrue(mapped is Result.Success)
        assertEquals(10, (mapped as Result.Success).data)
    }

    @Test
    fun mapPreservesError() {
        val error = AppError.Common("failure")
        val result: Result<Int> = Result.error(error)
        val mapped = result.map { it * 2 }

        assertTrue(mapped is Result.Error)
        assertEquals(error, (mapped as Result.Error).error)
    }

    @Test
    fun flatMapChainsSuccess() {
        val result: Result<Int> = Result.success(5)
        val chained = result.flatMap { Result.success("$it items") }

        assertTrue(chained is Result.Success)
        assertEquals("5 items", (chained as Result.Success).data)
    }

    @Test
    fun flatMapPreservesOriginalError() {
        val error = AppError.ApiKeyError
        val result: Result<Int> = Result.error(error)
        val chained = result.flatMap { Result.success("$it items") }

        assertTrue(chained is Result.Error)
        assertEquals(error, (chained as Result.Error).error)
    }

    @Test
    fun callbacksTriggerAppropriately() {
        var successData: String? = null
        var failureError: AppError? = null

        Result.success("ok")
            .onSuccess { successData = it }
            .onFailure { failureError = it }

        assertEquals("ok", successData)
        assertNull(failureError)

        val err = AppError.Database("disk full")
        Result.error(err)
            .onSuccess { successData = "changed" }
            .onFailure { failureError = it }

        assertEquals("ok", successData)
        assertEquals(err, failureError)
    }

    @Test
    fun foldHandlesBothBranches() {
        val successResult: Result<Int> = Result.success(42)
        val successFolded = successResult.fold(
            onSuccess = { "Value: $it" },
            onFailure = { "Error: ${it.errorMessage}" },
        )
        assertEquals("Value: 42", successFolded)

        val errorResult: Result<Int> = Result.error(AppError.ApiKeyError)
        val errorFolded = errorResult.fold(
            onSuccess = { "Value: $it" },
            onFailure = { "Error: ${it.errorMessage}" },
        )
        assertEquals("Error: API key error", errorFolded)
    }

    @Test
    fun toEmptyResultReturnsUnitPayload() {
        val result: Result<String> = Result.success("ignored")
        val emptyResult = result.toEmptyResult()

        assertTrue(emptyResult is Result.Success)
        assertEquals(Unit, (emptyResult as Result.Success).data)
    }

    @Test
    fun mapErrorTransformsError() {
        val original = Result.error(AppError.Network.TIMEOUT)
        val mapped = original.mapError { AppError.Common("Timeout occurred") }

        assertTrue(mapped is Result.Error)
        assertEquals(AppError.Common("Timeout occurred"), (mapped as Result.Error).error)
    }

    @Test
    fun recoverProvidesFallbackValue() {
        val errorResult: Result<String> = Result.error(AppError.Network.SERVER_ERROR)
        val recovered = errorResult.recover { "recovered value" }

        assertTrue(recovered is Result.Success)
        assertEquals("recovered value", (recovered as Result.Success).data)
    }

    @Test
    fun appErrorHelperPropertiesWorkCorrectly() {
        val noInternet = AppError.Network.NO_INTERNET
        assertTrue(noInternet.isNoNetwork)
        assertFalse(noInternet.unAuthorized)
        assertEquals("NO_INTERNET", noInternet.errorMessage)

        val apiKeyError = AppError.ApiKeyError
        assertTrue(apiKeyError.unAuthorized)
        assertFalse(apiKeyError.isNoNetwork)
        assertEquals("API key error", apiKeyError.errorMessage)

        val api401 = AppError.ApiError(message = "Unauthorized call", code = 401)
        assertTrue(api401.unAuthorized)
        assertEquals("Unauthorized call", api401.errorMessage)

        val dbError = AppError.Database("SQLite table not found")
        assertEquals("SQLite table not found", dbError.errorMessage)
    }

    @Test
    fun asResultEmitsSuccess() = runTest {
        val flow = flowOf("hello")

        flow.asResult().test {
            assertEquals(Result.Success("hello"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun asResultEmitsErrorOnFailure() = runTest {
        val flow = flow<String> {
            throw IllegalStateException("Test stream failure")
        }

        flow.asResult().test {
            val errorItem = awaitItem()
            assertTrue(errorItem is Result.Error)
            val appError = (errorItem as Result.Error).error
            assertTrue(appError is AppError.Common)
            assertEquals("Test stream failure", appError.errorMessage)
            awaitComplete()
        }
    }
}
