package app.lucent.firefly.core.common.base

import kotlinx.coroutines.flow.Flow

/**
 * Base interface for Use Cases in the domain layer.
 */
interface BaseUseCase<in Params, out Result> {
    suspend fun perform(params: Params): Result = throw NotImplementedError("BaseUseCase perform(params) not implemented")

    fun performStreaming(params: Params): Flow<Result> = throw NotImplementedError("BaseUseCase performStreaming(params) not implemented")
}

/**
 * Convenience UseCase interface for operations requiring no parameters.
 */
interface NoParamsUseCase<out Result> {
    suspend fun perform(): Result = throw NotImplementedError("NoParamsUseCase perform() not implemented")

    fun performStreaming(): Flow<Result> = throw NotImplementedError("NoParamsUseCase performStreaming() not implemented")
}
