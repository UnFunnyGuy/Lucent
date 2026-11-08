package app.lucent.firefly.data.transactions.api.usecase

import app.lucent.firefly.core.common.base.BaseUseCase
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.transactions.api.repository.TransactionRepository
import org.koin.core.annotation.Factory

@Factory
class DeleteTransactionUseCase(
    private val repository: TransactionRepository,
) : BaseUseCase<String, Result<Unit>> {
    override suspend fun perform(params: String): Result<Unit> = repository.deleteTransaction(params)
}
