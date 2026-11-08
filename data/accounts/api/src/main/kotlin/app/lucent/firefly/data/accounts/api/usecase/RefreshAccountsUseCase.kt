package app.lucent.firefly.data.accounts.api.usecase

import app.lucent.firefly.core.common.base.NoParamsUseCase
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.accounts.api.model.Account
import app.lucent.firefly.data.accounts.api.repository.AccountRepository
import org.koin.core.annotation.Factory

@Factory
class RefreshAccountsUseCase(
    private val repository: AccountRepository,
) : NoParamsUseCase<Result<List<Account>>> {
    override suspend fun perform(): Result<List<Account>> = repository.refreshAccounts()
}
