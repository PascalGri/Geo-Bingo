package pg.geobingo.one.auth

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

actual object NativeGoogleSignIn {
    actual val isSupported: Boolean
        get() = GoogleSignInBridgeCompanion.isConfigured

    actual suspend fun signIn(): String? {
        if (!isSupported) return null
        return suspendCancellableCoroutine { cont ->
            GoogleSignInBridge.install(
                onSuccess = { idToken -> if (cont.isActive) cont.resume(idToken) },
                onError = { if (cont.isActive) cont.resume(null) },
            )
            GoogleSignInBridgeCompanion.start()
        }
    }
}

object GoogleSignInBridge {
    private var onSuccessHandler: ((String) -> Unit)? = null
    private var onErrorHandler: ((String) -> Unit)? = null

    fun install(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        onSuccessHandler = onSuccess
        onErrorHandler = onError
    }

    // Called from Swift
    fun onSuccess(idToken: String) {
        val handler = onSuccessHandler
        clear()
        handler?.invoke(idToken)
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
