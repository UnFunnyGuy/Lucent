package app.lucent.firefly.data.accounts.api.usecase

import app.lucent.firefly.core.common.base.BaseUseCase
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.accounts.api.model.Account
import app.lucent.firefly.data.accounts.api.repository.AccountRepository
import org.koin.core.annotation.Factory

@Factory
class GetAccountUseCase(
    private val repository: AccountRepository,
) : BaseUseCase<String, Result<Account>> {
    override suspend fun perform(params: String): Result<Account> = repository.getAccount(params)
}
