package app.lucent.firefly.data.auth.api.usecase

import app.lucent.firefly.data.auth.api.model.AuthSession
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

/**
 * Emits the currently active [AuthSession], or `null` if the user is unauthenticated.
 */
@Factory
class GetActiveSessionUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthSession?> = authRepository.activeSession
}
