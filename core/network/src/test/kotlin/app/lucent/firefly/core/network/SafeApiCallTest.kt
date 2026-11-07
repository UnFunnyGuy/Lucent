package app.lucent.firefly.core.network

import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.network.client.FireflyJson
import app.lucent.firefly.core.network.client.MissingCredentialsException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

@Serializable
private data class TestDto(val id: Int, val name: String)

class SafeApiCallTest {

    @Test
    fun safeApiCallReturnsSuccessOn200() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"id":1,"name":"Checking Account"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(FireflyJson) }
        }

        val result: Result<TestDto> = safeApiCall { client.get("https://example.com/api/test") }

        assertTrue(result is Result.Success)
        assertEquals(TestDto(1, "Checking Account"), (result as Result.Success).data)
    }

    @Test
    fun safeApiCallMaps401ToApiKeyError() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"message":"Unauthenticated."}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(FireflyJson) }
        }

        val result: Result<TestDto> = safeApiCall { client.get("https://example.com/api/test") }

        assertTrue(result is Result.Error)
        assertEquals(AppError.ApiKeyError, (result as Result.Error).error)
    }

    @Test
    fun safeApiCallMaps500ToServerError() = runTest {
        val mockEngine = MockEngine { _ ->
            respondError(HttpStatusCode.InternalServerError, "Internal Server Error")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(FireflyJson) }
        }

        val result: Result<TestDto> = safeApiCall { client.get("https://example.com/api/test") }

        assertTrue(result is Result.Error)
        assertEquals(AppError.Network.SERVER_ERROR, (result as Result.Error).error)
    }

    @Test
    fun safeApiCallMaps404ToApiError() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "Resource not found",
                status = HttpStatusCode.NotFound,
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(FireflyJson) }
        }

        val result: Result<TestDto> = safeApiCall { client.get("https://example.com/api/test") }

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.ApiError)
        assertEquals(404, (error as AppError.ApiError).code)
        assertEquals("Resource not found", error.errorBody)
    }

    @Test
    fun safeApiCallMapsTimeoutException() = runTest {
        val result: Result<TestDto> = safeApiCall {
            throw SocketTimeoutException("Read timed out")
        }

        assertTrue(result is Result.Error)
        assertEquals(AppError.Network.TIMEOUT, (result as Result.Error).error)
    }

    @Test
    fun safeApiCallMapsUnknownHostExceptionToNoInternet() = runTest {
        val result: Result<TestDto> = safeApiCall {
            throw UnknownHostException("No address associated with hostname")
        }

        assertTrue(result is Result.Error)
        assertEquals(AppError.Network.NO_INTERNET, (result as Result.Error).error)
    }

    @Test
    fun safeApiCallMapsSSLExceptionToSslUntrusted() = runTest {
        val result: Result<TestDto> = safeApiCall {
            throw SSLException("Certificate untrusted")
        }

        assertTrue(result is Result.Error)
        assertEquals(AppError.Network.SSL_UNTRUSTED, (result as Result.Error).error)
    }

    @Test
    fun safeApiCallMapsMissingTokenToApiKeyError() = runTest {
        val result: Result<TestDto> = safeApiCall {
            throw MissingCredentialsException()
        }

        assertTrue(result is Result.Error)
        assertEquals(AppError.ApiKeyError, (result as Result.Error).error)
    }

    @Test
    fun safeApiCallRethrowsCancellationException() = runTest {
        try {
            safeApiCall<TestDto> {
                throw CancellationException("Cancelled")
            }
            fail("Expected CancellationException to be rethrown")
        } catch (e: CancellationException) {
            assertEquals("Cancelled", e.message)
        }
    }

    @Test
    fun safeApiCallMaps422ValidationErrorsToReadableMessage() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"message":"The given data was invalid.","errors":{"transactions.0.amount":["The amount must be more than zero."],"transactions.0.description":["The description field is required."]}}""",
                status = HttpStatusCode.UnprocessableEntity,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(FireflyJson) }
        }

        val result: Result<TestDto> = safeApiCall { client.get("https://example.com/api/test") }

        val error = (result as Result.Error).error as AppError.ApiError
        assertEquals(422, error.code)
        assertEquals(
            "The amount must be more than zero.\nThe description field is required.",
            error.message,
        )
    }

    @Test
    fun safeApiCallUsesFireflyMessageForOtherClientErrors() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"message":"Resource not found","exception":"NotFoundHttpException"}""",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(FireflyJson) }
        }

        val result: Result<TestDto> = safeApiCall { client.get("https://example.com/api/test") }

        assertEquals("Resource not found", ((result as Result.Error).error as AppError.ApiError).message)
    }

    @Test
    fun safeApiCallMaps403ToApiErrorNotApiKeyError() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(content = """{"message":"Forbidden"}""", status = HttpStatusCode.Forbidden)
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(FireflyJson) }
        }

        val result: Result<TestDto> = safeApiCall { client.get("https://example.com/api/test") }

        assertEquals(403, ((result as Result.Error).error as AppError.ApiError).code)
    }

    @Test
    fun safeApiCallMapsMalformedSuccessBodyToSerializationError() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"id":"not-a-number"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/vnd.api+json"),
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(FireflyJson)
                json(FireflyJson, app.lucent.firefly.core.network.client.JsonApiContentType)
            }
        }

        val result: Result<TestDto> = safeApiCall { client.get("https://example.com/api/test") }

        assertEquals(AppError.Network.SERIALIZATION, (result as Result.Error).error)
    }

    @Test
    fun safeApiCallMapsConnectionRefusedToServerUnreachable() = runTest {
        val result: Result<TestDto> = safeApiCall { throw ConnectException("Connection refused") }

        assertEquals(AppError.Network.SERVER_UNREACHABLE, (result as Result.Error).error)
    }
}
