package pg.geobingo.one.platform

actual object PlatformHaptics {
    actual fun vibrate(durationMs: Int) {
        try {
            js("if (navigator.vibrate) { navigator.vibrate(durationMs); }")
        } catch (e: Exception) {
            println("[W] [Haptics] Vibrate failed: ${e.message}")
        }
    }
}
