package pg.geobingo.one

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import pg.geobingo.one.util.Analytics

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    Analytics.platform = "web"
    ComposeViewport(document.getElementById("app")!!) {
        App()
    }
}