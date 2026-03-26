package pg.geobingo.one.platform

actual object PlatformHaptics {
    actual fun vibrate(durationMs: Int) {
        // No-op on JVM/Desktop
    }
}
