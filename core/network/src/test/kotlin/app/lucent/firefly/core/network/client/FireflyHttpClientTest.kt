package app.lucent.firefly.core.network.client

import app.lucent.firefly.core.network.ApiVersion
import app.lucent.firefly.core.network.model.ServerCredentials
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FireflyHttpClientTest {
    @Serializable
    private data class Body(
        val description: String,
        val budgetId: String? = null,
    )

    private val requests = mutableListOf<HttpRequestData>()

    private fun client(baseUrl: String): HttpClient {
        val engine =
            MockEngine { request ->
                requests += request
                ok()
            }
        return HttpClient(engine) {
            installFirefly(ServerCredentials(baseUrl = baseUrl, token = "pat-123"))
        }
    }

    private fun MockRequestHandleScope.ok(): HttpResponseData = respond("{}", headers = headersOf(HttpHeaders.ContentType, "application/vnd.api+json"))

    @Test
    fun relativePathIsResolvedAgainstBaseUrl() =
        runTest {
            client("https://firefly.example.com").get(ApiVersion.V1("accounts")) { parameter("page", 2) }

            assertEquals("https://firefly.example.com/api/v1/accounts?page=2", requests.single().url.toString())
        }

    @Test
    fun subPathInstallationIsPreserved() =
        runTest {
            client("https://home.example.com/firefly/").get(ApiVersion.V1("transactions/42"))

            assertEquals("https://home.example.com/firefly/api/v1/transactions/42", requests.single().url.toString())
        }

    @Test
    fun localhostServerIsNotRewritten() =
        runTest {
            client("http://localhost:8080").get(ApiVersion.V1("about"))

            assertEquals("http://localhost:8080/api/v1/about", requests.single().url.toString())
        }

    @Test
    fun bearerTokenAndAcceptHeadersAreSent() =
        runTest {
            client("https://firefly.example.com").get(ApiVersion.V1("about/user"))

            val headers = requests.single().headers
            assertEquals("Bearer pat-123", headers[HttpHeaders.Authorization])
            val accept = headers.getAll(HttpHeaders.Accept).orEmpty().joinToString()
            assertTrue(accept.contains("application/json"))
            assertTrue(accept.contains("application/vnd.api+json"))
        }

    @Test
    fun postBodyIsJsonWithoutNullFields() =
        runTest {
            client("https://firefly.example.com").post(ApiVersion.V1("transactions")) {
                contentType(ContentType.Application.Json)
                setBody(Body(description = "Coffee"))
            }

            val body = requests.single().body as OutgoingContent.ByteArrayContent
            assertEquals(ContentType.Application.Json, body.contentType?.withoutParameters())
            val json = body.bytes().decodeToString()
            assertEquals("""{"description":"Coffee"}""", json)
            assertFalse(json.contains("null"))
        }
}
