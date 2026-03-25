package pg.geobingo.one.platform

import androidx.compose.runtime.Composable

interface ShareManager {
    fun shareText(text: String)
}

@Composable
expect fun rememberShareManager(): ShareManager