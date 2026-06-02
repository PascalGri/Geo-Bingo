package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pg.geobingo.one.data.Category

/**
 * The different solo game variants. IMPORTANT: only [STANDARD] submits to the
 * shared online leaderboard (solo_scores). Every other mode is a personal /
 * local experience — see the gate in SoloResultsScreen. This is intentional
 * so the global ranking stays comparable (same rules for everyone).
 */
enum class SoloMode {
    /** Classic AI solo challenge — the ONLY mode that counts for the leaderboard. */
    STANDARD,
    /** Endless / Survival — handled by its own screen, local best streak. */
    ENDLESS,
    /** Curated Daily Run — date-seeded set, same for everyone, different each day. */
    DAILY_RUN,
    /** Weird Core solo — absurd categories, reuses the standard grid screen. */
    WEIRD_CORE,
    /** Category Roulette — one rolled category group, 5 tasks from it. */
    CATEGORY_ROULETTE,
}

/**
 * State for solo challenge mode with AI photo validation.
 *
 * Scoring formula:
 *   starScore       = sum of AI ratings (each 1-5) * 10
 *   timeBonus       = remaining seconds (only when ALL categories captured)
 *   perfectBonus    = 100 (only when ALL categories rated 5 stars)
 *   totalScore      = starScore + timeBonus + perfectBonus
 */
class SoloState {
    var mode by mutableStateOf(SoloMode.STANDARD)
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
    var isOutdoor by mutableStateOf(true)
    var categoryCount by mutableStateOf(5) // 5 or 10

    /** Guards: set once on the results screen so stats/history are recorded a
     * single time and the leaderboard score is submitted once — even if the
     * user leaves this screen (e.g. to sign in / set a name) and returns. */
    var resultsProcessed by mutableStateOf(false)
    var scoreSubmitted by mutableStateOf(false)

    /** Raw sum of all AI star ratings (each 1-5). */
    val starSum: Int get() = categoryRatings.values.sum()

    /** Star score weighted x10. */
    val starScore: Int get() = starSum * 10

    /** Time bonus: remaining seconds when all captured, else 0. */
    val timeBonus: Int get() = if (capturedCategories.size == categories.size && categories.isNotEmpty()) timeRemainingSeconds else 0

    /** Perfect game: all categories captured AND all rated 5 stars. */
    val isPerfectGame: Boolean get() =
        categories.isNotEmpty() &&
        categoryRatings.size == categories.size &&
        categoryRatings.values.all { it == 5 }

    /** Perfect game bonus: 100 points if all 5 stars. */
    val perfectBonus: Int get() = if (isPerfectGame) 100 else 0

    /** Total score = starScore + timeBonus + perfectBonus. */
    val totalScore: Int get() = starScore + timeBonus + perfectBonus

    /** Average star rating (for display). */
    val averageRating: Float get() = if (categoryRatings.isNotEmpty()) categoryRatings.values.average().toFloat() else 0f

    /** Speed for each category: seconds it took from game start. */
    fun getCaptureSpeed(categoryId: String): Int {
        val ts = captureTimestamps[categoryId] ?: return 0
        return ((ts - startTimeMillis) / 1000).toInt()
    }

    fun reset() {
        mode = SoloMode.STANDARD
        categories = emptyList()
        capturedCategories = emptySet()
        captureTimestamps = emptyMap()
        categoryRatings = emptyMap()
        categoryReasons = emptyMap()
        validatingCategories = emptySet()
        isRunning = false
        categoryCount = 5
        totalDurationSeconds = 300
        timeRemainingSeconds = totalDurationSeconds
        startTimeMillis = 0L
        isOutdoor = true
        resultsProcessed = false
        scoreSubmitted = false
    }
}
