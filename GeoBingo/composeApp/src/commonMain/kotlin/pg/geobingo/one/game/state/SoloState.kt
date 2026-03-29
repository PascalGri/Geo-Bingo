package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pg.geobingo.one.data.Category

/**
 * State for solo offline challenge mode.
 * Player photographs categories against the clock.
 * Score = sum of AI ratings + time bonus (seconds remaining if all captured).
 */
class SoloState {
    var categories by mutableStateOf(listOf<Category>())
    var capturedCategories by mutableStateOf(setOf<String>())
    var captureTimestamps by mutableStateOf(mapOf<String, Long>()) // categoryId -> epochMillis
    var categoryRatings by mutableStateOf(mapOf<String, Int>()) // categoryId -> AI rating 1-10
    var categoryReasons by mutableStateOf(mapOf<String, String>()) // categoryId -> AI reason
    var validatingCategories by mutableStateOf(setOf<String>()) // categories currently being validated
    var isRunning by mutableStateOf(false)
    var totalDurationSeconds by mutableStateOf(300) // 5 min default
    var timeRemainingSeconds by mutableStateOf(300)
    var startTimeMillis by mutableStateOf(0L)
    var playerName by mutableStateOf("")

    /** Sum of all AI ratings (each 1-10). */
    val ratingScore: Int get() = categoryRatings.values.sum()

    /** Time bonus: remaining seconds when all captured, or 0 if time ran out. */
    val timeBonus: Int get() = if (capturedCategories.size == categories.size) timeRemainingSeconds else 0

    /** Total score: sum of AI ratings + time bonus. */
    val totalScore: Int get() = ratingScore + timeBonus

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
