package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pg.geobingo.one.data.Category

/**
 * State for solo offline challenge mode.
 * Player photographs categories against the clock.
 * Score = base points per category + time bonus (seconds remaining).
 */
class SoloState {
    var categories by mutableStateOf(listOf<Category>())
    var capturedCategories by mutableStateOf(setOf<String>())
    var captureTimestamps by mutableStateOf(mapOf<String, Long>()) // categoryId -> epochMillis
    var isRunning by mutableStateOf(false)
    var totalDurationSeconds by mutableStateOf(300) // 5 min default
    var timeRemainingSeconds by mutableStateOf(300)
    var startTimeMillis by mutableStateOf(0L)
    var playerName by mutableStateOf("")

    /** Base points per captured category. */
    val basePointsPerCategory: Int get() = 10

    /** Time bonus: remaining seconds when all captured, or 0 if time ran out. */
    val timeBonus: Int get() = if (capturedCategories.size == categories.size) timeRemainingSeconds else 0

    /** Total score: base * captured + time bonus. */
    val totalScore: Int get() = capturedCategories.size * basePointsPerCategory + timeBonus

    /** Speed for each category: seconds it took from game start. */
    fun getCaptureSpeed(categoryId: String): Int {
        val ts = captureTimestamps[categoryId] ?: return 0
        return ((ts - startTimeMillis) / 1000).toInt()
    }

    fun reset() {
        categories = emptyList()
        capturedCategories = emptySet()
        captureTimestamps = emptyMap()
        isRunning = false
        timeRemainingSeconds = totalDurationSeconds
        startTimeMillis = 0L
    }
}
