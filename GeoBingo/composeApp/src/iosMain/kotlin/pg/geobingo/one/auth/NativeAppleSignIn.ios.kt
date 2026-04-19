package pg.geobingo.one.auth

import kotlin.coroutines.resume
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
            onError = {
                if (cont.isActive) cont.resume(null)
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
