package pg.geobingo.one.platform

import androidx.compose.runtime.Composable

@Composable
actual fun SystemBackHandler(onBack: () -> Unit) {
    // Desktop hat keinen Hardware-Zurück-Button
}