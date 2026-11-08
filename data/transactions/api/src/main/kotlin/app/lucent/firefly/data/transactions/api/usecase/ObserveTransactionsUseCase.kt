package app.lucent.firefly.data.transactions.api.usecase

import app.lucent.firefly.core.common.base.NoParamsUseCase
import app.lucent.firefly.data.transactions.api.model.Transaction
import app.lucent.firefly.data.transactions.api.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class ObserveTransactionsUseCase(
    private val repository: TransactionRepository,
) : NoParamsUseCase<List<Transaction>> {
    override fun performStreaming(): Flow<List<Transaction>> = repository.observeTransactions()
}
