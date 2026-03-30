package pg.geobingo.one.platform

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import pg.geobingo.one.util.AppLogger

actual object PushManager {
    actual val isPushSupported: Boolean = true
    actual val platformName: String = "android"

    actual suspend fun getDeviceToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            AppLogger.d("Push", "FCM token retrieval failed", e)
            null
        }
    }
}
