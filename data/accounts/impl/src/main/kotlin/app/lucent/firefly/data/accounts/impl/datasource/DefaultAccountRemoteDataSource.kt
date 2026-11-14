package app.lucent.firefly.data.accounts.impl.datasource

import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.network.ApiVersion
import app.lucent.firefly.core.network.client.FireflyClientProvider
import app.lucent.firefly.core.network.safeApiCall
import app.lucent.firefly.data.accounts.impl.dto.AccountArrayDto
import app.lucent.firefly.data.accounts.impl.dto.AccountSingleDto
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.koin.core.annotation.Singleton

@Singleton
class DefaultAccountRemoteDataSource(
    private val clientProvider: FireflyClientProvider,
) : AccountRemoteDataSource {
    override suspend fun getAccounts(
        type: String,
        page: Int,
    ): Result<AccountArrayDto> =
        safeApiCall {
            clientProvider.authenticated().get(ApiVersion.V1("accounts")) {
                parameter("type", type)
                parameter("page", page)
            }
        }

    override suspend fun getAccount(accountId: String): Result<AccountSingleDto> =
        safeApiCall {
            clientProvider.authenticated().get(ApiVersion.V1("accounts/$accountId"))
        }
}
