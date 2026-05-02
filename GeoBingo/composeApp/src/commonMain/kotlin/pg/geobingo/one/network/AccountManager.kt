package pg.geobingo.one.network

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import pg.geobingo.one.auth.NativeAppleSignIn
import pg.geobingo.one.auth.NativeGoogleSignIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.util.Analytics
import pg.geobingo.one.util.AppLogger

@Serializable
data class UserProfile(
    val id: String = "",
    val email: String = "",
    val display_name: String = "",
    val avatar_url: String = "",
    val star_count: Int = 0,
    val skip_cards_count: Int = 0,
    val no_ads_purchased: Boolean = false,
    val games_played: Int = 0,
    val games_won: Int = 0,
    val longest_win_streak: Int = 0,
    val sound_enabled: Boolean = true,
    val haptic_enabled: Boolean = true,
    val language: String = "de",
    val last_seen: String? = null,
    val friend_code: String? = null,
    // ── Equipped cosmetics (synced via Supabase profiles table) ──────────
    val equipped_frame: String = "frame_none",
    val equipped_name_effect: String = "name_none",
    val equipped_title: String = "title_none",
    val equipped_card_design: String = "card_none",
    val equipped_banner_background: String = "banner_none",
)

/**
 * Lightweight per-player cosmetic snapshot used to render PlayerBanner across the app.
 * Constructed from a UserProfile or from per-player metadata in game DTOs.
 */
data class PlayerCosmetics(
    val frameId: String = "frame_none",
    val nameEffectId: String = "name_none",
    val titleId: String = "title_none",
    val bannerBackgroundId: String = "banner_none",
) {
    companion object {
        val NONE = PlayerCosmetics()
        fun fromProfile(p: UserProfile): PlayerCosmetics = PlayerCosmetics(
            frameId = p.equipped_frame,
            nameEffectId = p.equipped_name_effect,
            titleId = p.equipped_title,
            bannerBackgroundId = p.equipped_banner_background,
        )
    }
}

object AccountManager {
    private const val TAG = "AccountManager"

    /**
     * Reactive version counter that bumps whenever the local profile identity
     * (display name, avatar, or auth state) changes. Compose screens that read
     * `last_player_name` or the cached avatar blob can observe this value to
     * trigger recomposition on sign-out, sign-in, user-switch, display-name
     * updates, or avatar changes.
     */
    var profileVersion: Int by mutableIntStateOf(0)
        private set

    fun bumpProfileVersion() {
        profileVersion++
    }

