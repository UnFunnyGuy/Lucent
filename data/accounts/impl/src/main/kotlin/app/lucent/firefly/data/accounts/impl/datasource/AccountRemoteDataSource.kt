package app.lucent.firefly.data.accounts.impl.datasource

import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.accounts.impl.dto.AccountArrayDto
import app.lucent.firefly.data.accounts.impl.dto.AccountSingleDto

interface AccountRemoteDataSource {
    /** [type] is a Firefly `AccountTypeFilter` value such as `asset` or `liabilities`. */
    suspend fun getAccounts(type: String, page: Int = 1): Result<AccountArrayDto>
    suspend fun getAccount(accountId: String): Result<AccountSingleDto>
}
