package pg.geobingo.one.network

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.builtin.Email
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
)

object AccountManager {
    private const val TAG = "AccountManager"

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
            supabase.auth.signInWith(Google)
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
            supabase.auth.signInWith(Apple)
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
            // Clear local data
            AppSettings.setString("last_player_name", "")
            try { LocalPhotoStore.saveAvatar("profile", ByteArray(0)) } catch (e: Exception) {
                AppLogger.w(TAG, "Avatar cleanup failed during account deletion", e)
            }
            // Sign out locally
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
            try { LocalPhotoStore.saveAvatar("profile", bytes) } catch (_: Exception) {}
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
            } catch (_: Exception) {}
            supabase.postgrest["profiles"].update({
                set("avatar_url", "")
            }) {
                filter { eq("id", userId) }
            }
            try { LocalPhotoStore.saveAvatar("profile", ByteArray(0)) } catch (_: Exception) {}
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
        } catch (_: Exception) {}
        // Download from storage
        return try {
            val path = "avatars/$userId.jpg"
            val url = supabase.storage.from("photos").createSignedUrl(path, pg.geobingo.one.game.GameConstants.AVATAR_URL_EXPIRY)
            val bytes: ByteArray = ServiceLocator.httpClient.get(url).body()
            try { LocalPhotoStore.saveAvatar("profile", bytes) } catch (_: Exception) {}
            bytes
        } catch (e: Exception) {
            AppLogger.d(TAG, "Profile avatar download failed", e)
            null
        }
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
                } catch (_: Exception) {}
            }
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
