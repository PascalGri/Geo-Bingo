package pg.geobingo.one.auth

/**
 * Platform bridge for native "Continue with Google". Returns an OIDC id_token
 * that can be exchanged with Supabase via signInWith(IDToken, provider=Google).
 *
 * Only supported on iOS at the moment. Other platforms return null → caller
 * falls back to Supabase's web OAuth flow.
 */
expect object NativeGoogleSignIn {
    /**
     * True if the platform can do native Google sign-in RIGHT NOW.
     * On iOS this checks whether GoogleService-Info.plist has a CLIENT_ID
     * (i.e. Google Sign-In has actually been enabled in Firebase Console
     * and the updated plist has been downloaded).
     */
    val isSupported: Boolean

    /**
     * Launches the native Google sign-in sheet. Returns the id_token on
     * success, null on user-cancel or any failure.
     */
    suspend fun signIn(): String?
}
