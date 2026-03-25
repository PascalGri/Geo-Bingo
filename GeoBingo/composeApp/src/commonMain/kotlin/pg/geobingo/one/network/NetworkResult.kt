package pg.geobingo.one.network

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val type: ErrorType, val cause: Exception? = null) : NetworkResult<Nothing>()
}

enum class ErrorType {
    NETWORK,
    DUPLICATE,
    AUTH,
    NOT_FOUND,
    UNKNOWN,
}

fun classifyError(e: Exception): ErrorType {
    val msg = e.message ?: ""
    return when {
        msg.contains("duplicate", true) || msg.contains("23505") || msg.contains("unique", true) -> ErrorType.DUPLICATE
        msg.contains("network", true) || msg.contains("timeout", true) || msg.contains("connect", true) -> ErrorType.NETWORK
        msg.contains("401") || msg.contains("auth", true) || msg.contains("unauthorized", true) -> ErrorType.AUTH
        msg.contains("404") || msg.contains("not found", true) -> ErrorType.NOT_FOUND
        else -> ErrorType.UNKNOWN
    }
}
