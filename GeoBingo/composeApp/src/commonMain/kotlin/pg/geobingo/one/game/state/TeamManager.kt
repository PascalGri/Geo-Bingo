package pg.geobingo.one.game.state

import pg.geobingo.one.data.Player
import pg.geobingo.one.game.ScoringManager

/**
 * Encapsulates all team-related game logic.
 * Extracted from GameState to reduce its responsibility surface.
 */
class TeamManager(
    private val session: SessionState,
    private val gameplay: GamePlayState,
    private val review: ReviewState,
    private val scoring: ScoringManager,
) {
    val isTeamMode: Boolean get() = gameplay.teamModeEnabled

    fun getTeamNumbers(): List<Int> =
        gameplay.teamAssignments.values.toSet().sorted()

    fun getTeamPlayers(teamNumber: Int): List<Player> =
        gameplay.players.filter { gameplay.teamAssignments[it.id] == teamNumber }

    /** All captures from any member of a team, merged. */
    fun getTeamCaptures(teamNumber: Int): Set<String> {
        val teamPlayerIds = getTeamPlayers(teamNumber).map { it.id }
        return teamPlayerIds.flatMap { gameplay.captures[it] ?: emptySet() }.toSet()
    }

    fun isTeamCaptured(teamNumber: Int, categoryId: String): Boolean =
        getTeamCaptures(teamNumber).contains(categoryId)

    /** Find which player on a team captured a specific category. */
    fun getTeamCapturer(teamNumber: Int, categoryId: String): Player? {
        val teamPlayers = getTeamPlayers(teamNumber)
        // Prefer allCaptures (server truth) if available
        if (review.allCaptures.isNotEmpty()) {
            val teamPlayerIds = teamPlayers.map { it.id }.toSet()
            val capture = review.allCaptures
                .filter { it.category_id == categoryId && it.player_id in teamPlayerIds }
                .minByOrNull { it.created_at }
            if (capture != null) return teamPlayers.find { it.id == capture.player_id }
        }
        // Fallback to local captures
        return teamPlayers.firstOrNull { gameplay.captures[it.id]?.contains(categoryId) == true }
    }

    fun getMyTeamNumber(): Int? =
        gameplay.teamAssignments[session.myPlayerId]

    /** Team score: sum of star ratings + speed bonuses. */
    fun getTeamScore(teamNumber: Int): Int {
        if (review.allVotes.isNotEmpty()) {
            var starScore = 0.0
            for (category in gameplay.selectedCategories) {
                val capturer = getTeamCapturer(teamNumber, category.id) ?: continue
                val avg = scoring.getCategoryAverageRating(capturer.id, category.id) ?: continue
                starScore += avg
            }
            val speedBonus = getTeamSpeedBonusCount(teamNumber)
            return (starScore + 0.5).toInt() + speedBonus
        }
        // Fallback: count captured categories
        return getTeamCaptures(teamNumber).size
    }

    /** Speed bonuses at team level. */
    fun getTeamSpeedBonusCount(teamNumber: Int): Int {
        val firstCapturers = scoring.getFirstCapturers()
        val teamPlayerIds = getTeamPlayers(teamNumber).map { it.id }.toSet()
        return firstCapturers.values.count { it in teamPlayerIds }
    }

    /** Ranked teams by score. Returns (teamNumber, teamName, score). */
    val rankedTeams: List<Triple<Int, String, Int>>
        get() {
            if (!gameplay.teamModeEnabled) return emptyList()
            return getTeamNumbers().map { teamNum ->
                val teamName = gameplay.teamNames[teamNum] ?: "Team $teamNum"
                Triple(teamNum, teamName, getTeamScore(teamNum))
            }.sortedByDescending { it.third }
        }
}
