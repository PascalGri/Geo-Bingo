package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun rememberShareManager(): ShareManager {
    return remember {
        object : ShareManager {
            override fun shareText(text: String) {
                val activityController = UIActivityViewController(
                    activityItems = listOf(text),
                    applicationActivities = null
                )
                val window = UIApplication.sharedApplication.keyWindow
                window?.rootViewController?.presentViewController(activityController, animated = true, completion = null)
            }
        }
    }
}