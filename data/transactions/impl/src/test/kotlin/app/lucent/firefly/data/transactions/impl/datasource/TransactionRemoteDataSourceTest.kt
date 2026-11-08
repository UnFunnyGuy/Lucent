package app.lucent.firefly.data.transactions.impl.datasource

import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.network.client.FireflyClientProvider
import app.lucent.firefly.core.network.client.installFirefly
import app.lucent.firefly.core.network.model.ServerCredentials
import app.lucent.firefly.core.network.model.hasNextPage
import app.lucent.firefly.data.transactions.impl.dto.TransactionSplitStoreDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionSplitUpdateDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionStoreDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionUpdateDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TransactionRemoteDataSourceTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun dataSource(status: HttpStatusCode = HttpStatusCode.OK, body: String = "{}"): DefaultTransactionRemoteDataSource {
        val client = HttpClient(
            MockEngine { request ->
                requests += request
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/vnd.api+json"))
            },
        ) { installFirefly(ServerCredentials("https://demo.firefly.org", "pat")) }
        return DefaultTransactionRemoteDataSource(
            object : FireflyClientProvider {
                override suspend fun authenticated(): HttpClient = client
                override fun forServer(credentials: ServerCredentials): HttpClient = error("not used")
            },
        )
    }

    @Test
    fun getTransactionsParsesGroupsAndPagination() = runTest {
        val result = dataSource(
            body = """
            {
              "data": [{
                "type": "transactions", "id": "100",
                "attributes": {
                  "created_at": "2026-11-01T12:00:00+01:00", "updated_at": "2026-11-01T12:00:00+01:00",
                  "group_title": null,
                  "transactions": [{
                    "transaction_journal_id": "200", "type": "withdrawal", "date": "2026-11-01T12:00:00+01:00",
                    "amount": "12.50", "description": "Lunch", "source_id": "1", "source_name": "Checking",
                    "destination_id": "9", "destination_name": "Cafe", "currency_code": "EUR",
                    "currency_symbol": "€", "currency_decimal_places": 2, "tags": [], "external_id": null
                  }]
                }
              }],
              "meta": {"pagination": {"total": 1, "count": 1, "per_page": 50, "current_page": 1, "total_pages": 1}}
            }
            """.trimIndent(),
        ).getTransactions(start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 11, 4), page = 1)

        val data = (result as Result.Success).data
        assertEquals("Lunch", data.data.single().attributes.transactions.single().description)
        assertFalse(data.meta.hasNextPage)
        assertEquals(
            "https://demo.firefly.org/api/v1/transactions?start=2026-10-01&end=2026-11-04&page=1",
            requests.single().url.toString(),
        )
    }

    @Test
    fun postTransactionSendsJsonBodyWithoutNullFields() = runTest {
        val result = dataSource(
            status = HttpStatusCode.OK,
            body = """{"data":{"type":"transactions","id":"101","attributes":{"transactions":[{"type":"withdrawal","date":"2026-11-01T12:00:00+00:00","amount":"4.50","description":"Coffee","external_id":"g-1"}]}}}""",
        ).postTransaction(
            TransactionStoreDto(
                transactions = listOf(
                    TransactionSplitStoreDto(
                        type = "withdrawal",
                        date = "2026-11-01T12:00:00+00:00",
                        amount = "4.50",
                        description = "Coffee",
                        sourceId = "1",
                        currencyCode = "EUR",
                        externalId = "g-1",
                    ),
                ),
            ),
        )

        assertEquals("101", (result as Result.Success).data.data.id)
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("https://demo.firefly.org/api/v1/transactions", request.url.toString())
        val body = request.body as OutgoingContent.ByteArrayContent
        assertEquals(ContentType.Application.Json, body.contentType?.withoutParameters())
        assertEquals(
            """{"error_if_duplicate_hash":false,"apply_rules":true,"fire_webhooks":true,"transactions":[{"type":"withdrawal","date":"2026-11-01T12:00:00+00:00","amount":"4.50","description":"Coffee","source_id":"1","currency_code":"EUR","external_id":"g-1"}]}""",
            body.bytes().decodeToString(),
        )
    }

    @Test
    fun updateTransactionPutsJournalIdsAndClearedNotes() = runTest {
        dataSource(
            body = """{"data":{"type":"transactions","id":"101","attributes":{"transactions":[{"type":"withdrawal","date":"2026-11-01T12:00:00+00:00","amount":"6.00","description":"Snack"}]}}}""",
        ).updateTransaction(
            "101",
            TransactionUpdateDto(
                transactions = listOf(
                    TransactionSplitUpdateDto(
                        transactionJournalId = "j-1",
                        type = "withdrawal",
                        date = "2026-11-01T12:00:00+00:00",
                        amount = "6.00",
                        description = "Snack",
                        notes = "",
                    ),
                ),
            ),
        )

        val request = requests.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("https://demo.firefly.org/api/v1/transactions/101", request.url.toString())
        assertEquals(
            """{"apply_rules":true,"fire_webhooks":true,"transactions":[{"transaction_journal_id":"j-1","type":"withdrawal","date":"2026-11-01T12:00:00+00:00","amount":"6.00","description":"Snack","notes":""}]}""",
            (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString(),
        )
    }

    @Test
    fun deleteTransactionAcceptsNoContent() = runTest {
        val result = dataSource(status = HttpStatusCode.NoContent, body = "").deleteTransaction("101")

        assertTrue(result is Result.Success)
        assertEquals(HttpMethod.Delete, requests.single().method)
        assertEquals("https://demo.firefly.org/api/v1/transactions/101", requests.single().url.toString())
    }

    @Test
    fun findByExternalIdUsesSearchOperator() = runTest {
        dataSource(body = """{"data":[]}""").findTransactionsByExternalId("g-1")

        val url = requests.single().url
        assertEquals("/api/v1/search/transactions", url.encodedPath)
        assertEquals("external_id_is:\"g-1\"", url.parameters["query"])
    }
}
