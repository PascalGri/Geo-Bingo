package pg.geobingo.one.platform

actual object PushManager {
    actual val isPushSupported: Boolean = false
    actual val platformName: String = "web"
    actual suspend fun getDeviceToken(): String? = null
}
