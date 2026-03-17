package pg.geobingo.one.platform

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberShareManager(): ShareManager {
    val context = LocalContext.current
    return remember(context) {
        object : ShareManager {
            override fun shareText(text: String) {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Ergebnis teilen")
                context.startActivity(shareIntent)
            }
        }
    }
}