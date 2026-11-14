package app.lucent.firefly.data.transactions.api.usecase

import app.lucent.firefly.core.common.base.BaseUseCase
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.transactions.api.model.TransactionDraft
import app.lucent.firefly.data.transactions.api.repository.TransactionRepository
import org.koin.core.annotation.Factory

@Factory
class UpdateTransactionUseCase(
    private val repository: TransactionRepository,
) : BaseUseCase<UpdateTransactionUseCase.Params, Result<Unit>> {
    data class Params(
        val transactionId: String,
        val draft: TransactionDraft,
    )

    override suspend fun perform(params: Params): Result<Unit> = repository.updateTransaction(params.transactionId, params.draft)
}
