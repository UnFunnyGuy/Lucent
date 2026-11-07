package app.lucent.firefly.core.common.base

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Represents the state of an asynchronous component or screen sub-content.
 *
 * - [None]: Idle / uninitialized state (before any work is requested).
 * - [Loading]: Operation in progress.
 * - [Success]: Operation completed successfully with [data].
 * - [Error]: Operation failed with an error [message] represented as [UiText].
 */
@Immutable
sealed interface UIContentState<out T> {

    data class Success<out T>(val data: T) : UIContentState<T>

    data class Error(val message: UiText) : UIContentState<Nothing> {
        constructor(message: String) : this(UiText.DynamicString(message))
        constructor(@StringRes resId: Int, vararg args: Any) : this(UiText.StringResource(resId, arrayOf(*args)))
        constructor(error: AppError) : this(UiText.DynamicString(error.errorMessage))
    }

    data object Loading : UIContentState<Nothing>

    data object None : UIContentState<Nothing>

    val isLoading: Boolean
        get() = this is Loading

    val isSuccess: Boolean
        get() = this is Success<*>

    val isError: Boolean
        get() = this is Error

    val isNone: Boolean
        get() = this is None
}

/** Returns [UIContentState.Success.data] if this is [UIContentState.Success], or null otherwise. */
fun <T> UIContentState<T>.getOrNull(): T? = (this as? UIContentState.Success<T>)?.data

/** Alias for [getOrNull]. */
fun <T> UIContentState<T>.unwrap(): T? = getOrNull()

/** Returns [UIContentState.Success.data] if this is [UIContentState.Success], or [default] otherwise. */
inline fun <T> UIContentState<T>.getOrElse(default: () -> T): T = getOrNull() ?: default()

/** Transforms [UIContentState.Success.data]; passes all other states through. */
inline fun <T, R> UIContentState<T>.mapSuccess(transform: (T) -> R): UIContentState<R> = when (this) {
    is UIContentState.Success -> UIContentState.Success(transform(data))
    is UIContentState.Error -> this
    is UIContentState.Loading -> this
    is UIContentState.None -> this
}

/** Runs [action] if this is [UIContentState.Success]. Returns `this` for chaining. */
inline fun <T> UIContentState<T>.onSuccess(action: (T) -> Unit): UIContentState<T> = also {
    if (it is UIContentState.Success) action(it.data)
}

/** Runs [action] if this is [UIContentState.Error]. Returns `this` for chaining. */
inline fun <T> UIContentState<T>.onError(action: (message: UiText) -> Unit): UIContentState<T> = also {
    if (it is UIContentState.Error) action(it.message)
}

/** Runs [action] if this is [UIContentState.Loading]. Returns `this` for chaining. */
inline fun <T> UIContentState<T>.onLoading(action: () -> Unit): UIContentState<T> = also {
    if (it is UIContentState.Loading) action()
}

/** Runs [action] if this is [UIContentState.None]. Returns `this` for chaining. */
inline fun <T> UIContentState<T>.onNone(action: () -> Unit): UIContentState<T> = also {
    if (it is UIContentState.None) action()
}

/** Folds all 4 states into a single value of type [R]. */
inline fun <T, R> UIContentState<T>.fold(
    onSuccess: (T) -> R,
    onError: (UiText) -> R,
    onLoading: () -> R,
    onNone: () -> R,
): R = when (this) {
    is UIContentState.Success -> onSuccess(data)
    is UIContentState.Error -> onError(message)
    is UIContentState.Loading -> onLoading()
    is UIContentState.None -> onNone()
}

/**
 * Maps a domain [Result] directly into a presentation [UIContentState].
 */
fun <T> Result<T>.toUIContentState(): UIContentState<T> = when (this) {
    is Result.Success -> UIContentState.Success(data)
    is Result.Error -> UIContentState.Error(error)
}

/**
 * Converts a cold [Flow] into a [Flow] of [UIContentState], emitting [UIContentState.Loading] on start
 * and catching exceptions as [UIContentState.Error].
 */
fun <T> Flow<T>.asUIContentState(): Flow<UIContentState<T>> =
    map<T, UIContentState<T>> { UIContentState.Success(it) }
        .onStart { emit(UIContentState.Loading) }
        .catch { emit(UIContentState.Error(it.message ?: "Unknown error")) }