    // Backing state driven by supabase.auth.sessionStatus (see init block).
    // Keeping our own MutableState rather than delegating to a non-reactive
    // getter guarantees every Compose read auto-subscribes — no "stale email
    // on other screens after sign-out" bug even if a network-failed signOut()
    // leaves Supabase's internal currentUserOrNull() momentarily out of sync.
    private var _currentUser by mutableStateOf<UserInfo?>(null)
    private var _isLoggedIn by mutableStateOf(false)

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        // Seed from whatever Supabase already has cached (e.g. restored session),
        // then follow every future auth event via the StateFlow. Running a single
        // long-lived collector on an object-scope keeps the contract simple: UI
        // state == sessionStatus, always.
        //
        // Wrapped defensively: if the Supabase client failed to initialize (e.g.
        // iPad-only iOS 26.4.x networking regression), don't take the whole app
        // down — leave the user unauthenticated and let sign-in retry later.
        try {
            val seeded = supabase.auth.currentUserOrNull()
            _currentUser = seeded
            _isLoggedIn = seeded != null
        } catch (t: Throwable) {
            AppLogger.e(TAG, "Initial auth read failed", t)
            _currentUser = null
            _isLoggedIn = false
        }
        scope.launch {
            try {
                supabase.auth.sessionStatus.collect { status ->
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            _currentUser = status.session.user
                            _isLoggedIn = status.session.user != null
                        }
                        is SessionStatus.NotAuthenticated -> {
                            _currentUser = null
                            _isLoggedIn = false
                            // Bump so screens reading `profileVersion` (name/avatar caches)
                            // also refresh — sessionStatus alone only covers _isLoggedIn/_currentUser.
                            if (status.isSignOut) bumpProfileVersion()
                        }
                        else -> Unit  // LoadingFromStorage, RefreshFailure, etc. — keep current state
                    }
                }
            } catch (t: Throwable) {
                AppLogger.e(TAG, "sessionStatus collector failed", t)
            }
        }
    }

    val isLoggedIn: Boolean get() = _isLoggedIn

    val currentUser: UserInfo? get() = _currentUser

    val currentUserId: String? get() = _currentUser?.id

    /**
     * User-friendly email string for the Account screen. Apple's "Hide My Email"
     * feature returns `xxxxxxxx@privaterelay.appleid.com` for users who chose
     * to conceal their real address — showing that raw relay address is just
     * noise. We surface a labelled placeholder instead so the user sees the
     * account is real without staring at a random-looking relay handle.
     */
    /**
     * Maps a raw auth-error Throwable to a short, user-friendly string.
     * The full technical message/stack goes to the logs (AppLogger.w) but
     * NEVER surfaces in the UI — Apple reviewers see HTTP status codes,
     * JWT-claim errors, and URLs as "unprofessional error handling".
     */
    fun friendlyAuthError(throwable: Throwable?): String {
        val raw = throwable?.message.orEmpty().lowercase()
        val s = pg.geobingo.one.i18n.S.current
        return when {
            raw.isBlank() -> s.authError
            // User dismissed native sheet / hit cancel — not actually an error
            raw.contains("cancelled") || raw.contains("canceled") -> ""
            raw.contains("network") || raw.contains("socket") ||
                raw.contains("timeout") || raw.contains("unreachable") ||
                raw.contains("connect") || raw.contains("host") ||
                raw.contains("resolve") -> s.authNetworkError
            raw.contains("already registered") || raw.contains("already exists") ||
                raw.contains("user already") -> s.authEmailAlreadyUsed
            raw.contains("invalid") && raw.contains("email") -> s.authEmailInvalid
            raw.contains("invalid login") || raw.contains("invalid credentials") ||
                raw.contains("bad credentials") -> s.authError
            // Bucket everything else (audience mismatch, 4xx/5xx, malformed JSON, …)
            // under a generic message. The reviewer — and any end-user — sees a
            // short, actionable string; debug details live in the logger.
            else -> s.authError
        }
    }

    val displayEmail: String
        get() {
            val raw = _currentUser?.email.orEmpty()
            return when {
                raw.isBlank() -> ""
                raw.endsWith("@privaterelay.appleid.com", ignoreCase = true) ->
                    pg.geobingo.one.i18n.S.current.emailHiddenByApple
                else -> raw
            }
        }

    /** True if the user has signed up but hasn't set a display name yet. */
    val needsProfileSetup: Boolean
        get() {
            if (!isLoggedIn) return false
            val name = AppSettings.getString("last_player_name", "")
            return name.isBlank()
        }

    // ── Email Auth ─────────────────────────────────────────────────

    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            // Create profile row
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                createProfileIfNeeded(userId, email)
                syncLocalToCloud(userId)
            }
            Analytics.track(Analytics.SIGN_UP, mapOf("method" to "email"))
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Sign up failed", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                syncCloudToLocal(userId)
            }
            Analytics.track(Analytics.SIGN_IN, mapOf("method" to "email"))
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }

    // ── OAuth Providers ────────────────────────────────────────────

    suspend fun signInWithGoogle(): Result<Unit> {
        // Prefer the native GIDSignIn sheet when available — no Safari redirect,
        // no iPad multi-scene issues, and the id_token goes straight to Supabase.
        // Falls back to the web OAuth flow if the native SDK isn't configured
        // yet (GoogleService-Info.plist without CLIENT_ID on iOS, or any non-iOS
        // target).
        if (NativeGoogleSignIn.isSupported) {
            val native = try {
                NativeGoogleSignIn.signIn()
            } catch (e: Exception) {
                AppLogger.w(TAG, "Native Google sign-in threw", e)
                return Result.failure(e)
            } ?: return Result.failure(IllegalStateException("google_sign_in_cancelled"))

            return try {
                supabase.auth.signInWith(IDToken) {
                    provider = Google
                    idToken = native.idToken
                    nonce = native.rawNonce
                }
                val userId = supabase.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val email = supabase.auth.currentUserOrNull()?.email ?: ""
                    createProfileIfNeeded(userId, email)
                    syncCloudToLocal(userId)
                }
                Analytics.track(Analytics.SIGN_IN, mapOf("method" to "google"))
                Result.success(Unit)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Google IDToken exchange failed: ${e.message}", e)
                Result.failure(e)
            }
        }

        return try {
            supabase.auth.signInWith(Google) {
                if (Analytics.platform == "web") {
                    queryParams["redirect_to"] = "https://katchit.app/play.html"
                }
            }
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                val email = supabase.auth.currentUserOrNull()?.email ?: ""
                createProfileIfNeeded(userId, email)
                syncCloudToLocal(userId)
            }
            Analytics.track(Analytics.SIGN_IN, mapOf("method" to "google"))
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Google sign in failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithApple(): Result<Unit> {
        // On iOS we MUST use the native ASAuthorizationController flow —
        // Apple's HIG 4.1.2 requires it, and the Safari-redirect path crashes
        // on iPad. Other platforms fall back to Supabase's OAuth web flow.
        if (NativeAppleSignIn.isSupported) {
            val native = try {
                NativeAppleSignIn.signIn()
            } catch (e: Exception) {
                AppLogger.w(TAG, "Apple native sign-in threw", e)
                return Result.failure(e)
            } ?: return Result.failure(IllegalStateException("apple_sign_in_cancelled"))

            return try {
                supabase.auth.signInWith(IDToken) {
                    provider = Apple
                    idToken = native.idToken
                    nonce = native.rawNonce
                }
                val userId = supabase.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val email = supabase.auth.currentUserOrNull()?.email ?: ""
                    createProfileIfNeeded(userId, email)
                    syncCloudToLocal(userId)
                }
                Analytics.track(Analytics.SIGN_IN, mapOf("method" to "apple"))
                Result.success(Unit)
            } catch (e: Exception) {
                // Log the FULL exception so we can see what Supabase actually says —
                // "Unacceptable audience in id_token" means the Bundle ID isn't in
                // Supabase → Auth → Providers → Apple → Authorized Client IDs.
                AppLogger.w(TAG, "Apple IDToken exchange failed: ${e.message}", e)
                Result.failure(e)
            }
        }

        return try {
            supabase.auth.signInWith(Apple) {
                if (Analytics.platform == "web") {
                    queryParams["redirect_to"] = "https://katchit.app/play.html"
                }
            }
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                val email = supabase.auth.currentUserOrNull()?.email ?: ""
                createProfileIfNeeded(userId, email)
                syncCloudToLocal(userId)
            }
            Analytics.track(Analytics.SIGN_IN, mapOf("method" to "apple"))
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Apple sign in failed", e)
            Result.failure(e)
        }
    }

    // ── Password Reset ─────────────────────────────────────────────

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            supabase.auth.resetPasswordForEmail(
                email = email,
                redirectUrl = "https://katchit.app/reset-password.html",
            )
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Password reset failed", e)
            Result.failure(e)
        }
    }

    // ── Change Email ──────────────────────────────────────────────

    suspend fun changeEmail(newEmail: String): Result<Unit> {
        return try {
            supabase.auth.updateUser { email = newEmail }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Change email failed", e)
            Result.failure(e)
        }
    }

    // ── Change Password ────────────────────────────────────────────

    suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            supabase.auth.updateUser { password = newPassword }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Change password failed", e)
            Result.failure(e)
        }
    }

    // ── Sign Out ───────────────────────────────────────────────────

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            // supabase-kt's signOut() only calls clearSession() when the server
            // /logout POST succeeds (or returns an ignored 4xx). On network
            // failure / 5xx it throws BEFORE clearing local state — leaving
            // currentUserOrNull() still returning the old user. The UI then
            // shows stale "logged in" data on every screen that reads email.
            // Force the local session clear so sign-out is actually observable.
            AppLogger.w(TAG, "Supabase signOut threw — forcing local clearSession", e)
            try {
                supabase.auth.clearSession()
            } catch (e2: Exception) {
                AppLogger.w(TAG, "Forced clearSession also failed", e2)
            }
        }
        // Wipe per-user cached state so the signed-out user's data isn't
        // visible in the UI (or cross-contaminates the next login). Device
        // preferences are preserved. last_known_user_id is cleared so the
        // next login doesn't falsely trigger the user-switch branch.
        clearUserScopedLocalState()
        AppSettings.setString("last_known_user_id", "")
    }

    // ── Delete Account ─────────────────────────────────────────────

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val session = supabase.auth.currentSessionOrNull()
                ?: return Result.failure(Exception("Not logged in"))
            // Call Edge Function to delete auth user, profile, and avatar server-side.
            // We MUST verify the HTTP response — previously this code ignored the
            // status and signed the user out even on a 5xx, leaving server-side
            // data intact while the UI signalled "deleted". Apple guideline
            // 5.1.1(v) requires server-side deletion to actually happen, so a
            // silent failure here is a real reject risk.
            val url = "${SupabaseConfig.current.url}/functions/v1/delete-account"
            val response = ServiceLocator.httpClient.post(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
                    append("apikey", SupabaseConfig.current.anonKey)
                }
            }
            if (!response.status.isSuccess()) {
                // Try to surface the server's error body for the toast/log so
                // the user sees a real reason instead of "deletion failed" with
                // no detail. Body read may fail (e.g. empty response) — treat
                // that as best-effort.
                val body = try { response.body<String>() } catch (_: Exception) { "" }
                val msg = "delete-account ${response.status.value}: ${body.take(200)}"
                AppLogger.w(TAG, msg)
                return Result.failure(Exception(msg))
            }
            // Server confirmed deletion — only NOW clear local state and sign out.
            clearUserScopedLocalState()
            AppSettings.setString("last_known_user_id", "")
            try { supabase.auth.signOut() } catch (e: Exception) {
                AppLogger.w(TAG, "Sign out failed during account deletion", e)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Account deletion failed", e)
            Result.failure(e)
        }
    }

    // ── Profile Management ─────────────────────────────────────────

    suspend fun updateDisplayName(name: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        // Hit the server FIRST — if our server-side moderation trigger blocks
        // the name (check_violation 'display_name_rejected'), we don't want the
        // rejected name sitting in local AppSettings. Only mirror locally after
        // the DB accepts it.
        return try {
            supabase.postgrest["profiles"].update({
                set("display_name", name)
            }) {
                filter { eq("id", userId) }
            }
            AppSettings.setString("last_player_name", name)
            bumpProfileVersion()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Update display name failed", e)
            // Surface the server-side moderation rejection as a recognisable
            // signal so the UI can show the "profanity" toast instead of a
            // generic error.
            if (e.message?.contains("display_name_rejected", ignoreCase = true) == true) {
                Result.failure(IllegalArgumentException("display_name_rejected"))
            } else Result.failure(e)
        }
    }

    suspend fun uploadProfileAvatar(bytes: ByteArray): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
            // Proactive safety check BEFORE anything hits Storage — we never
            // want an NSFW avatar to live in the cloud even briefly. If the
            // moderator flags it, bail out with a recognisable error so the
            // UI can show a friendly toast.
            val rejection = ModerationManager.moderateImage(bytes)
            if (rejection != null) {
                AppLogger.w(TAG, "Avatar rejected by moderation: $rejection")
                return Result.failure(IllegalArgumentException("image_rejected"))
            }
            val path = "avatars/$userId.jpg"
            supabase.storage.from("photos").upload(path, bytes) { upsert = true }
            // Update profile to mark avatar as present
            supabase.postgrest["profiles"].update({
                set("avatar_url", path)
            }) {
                filter { eq("id", userId) }
            }
            // Cache locally
            try { LocalPhotoStore.saveAvatar("profile", bytes) } catch (e: Exception) {
                AppLogger.w(TAG, "Avatar local cache failed", e)
            }
            bumpProfileVersion()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Avatar upload failed", e)
            Result.failure(e)
        }
    }

    suspend fun removeProfileAvatar(): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
            try {
                supabase.storage.from("photos").delete("avatars/$userId.jpg")
            } catch (e: Exception) {
                AppLogger.w(TAG, "Avatar storage delete failed", e)
            }
            supabase.postgrest["profiles"].update({
                set("avatar_url", "")
            }) {
                filter { eq("id", userId) }
            }
            try { LocalPhotoStore.deleteAvatar("profile") } catch (e: Exception) {
                AppLogger.w(TAG, "Avatar local clear failed", e)
            }
            bumpProfileVersion()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Avatar removal failed", e)
            Result.failure(e)
        }
    }

    suspend fun downloadProfileAvatar(): ByteArray? {
        val userId = currentUserId ?: return null
        // Try local cache first
        try {
            val cached = LocalPhotoStore.loadAvatar("profile")
            if (cached != null && cached.isNotEmpty()) return cached
        } catch (e: Exception) {
            AppLogger.w(TAG, "Avatar local cache read failed", e)
        }
        // Download from storage
        return try {
            val path = "avatars/$userId.jpg"
            val url = supabase.storage.from("photos").createSignedUrl(path, pg.geobingo.one.game.GameConstants.AVATAR_URL_EXPIRY)
            val bytes: ByteArray = ServiceLocator.httpClient.get(url).body()
            try { LocalPhotoStore.saveAvatar("profile", bytes) } catch (e: Exception) {
                AppLogger.w(TAG, "Avatar download cache failed", e)
            }
            bytes
        } catch (e: Exception) {
            AppLogger.d(TAG, "Profile avatar download failed", e)
            null
        }
    }

    // ── App Startup (handles OAuth redirect on web) ──────────────────

    /**
     * Called on app startup. Awaits auth session restoration (important for
     * web OAuth redirects where the page reloads) and runs profile setup
     * + cloud sync if a user session exists.
     */
    suspend fun handleAppStartup() {
        try {
            supabase.auth.awaitInitialization()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Auth initialization failed", e)
        }
        val userId = currentUserId ?: return

        // Detect user switch since last session (e.g. email -> Apple login, or
        // a guest user who just logged in for the first time). If the user_id
        // changed OR there was no previous logged-in user, we wipe per-user
        // local state so the previous identity's progress / name / avatar don't
        // leak into the new session. The cloud sync below then hydrates all
        // cloud-backed fields (stars, games_played/won, display_name, avatar)
        // with the authoritative cloud values. Device-level preferences
        // (sound, haptic, language, consent) are intentionally preserved.
        val lastKnownUserId = AppSettings.getString("last_known_user_id", "")
        val isUserSwitch = lastKnownUserId.isNotEmpty() && lastKnownUserId != userId
        if (isUserSwitch) {
            AppLogger.i(TAG, "User switched (${lastKnownUserId.take(8)}... -> ${userId.take(8)}...), clearing local state")
            clearUserScopedLocalState()
        }
        AppSettings.setString("last_known_user_id", userId)

        val email = currentUser?.email ?: ""
        createProfileIfNeeded(userId, email)
        // syncCloudToLocal pulls profile data + cosmetics in one go now —
        // see the cosmetics block inside syncCloudToLocal. After it returns,
        // the new user's unlocks and equipped state are visible.
        syncCloudToLocal(userId)
        // After cloud->local sync, refresh the in-memory StarsState so any UI
        // that already composed with the pre-sync values picks up the new ones.
        try {
            ServiceLocator.gameState.stars.reload()
        } catch (e: Exception) {
            AppLogger.w(TAG, "StarsState reload after sync failed", e)
        }
        // Only push local->cloud when we didn't just wipe local (otherwise we'd
        // be pushing freshly-cleared zeros back up). For same-user sessions this
        // preserves the offline-progress-survives-reconnect semantics.
        if (!isUserSwitch) {
            syncLocalToCloud(userId)
        }
    }

    /**
     * Clear all per-user local state. Called on sign-out and on user-switch
     * detection so the previous identity's data never bleeds into the next
     * session. Device-level preferences (sound, haptic, language, consent,
     * onboarding-completed) are intentionally preserved — they belong to the
     * device, not the user.
     *
     * Cloud-backed fields (stars, skip_cards, games_played/won, longest_streak,
     * display_name, avatar) are re-hydrated from cloud on the next login. Non-
     * cloud-backed fields (mode counts, totals, achievements, daily/weekly
     * challenge state) reset to defaults for the new user — future schema
     * migrations will promote these to cloud-backed.
     *
     * NO_ADS_PURCHASED is re-verified from StoreKit on app init (BillingBridge),
     * so we clear the flag here but it auto-restores if the Apple ID owns it.
     */
    private fun clearUserScopedLocalState() {
        // Identity
        AppSettings.setString("last_player_name", "")
        // Cloud-backed economy / stats (will be re-hydrated from cloud)
        AppSettings.setInt(SettingsKeys.STAR_COUNT, 0)
        AppSettings.setInt(SettingsKeys.SKIP_CARDS_COUNT, 0)
        AppSettings.setBoolean(SettingsKeys.NO_ADS_PURCHASED, false)
        AppSettings.setInt(SettingsKeys.GAMES_PLAYED, 0)
        AppSettings.setInt(SettingsKeys.GAMES_WON, 0)
        AppSettings.setInt(SettingsKeys.LONGEST_WIN_STREAK, 0)
        AppSettings.setInt(SettingsKeys.CURRENT_WIN_STREAK, 0)
        // Device-only statistics (reset on user switch — not in cloud schema yet)
        AppSettings.setInt(SettingsKeys.TOTAL_STARS_EARNED, 0)
        AppSettings.setInt(SettingsKeys.TOTAL_STARS_COUNT, 0)
        AppSettings.setInt(SettingsKeys.TOTAL_CAPTURES, 0)
        AppSettings.setInt(SettingsKeys.TOTAL_SPEED_BONUSES, 0)
        AppSettings.setInt(SettingsKeys.BEST_GAME_SCORE, 0)
        AppSettings.setInt(SettingsKeys.TOTAL_GAME_TIME_SECONDS, 0)
        AppSettings.setInt(SettingsKeys.TOTAL_CATEGORIES_PLAYED, 0)
        AppSettings.setString(SettingsKeys.FAVORITE_MODE, "")
        AppSettings.setInt(SettingsKeys.MODE_CLASSIC_COUNT, 0)
        AppSettings.setInt(SettingsKeys.MODE_BLIND_COUNT, 0)
        AppSettings.setInt(SettingsKeys.MODE_WEIRD_COUNT, 0)
        AppSettings.setInt(SettingsKeys.MODE_QUICK_COUNT, 0)
        AppSettings.setInt(SettingsKeys.MODE_AI_JUDGE_COUNT, 0)
        // Daily / weekly challenge state (per-user)
        AppSettings.setInt(SettingsKeys.ADS_WATCHED_TODAY, 0)
        AppSettings.setString(SettingsKeys.LAST_AD_DATE, "")
        AppSettings.setString(SettingsKeys.LAST_LOGIN_DATE, "")
        AppSettings.setString(SettingsKeys.LAST_DAILY_DATE, "")
        AppSettings.setBoolean(SettingsKeys.DAILY_CHALLENGE_COMPLETED, false)
        AppSettings.setString(SettingsKeys.DAILY_CHALLENGE_TYPE, "")
        AppSettings.setBoolean(SettingsKeys.EXTREME_MODE_UNLOCKED, false)
        AppSettings.setInt(SettingsKeys.WEEKLY_CHALLENGE_PROGRESS, 0)
        AppSettings.setBoolean(SettingsKeys.WEEKLY_CHALLENGE_COMPLETED, false)
        AppSettings.setString(SettingsKeys.LAST_WEEKLY_WEEK, "")
        // AI/data-sharing consent must be re-obtained per user (Apple guideline
        // 5.1.1(i)) — it's a privacy decision tied to the user, not the device.
        AppSettings.setBoolean(SettingsKeys.AI_CONSENT_ACCEPTED, false)
        // Cosmetics: owned + equipped + migration flag. Without this, user A's
        // unlocks register as owned for user B until cloud sync overrides them
        // (and cloud sync is additive, so equipped-state never gets pushed back
        // to "_none"). See CosmeticsManager.clearLocalUserState for details.
        pg.geobingo.one.game.state.CosmeticsManager.clearLocalUserState()
        // Cached avatar blob — actually remove the file (writing empty bytes
        // leaves a 0-byte file that some decoders still try to parse, causing
        // the old photo to linger after sign-out).
        try {
            LocalPhotoStore.deleteAvatar("profile")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Avatar clear failed", e)
        }
        // Recursively wipe every cached game folder. Each folder contains
        // photos (with EXIF location + faces) and a meta.json — privacy-
        // sensitive per-user data that must not survive a sign-out into the
        // next user's session. Until now these folders were leaking on disk
        // (UI couldn't reach them because the gameHistory was cleared, but
        // they'd be backed up to iCloud and visible to anyone with file
        // access). Apple guideline 5.1.1: account-bound data should be
        // removed on logout.
        try {
            LocalPhotoStore.deleteAllGameData()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Game data clear failed", e)
        }
        // Refresh in-memory StarsState so the UI picks up the cleared values
        try {
            ServiceLocator.gameState.stars.reload()
        } catch (e: Exception) {
            AppLogger.w(TAG, "StarsState reload after clear failed", e)
        }
        // Clear per-user game history — the previous identity's match history
        // should not leak into the new session.
        try {
            ServiceLocator.gameState.ui.clearGameHistory()
        } catch (e: Exception) {
            AppLogger.w(TAG, "gameHistory clear failed", e)
        }
        // Notify Compose screens that the profile identity has changed so
        // cached display-name / avatar reads recompose.
        bumpProfileVersion()
    }

    // ── Cloud Sync ─────────────────────────────────────────────────

    suspend fun syncLocalToCloud(userId: String) {
        try {
            val profile = UserProfile(
                id = userId,
                display_name = AppSettings.getString("last_player_name", ""),
                star_count = AppSettings.getInt(SettingsKeys.STAR_COUNT, 0),
                skip_cards_count = AppSettings.getInt(SettingsKeys.SKIP_CARDS_COUNT, 0),
                no_ads_purchased = AppSettings.getBoolean(SettingsKeys.NO_ADS_PURCHASED, false),
                games_played = AppSettings.getInt(SettingsKeys.GAMES_PLAYED, 0),
                games_won = AppSettings.getInt(SettingsKeys.GAMES_WON, 0),
                longest_win_streak = AppSettings.getInt(SettingsKeys.LONGEST_WIN_STREAK, 0),
                sound_enabled = AppSettings.getBoolean(SettingsKeys.SOUND_ENABLED, true),
                haptic_enabled = AppSettings.getBoolean(SettingsKeys.HAPTIC_ENABLED, true),
                language = AppSettings.getString(SettingsKeys.LANGUAGE, "de"),
            )
            supabase.postgrest["profiles"].upsert(profile)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Sync to cloud failed", e)
        }
    }

    suspend fun syncCloudToLocal(userId: String) {
        try {
            val profile = supabase.postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<UserProfile>() ?: return

            // Batch all settings writes: read local values first, then write all at once
            val localStars = AppSettings.getInt(SettingsKeys.STAR_COUNT, 0)
            val localSkipCards = AppSettings.getInt(SettingsKeys.SKIP_CARDS_COUNT, 0)
            val localPlayed = AppSettings.getInt(SettingsKeys.GAMES_PLAYED, 0)
            val localWon = AppSettings.getInt(SettingsKeys.GAMES_WON, 0)
            val localStreak = AppSettings.getInt(SettingsKeys.LONGEST_WIN_STREAK, 0)

            // Take the higher value for progress-related fields (don't lose progress)
            AppSettings.setInt(SettingsKeys.STAR_COUNT, maxOf(localStars, profile.star_count))
            AppSettings.setInt(SettingsKeys.SKIP_CARDS_COUNT, maxOf(localSkipCards, profile.skip_cards_count))
            if (profile.no_ads_purchased) AppSettings.setBoolean(SettingsKeys.NO_ADS_PURCHASED, true)
            AppSettings.setInt(SettingsKeys.GAMES_PLAYED, maxOf(localPlayed, profile.games_played))
            AppSettings.setInt(SettingsKeys.GAMES_WON, maxOf(localWon, profile.games_won))
            AppSettings.setInt(SettingsKeys.LONGEST_WIN_STREAK, maxOf(localStreak, profile.longest_win_streak))
            if (profile.display_name.isNotBlank()) AppSettings.setString("last_player_name", profile.display_name)
            AppSettings.setBoolean(SettingsKeys.SOUND_ENABLED, profile.sound_enabled)
            AppSettings.setBoolean(SettingsKeys.HAPTIC_ENABLED, profile.haptic_enabled)
            AppSettings.setString(SettingsKeys.LANGUAGE, profile.language)

            // Avatar is ACCOUNT-BOUND, not device-bound: the local file is a
            // cache, the cloud is the source of truth. Force the cache to match
            // whatever the cloud says — don't trust whatever was sitting in
            // local storage from a previous session / another user / a guest
            // photo taken before login.
            try {
                if (profile.avatar_url.isNotBlank()) {
                    // Cloud has an avatar — (re)download, bypassing any stale
                    // cache that might belong to a different user.
                    val path = profile.avatar_url
                    val url = supabase.storage.from("photos").createSignedUrl(
                        path,
                        pg.geobingo.one.game.GameConstants.AVATAR_URL_EXPIRY,
                    )
                    val bytes: ByteArray = ServiceLocator.httpClient.get(url).body()
                    LocalPhotoStore.saveAvatar("profile", bytes)
                } else {
                    // Cloud has no avatar — wipe local so a leftover photo
                    // (e.g. from a guest session before this sign-in) can't
                    // masquerade as this user's profile picture.
                    LocalPhotoStore.deleteAvatar("profile")
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Avatar sync failed", e)
            }
            // Cosmetics live in their own tables (owned_cosmetics + the
            // equipped_* columns on profiles). Pull them here so every
            // sign-in path picks up the new user's unlocks/equipped state
            // — without this, switching accounts on the same device would
            // leave the previous user's wiped-then-empty cosmetic mirror
            // until the next app restart.
            try {
                pg.geobingo.one.game.state.CosmeticsManager.syncFromCloud()
            } catch (e: Exception) {
                AppLogger.w(TAG, "Cosmetics sync from cloud failed", e)
            }
            // Notify UI — display name and/or avatar may have changed
            bumpProfileVersion()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Sync from cloud failed", e)
        }
    }

    private suspend fun createProfileIfNeeded(userId: String, email: String) {
        try {
            val existing = supabase.postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<UserProfile>()
            if (existing == null) {
                supabase.postgrest["profiles"].insert(
                    UserProfile(
                        id = userId,
                        email = email,
                        display_name = AppSettings.getString("last_player_name", ""),
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Profile creation failed", e)
        }
    }
}
