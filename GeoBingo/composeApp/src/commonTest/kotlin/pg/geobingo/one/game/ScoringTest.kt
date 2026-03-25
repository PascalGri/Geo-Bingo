package pg.geobingo.one.game

import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.state.GamePlayState
import pg.geobingo.one.game.state.ReviewState
import pg.geobingo.one.network.CaptureDto
import pg.geobingo.one.network.VoteDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScoringTest {

    private fun createScoring(
        players: List<Player> = emptyList(),
        categories: List<Category> = emptyList(),
        captures: Map<String, Set<String>> = emptyMap(),
        allCaptures: List<CaptureDto> = emptyList(),
        allVotes: List<VoteDto> = emptyList(),
    ): ScoringManager {
        val gameplay = GamePlayState().apply {
            this.players = players
            this.selectedCategories = categories
            this.captures = captures
        }
        val review = ReviewState().apply {
            this.allCaptures = allCaptures
            this.allVotes = allVotes
        }
        return ScoringManager(gameplay, review)
    }

    @Test
    fun speedBonusGoesToFirstCapturer() {
        val scoring = createScoring(
            categories = listOf(Category("cat1", "Test", "icon")),
            allCaptures = listOf(
                CaptureDto(id = "1", game_id = "g", player_id = "p1", category_id = "cat1", created_at = "2024-01-01T10:00:00Z"),
                CaptureDto(id = "2", game_id = "g", player_id = "p2", category_id = "cat1", created_at = "2024-01-01T10:01:00Z"),
            )
        )
        assertEquals(1, scoring.getSpeedBonusCount("p1"))
        assertEquals(0, scoring.getSpeedBonusCount("p2"))
    }

    @Test
    fun noSpeedBonusWithoutCaptures() {
        val scoring = createScoring(
            categories = listOf(Category("cat1", "Test", "icon")),
            allCaptures = emptyList(),
        )
        assertEquals(0, scoring.getSpeedBonusCount("p1"))
    }

    @Test
    fun getFirstCapturersMultipleCategories() {
        val scoring = createScoring(
            categories = listOf(
                Category("cat1", "Cat 1", "icon"),
                Category("cat2", "Cat 2", "icon"),
            ),
            allCaptures = listOf(
                CaptureDto(id = "1", game_id = "g", player_id = "p1", category_id = "cat1", created_at = "2024-01-01T10:00:00Z"),
                CaptureDto(id = "2", game_id = "g", player_id = "p2", category_id = "cat1", created_at = "2024-01-01T10:01:00Z"),
                CaptureDto(id = "3", game_id = "g", player_id = "p2", category_id = "cat2", created_at = "2024-01-01T10:00:30Z"),
                CaptureDto(id = "4", game_id = "g", player_id = "p1", category_id = "cat2", created_at = "2024-01-01T10:02:00Z"),
            )
        )
        val first = scoring.getFirstCapturers()
        assertEquals("p1", first["cat1"])
        assertEquals("p2", first["cat2"])
    }

    @Test
    fun categoryAverageRating() {
        val scoring = createScoring(
            allVotes = listOf(
                VoteDto(id = "1", game_id = "g", voter_id = "v1", target_player_id = "p1", category_id = "cat1", rating = 3),
                VoteDto(id = "2", game_id = "g", voter_id = "v2", target_player_id = "p1", category_id = "cat1", rating = 5),
            )
        )
        assertEquals(4.0, scoring.getCategoryAverageRating("p1", "cat1"))
    }

    @Test
    fun categoryAverageRatingNullWhenNoVotes() {
        val scoring = createScoring(allVotes = emptyList())
        assertNull(scoring.getCategoryAverageRating("p1", "cat1"))
    }

    @Test
    fun lastCaptureTimeReturnsInfinityWhenNone() {
        val scoring = createScoring(allCaptures = emptyList())
        assertEquals(GameConstants.INFINITY_TIME, scoring.getLastCaptureTime("p1"))
    }

    @Test
    fun isCapturedReturnsTrueForCaptured() {
        val scoring = createScoring(
            captures = mapOf("p1" to setOf("cat1", "cat2")),
        )
        assertEquals(true, scoring.isCaptured("p1", "cat1"))
        assertEquals(false, scoring.isCaptured("p1", "cat3"))
        assertEquals(false, scoring.isCaptured("p2", "cat1"))
    }
}
