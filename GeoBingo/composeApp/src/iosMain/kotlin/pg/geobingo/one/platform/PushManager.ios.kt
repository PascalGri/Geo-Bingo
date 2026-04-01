package pg.geobingo.one.platform

import kotlinx.coroutines.delay
import pg.geobingo.one.util.AppLogger

actual object PushManager {
    actual val isPushSupported: Boolean = true
    actual val platformName: String = "ios"

    // Set from Swift side when FCM token is received via AppDelegate
    var apnsToken: String? = null

    actual suspend fun getDeviceToken(): String? {
        // The Swift AppDelegate sets apnsToken asynchronously after FCM registration.
        // Wait briefly for it to arrive if not yet available.
        repeat(10) {
            apnsToken?.let { return it }
            delay(300)
        }
        AppLogger.w("Push", "FCM token not received from Swift bridge after timeout")
        return apnsToken
    }
}
