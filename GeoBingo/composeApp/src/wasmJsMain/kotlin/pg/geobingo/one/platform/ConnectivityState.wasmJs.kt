package pg.geobingo.one.platform

import androidx.compose.runtime.*

@Composable
actual fun rememberConnectivityState(): State<Boolean> {
    // WasmJs: assume online by default
    return remember { mutableStateOf(true) }
}
