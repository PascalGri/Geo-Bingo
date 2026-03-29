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
