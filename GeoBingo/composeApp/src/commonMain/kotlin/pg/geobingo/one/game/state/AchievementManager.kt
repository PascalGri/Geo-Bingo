package pg.geobingo.one.game.state

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import pg.geobingo.one.platform.AppSettings

data class Achievement(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val icon: ImageVector,
)

/**
 * Achievement system for solo mode.
 * Checks conditions after each game and unlocks new achievements.
 */
object AchievementManager {
    private const val PREFIX = "achievement_"

    val ALL_ACHIEVEMENTS = listOf(
        Achievement("perfect_game", "Perfect Game", "Alle Kategorien mit 5 Sternen abgeschlossen", Icons.Default.Star),
        Achievement("speed_demon", "Speed Demon", "Alle Kategorien mit 2+ Minuten Restzeit", Icons.Default.Bolt),
        Achievement("ten_games", "10 Spiele", "10 Solo-Spiele gespielt", Icons.Default.SportsScore),
        Achievement("fifty_games", "50 Spiele", "50 Solo-Spiele gespielt", Icons.Default.EmojiEvents),
        Achievement("score_300", "Score-Meister", "300+ Punkte in einem Spiel erreicht", Icons.AutoMirrored.Filled.TrendingUp),
        Achievement("score_500", "Score-Legende", "500+ Punkte in einem Spiel erreicht", Icons.Default.Whatshot),
        Achievement("outdoor_10", "Outdoor-Experte", "10 Outdoor-Spiele gewonnen", Icons.Default.WbSunny),
        Achievement("indoor_10", "Indoor-Experte", "10 Indoor-Spiele gewonnen", Icons.Default.House),
        Achievement("star_100", "Sternesammler", "100 Sterne insgesamt verdient", Icons.Default.Stars),
        Achievement("star_500", "Sternenjäger", "500 Sterne insgesamt verdient", Icons.Default.AutoAwesome),
        Achievement("all_captured_10", "Vollständig x10", "10x alle Kategorien in einem Spiel eingefangen", Icons.Default.CheckCircle),
        Achievement("marathon", "Marathon", "Ein 10-Kategorien-Spiel abgeschlossen", Icons.AutoMirrored.Filled.DirectionsRun),
        Achievement("perfect_3", "Perfektion x3", "3 perfekte Spiele abgeschlossen", Icons.Default.Verified),
        Achievement("fast_finish", "Blitzschnell", "Alle Kategorien in unter 2 Minuten", Icons.Default.Timer),
    )

    fun isUnlocked(id: String): Boolean = AppSettings.getBoolean("$PREFIX$id", false)

    private fun unlock(id: String) = AppSettings.setBoolean("$PREFIX$id", true)

    fun getUnlockedCount(): Int = ALL_ACHIEVEMENTS.count { isUnlocked(it.id) }

    fun areAllUnlocked(): Boolean = getUnlockedCount() == ALL_ACHIEVEMENTS.size

    private const val ALL_COMPLETE_KEY = "achievement_all_complete_rewarded"
    private const val ALL_COMPLETE_REWARD = 50

    /**
     * Check and unlock achievements after a solo game.
     * Returns the list of **newly** unlocked achievements.
     * Awards 50 bonus stars when all achievements are completed for the first time.
     */
    fun checkAfterGame(solo: SoloState, stats: SoloStats, starsState: StarsState? = null): List<Achievement> {
        val newlyUnlocked = mutableListOf<Achievement>()

        fun check(id: String, condition: Boolean) {
            if (condition && !isUnlocked(id)) {
                unlock(id)
                ALL_ACHIEVEMENTS.find { it.id == id }?.let { newlyUnlocked.add(it) }
            }
        }

        val allCaptured = solo.capturedCategories.size == solo.categories.size && solo.categories.isNotEmpty()
        val timeUsed = solo.totalDurationSeconds - solo.timeRemainingSeconds

        check("perfect_game", solo.isPerfectGame)
        check("speed_demon", allCaptured && solo.timeRemainingSeconds >= 120)
        check("ten_games", stats.gamesPlayed >= 10)
        check("fifty_games", stats.gamesPlayed >= 50)
        check("score_300", solo.totalScore >= 300)
        check("score_500", solo.totalScore >= 500)
        check("outdoor_10", stats.outdoorGames >= 10)
        check("indoor_10", stats.indoorGames >= 10)
        check("star_100", stats.totalStars >= 100)
        check("star_500", stats.totalStars >= 500)
        check("all_captured_10", stats.allCapturedCount >= 10)
        check("marathon", solo.categoryCount == 10 && allCaptured)
        check("perfect_3", stats.perfectGames >= 3)
        check("fast_finish", allCaptured && timeUsed <= 120)

        // Award 50 bonus stars when all achievements are completed for the first time
        if (newlyUnlocked.isNotEmpty() && areAllUnlocked() && !AppSettings.getBoolean(ALL_COMPLETE_KEY, false)) {
            AppSettings.setBoolean(ALL_COMPLETE_KEY, true)
            starsState?.add(ALL_COMPLETE_REWARD)
        }

        return newlyUnlocked
    }
}
