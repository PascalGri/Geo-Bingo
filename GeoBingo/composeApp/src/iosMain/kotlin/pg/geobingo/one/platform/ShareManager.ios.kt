package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberShareManager(): ShareManager {
    return remember {
        object : ShareManager {
            override fun shareText(text: String) {
                val rootVC = activeRootViewController() ?: return
                val activityController = UIActivityViewController(
                    activityItems = listOf(text),
                    applicationActivities = null,
                )
                // On iPad, UIActivityViewController MUST be presented as a
                // popover with an explicit anchor, otherwise UIKit raises
                // NSInvalidArgumentException and crashes the app.
                if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) {
                    val rootView = rootVC.view
                    val midX = rootView.bounds.useContents { origin.x + size.width / 2.0 }
                    val midY = rootView.bounds.useContents { origin.y + size.height / 2.0 }
                    val popover = activityController.popoverPresentationController()
                    popover?.setSourceView(rootView)
                    popover?.setSourceRect(CGRectMake(midX, midY, 0.0, 0.0))
                }
                rootVC.presentViewController(activityController, animated = true, completion = null)
            }
        }
    }
}
