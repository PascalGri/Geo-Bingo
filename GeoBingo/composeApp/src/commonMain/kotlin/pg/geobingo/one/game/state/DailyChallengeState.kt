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
    val description: String,
    val reward: Int,
    val targetMode: String? = null,
)

object DailyChallengeManager {
    fun getTodayChallenge(): DailyChallenge {
        val today = todayString()
        val hash = today.hashCode()
        val types = ChallengeType.entries
        val type = types[(hash and Int.MAX_VALUE) % types.size]
        return when (type) {
            ChallengeType.WIN_ROUND -> DailyChallenge(
                type = type,
                description = "", // filled by UI via i18n
                reward = 30,
            )
            ChallengeType.PLAY_MODE -> {
                val modes = listOf("CLASSIC", "BLIND_BINGO", "WEIRD_CORE", "QUICK_START")
                val mode = modes[(hash and Int.MAX_VALUE) % modes.size]
                DailyChallenge(
                    type = type,
                    description = "",
                    reward = 25,
                    targetMode = mode,
                )
            }
            ChallengeType.CAPTURE_CATEGORIES -> DailyChallenge(
                type = type,
                description = "",
                reward = 25,
            )
        }
    }

    fun isCompleted(): Boolean =
        AppSettings.getBoolean(SettingsKeys.DAILY_CHALLENGE_COMPLETED, false)

    private fun todayString(): String {
        val now = kotlinx.datetime.Clock.System.now()
        val local = now.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        return local.date.toString()
    }
}
