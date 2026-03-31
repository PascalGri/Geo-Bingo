package pg.geobingo.one.game

import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.state.GamePlayState
import pg.geobingo.one.game.state.ReviewState
import pg.geobingo.one.network.CaptureDto
import pg.geobingo.one.network.VoteDto

/**
 * Encapsulates all scoring logic: player scores, speed bonuses, rankings.
 * Reads from GamePlayState and ReviewState but does not mutate them.
 * Caches expensive computations and invalidates when underlying data changes.
 */
class ScoringManager(
    private val gameplay: GamePlayState,
    private val review: ReviewState,
) {
    // Cache for first capturers per category (invalidated when captures change)
    private var cachedFirstCapturers: Map<String, String>? = null
    private var cachedFirstCapturersVersion = -1

    // Cache for category average ratings (invalidated when votes change)
    private var cachedCategoryRatings: MutableMap<String, Double?> = mutableMapOf()
    private var cachedRatingsVoteCount = -1

    private fun capturesVersion(): Int = review.allCaptures.size
    private fun votesVersion(): Int = review.allVotes.size

    fun getFirstCapturers(): Map<String, String> {
        val version = capturesVersion()
        if (cachedFirstCapturers != null && cachedFirstCapturersVersion == version) {
            return cachedFirstCapturers!!
        }
        if (review.allCaptures.isEmpty()) {
            cachedFirstCapturers = emptyMap()
            cachedFirstCapturersVersion = version
            return emptyMap()
        }
        val result = gameplay.selectedCategories.mapNotNull { category ->
            val first = review.allCaptures
                .filter { it.category_id == category.id && it.created_at.isNotEmpty() }
                .minByOrNull { it.created_at }
            if (first != null) category.id to first.player_id else null
        }.toMap()
        cachedFirstCapturers = result
        cachedFirstCapturersVersion = version
        return result
    }

    fun getSpeedBonusCount(playerId: String): Int {
        return getFirstCapturers().values.count { it == playerId }
    }

    fun getCategoryAverageRating(playerId: String, categoryId: String): Double? {
        val voteCount = votesVersion()
        if (cachedRatingsVoteCount != voteCount) {
            // Votes changed — rebuild entire ratings cache in a single pass
            cachedCategoryRatings.clear()
            cachedRatingsVoteCount = voteCount
            if (review.allVotes.isNotEmpty()) {
                // Group all votes by (target_player_id, category_id) and compute averages
                val grouped = review.allVotes.groupBy { "${it.target_player_id}:${it.category_id}" }
                for ((key, votes) in grouped) {
                    cachedCategoryRatings[key] = votes.map { it.rating }.average()
                }
            }
        }
        return cachedCategoryRatings["$playerId:$categoryId"]
    }

    fun getPlayerAverageRating(playerId: String): Double? {
        val ratings = gameplay.selectedCategories.mapNotNull { getCategoryAverageRating(playerId, it.id) }
        if (ratings.isEmpty()) return null
        return ratings.average()
    }

    fun isCaptured(playerId: String, categoryId: String): Boolean =
        gameplay.captures[playerId]?.contains(categoryId) == true

    fun getVoteResult(playerId: String, categoryId: String, votes: Map<String, List<Boolean>>): Boolean? {
        val key = "$playerId-$categoryId"
        val v = votes[key] ?: return null
        if (v.isEmpty()) return null
        return v.count { it } > v.size / 2
    }

    fun getPlayerScore(playerId: String, votes: Map<String, List<Boolean>>): Int {
        val starScore: Int
        if (review.allVotes.isNotEmpty()) {
            var sum = 0.0
            val capturedCategories = gameplay.captures[playerId] ?: emptySet()
            for (category in gameplay.selectedCategories) {
                if (category.id !in capturedCategories) continue
                val avg = getCategoryAverageRating(playerId, category.id) ?: continue
                sum += avg
            }
            starScore = (sum + 0.5).toInt()
        } else {
            starScore = gameplay.selectedCategories.count { category ->
                if (!isCaptured(playerId, category.id)) return@count false
                getVoteResult(playerId, category.id, votes) ?: true
            }
        }
        return starScore + getSpeedBonusCount(playerId)
    }

    fun getPlayerCaptures(playerId: String): List<Category> {
        val capturedIds = gameplay.captures[playerId] ?: emptySet()
        return gameplay.selectedCategories.filter { it.id in capturedIds }
    }

    fun getLastCaptureTime(playerId: String): String {
        return review.allCaptures
            .filter { it.player_id == playerId && it.created_at.isNotEmpty() }
            .maxOfOrNull { it.created_at } ?: GameConstants.INFINITY_TIME
    }
}
