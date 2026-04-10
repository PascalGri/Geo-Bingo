package pg.geobingo.one.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity

/**
 * Reactive state that tracks whether the soft keyboard is currently visible.
 * Used to hide the bottom navigation bar so it does not overlap input fields.
 *
 * Implementation reads `WindowInsets.ime.getBottom()` — non-zero means the IME
 * is taking up space at the bottom (i.e. keyboard is open). On platforms
 * without an IME concept (Web, Desktop) this is always 0 → returns `false`.
 */
@Composable
fun rememberKeyboardOpen(): State<Boolean> {
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    return rememberUpdatedState(imeBottomPx > 0)
}
