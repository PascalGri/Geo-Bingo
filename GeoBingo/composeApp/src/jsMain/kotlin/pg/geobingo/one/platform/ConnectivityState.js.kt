package pg.geobingo.one.platform

import androidx.compose.runtime.*
import kotlinx.browser.window

@Composable
actual fun rememberConnectivityState(): State<Boolean> {
    val isConnected = remember { mutableStateOf(window.navigator.onLine) }

    DisposableEffect(Unit) {
        val onlineHandler: (org.w3c.dom.events.Event) -> Unit = { isConnected.value = true }
        val offlineHandler: (org.w3c.dom.events.Event) -> Unit = { isConnected.value = false }
        window.addEventListener("online", onlineHandler)
        window.addEventListener("offline", offlineHandler)
        onDispose {
            window.removeEventListener("online", onlineHandler)
            window.removeEventListener("offline", offlineHandler)
        }
    }

    return isConnected
}
