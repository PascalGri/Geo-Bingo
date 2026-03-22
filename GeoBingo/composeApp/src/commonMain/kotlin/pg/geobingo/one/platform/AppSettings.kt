package pg.geobingo.one.platform

/**
 * Platform-agnostic key-value storage for persistent app settings.
 * Backed by SharedPreferences (Android), NSUserDefaults (iOS),
 * java.util.prefs.Preferences (Desktop), and localStorage (Web).
 */
expect object AppSettings {
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun setBoolean(key: String, value: Boolean)
    fun getString(key: String, default: String = ""): String
    fun setString(key: String, value: String)
    fun getInt(key: String, default: Int = 0): Int
    fun setInt(key: String, value: Int)
}

// Keys
object SettingsKeys {
    const val SOUND_ENABLED = "sound_enabled"
    const val HAPTIC_ENABLED = "haptic_enabled"
    const val AD_CONSENT_GIVEN = "ad_consent_given"
    const val INTERSTITIAL_GAME_COUNT = "interstitial_game_count"
}
