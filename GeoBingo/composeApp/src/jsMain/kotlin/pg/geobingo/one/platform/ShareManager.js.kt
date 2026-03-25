package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

@Composable
actual fun rememberShareManager(): ShareManager {
    return remember {
        object : ShareManager {
            override fun shareText(text: String) {
                try {
                    window.navigator.clipboard.writeText(text)
                    window.alert("Ergebnis in die Zwischenablage kopiert!")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
