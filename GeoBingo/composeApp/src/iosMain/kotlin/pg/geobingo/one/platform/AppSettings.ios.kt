package pg.geobingo.one.platform

import platform.Foundation.NSUserDefaults

private val defaults = NSUserDefaults.standardUserDefaults

actual object AppSettings {
    actual fun getBoolean(key: String, default: Boolean): Boolean {
        return if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else default
    }

    actual fun setBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    actual fun getString(key: String, default: String): String =
        defaults.stringForKey(key) ?: default

    actual fun setString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    actual fun getInt(key: String, default: Int): Int {
        return if (defaults.objectForKey(key) != null) defaults.integerForKey(key).toInt() else default
    }

    actual fun setInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }
}
