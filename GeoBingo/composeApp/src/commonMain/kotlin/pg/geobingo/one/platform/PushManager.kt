package pg.geobingo.one.platform

/**
 * Platform-specific push notification manager.
 * Android: FCM, iOS: APNs
 */
expect object PushManager {
    /** Whether push is supported on this platform. */
    val isPushSupported: Boolean

    /** Request notification permission and retrieve device token. Returns null if not available. */
    suspend fun getDeviceToken(): String?

    /** The platform name for the device_tokens table ("android" or "ios"). */
    val platformName: String
}
