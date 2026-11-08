package app.lucent.firefly.data.accounts.api.repository

import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.accounts.api.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    fun observeActiveAccounts(): Flow<List<Account>>
    fun observeAccount(id: String): Flow<Account?>
    suspend fun refreshAccounts(): Result<List<Account>>
    suspend fun getAccount(id: String): Result<Account>
}
