package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
actual fun rememberShareManager(): ShareManager {
    return remember {
        object : ShareManager {
            override fun shareText(text: String) {
                try {
                    val selection = StringSelection(text)
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}