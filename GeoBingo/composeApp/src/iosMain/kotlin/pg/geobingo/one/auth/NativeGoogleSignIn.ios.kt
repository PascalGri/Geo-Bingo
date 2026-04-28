package pg.geobingo.one.auth

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

actual object NativeGoogleSignIn {
    actual val isSupported: Boolean
        get() = GoogleSignInBridgeCompanion.isConfigured

    actual suspend fun signIn(): NativeSignInResult? {
        if (!isSupported) return null
        return suspendCancellableCoroutine { cont ->
            GoogleSignInBridge.install(
                onSuccess = { idToken, rawNonce ->
                    if (cont.isActive) cont.resume(NativeSignInResult(idToken, rawNonce))
                },
                onError = { message ->
                    if (!cont.isActive) return@install
                    // "cancelled" → silent (user dismissed sheet); any other
                    // failure (no_presenter, not_configured, network, …) is
                    // surfaced as an exception so the UI can show feedback.
                    if (message.equals("cancelled", ignoreCase = true)) {
                        cont.resume(null)
                    } else {
                        cont.resumeWithException(IllegalStateException(message))
                    }
                },
            )
            GoogleSignInBridgeCompanion.start()
        }
    }
}

object GoogleSignInBridge {
    private var onSuccessHandler: ((String, String) -> Unit)? = null
    private var onErrorHandler: ((String) -> Unit)? = null

    fun install(onSuccess: (String, String) -> Unit, onError: (String) -> Unit) {
        onSuccessHandler = onSuccess
        onErrorHandler = onError
    }

    // Called from Swift. rawNonce must match the `nonce` claim Google embedded
    // in the id_token so Supabase accepts it.
    fun onSuccess(idToken: String, rawNonce: String) {
        val handler = onSuccessHandler
        clear()
        handler?.invoke(idToken, rawNonce)
    }

    // Called from Swift
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

/**
 * Swift-facing companion. Swift flips `isConfigured` after reading
 * GoogleService-Info.plist; Kotlin reads it to decide whether to take the
 * native path or fall back to the Supabase web OAuth flow.
 */
object GoogleSignInBridgeCompanion {
    var isConfigured: Boolean = false
        private set

    var startHandler: (() -> Unit)? = null

    fun setConfigured(configured: Boolean) {
        isConfigured = configured
    }

    fun start() {
        startHandler?.invoke()
    }
}
