package pg.geobingo.one.auth

actual object NativeGoogleSignIn {
    actual val isSupported: Boolean = false
    actual suspend fun signIn(): String? = null
}
