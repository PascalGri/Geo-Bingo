package pg.geobingo.one.platform

/**
 * Platform-specific haptic feedback that works beyond Compose's HapticFeedback.
 * On web, uses navigator.vibrate(). On native platforms, this is a no-op
 * since Compose's HapticFeedback already handles it.
 */
expect object PlatformHaptics {
    fun vibrate(durationMs: Int = 15)
}
