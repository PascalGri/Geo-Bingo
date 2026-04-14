package pg.geobingo.one.network

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.builtin.Email
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
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

    private fun bumpProfileVersion() {
        profileVersion++
    }

    val isLoggedIn: Boolean
        get() = supabase.auth.currentUserOrNull() != null

    val currentUser: UserInfo?
        get() = supabase.auth.currentUserOrNull()

    val currentUserId: String?
        get() = supabase.auth.currentUserOrNull()?.id

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
            AppLogger.w(TAG, "Sign out failed", e)
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
            // Call Edge Function to delete auth user, profile, and avatar server-side
            val url = "${SupabaseConfig.current.url}/functions/v1/delete-account"
            ServiceLocator.httpClient.post(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
                    append("apikey", SupabaseConfig.current.anonKey)
                }
            }
            // Clear all local per-user state and sign out locally
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
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
            AppSettings.setString("last_player_name", name)
            bumpProfileVersion()
            supabase.postgrest["profiles"].update({
                set("display_name", name)
            }) {
                filter { eq("id", userId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Update display name failed", e)
            Result.failure(e)
        }
    }

    suspend fun uploadProfileAvatar(bytes: ByteArray): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
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
            try { LocalPhotoStore.saveAvatar("profile", ByteArray(0)) } catch (e: Exception) {
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
        // Cached avatar blob
        try {
            LocalPhotoStore.saveAvatar("profile", ByteArray(0))
        } catch (e: Exception) {
            AppLogger.w(TAG, "Avatar clear failed", e)
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

            // Download avatar if profile has one and local cache is empty
            if (profile.avatar_url.isNotBlank()) {
                try {
                    val cached = LocalPhotoStore.loadAvatar("profile")
                    if (cached == null || cached.isEmpty()) {
                        downloadProfileAvatar()
                    }
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Avatar sync failed", e)
                }
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
