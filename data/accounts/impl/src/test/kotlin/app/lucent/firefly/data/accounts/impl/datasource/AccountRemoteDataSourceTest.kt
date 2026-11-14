package app.lucent.firefly.data.accounts.impl.datasource

import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.network.client.FireflyClientProvider
import app.lucent.firefly.core.network.client.installFirefly
import app.lucent.firefly.core.network.model.ServerCredentials
import app.lucent.firefly.core.network.model.hasNextPage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRemoteDataSourceTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun dataSource(body: String): DefaultAccountRemoteDataSource {
        val client =
            HttpClient(
                MockEngine { request ->
                    requests += request
                    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/vnd.api+json"))
                },
            ) { installFirefly(ServerCredentials("https://demo.firefly-iii.org", "pat")) }
        return DefaultAccountRemoteDataSource(
            object : FireflyClientProvider {
                override suspend fun authenticated(): HttpClient = client

                override fun forServer(credentials: ServerCredentials): HttpClient = error("not used")
            },
        )
    }

    @Test
    fun getAccountsSendsTypeAndPageAndParsesPagination() =
        runTest {
            val result =
                dataSource(
                    """
                    {
                      "data": [{
                        "type": "accounts", "id": "1",
                        "attributes": {
                          "name": "Checking Account", "type": "asset", "account_role": "defaultAsset",
                          "include_net_worth": true, "current_balance": "1250.50", "pc_current_balance": null,
                          "currency_id": "1", "currency_code": "EUR", "currency_symbol": "€",
                          "currency_decimal_places": 2, "active": true, "order": null
                        }
                      }],
                      "meta": {"pagination": {"total": 60, "count": 50, "per_page": 50, "current_page": 1, "total_pages": 2}},
                      "links": {"self": "x", "first": "x", "last": "x"}
                    }
                    """.trimIndent(),
                ).getAccounts(type = "asset", page = 1)

            val data = (result as Result.Success).data
            assertEquals(
                "1250.50",
                data.data
                    .single()
                    .attributes.currentBalance,
            )
            assertEquals(
                true,
                data.data
                    .single()
                    .attributes.includeNetWorth,
            )
            assertTrue(data.meta.hasNextPage)
            assertEquals(
                "https://demo.firefly-iii.org/api/v1/accounts?type=asset&page=1",
                requests.single().url.toString(),
            )
        }

    @Test
    fun getAccountReturnsSingleAccount() =
        runTest {
            val result =
                dataSource(
                    """{"data":{"type":"accounts","id":"42","attributes":{"name":"Savings Account","type":"asset","current_balance":"5000.00","active":true}}}""",
                ).getAccount("42")

            assertEquals(
                "Savings Account",
                (result as Result.Success)
                    .data.data.attributes.name,
            )
            assertEquals("https://demo.firefly-iii.org/api/v1/accounts/42", requests.single().url.toString())
        }
}
