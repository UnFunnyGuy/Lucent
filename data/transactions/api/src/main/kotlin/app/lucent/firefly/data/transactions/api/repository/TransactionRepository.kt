package app.lucent.firefly.data.transactions.api.repository

import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.transactions.api.model.Transaction
import app.lucent.firefly.data.transactions.api.model.TransactionDraft
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>

    /** Full history, newest first; read from Room and paged in from the server as the user scrolls. */
    fun pagedTransactions(): Flow<PagingData<Transaction>>

    suspend fun getTransaction(id: String): Result<Transaction>
    suspend fun createTransaction(draft: TransactionDraft): Result<String>

    /** Edits a single-split transaction; the change is pushed later. */
    suspend fun updateTransaction(id: String, draft: TransactionDraft): Result<Unit>
    suspend fun deleteTransaction(id: String): Result<Unit>
}
