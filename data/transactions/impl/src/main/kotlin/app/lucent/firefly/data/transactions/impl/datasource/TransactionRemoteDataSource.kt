package app.lucent.firefly.data.transactions.impl.datasource

import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.transactions.impl.dto.TransactionArrayDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionSingleDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionStoreDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionUpdateDto
import java.time.LocalDate

interface TransactionRemoteDataSource {
    /** Transactions dated between [start] and [end] (inclusive, server dates). */
    suspend fun getTransactions(
        start: LocalDate,
        end: LocalDate,
        page: Int = 1,
    ): Result<TransactionArrayDto>

    /** Newest first, as the server orders them; used for paging the full history. */
    suspend fun getTransactionPage(page: Int): Result<TransactionArrayDto>

    suspend fun getTransaction(transactionId: String): Result<TransactionSingleDto>

    suspend fun postTransaction(body: TransactionStoreDto): Result<TransactionSingleDto>

    suspend fun updateTransaction(
        transactionId: String,
        body: TransactionUpdateDto,
    ): Result<TransactionSingleDto>

    suspend fun deleteTransaction(transactionId: String): Result<Unit>

    suspend fun findTransactionsByExternalId(externalId: String): Result<TransactionArrayDto>
}
