package pg.geobingo.one.auth

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random
import kotlinx.coroutines.suspendCancellableCoroutine

/** Kotlin side of the Swift AppleSignInBridgeImpl. */
actual object NativeAppleSignIn {
    actual val isSupported: Boolean = true

    actual suspend fun signIn(): NativeSignInResult? = suspendCancellableCoroutine { cont ->
        val rawNonce = randomNonce()
        AppleSignInBridge.install(
            onSuccess = { token, nonce ->
                if (cont.isActive) cont.resume(NativeSignInResult(idToken = token, rawNonce = nonce))
            },
            onError = { message ->
                if (!cont.isActive) return@install
                // "cancelled" → resume(null) so the caller treats it as a quiet
                // user cancel. Any other message (no_presentation_anchor,
                // invalid_presentation_context, …) is a real failure that the
                // UI must surface — propagate it via an exception so
                // friendlyAuthError can show a generic "auth failed" toast
                // instead of swallowing it as a cancel.
                if (message.equals("cancelled", ignoreCase = true)) {
                    cont.resume(null)
                } else {
                    cont.resumeWithException(IllegalStateException(message))
                }
            },
        )
        AppleSignInBridgeCompanion.start(rawNonce = rawNonce)
    }

    private fun randomNonce(length: Int = 32): String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length).map { charset[Random.nextInt(charset.size)] }.joinToString("")
    }
}

/**
 * Receives callbacks from Swift AppleSignInBridgeImpl. Kept one-shot: only the
 * most recent coroutine's handlers are live, stale ones are cleared on completion.
 */
object AppleSignInBridge {
    private var onSuccessHandler: ((String, String) -> Unit)? = null
    private var onErrorHandler: ((String) -> Unit)? = null

    fun install(onSuccess: (String, String) -> Unit, onError: (String) -> Unit) {
        onSuccessHandler = onSuccess
        onErrorHandler = onError
    }

    // Called from Swift on successful auth.
    fun onSuccess(idToken: String, rawNonce: String) {
        val handler = onSuccessHandler
        clear()
        handler?.invoke(idToken, rawNonce)
    }

    // Called from Swift on cancel / failure.
    fun onError(message: String) {
        val handler = onErrorHandler
        clear()
        handler?.invoke(message)
    }

    private fun clear() {
        onSuccessHandler = null
        onErrorHandler = null
    }
}

/** Swift-facing companion to trigger the native sheet. */
object AppleSignInBridgeCompanion {
    var startHandler: ((String) -> Unit)? = null

    fun start(rawNonce: String) {
        startHandler?.invoke(rawNonce)
    }
}
