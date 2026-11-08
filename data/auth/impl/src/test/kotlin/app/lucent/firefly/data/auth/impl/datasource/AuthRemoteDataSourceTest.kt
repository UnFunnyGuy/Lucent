package app.lucent.firefly.data.auth.impl.datasource

import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.network.client.FireflyClientProvider
import app.lucent.firefly.core.network.client.installFirefly
import app.lucent.firefly.core.network.model.ServerCredentials
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRemoteDataSourceTest {

    private val credentials = ServerCredentials(baseUrl = "https://firefly.example.com", token = "valid-pat")
    private val requests = mutableListOf<HttpRequestData>()

    private fun dataSource(status: HttpStatusCode, body: String): DefaultAuthRemoteDataSource {
        val provider = object : FireflyClientProvider {
            override suspend fun authenticated(): HttpClient = error("not used before login")
            override fun forServer(credentials: ServerCredentials): HttpClient =
                HttpClient(
                    MockEngine { request ->
                        requests += request
                        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                    },
                ) { installFirefly(credentials) }
        }
        return DefaultAuthRemoteDataSource(provider)
    }

    @Test
    fun getAboutCallsAboutEndpointWithToken() = runTest {
        val result = dataSource(
            HttpStatusCode.OK,
            """{"data":{"version":"6.7.8","api_version":"6.7.8","php_version":"8.3.0","os":"Linux","driver":"pgsql"}}""",
        ).getAbout(credentials)

        assertEquals("6.7.8", (result as Result.Success).data.data.version)
        val request = requests.single()
        assertEquals("https://firefly.example.com/api/v1/about", request.url.toString())
        assertEquals("Bearer valid-pat", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun getCurrentUserParsesUser() = runTest {
        val result = dataSource(
            HttpStatusCode.OK,
            """{"data":{"type":"users","id":"2","attributes":{"email":"user@example.com","blocked":false,"role":"owner"}}}""",
        ).getCurrentUser(credentials)

        val user = (result as Result.Success).data.data
        assertEquals("2", user.id)
        assertEquals("owner", user.attributes.role)
        assertEquals("https://firefly.example.com/api/v1/about/user", requests.single().url.toString())
    }

    @Test
    fun getCurrentUserAcceptsNullRole() = runTest {
        val result = dataSource(
            HttpStatusCode.OK,
            """{"data":{"type":"users","id":"3","attributes":{"email":"u@example.com","blocked":false,"role":null}}}""",
        ).getCurrentUser(credentials)

        assertNull((result as Result.Success).data.data.attributes.role)
    }

    @Test
    fun getCurrentUserMaps401ToApiKeyError() = runTest {
        val result = dataSource(HttpStatusCode.Unauthorized, """{"message":"Unauthenticated."}""")
            .getCurrentUser(credentials)

        assertTrue(result is Result.Error)
        assertEquals(AppError.ApiKeyError, (result as Result.Error).error)
    }
}
