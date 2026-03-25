package pg.geobingo.one.platform

import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userRoot().node("pg/geobingo/katchit")

actual object AppSettings {
    actual fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    actual fun setBoolean(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
    }

    actual fun getString(key: String, default: String): String =
        prefs.get(key, default)

    actual fun setString(key: String, value: String) {
        prefs.put(key, value)
    }

    actual fun getInt(key: String, default: Int): Int =
        prefs.getInt(key, default)

    actual fun setInt(key: String, value: Int) {
        prefs.putInt(key, value)
    }
}
