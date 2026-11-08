package app.lucent.firefly.data.transactions.api.usecase

import app.lucent.firefly.core.common.base.NoParamsUseCase
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.transactions.api.repository.TransactionReconciler
import org.koin.core.annotation.Factory

@Factory
class RefreshTransactionsUseCase(
    private val reconciler: TransactionReconciler,
) : NoParamsUseCase<Result<Unit>> {

    override suspend fun perform(): Result<Unit> = reconciler.syncRecentTransactions()
}
