package app.lucent.firefly.core.common.result

/**
 * Application errors categorized for domain UseCases and presentation ViewModels.
 */
sealed interface AppError : Error {

    data class Database(
        val message: String,
        val cause: Throwable? = null,
    ) : AppError

    data class Common(
        val message: String,
        val cause: Throwable? = null,
    ) : AppError

    data object ApiKeyError : AppError

    /** The server copy changed since the local edit was based on it. */
    data class Conflict(val message: String) : AppError

    enum class Network : AppError {
        NO_INTERNET,
        TIMEOUT,
        SERVER_UNREACHABLE,
        SERVER_ERROR,
        SERIALIZATION,
        SSL_UNTRUSTED,
        UNKNOWN,
    }

    data class ApiError(
        val message: String,
        val code: Int? = null,
        val exception: Throwable? = null,
        val errorBody: String? = null,
    ) : AppError

    val unAuthorized: Boolean
        get() = this == ApiKeyError || (this is ApiError && (code == 401 || code == 403))

    val isNoNetwork: Boolean
        get() = this == Network.NO_INTERNET

    val errorMessage: String
        get() = when (this) {
            is Database -> message
            is Common -> message
            is Network -> name
            is ApiError -> message
            is Conflict -> message
            ApiKeyError -> "API key error"
        }
}
