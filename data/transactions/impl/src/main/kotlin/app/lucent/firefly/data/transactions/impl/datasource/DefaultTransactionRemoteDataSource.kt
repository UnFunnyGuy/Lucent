package app.lucent.firefly.data.transactions.impl.datasource

import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.network.ApiVersion
import app.lucent.firefly.core.network.client.FireflyClientProvider
import app.lucent.firefly.core.network.safeApiCall
import app.lucent.firefly.data.transactions.impl.dto.TransactionArrayDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionSingleDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionStoreDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionUpdateDto
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.koin.core.annotation.Singleton
import java.time.LocalDate

@Singleton
class DefaultTransactionRemoteDataSource(
    private val clientProvider: FireflyClientProvider,
) : TransactionRemoteDataSource {
    override suspend fun getTransactions(
        start: LocalDate,
        end: LocalDate,
        page: Int,
    ): Result<TransactionArrayDto> =
        safeApiCall {
            clientProvider.authenticated().get(ApiVersion.V1("transactions")) {
                parameter("start", start.toString())
                parameter("end", end.toString())
                parameter("page", page)
            }
        }

    override suspend fun getTransactionPage(page: Int): Result<TransactionArrayDto> =
        safeApiCall {
            clientProvider.authenticated().get(ApiVersion.V1("transactions")) {
                parameter("page", page)
            }
        }

    override suspend fun getTransaction(transactionId: String): Result<TransactionSingleDto> =
        safeApiCall {
            clientProvider.authenticated().get(ApiVersion.V1("transactions/$transactionId"))
        }

    override suspend fun postTransaction(body: TransactionStoreDto): Result<TransactionSingleDto> =
        safeApiCall {
            clientProvider.authenticated().post(ApiVersion.V1("transactions")) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }

    override suspend fun updateTransaction(
        transactionId: String,
        body: TransactionUpdateDto,
    ): Result<TransactionSingleDto> =
        safeApiCall {
            clientProvider.authenticated().put(ApiVersion.V1("transactions/$transactionId")) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> =
        safeApiCall {
            clientProvider.authenticated().delete(ApiVersion.V1("transactions/$transactionId"))
        }

    override suspend fun findTransactionsByExternalId(externalId: String): Result<TransactionArrayDto> =
        safeApiCall {
            clientProvider.authenticated().get(ApiVersion.V1("search/transactions")) {
                parameter("query", "external_id_is:\"$externalId\"")
            }
        }
}
