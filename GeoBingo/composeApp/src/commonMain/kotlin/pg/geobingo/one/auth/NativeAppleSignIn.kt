package pg.geobingo.one.auth

/**
 * Native Sign in with Apple result.
 *
 * [idToken] is the JWT from Apple's ASAuthorizationAppleIDCredential (or Google's
 * native SDK) that can be exchanged with Supabase via signInWith(IDToken).
 * [rawNonce] is the unhashed nonce we generated locally and passed into the request —
 * Supabase needs it to verify the `nonce` claim inside the signed JWT.
 */
data class NativeSignInResult(
    val idToken: String,
    val rawNonce: String,
)

/**
 * Platform bridge for native Sign in with Apple. On iOS this drives
 * ASAuthorizationController; on other platforms it's a no-op that returns null,
 * so callers should fall back to Supabase's web OAuth flow.
 */
expect object NativeAppleSignIn {
    /** True if this platform supports native Apple auth (iOS only for now). */
    val isSupported: Boolean

    /**
     * Launches the system Sign-in-with-Apple sheet and suspends until the user
     * completes or cancels. Returns null on cancel/failure.
     */
    suspend fun signIn(): NativeSignInResult?
}
