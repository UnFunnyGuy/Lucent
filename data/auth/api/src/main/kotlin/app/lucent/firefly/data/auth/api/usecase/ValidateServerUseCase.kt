package app.lucent.firefly.data.auth.api.usecase

import app.lucent.firefly.core.common.base.BaseUseCase
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.auth.api.model.ServerInfo
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import org.koin.core.annotation.Factory

/**
 * Validates whether a target [baseUrl] is an accessible, compliant Firefly III instance.
 * Firefly III v6+ requires a valid token even for `/api/v1/about`.
 */
@Factory
class ValidateServerUseCase(
    private val authRepository: AuthRepository,
) : BaseUseCase<ValidateServerUseCase.Params, Result<ServerInfo>> {

    data class Params(
        val baseUrl: String,
        val token: String? = null,
        val allowSelfSignedCert: Boolean = false,
    )

    override suspend fun perform(params: Params): Result<ServerInfo> =
        authRepository.validateServer(params.baseUrl, params.token, params.allowSelfSignedCert)
}
