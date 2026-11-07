package app.lucent.firefly.core.common.result

/**
 * Monadic result type representing either [Success] with data or [Error] with [AppError].
 */
sealed interface Result<out T> {

    data class Success<out T>(val data: T) : Result<T>

    data class Error(val error: AppError) : Result<Nothing>

    companion object {
        val empty: Result<Unit> = Success(Unit)

        fun <T> success(data: T): Result<T> = Success(data)

        fun error(error: AppError): Result<Nothing> = Error(error)
    }
}

/**
 * Convenient alias for results with no payload (Unit).
 */
typealias EmptyResult = Result<Unit>
