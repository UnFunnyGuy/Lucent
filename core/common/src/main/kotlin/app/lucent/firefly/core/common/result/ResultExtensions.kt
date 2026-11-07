package app.lucent.firefly.core.common.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

inline fun <T, R> Result<T>.map(
    transform: (T) -> R,
): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
}

inline fun <T, R> Result<T>.flatMap(
    transform: (T) -> Result<R>,
): Result<R> = when (this) {
    is Result.Success -> transform(data)
    is Result.Error -> this
}

inline fun <T> Result<T>.onSuccess(
    action: (T) -> Unit,
): Result<T> = also {
    if (it is Result.Success) action(it.data)
}

inline fun <T> Result<T>.onFailure(
    action: (AppError) -> Unit,
): Result<T> = also {
    if (it is Result.Error) action(it.error)
}

inline fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (AppError) -> R,
): R = when (this) {
    is Result.Success -> onSuccess(data)
    is Result.Error -> onFailure(error)
}

fun <T> Result<T>.getOrNull(): T? = (this as? Result.Success)?.data

fun <T> Result<T>.getOrDefault(defaultValue: T): T = getOrNull() ?: defaultValue

inline fun <T> Result<T>.getOrElse(
    onFailure: (AppError) -> T,
): T = when (this) {
    is Result.Success -> data
    is Result.Error -> onFailure(error)
}

fun <T> Result<T>.toEmptyResult(): EmptyResult = map { }

fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

fun <T> Result<T>.isFailure(): Boolean = this is Result.Error

inline fun <T> Result<T>.mapError(
    transform: (AppError) -> AppError,
): Result<T> = when (this) {
    is Result.Success -> this
    is Result.Error -> Result.Error(transform(error))
}

inline fun <T> Result<T>.recover(
    transform: (AppError) -> T,
): Result<T> = when (this) {
    is Result.Success -> this
    is Result.Error -> Result.Success(transform(error))
}

fun <T> Flow<T>.asResult(): Flow<Result<T>> =
    map<T, Result<T>> { Result.Success(it) }
        .catch { emit(Result.Error(AppError.Common(it.message ?: "Unknown error", it))) }
