package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pg.geobingo.one.data.Category

/**
 * State for solo challenge mode with AI photo validation.
 *
 * Scoring formula:
 *   starScore  = sum of AI ratings (each 1-5, max 25 for 5 categories)
 *   timeBonus  = remaining seconds (only when ALL categories captured)
 *   totalScore = starScore * 10 + timeBonus
 *
 * This weights photo quality heavily (max 250) while still rewarding speed (max 300).
 */
class SoloState {
    var categories by mutableStateOf(listOf<Category>())
    var capturedCategories by mutableStateOf(setOf<String>())
    var captureTimestamps by mutableStateOf(mapOf<String, Long>()) // categoryId -> epochMillis
    var categoryRatings by mutableStateOf(mapOf<String, Int>()) // categoryId -> AI rating 1-5
    var categoryReasons by mutableStateOf(mapOf<String, String>()) // categoryId -> AI reason
    var validatingCategories by mutableStateOf(setOf<String>()) // categories currently being validated
    var isRunning by mutableStateOf(false)
    var totalDurationSeconds by mutableStateOf(300) // 5 min default
    var timeRemainingSeconds by mutableStateOf(300)
    var startTimeMillis by mutableStateOf(0L)
    var playerName by mutableStateOf("")

    /** Raw sum of all AI star ratings (each 1-5). */
    val starSum: Int get() = categoryRatings.values.sum()

    /** Star score weighted x10 (max 250 for 5 perfect photos). */
    val starScore: Int get() = starSum * 10

    /** Time bonus: remaining seconds when all captured, else 0. */
    val timeBonus: Int get() = if (capturedCategories.size == categories.size) timeRemainingSeconds else 0

    /** Total score = starScore + timeBonus. */
    val totalScore: Int get() = starScore + timeBonus

    /** Average star rating (for display). */
    val averageRating: Float get() = if (categoryRatings.isNotEmpty()) categoryRatings.values.average().toFloat() else 0f

    /** Speed for each category: seconds it took from game start. */
    fun getCaptureSpeed(categoryId: String): Int {
        val ts = captureTimestamps[categoryId] ?: return 0
        return ((ts - startTimeMillis) / 1000).toInt()
    }

    fun reset() {
        categories = emptyList()
        capturedCategories = emptySet()
        captureTimestamps = emptyMap()
        categoryRatings = emptyMap()
        categoryReasons = emptyMap()
        validatingCategories = emptySet()
        isRunning = false
        timeRemainingSeconds = totalDurationSeconds
        startTimeMillis = 0L
    }
}
