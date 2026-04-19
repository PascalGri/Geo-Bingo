package pg.geobingo.one.auth

actual object NativeAppleSignIn {
    actual val isSupported: Boolean = false
    actual suspend fun signIn(): NativeSignInResult? = null
}
