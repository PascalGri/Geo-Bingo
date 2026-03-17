package pg.geobingo.one.platform

import androidx.compose.runtime.Composable

@Composable
actual fun SystemBackHandler(onBack: () -> Unit) {
    // iOS verwendet native Swipe-Gesten
}