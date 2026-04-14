package pg.geobingo.one.platform

/**
 * Platform-agnostic key-value storage for persistent app settings.
 * Backed by SharedPreferences (Android), NSUserDefaults (iOS),
 * java.util.prefs.Preferences (Desktop), and localStorage (Web).
 */
expect object AppSettings {
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun setBoolean(key: String, value: Boolean)
    fun getString(key: String, default: String = ""): String
    fun setString(key: String, value: String)
    fun getInt(key: String, default: Int = 0): Int
    fun setInt(key: String, value: Int)
}

// Keys
object SettingsKeys {
    const val SOUND_ENABLED = "sound_enabled"
    const val HAPTIC_ENABLED = "haptic_enabled"
    const val AD_CONSENT_GIVEN = "ad_consent_given"
    const val INTERSTITIAL_GAME_COUNT = "interstitial_game_count"
    const val LANGUAGE = "language"
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    const val GAMES_PLAYED = "games_played"
    const val GAMES_WON = "games_won"
    const val LONGEST_WIN_STREAK = "longest_win_streak"
    const val CURRENT_WIN_STREAK = "current_win_streak"
    const val TOTAL_STARS_EARNED = "total_stars_earned"
    const val TOTAL_STARS_COUNT = "total_stars_count"

    // ── Detailed Stats ─────────────────────────────────────────────────
    const val TOTAL_CAPTURES = "total_captures"
    const val TOTAL_SPEED_BONUSES = "total_speed_bonuses"
    const val BEST_GAME_SCORE = "best_game_score"
    const val TOTAL_GAME_TIME_SECONDS = "total_game_time_seconds"
    const val TOTAL_CATEGORIES_PLAYED = "total_categories_played"
    const val FAVORITE_MODE = "favorite_mode"
    const val MODE_CLASSIC_COUNT = "mode_classic_count"
    const val MODE_BLIND_COUNT = "mode_blind_count"
    const val MODE_WEIRD_COUNT = "mode_weird_count"
    const val MODE_QUICK_COUNT = "mode_quick_count"
    const val MODE_AI_JUDGE_COUNT = "mode_ai_judge_count"

    // ── Stars Currency ────────────────────────────────────────────────
    const val STAR_COUNT = "star_count"
    const val ADS_WATCHED_TODAY = "ads_watched_today"
    const val LAST_AD_DATE = "last_ad_date"
    const val LAST_LOGIN_DATE = "last_login_date"
    const val LAST_DAILY_DATE = "last_daily_date"
    const val SKIP_CARDS_COUNT = "skip_cards_count"
    const val NO_ADS_PURCHASED = "no_ads_purchased"
    const val DAILY_CHALLENGE_COMPLETED = "daily_challenge_completed"
    const val DAILY_CHALLENGE_TYPE = "daily_challenge_type"
    const val EXTREME_MODE_UNLOCKED = "extreme_mode_unlocked"
    const val AI_CONSENT_ACCEPTED = "ai_consent_accepted"
    const val GAME_HISTORY_JSON = "game_history_json"

    // ── Weekly Challenges ─────────────────────────────────────────────
    const val WEEKLY_CHALLENGE_PROGRESS = "weekly_challenge_progress"
    const val WEEKLY_CHALLENGE_COMPLETED = "weekly_challenge_completed"
    const val LAST_WEEKLY_WEEK = "last_weekly_week"
}
