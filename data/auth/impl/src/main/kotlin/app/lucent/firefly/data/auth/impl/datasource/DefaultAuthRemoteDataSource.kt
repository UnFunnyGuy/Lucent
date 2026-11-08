package app.lucent.firefly.data.auth.impl.datasource

import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.network.ApiVersion
import app.lucent.firefly.core.network.client.FireflyClientProvider
import app.lucent.firefly.core.network.model.ServerCredentials
import app.lucent.firefly.core.network.safeApiCall
import app.lucent.firefly.data.auth.impl.dto.SystemInfoDto
import app.lucent.firefly.data.auth.impl.dto.UserSingleDto
import io.ktor.client.request.get
import org.koin.core.annotation.Singleton

@Singleton
class DefaultAuthRemoteDataSource(
    private val clientProvider: FireflyClientProvider,
) : AuthRemoteDataSource {

    override suspend fun getAbout(credentials: ServerCredentials): Result<SystemInfoDto> =
        clientProvider.forServer(credentials).use { client ->
            safeApiCall { client.get(ApiVersion.V1("about")) }
        }

    override suspend fun getCurrentUser(credentials: ServerCredentials): Result<UserSingleDto> =
        clientProvider.forServer(credentials).use { client ->
            safeApiCall { client.get(ApiVersion.V1("about/user")) }
        }
}
