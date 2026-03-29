package pg.geobingo.one.platform

import pg.geobingo.one.util.AppLogger

actual object PushManager {
    actual val isPushSupported: Boolean = true
    actual val platformName: String = "android"

    actual suspend fun getDeviceToken(): String? {
        // TODO: Implement FCM token retrieval once google-services.json is added
        // FirebaseMessaging.getInstance().token.await()
        return try {
            // Placeholder: will return real FCM token when Firebase is configured
            null
        } catch (e: Exception) {
            AppLogger.d("Push", "FCM token retrieval failed", e)
            null
        }
    }
}
