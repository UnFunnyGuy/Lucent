package app.lucent.firefly.data.auth.api.usecase

import app.lucent.firefly.core.common.base.BaseUseCase
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import org.koin.core.annotation.Factory

/**
 * Validates a Personal Access Token (PAT) against the target server, creates a new
 * encrypted profile session, and marks it active.
 */
@Factory
class AuthenticateUseCase(
    private val authRepository: AuthRepository,
) : BaseUseCase<AuthenticateUseCase.Params, Result<AuthSession>> {
    data class Params(
        val baseUrl: String,
        val token: String,
        val allowSelfSignedCert: Boolean = false,
    )

    override suspend fun perform(params: Params): Result<AuthSession> =
        authRepository.authenticate(
            baseUrl = params.baseUrl,
            token = params.token,
            allowSelfSignedCert = params.allowSelfSignedCert,
        )
}
