package pg.geobingo.one.platform

actual object AppSettings {
    actual fun getBoolean(key: String, default: Boolean): Boolean =
        js("localStorage.getItem(key)")?.toString()?.toBoolean() ?: default

    actual fun setBoolean(key: String, value: Boolean) {
        js("localStorage.setItem(key, value.toString())")
    }

    actual fun getString(key: String, default: String): String =
        js("localStorage.getItem(key)")?.toString() ?: default

    actual fun setString(key: String, value: String) {
        js("localStorage.setItem(key, value)")
    }

    actual fun getInt(key: String, default: Int): Int =
        js("localStorage.getItem(key)")?.toString()?.toIntOrNull() ?: default

    actual fun setInt(key: String, value: Int) {
        js("localStorage.setItem(key, value.toString())")
    }
}
