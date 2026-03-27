package pg.geobingo.one.network

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.util.AppLogger

@Serializable
data class UserProfile(
    val id: String = "",
    val email: String = "",
    val display_name: String = "",
    val star_count: Int = 0,
    val skip_cards_count: Int = 0,
    val no_ads_purchased: Boolean = false,
    val games_played: Int = 0,
    val games_won: Int = 0,
    val longest_win_streak: Int = 0,
    val sound_enabled: Boolean = true,
    val haptic_enabled: Boolean = true,
    val language: String = "de",
)

object AccountManager {
    private const val TAG = "AccountManager"

    val isLoggedIn: Boolean
        get() = supabase.auth.currentUserOrNull() != null

    val currentUser: UserInfo?
        get() = supabase.auth.currentUserOrNull()

    val currentUserId: String?
        get() = supabase.auth.currentUserOrNull()?.id

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
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Sign out failed", e)
        }
    }

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

            // Take the higher value for stars (don't lose progress)
            val localStars = AppSettings.getInt(SettingsKeys.STAR_COUNT, 0)
            val cloudStars = profile.star_count
            AppSettings.setInt(SettingsKeys.STAR_COUNT, maxOf(localStars, cloudStars))

            val localSkipCards = AppSettings.getInt(SettingsKeys.SKIP_CARDS_COUNT, 0)
            AppSettings.setInt(SettingsKeys.SKIP_CARDS_COUNT, maxOf(localSkipCards, profile.skip_cards_count))

            if (profile.no_ads_purchased) {
                AppSettings.setBoolean(SettingsKeys.NO_ADS_PURCHASED, true)
            }

            // Stats: take higher values
            val localPlayed = AppSettings.getInt(SettingsKeys.GAMES_PLAYED, 0)
            AppSettings.setInt(SettingsKeys.GAMES_PLAYED, maxOf(localPlayed, profile.games_played))
            val localWon = AppSettings.getInt(SettingsKeys.GAMES_WON, 0)
            AppSettings.setInt(SettingsKeys.GAMES_WON, maxOf(localWon, profile.games_won))
            val localStreak = AppSettings.getInt(SettingsKeys.LONGEST_WIN_STREAK, 0)
            AppSettings.setInt(SettingsKeys.LONGEST_WIN_STREAK, maxOf(localStreak, profile.longest_win_streak))

            // Display name: use cloud value if local is empty
            if (profile.display_name.isNotBlank()) {
                AppSettings.setString("last_player_name", profile.display_name)
            }

            // Preferences: use cloud values
            AppSettings.setBoolean(SettingsKeys.SOUND_ENABLED, profile.sound_enabled)
            AppSettings.setBoolean(SettingsKeys.HAPTIC_ENABLED, profile.haptic_enabled)
            AppSettings.setString(SettingsKeys.LANGUAGE, profile.language)
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
