package pg.geobingo.one.platform

import androidx.compose.runtime.*

@Composable
actual fun rememberConnectivityState(): State<Boolean> {
    // iOS: assume online by default, NWPathMonitor can be added later
    return remember { mutableStateOf(true) }
}
