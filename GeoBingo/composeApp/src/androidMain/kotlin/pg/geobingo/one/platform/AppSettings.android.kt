package pg.geobingo.one.platform

import android.content.SharedPreferences

private val prefs: SharedPreferences by lazy {
    appContext.getSharedPreferences("katchit_settings", android.content.Context.MODE_PRIVATE)
}

actual object AppSettings {
    actual fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    actual fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    actual fun getString(key: String, default: String): String =
        prefs.getString(key, default) ?: default

    actual fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    actual fun getInt(key: String, default: Int): Int =
        prefs.getInt(key, default)

    actual fun setInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }
}
