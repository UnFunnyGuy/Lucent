package app.lucent.firefly.data.transactions.api.usecase

import androidx.paging.PagingData
import app.lucent.firefly.core.common.base.NoParamsUseCase
import app.lucent.firefly.data.transactions.api.model.Transaction
import app.lucent.firefly.data.transactions.api.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class ObservePagedTransactionsUseCase(
    private val repository: TransactionRepository,
) : NoParamsUseCase<PagingData<Transaction>> {
    override fun performStreaming(): Flow<PagingData<Transaction>> = repository.pagedTransactions()
}
