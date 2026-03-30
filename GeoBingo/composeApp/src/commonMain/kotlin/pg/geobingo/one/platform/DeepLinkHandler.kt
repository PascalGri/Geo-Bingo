package pg.geobingo.one.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Global deep link handler. Platform code sets [pendingGameCode] when a
 * deep link like katchit.app/join/CODE is received. App.kt consumes it.
 */
object DeepLinkHandler {
    var pendingGameCode by mutableStateOf<String?>(null)

    private val validCodeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toSet()

    fun handleUrl(url: String) {
        // Parse katchit.app/join/CODE or pg.geobingo.one://join/CODE
        val joinPrefix = "/join/"
        val idx = url.indexOf(joinPrefix)
        if (idx >= 0) {
            val code = url.substring(idx + joinPrefix.length).trim().uppercase().take(6)
            if (code.length == 6 && code.all { it in validCodeChars }) {
                pendingGameCode = code
            }
        }
    }
}
