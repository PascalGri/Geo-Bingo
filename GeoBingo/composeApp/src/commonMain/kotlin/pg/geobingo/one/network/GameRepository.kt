package pg.geobingo.one.network

import androidx.compose.ui.graphics.Color
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable
import pg.geobingo.one.data.CATEGORY_DESCRIPTIONS
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.data.PLAYER_COLORS
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Serializable
data class GameDto(
    val id: String = "",
    val code: String = "",
    val status: String = "lobby",
    val duration_s: Int = 300,
    val review_category_index: Int = 0,
    val joker_mode: Boolean = false
)

@Serializable
data class CaptureDto(
    val id: String = "",
    val game_id: String = "",
    val player_id: String = "",
    val category_id: String = "",
    val photo_url: String = "",
    val created_at: String = ""
)

@Serializable
private data class CaptureInsertDto(val game_id: String, val player_id: String, val category_id: String, val photo_url: String = "")

@Serializable
data class VoteDto(
    val id: String = "",
    val game_id: String = "",
    val voter_id: String = "",
    val target_player_id: String = "",
    val category_id: String = "",
    val approved: Boolean = false
)

@Serializable
private data class VoteInsertDto(val game_id: String, val voter_id: String, val target_player_id: String, val category_id: String, val approved: Boolean)

@Serializable
private data class VoteSubmissionInsertDto(val game_id: String, val voter_id: String, val category_id: String)

@Serializable
private data class GameInsertDto(val code: String, val duration_s: Int, val joker_mode: Boolean = false)

@Serializable
data class JokerLabelDto(val game_id: String = "", val player_id: String = "", val label: String = "")

@Serializable
private data class JokerLabelInsertDto(val game_id: String, val player_id: String, val label: String)

@Serializable
data class PlayerDto(
    val id: String = "",
    val game_id: String = "",
    val name: String = "",
    val color: String = "",
    val avatar: String = ""
)

@Serializable
private data class PlayerInsertDto(val game_id: String, val name: String, val color: String)

@Serializable
private data class PlayerAvatarUpdateDto(val avatar: String)

@Serializable
data class CategoryDto(
    val id: String = "",
    val game_id: String = "",
    val label: String = "",
    val icon_id: String = "",
    val sort_order: Int = 0
)

@Serializable
private data class CategoryInsertDto(val game_id: String, val label: String, val icon_id: String, val sort_order: Int)

fun PlayerDto.toPlayer(): Player = Player(
    id = id,
    name = name,
    color = parseHexColor(color),
    avatar = avatar
)

fun CategoryDto.toCategory(): Category = Category(
    id = id,
    name = label,
    emoji = icon_id,
    description = CATEGORY_DESCRIPTIONS[icon_id] ?: ""
)

fun Color.toHex(): String {
    val r = (red * 255).toInt().toString(16).padStart(2, '0')
    val g = (green * 255).toInt().toString(16).padStart(2, '0')
    val b = (blue * 255).toInt().toString(16).padStart(2, '0')
    return "#$r$g$b"
}

fun parseHexColor(hex: String): Color = try {
    val clean = hex.removePrefix("#")
    val r = clean.substring(0, 2).toInt(16) / 255f
    val g = clean.substring(2, 4).toInt(16) / 255f
    val b = clean.substring(4, 6).toInt(16) / 255f
    Color(r, g, b)
} catch (_: Exception) {
    PLAYER_COLORS[0]
}

fun generateCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
}

object VoteKeys {
    const val END_VOTE = "__end_vote__"
    const val ALL_CAPTURED = "__all_captured__"
    fun stepKey(categoryId: String, playerId: String) = "${categoryId}__${playerId}"
}

object GameRepository {

    suspend fun createGame(code: String, durationSeconds: Int, jokerMode: Boolean = false): GameDto =
        supabase.from("games").insert(
            GameInsertDto(code = code, duration_s = durationSeconds, joker_mode = jokerMode)
        ) { select() }.decodeSingle()

    suspend fun setJokerLabel(gameId: String, playerId: String, label: String) {
        supabase.from("joker_labels").insert(JokerLabelInsertDto(game_id = gameId, player_id = playerId, label = label))
    }

    suspend fun getJokerLabels(gameId: String): Map<String, String> =
        supabase.from("joker_labels")
            .select { filter { eq("game_id", gameId) } }
            .decodeList<JokerLabelDto>()
            .associate { it.player_id to it.label }

    suspend fun addPlayer(gameId: String, name: String, color: String): PlayerDto =
        supabase.from("players").insert(
            PlayerInsertDto(game_id = gameId, name = name, color = color)
        ) { select() }.decodeSingle()

    suspend fun setPlayerAvatar(playerId: String, avatar: String) {
        try {
            supabase.from("players").update({ set("avatar", avatar) }) {
                filter { eq("id", playerId) }
            }
        } catch (_: Exception) {} // Graceful: column may not exist yet
    }

    suspend fun uploadAvatarPhoto(playerId: String, bytes: ByteArray) {
        val path = "avatars/$playerId.jpg"
        supabase.storage.from("photos").upload(path, bytes) { upsert = true }
    }

    suspend fun downloadAvatarPhoto(playerId: String): ByteArray? = try {
        supabase.storage.from("photos").downloadAuthenticated("avatars/$playerId.jpg")
    } catch (_: Exception) { null }

    suspend fun addCategories(gameId: String, categories: List<Category>): List<CategoryDto> {
        val dtos = categories.mapIndexed { i, cat ->
            CategoryInsertDto(game_id = gameId, label = cat.name, icon_id = cat.id, sort_order = i)
        }
        return supabase.from("categories").insert(dtos) { select() }.decodeList<CategoryDto>()
            .sortedBy { it.sort_order }
    }

