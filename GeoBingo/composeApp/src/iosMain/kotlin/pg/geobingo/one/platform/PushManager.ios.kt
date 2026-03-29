package pg.geobingo.one.platform

import pg.geobingo.one.util.AppLogger

actual object PushManager {
    actual val isPushSupported: Boolean = true
    actual val platformName: String = "ios"

    // Set from Swift side when APNs token is received
    var apnsToken: String? = null

    actual suspend fun getDeviceToken(): String? {
        // TODO: Request notification permission and retrieve APNs device token from Swift bridge
        // The Swift side should call PushManager.shared.apnsToken = tokenString
        return apnsToken
    }
}
