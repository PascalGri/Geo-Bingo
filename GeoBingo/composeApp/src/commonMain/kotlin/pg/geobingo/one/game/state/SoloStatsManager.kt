package pg.geobingo.one.game.state

import pg.geobingo.one.platform.AppSettings

/**
 * Tracks persistent solo mode statistics across games via AppSettings.
 */
object SoloStatsManager {
    private const val SOLO_GAMES_PLAYED = "solo_games_played"
    private const val SOLO_BEST_SCORE = "solo_best_score"
    private const val SOLO_BEST_SCORE_5 = "solo_best_score_5"
    private const val SOLO_BEST_SCORE_10 = "solo_best_score_10"
    private const val SOLO_TOTAL_SCORE = "solo_total_score"
    private const val SOLO_PERFECT_GAMES = "solo_perfect_games"
    private const val SOLO_TOTAL_STARS = "solo_total_stars_earned"
    private const val SOLO_TOTAL_CAPTURES = "solo_total_captures"
    private const val SOLO_FASTEST_COMPLETE = "solo_fastest_complete_seconds"
    private const val SOLO_OUTDOOR_GAMES = "solo_outdoor_games"
    private const val SOLO_INDOOR_GAMES = "solo_indoor_games"
    private const val SOLO_ALL_CAPTURED_COUNT = "solo_all_captured_count"

    fun recordGame(solo: SoloState) {
        val gamesPlayed = AppSettings.getInt(SOLO_GAMES_PLAYED, 0) + 1
        AppSettings.setInt(SOLO_GAMES_PLAYED, gamesPlayed)

        // Total score accumulation
        AppSettings.setInt(SOLO_TOTAL_SCORE, AppSettings.getInt(SOLO_TOTAL_SCORE, 0) + solo.totalScore)

        // Best score overall
        val best = AppSettings.getInt(SOLO_BEST_SCORE, 0)
        if (solo.totalScore > best) AppSettings.setInt(SOLO_BEST_SCORE, solo.totalScore)

        // Best score per category count
        val bestKey = if (solo.categoryCount == 10) SOLO_BEST_SCORE_10 else SOLO_BEST_SCORE_5
        val bestForCount = AppSettings.getInt(bestKey, 0)
        if (solo.totalScore > bestForCount) AppSettings.setInt(bestKey, solo.totalScore)

        // Perfect games
        if (solo.isPerfectGame) {
            AppSettings.setInt(SOLO_PERFECT_GAMES, AppSettings.getInt(SOLO_PERFECT_GAMES, 0) + 1)
        }

        // Stars earned
        AppSettings.setInt(SOLO_TOTAL_STARS, AppSettings.getInt(SOLO_TOTAL_STARS, 0) + solo.starSum)

        // Categories captured
        AppSettings.setInt(SOLO_TOTAL_CAPTURES, AppSettings.getInt(SOLO_TOTAL_CAPTURES, 0) + solo.capturedCategories.size)

        // All captured count
        val allCaptured = solo.capturedCategories.size == solo.categories.size && solo.categories.isNotEmpty()
        if (allCaptured) {
            AppSettings.setInt(SOLO_ALL_CAPTURED_COUNT, AppSettings.getInt(SOLO_ALL_CAPTURED_COUNT, 0) + 1)

            // Fastest complete (seconds used)
            val timeUsed = solo.totalDurationSeconds - solo.timeRemainingSeconds
            val fastest = AppSettings.getInt(SOLO_FASTEST_COMPLETE, Int.MAX_VALUE)
            if (timeUsed < fastest) AppSettings.setInt(SOLO_FASTEST_COMPLETE, timeUsed)
        }

        // Outdoor/Indoor tracking
        if (solo.isOutdoor) {
            AppSettings.setInt(SOLO_OUTDOOR_GAMES, AppSettings.getInt(SOLO_OUTDOOR_GAMES, 0) + 1)
        } else {
            AppSettings.setInt(SOLO_INDOOR_GAMES, AppSettings.getInt(SOLO_INDOOR_GAMES, 0) + 1)
        }
    }

    fun getStats(): SoloStats = SoloStats(
        gamesPlayed = AppSettings.getInt(SOLO_GAMES_PLAYED, 0),
        bestScore = AppSettings.getInt(SOLO_BEST_SCORE, 0),
        bestScore5 = AppSettings.getInt(SOLO_BEST_SCORE_5, 0),
        bestScore10 = AppSettings.getInt(SOLO_BEST_SCORE_10, 0),
        totalScore = AppSettings.getInt(SOLO_TOTAL_SCORE, 0),
        perfectGames = AppSettings.getInt(SOLO_PERFECT_GAMES, 0),
        totalStars = AppSettings.getInt(SOLO_TOTAL_STARS, 0),
        totalCaptures = AppSettings.getInt(SOLO_TOTAL_CAPTURES, 0),
        fastestComplete = AppSettings.getInt(SOLO_FASTEST_COMPLETE, 0).let { if (it == Int.MAX_VALUE) 0 else it },
        outdoorGames = AppSettings.getInt(SOLO_OUTDOOR_GAMES, 0),
        indoorGames = AppSettings.getInt(SOLO_INDOOR_GAMES, 0),
        allCapturedCount = AppSettings.getInt(SOLO_ALL_CAPTURED_COUNT, 0),
    )
}

data class SoloStats(
    val gamesPlayed: Int,
    val bestScore: Int,
    val bestScore5: Int,
    val bestScore10: Int,
    val totalScore: Int,
    val perfectGames: Int,
    val totalStars: Int,
    val totalCaptures: Int,
    val fastestComplete: Int,
    val outdoorGames: Int,
    val indoorGames: Int,
    val allCapturedCount: Int,
) {
    val averageScore: Int get() = if (gamesPlayed > 0) totalScore / gamesPlayed else 0
}
