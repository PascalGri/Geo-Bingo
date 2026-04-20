package pg.geobingo.one.platform

import platform.UIKit.UIApplication
import platform.UIKit.UIScene
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UISceneActivationStateForegroundInactive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/**
 * Resolves the UIWindow that UIKit presentation (image picker, share sheet,
 * etc.) should anchor to.
 *
 * UIApplication.keyWindow is deprecated since iOS 13 and returns nil (or the
 * wrong window) in Stage Manager / multi-scene setups — iPad reviewers hit
 * exactly this case. Always scan `connectedScenes` for a foreground-active
 * UIWindowScene first, then fall back to foreground-inactive, and finally
 * any window we can find.
 */
internal fun activePresentationWindow(): UIWindow? {
    val scenes = UIApplication.sharedApplication.connectedScenes
    val windowScenes = scenes.mapNotNull { it as? UIWindowScene }
    val active = windowScenes.firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
        ?: windowScenes.firstOrNull { it.activationState == UISceneActivationStateForegroundInactive }
        ?: windowScenes.firstOrNull()
    val windows = active?.windows?.mapNotNull { it as? UIWindow } ?: emptyList()
    return windows.firstOrNull { it.isKeyWindow() } ?: windows.firstOrNull()
}

internal fun activeRootViewController(): UIViewController? =
    activePresentationWindow()?.rootViewController
