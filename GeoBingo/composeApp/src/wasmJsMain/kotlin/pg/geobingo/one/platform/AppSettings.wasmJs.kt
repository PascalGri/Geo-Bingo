package pg.geobingo.one.platform

@JsFun("(key, value) => { try { localStorage.setItem(key, value); } catch(e) {} }")
private external fun localStorageSet(key: String, value: String)

@JsFun("(key) => { try { return localStorage.getItem(key); } catch(e) { return null; } }")
private external fun localStorageGet(key: String): String?

actual object AppSettings {
    actual fun getBoolean(key: String, default: Boolean): Boolean =
        localStorageGet(key)?.toBoolean() ?: default

    actual fun setBoolean(key: String, value: Boolean) {
        localStorageSet(key, value.toString())
    }

    actual fun getString(key: String, default: String): String =
        localStorageGet(key) ?: default

    actual fun setString(key: String, value: String) {
        localStorageSet(key, value)
    }

    actual fun getInt(key: String, default: Int): Int =
        localStorageGet(key)?.toIntOrNull() ?: default

    actual fun setInt(key: String, value: Int) {
        localStorageSet(key, value.toString())
    }
}
