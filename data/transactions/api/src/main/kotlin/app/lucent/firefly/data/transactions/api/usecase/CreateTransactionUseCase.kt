package app.lucent.firefly.data.transactions.api.usecase

import app.lucent.firefly.core.common.base.BaseUseCase
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.transactions.api.model.TransactionDraft
import app.lucent.firefly.data.transactions.api.repository.TransactionRepository
import org.koin.core.annotation.Factory

@Factory
class CreateTransactionUseCase(
    private val repository: TransactionRepository,
) : BaseUseCase<TransactionDraft, Result<String>> {
    override suspend fun perform(params: TransactionDraft): Result<String> = repository.createTransaction(params)
}
