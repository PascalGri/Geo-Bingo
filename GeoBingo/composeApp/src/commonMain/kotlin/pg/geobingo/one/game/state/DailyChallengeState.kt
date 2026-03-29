package pg.geobingo.one.game.state

import kotlinx.datetime.toLocalDateTime
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys

enum class ChallengeType {
    WIN_ROUND,
    PLAY_MODE,
    CAPTURE_CATEGORIES,
}

data class DailyChallenge(
    val type: ChallengeType,
    val descriptionKey: String,
    val reward: Int = 5,
    val targetMode: String? = null,
)

// ── Weekly Challenges ──────────────────────────────────────────────────

enum class WeeklyChallengeType {
    WIN_ROUNDS,
    PLAY_ROUNDS,
    CAPTURE_TOTAL,
    PLAY_ALL_MODES,
    WIN_STREAK,
}

data class WeeklyChallenge(
    val type: WeeklyChallengeType,
    val descriptionKey: String,
    val target: Int,
    val reward: Int = 20,
)

private val CHALLENGES_30_DAYS = listOf(
    DailyChallenge(ChallengeType.WIN_ROUND, "win_round"),
    DailyChallenge(ChallengeType.PLAY_MODE, "play_classic", targetMode = "CLASSIC"),
    DailyChallenge(ChallengeType.CAPTURE_CATEGORIES, "capture_3"),
    DailyChallenge(ChallengeType.PLAY_MODE, "play_blind", targetMode = "BLIND_BINGO"),
    DailyChallenge(ChallengeType.WIN_ROUND, "win_round"),
    DailyChallenge(ChallengeType.CAPTURE_CATEGORIES, "capture_3"),
    DailyChallenge(ChallengeType.PLAY_MODE, "play_weird", targetMode = "WEIRD_CORE"),
    DailyChallenge(ChallengeType.WIN_ROUND, "win_round"),
    DailyChallenge(ChallengeType.PLAY_MODE, "play_quick", targetMode = "QUICK_START"),
    DailyChallenge(ChallengeType.CAPTURE_CATEGORIES, "capture_3"),
    DailyChallenge(ChallengeType.WIN_ROUND, "win_round"),
    DailyChallenge(ChallengeType.PLAY_MODE, "play_classic", targetMode = "CLASSIC"),
    DailyChallenge(ChallengeType.CAPTURE_CATEGORIES, "capture_3"),
    DailyChallenge(ChallengeType.PLAY_MODE, "play_blind", targetMode = "BLIND_BINGO"),
    DailyChallenge(ChallengeType.WIN_ROUND, "win_round"),
    DailyChallenge(ChallengeType.PLAY_MODE, "play_weird", targetMode = "WEIRD_CORE"),
    DailyChallenge(ChallengeType.CAPTURE_CATEGORIES, "capture_3"),
    DailyChallenge(ChallengeType.WIN_ROUND, "win_round"),
    DailyChallenge(ChallengeType.PLAY_MODE, "play_quick", targetMode = "QUICK_START"),
    DailyChallenge(ChallengeType.CAPTURE_CATEGORIES, "capture_3"),
    DailyChallenge(ChallengeType.WIN_ROUND, "win_round"),
    DailyChallenge(ChallengeType.PLAY_MODE, "play_classic", targetMode = "CLASSIC"),
    DailyChallenge(ChallengeType.CAPTURE_CATEGORIES, "capture_3"),
    DailyChallenge(ChallengeType.PLAY_MODE, "play_blind", targetMode = "BLIND_BINGO"),
    DailyChallenge(ChallengeType.WIN_ROUND, "win_round"),
    DailyChallenge(ChallengeType.CAPTURE_CATEGORIES, "capture_3"),
    DailyChallenge(ChallengeType.PLAY_MODE, "play_weird", targetMode = "WEIRD_CORE"),
    DailyChallenge(ChallengeType.WIN_ROUND, "win_round"),
    DailyChallenge(ChallengeType.PLAY_MODE, "play_quick", targetMode = "QUICK_START"),
    DailyChallenge(ChallengeType.CAPTURE_CATEGORIES, "capture_3"),
)

object DailyChallengeManager {
    fun getTodayChallenge(): DailyChallenge {
        val dayIndex = dayOfYear() % CHALLENGES_30_DAYS.size
        return CHALLENGES_30_DAYS[dayIndex]
    }

    fun isCompleted(): Boolean =
        AppSettings.getBoolean(SettingsKeys.DAILY_CHALLENGE_COMPLETED, false)

    private fun dayOfYear(): Int {
        val now = kotlinx.datetime.Clock.System.now()
        val utc = now.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        return utc.dayOfYear
    }
}

// ── Weekly Challenge Pool (8 rotating challenges) ──────────────────────

private val WEEKLY_CHALLENGES = listOf(
    WeeklyChallenge(WeeklyChallengeType.WIN_ROUNDS, "win_3_rounds", target = 3, reward = 20),
    WeeklyChallenge(WeeklyChallengeType.PLAY_ROUNDS, "play_5_rounds", target = 5, reward = 15),
    WeeklyChallenge(WeeklyChallengeType.CAPTURE_TOTAL, "capture_15", target = 15, reward = 20),
    WeeklyChallenge(WeeklyChallengeType.PLAY_ALL_MODES, "play_all_modes", target = 4, reward = 25),
    WeeklyChallenge(WeeklyChallengeType.WIN_STREAK, "win_streak_3", target = 3, reward = 30),
    WeeklyChallenge(WeeklyChallengeType.PLAY_ROUNDS, "play_7_rounds", target = 7, reward = 20),
    WeeklyChallenge(WeeklyChallengeType.WIN_ROUNDS, "win_5_rounds", target = 5, reward = 30),
    WeeklyChallenge(WeeklyChallengeType.CAPTURE_TOTAL, "capture_25", target = 25, reward = 25),
)

object WeeklyChallengeManager {
    fun getThisWeekChallenge(): WeeklyChallenge {
        val weekIndex = weekOfYear() % WEEKLY_CHALLENGES.size
        return WEEKLY_CHALLENGES[weekIndex]
    }

    fun isCompleted(): Boolean =
        AppSettings.getBoolean(SettingsKeys.WEEKLY_CHALLENGE_COMPLETED, false)

    fun getProgress(): Int =
        AppSettings.getInt(SettingsKeys.WEEKLY_CHALLENGE_PROGRESS, 0)

    fun setProgress(value: Int) {
        AppSettings.setInt(SettingsKeys.WEEKLY_CHALLENGE_PROGRESS, value)
    }

    private fun weekOfYear(): Int {
        val now = kotlinx.datetime.Clock.System.now()
        val utc = now.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        return utc.dayOfYear / 7
    }
}
