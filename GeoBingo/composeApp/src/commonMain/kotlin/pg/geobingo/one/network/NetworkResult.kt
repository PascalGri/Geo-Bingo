package pg.geobingo.one.network

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode

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
    // 1. Check Ktor ResponseException for HTTP status codes
    if (e is ResponseException) {
        return when (e.response.status) {
            HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> ErrorType.AUTH
            HttpStatusCode.NotFound -> ErrorType.NOT_FOUND
            HttpStatusCode.Conflict -> ErrorType.DUPLICATE
            HttpStatusCode.TooManyRequests,
            HttpStatusCode.ServiceUnavailable,
            HttpStatusCode.GatewayTimeout,
            HttpStatusCode.BadGateway -> ErrorType.NETWORK
            else -> ErrorType.UNKNOWN
        }
    }

    // 2. Check for common network exception types by class name
    val className = e::class.simpleName ?: ""
    if (className.contains("Timeout") || className.contains("Connect") ||
        className.contains("Socket") || className.contains("UnresolvedAddress") ||
        className.contains("NoRoute")) {
        return ErrorType.NETWORK
    }

    // 3. Fallback: string-matching on message (for Supabase/PostgREST errors)
    val msg = e.message ?: ""
    return when {
        msg.contains("duplicate", true) || msg.contains("23505") || msg.contains("unique", true) -> ErrorType.DUPLICATE
        msg.contains("network", true) || msg.contains("timeout", true) || msg.contains("connect", true) -> ErrorType.NETWORK
        msg.contains("401") || msg.contains("auth", true) || msg.contains("unauthorized", true) -> ErrorType.AUTH
        msg.contains("404") || msg.contains("not found", true) -> ErrorType.NOT_FOUND
        else -> ErrorType.UNKNOWN
    }
}