    suspend fun getGameByCode(code: String): GameDto? =
        supabase.from("games")
            .select { filter { eq("code", code.uppercase()) } }
            .decodeSingleOrNull()

    suspend fun getGameById(gameId: String): GameDto? =
        supabase.from("games")
            .select { filter { eq("id", gameId) } }
            .decodeSingleOrNull()

    suspend fun getPlayers(gameId: String): List<PlayerDto> =
        supabase.from("players")
            .select { filter { eq("game_id", gameId) } }
            .decodeList()

    suspend fun getCategories(gameId: String): List<CategoryDto> =
        supabase.from("categories")
            .select { filter { eq("game_id", gameId) } }
            .decodeList<CategoryDto>().sortedBy { it.sort_order }

    suspend fun startGame(gameId: String) {
        supabase.from("games").update({ set("status", "running") }) {
            filter { eq("id", gameId) }
        }
    }

    suspend fun uploadPhoto(gameId: String, playerId: String, categoryId: String, bytes: ByteArray): String {
        val path = "$gameId/$playerId/$categoryId.jpg"
        supabase.storage.from("photos").upload(path, bytes) { upsert = true }
        return supabase.storage.from("photos").createSignedUrl(path, 3600.seconds)
    }

    suspend fun recordCapture(gameId: String, playerId: String, categoryId: String, photoBytes: ByteArray) {
        val path = "$gameId/$playerId/$categoryId.jpg"
        supabase.storage.from("photos").upload(path, photoBytes) { upsert = true }
        val url = supabase.storage.from("photos").createSignedUrl(path, 86400.seconds)
        supabase.from("captures").insert(CaptureInsertDto(game_id = gameId, player_id = playerId, category_id = categoryId, photo_url = url))
    }

    suspend fun downloadPhoto(gameId: String, playerId: String, categoryId: String): ByteArray? = try {
        supabase.storage.from("photos").downloadAuthenticated("$gameId/$playerId/$categoryId.jpg")
    } catch (_: Exception) { null }

    suspend fun getCaptures(gameId: String): List<CaptureDto> =
        supabase.from("captures").select { filter { eq("game_id", gameId) } }.decodeList()

    suspend fun submitStepVote(
        gameId: String,
        voterId: String,
        targetPlayerId: String,
        categoryId: String,
        stepKey: String,
        approved: Boolean,
    ) {
        supabase.from("votes").insert(
            VoteInsertDto(game_id = gameId, voter_id = voterId, target_player_id = targetPlayerId, category_id = categoryId, approved = approved)
        )
        supabase.from("vote_submissions").insert(
            VoteSubmissionInsertDto(game_id = gameId, voter_id = voterId, category_id = stepKey)
        )
    }

    suspend fun submitStepSubmission(gameId: String, voterId: String, stepKey: String) {
        supabase.from("vote_submissions").insert(
            VoteSubmissionInsertDto(game_id = gameId, voter_id = voterId, category_id = stepKey)
        )
    }

    suspend fun submitCategoryVotes(gameId: String, voterId: String, categoryId: String, votes: List<Pair<String, Boolean>>) {
        if (votes.isNotEmpty()) {
            val dtos = votes.map { (targetId, approved) ->
                VoteInsertDto(game_id = gameId, voter_id = voterId, target_player_id = targetId, category_id = categoryId, approved = approved)
            }
            supabase.from("votes").insert(dtos)
        }
        supabase.from("vote_submissions").insert(VoteSubmissionInsertDto(game_id = gameId, voter_id = voterId, category_id = categoryId))
    }

    suspend fun getVoteSubmissionCount(gameId: String, categoryId: String): Int =
        supabase.from("vote_submissions")
            .select { filter { eq("game_id", gameId); eq("category_id", categoryId) } }
            .decodeList<VoteSubmissionInsertDto>().size

    suspend fun getVotes(gameId: String): List<VoteDto> =
        supabase.from("votes").select { filter { eq("game_id", gameId) } }.decodeList()

    suspend fun setReviewCategoryIndex(gameId: String, index: Int) {
        supabase.from("games").update({ set("review_category_index", index) }) {
            filter { eq("id", gameId) }
        }
    }

    suspend fun setGameStatus(gameId: String, status: String) {
        supabase.from("games").update({ set("status", status) }) {
            filter { eq("id", gameId) }
        }
    }

    suspend fun endGameAsVoting(gameId: String) {
        supabase.from("games").update({
            set("status", "voting")
            set("review_category_index", 0)
        }) { filter { eq("id", gameId) } }
    }

    suspend fun submitEndVote(gameId: String, voterId: String) {
        supabase.from("vote_submissions").insert(
            VoteSubmissionInsertDto(game_id = gameId, voter_id = voterId, category_id = VoteKeys.END_VOTE)
        )
    }

    suspend fun getEndVoteCount(gameId: String): Int =
        supabase.from("vote_submissions")
            .select { filter { eq("game_id", gameId); eq("category_id", VoteKeys.END_VOTE) } }
            .decodeList<VoteSubmissionInsertDto>().size

    suspend fun signalAllCaptured(gameId: String, playerId: String) {
        supabase.from("vote_submissions").insert(
            VoteSubmissionInsertDto(game_id = gameId, voter_id = playerId, category_id = VoteKeys.ALL_CAPTURED)
        )
    }

    suspend fun hasAllCapturedSignal(gameId: String): Boolean =
        supabase.from("vote_submissions")
            .select { filter { eq("game_id", gameId); eq("category_id", VoteKeys.ALL_CAPTURED) } }
            .decodeList<VoteSubmissionInsertDto>().isNotEmpty()
}
