package app.lucent.firefly.data.accounts.api.usecase

import app.lucent.firefly.core.common.base.NoParamsUseCase
import app.lucent.firefly.data.accounts.api.model.Account
import app.lucent.firefly.data.accounts.api.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class ObserveActiveAccountsUseCase(
    private val repository: AccountRepository,
) : NoParamsUseCase<List<Account>> {
    override fun performStreaming(): Flow<List<Account>> = repository.observeActiveAccounts()
}
