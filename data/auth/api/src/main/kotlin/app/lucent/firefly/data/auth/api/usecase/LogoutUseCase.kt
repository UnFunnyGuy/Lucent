package app.lucent.firefly.data.auth.api.usecase

import app.lucent.firefly.core.common.base.NoParamsUseCase
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import org.koin.core.annotation.Factory

/**
 * Terminates the active session by removing credentials from secure storage.
 */
@Factory
class LogoutUseCase(
    private val authRepository: AuthRepository,
) : NoParamsUseCase<Result<Unit>> {
    override suspend fun perform(): Result<Unit> = authRepository.logout()
}
