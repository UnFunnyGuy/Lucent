package app.lucent.firefly.data.auth.impl.datasource

import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.network.model.ServerCredentials
import app.lucent.firefly.data.auth.impl.dto.SystemInfoDto
import app.lucent.firefly.data.auth.impl.dto.UserSingleDto

/** Calls made before a profile is stored, so each takes the server it should talk to. */
interface AuthRemoteDataSource {
    suspend fun getAbout(credentials: ServerCredentials): Result<SystemInfoDto>
    suspend fun getCurrentUser(credentials: ServerCredentials): Result<UserSingleDto>
}
