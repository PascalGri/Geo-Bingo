package pg.geobingo.one.network

import androidx.compose.ui.graphics.Color
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable
import pg.geobingo.one.data.CATEGORY_DESCRIPTIONS
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.data.PLAYER_COLORS
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.util.AppLogger
import kotlin.random.Random
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pg.geobingo.one.di.ServiceLocator

@Serializable
data class GameDto(
    val id: String = "",
    val code: String = "",
    val status: String = "lobby",
    val duration_s: Int = 300,
    val review_category_index: Int = 0,
    val joker_mode: Boolean = false,
    val game_mode: String = "CLASSIC",
    val created_at: String = "",
)

@Serializable
data class CaptureDto(
    val id: String = "",
    val game_id: String = "",
    val player_id: String = "",
    val category_id: String = "",
    val photo_url: String = "",
    val created_at: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
private data class CaptureInsertDto(
    val game_id: String,
    val player_id: String,
    val category_id: String,
    val photo_url: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class VoteDto(
    val id: String = "",
    val game_id: String = "",
    val voter_id: String = "",
    val target_player_id: String = "",
    val category_id: String = "",
    val rating: Int = 0
)

@Serializable
private data class VoteInsertDto(val game_id: String, val voter_id: String, val target_player_id: String, val category_id: String, val rating: Int)

@Serializable
data class VoteSubmissionDto(val id: String = "", val game_id: String = "", val voter_id: String = "", val category_id: String = "")

@Serializable
private data class VoteSubmissionInsertDto(val game_id: String, val voter_id: String, val category_id: String)

@Serializable
private data class GameInsertDto(val code: String, val duration_s: Int, val joker_mode: Boolean = false, val game_mode: String = "CLASSIC")

@Serializable
data class SoloScoreDto(
    val id: String = "",
    val player_name: String = "",
    val user_id: String? = null,
    val score: Int = 0,
    val categories_count: Int = 0,
    val time_bonus: Int = 0,
    val duration_seconds: Int = 0,
    val is_outdoor: Boolean = true,
    val created_at: String = "",
)

data class PhotoValidationResult(val rating: Int, val reason: String)

/** Lightweight DTO for leaderboard count queries — fetches only the player_name column. */
@Serializable
data class SoloScoreNameDto(val player_name: String = "")

@Serializable
private data class SoloScoreInsertDto(
    val player_name: String,
    val user_id: String? = null,
    val score: Int,
    val categories_count: Int,
    val time_bonus: Int,
    val duration_seconds: Int,
    val is_outdoor: Boolean,
)

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
    val avatar: String = "",
    val user_id: String? = null,
)

@Serializable
private data class PlayerInsertDto(val game_id: String, val name: String, val color: String, val user_id: String? = null)

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

/** Check if an exception is a PostgreSQL unique constraint violation (duplicate). */
private fun isDuplicateError(e: Exception): Boolean {
    val msg = e.message ?: ""
    return msg.contains("duplicate", ignoreCase = true)
            || msg.contains("unique", ignoreCase = true)
            || msg.contains("23505", ignoreCase = true)
}

object VoteKeys {
    const val END_VOTE = "__end_vote__"
    const val ALL_CAPTURED = "__all_captured__"
    fun stepKey(categoryId: String, playerId: String) = "${categoryId}__${playerId}"
}

object GameRepository {
    /** Uses the shared HttpClient from ServiceLocator for proper lifecycle management. */
    private val httpClient get() = ServiceLocator.httpClient

    private val soloLeaderboardCaches = mutableMapOf<String, ResponseCache<List<SoloScoreDto>>>()

    suspend fun createGame(code: String, durationSeconds: Int, jokerMode: Boolean = false, gameMode: String = "CLASSIC"): GameDto =
        supabase.from("games").insert(
            GameInsertDto(code = code, duration_s = durationSeconds, joker_mode = jokerMode, game_mode = gameMode)
        ) { select() }.decodeSingle()

    suspend fun setJokerLabel(gameId: String, playerId: String, label: String) {
        supabase.from("joker_labels").insert(JokerLabelInsertDto(game_id = gameId, player_id = playerId, label = label))
    }

    suspend fun getJokerLabels(gameId: String): Map<String, String> =
        supabase.from("joker_labels")
            .select { filter { eq("game_id", gameId) } }
            .decodeList<JokerLabelDto>()
            .associate { it.player_id to it.label }

    // ── Team assignments ────────────────────────────────────────────────
    // Uses dedicated team_assignments table. Falls back to joker_labels
    // with "__team__" prefix for backward compatibility with existing games.

    @Serializable
    private data class TeamAssignmentDto(
        val game_id: String = "",
        val player_id: String = "",
        val team_number: Int = 1,
    )

    suspend fun saveTeamAssignments(gameId: String, assignments: Map<String, Int>) {
        if (assignments.isEmpty()) return
        // Try dedicated table first, fall back to joker_labels
        try {
            val dtos = assignments.map { (playerId, team) ->
                TeamAssignmentDto(game_id = gameId, player_id = playerId, team_number = team)
            }
            supabase.from("team_assignments").insert(dtos)
            return
        } catch (e: Exception) {
            AppLogger.d("Repo", "team_assignments table not available, using fallback", e)
        }
        // Fallback: store in joker_labels with prefix (upsert to avoid race conditions)
        assignments.forEach { (playerId, team) ->
            try {
                supabase.from("joker_labels").upsert(
                    JokerLabelInsertDto(game_id = gameId, player_id = "__team__$playerId", label = team.toString())
                )
            } catch (e: Exception) {
                AppLogger.w("Repo", "Team assignment save failed for $playerId", e)
            }
        }
    }

    suspend fun getTeamAssignments(gameId: String): Map<String, Int> {
        // Try dedicated table first
        try {
            val result = supabase.from("team_assignments")
                .select { filter { eq("game_id", gameId) } }
                .decodeList<TeamAssignmentDto>()
            if (result.isNotEmpty()) {
                return result.associate { it.player_id to it.team_number }
            }
        } catch (e: Exception) {
            AppLogger.d("Repo", "team_assignments table not available, using fallback", e)
        }
        // Fallback: read from joker_labels
        return supabase.from("joker_labels")
            .select { filter { eq("game_id", gameId) } }
            .decodeList<JokerLabelDto>()
            .filter { it.player_id.startsWith("__team__") }
            .associate { it.player_id.removePrefix("__team__") to (it.label.toIntOrNull() ?: 1) }
    }

    // ── Team names ───────────────────────────────────────────────────────
    // Stored in joker_labels with "__team_name__<teamNumber>" as player_id.

    suspend fun saveTeamNames(gameId: String, teamNames: Map<Int, String>) {
        if (teamNames.isEmpty()) return
        teamNames.forEach { (teamNum, name) ->
            try {
                supabase.from("joker_labels").upsert(
                    JokerLabelInsertDto(game_id = gameId, player_id = "__team_name__$teamNum", label = name)
                )
            } catch (e: Exception) {
                AppLogger.w("Repo", "Team name save failed for team $teamNum", e)
            }
        }
    }

    suspend fun getTeamNames(gameId: String): Map<Int, String> =
        supabase.from("joker_labels")
            .select { filter { eq("game_id", gameId) } }
            .decodeList<JokerLabelDto>()
            .filter { it.player_id.startsWith("__team_name__") }
            .associate {
                val teamNum = it.player_id.removePrefix("__team_name__").toIntOrNull() ?: 1
                teamNum to it.label
            }

    suspend fun addPlayer(gameId: String, name: String, color: String, userId: String? = null): PlayerDto =
        supabase.from("players").insert(
            PlayerInsertDto(game_id = gameId, name = name, color = color, user_id = userId)
        ) { select() }.decodeSingle()

    suspend fun setPlayerAvatar(playerId: String, avatar: String) {
        supabase.from("players").update({ set("avatar", avatar) }) {
            filter { eq("id", playerId) }
        }
    }

    suspend fun uploadAvatarPhoto(playerId: String, bytes: ByteArray) {
        val path = "avatars/$playerId.jpg"
        supabase.storage.from("photos").upload(path, bytes) { upsert = true }
    }

    suspend fun downloadAvatarPhoto(playerId: String): ByteArray? {
        // Try local cache first
        try {
            val cached = LocalPhotoStore.loadAvatar(playerId)
            if (cached != null) return cached
        } catch (e: Exception) {
            AppLogger.d("Repo", "Avatar cache miss for $playerId", e)
        }
        return try {
            withRetry {
                val path = "avatars/$playerId.jpg"
                val url = supabase.storage.from("photos").createSignedUrl(path, GameConstants.AVATAR_URL_EXPIRY)
                val bytes = httpClient.get(url).readRawBytes()
                try { LocalPhotoStore.saveAvatar(playerId, bytes) } catch (e: Exception) {
                    AppLogger.d("Repo", "Avatar local save failed for $playerId", e)
                }
                bytes
            }
        } catch (e: Exception) {
            AppLogger.w("Repo", "Avatar download failed for $playerId", e)
            null
        }
    }

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
        return supabase.storage.from("photos").createSignedUrl(path, GameConstants.CAPTURE_URL_EXPIRY)
    }

    suspend fun recordCapture(gameId: String, playerId: String, categoryId: String, photoBytes: ByteArray, latitude: Double? = null, longitude: Double? = null) {
        val path = "$gameId/$playerId/$categoryId.jpg"
        supabase.storage.from("photos").upload(path, photoBytes) { upsert = true }
        val url = supabase.storage.from("photos").createSignedUrl(path, GameConstants.CAPTURE_URL_EXPIRY)
        supabase.from("captures").insert(CaptureInsertDto(game_id = gameId, player_id = playerId, category_id = categoryId, photo_url = url, latitude = latitude, longitude = longitude))
    }

    suspend fun downloadPhoto(gameId: String, playerId: String, categoryId: String): ByteArray? {
        // 1. Check local cache first
        try {
            val cached = LocalPhotoStore.loadPhoto(gameId, playerId, categoryId)
            if (cached != null) return cached
        } catch (e: Exception) {
            AppLogger.d("Repo", "Photo cache miss for $playerId/$categoryId", e)
        }

        // 2. Network download with retry
        return try {
            withRetry {
                val path = "$gameId/$playerId/$categoryId.jpg"
                val url = supabase.storage.from("photos").createSignedUrl(path, GameConstants.CAPTURE_URL_EXPIRY)
                val bytes = httpClient.get(url).readRawBytes()
                // 3. Cache locally for future access
                try { LocalPhotoStore.savePhoto(gameId, playerId, categoryId, bytes) } catch (e: Exception) {
                    AppLogger.d("Repo", "Photo local save failed", e)
                }
                bytes
            }
        } catch (e: Exception) {
            AppLogger.w("Repo", "Photo download failed for $playerId/$categoryId", e)
            null
        }
    }

    suspend fun getCaptures(gameId: String): List<CaptureDto> =
        supabase.from("captures").select { filter { eq("game_id", gameId) } }.decodeList()

    suspend fun submitStepVote(
        gameId: String,
        voterId: String,
        targetPlayerId: String,
        categoryId: String,
        stepKey: String,
        rating: Int,
    ) {
        // Insert vote first, then submission. If vote is a duplicate that's fine -
        // we still record the submission. Only re-throw truly unexpected errors.
        try {
            supabase.from("votes").insert(
                VoteInsertDto(game_id = gameId, voter_id = voterId, target_player_id = targetPlayerId, category_id = categoryId, rating = rating)
            )
        } catch (e: Exception) {
            if (!isDuplicateError(e)) throw e
            AppLogger.d("Repo", "Vote already exists (duplicate)", e)
        }
        try {
            supabase.from("vote_submissions").insert(
                VoteSubmissionInsertDto(game_id = gameId, voter_id = voterId, category_id = stepKey)
            )
        } catch (e: Exception) {
            if (!isDuplicateError(e)) throw e
        }
    }

    suspend fun submitStepSubmission(gameId: String, voterId: String, stepKey: String) {
        try {
            supabase.from("vote_submissions").insert(
                VoteSubmissionInsertDto(game_id = gameId, voter_id = voterId, category_id = stepKey)
            )
        } catch (e: Exception) {
            if (!isDuplicateError(e)) throw e
        }
    }

    suspend fun submitCategoryVotes(gameId: String, voterId: String, categoryId: String, votes: List<Pair<String, Int>>) {
        if (votes.isNotEmpty()) {
            val dtos = votes.map { (targetId, rating) ->
                VoteInsertDto(game_id = gameId, voter_id = voterId, target_player_id = targetId, category_id = categoryId, rating = rating)
            }
            supabase.from("votes").insert(dtos)
        }
        supabase.from("vote_submissions").insert(VoteSubmissionInsertDto(game_id = gameId, voter_id = voterId, category_id = categoryId))
    }

    suspend fun getVoteSubmissionCount(gameId: String, categoryId: String): Int =
        supabase.from("vote_submissions")
            .select {
                head = true
                count(Count.EXACT)
                filter { eq("game_id", gameId); eq("category_id", categoryId) }
            }
            .countOrNull()?.toInt() ?: 0

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
        try {
            supabase.from("vote_submissions").insert(
                VoteSubmissionInsertDto(game_id = gameId, voter_id = voterId, category_id = VoteKeys.END_VOTE)
            )
        } catch (e: Exception) {
            if (!isDuplicateError(e)) throw e
        }
    }

    suspend fun getEndVoteCount(gameId: String): Int =
        supabase.from("vote_submissions")
            .select {
                head = true
                count(Count.EXACT)
                filter { eq("game_id", gameId); eq("category_id", VoteKeys.END_VOTE) }
            }
            .countOrNull()?.toInt() ?: 0

    suspend fun signalAllCaptured(gameId: String, playerId: String) {
        supabase.from("vote_submissions").insert(
            VoteSubmissionInsertDto(game_id = gameId, voter_id = playerId, category_id = VoteKeys.ALL_CAPTURED)
        )
    }

    suspend fun hasAllCapturedSignal(gameId: String): Boolean {
        val count = supabase.from("vote_submissions")
            .select {
                head = true
                count(Count.EXACT)
                filter { eq("game_id", gameId); eq("category_id", VoteKeys.ALL_CAPTURED) }
            }
            .countOrNull() ?: 0
        return count > 0
    }

    // ── Solo Photo Validation ─────────────────────────────────────────

    /**
     * Validates a solo-mode photo via the validate-photo Edge Function.
     * Returns an AI rating 1-10 and a brief reason.
     */
    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    suspend fun validateSoloPhoto(
        imageBytes: ByteArray,
        categoryName: String,
        categoryDescription: String,
    ): PhotoValidationResult {
        val base64 = kotlin.io.encoding.Base64.encode(imageBytes)
        val url = "${SupabaseConfig.current.url}/functions/v1/validate-photo"
        val jsonBody = kotlinx.serialization.json.buildJsonObject {
            put("imageBase64", kotlinx.serialization.json.JsonPrimitive(base64))
            put("categoryName", kotlinx.serialization.json.JsonPrimitive(categoryName))
            put("categoryDescription", kotlinx.serialization.json.JsonPrimitive(categoryDescription))
        }.toString()
        val response = ServiceLocator.httpClient.post(url) {
            headers {
                append("apikey", SupabaseConfig.current.anonKey)
            }
            setBody(io.ktor.http.content.TextContent(jsonBody, io.ktor.http.ContentType.Application.Json))
        }
        val body: String = response.body()
        val json = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject
        val rating = (json["rating"]?.jsonPrimitive?.int ?: 5).coerceIn(1, 5)
        val reasonPrimitive = json["reason"]?.jsonPrimitive
        val reason = reasonPrimitive?.content ?: ""
        return PhotoValidationResult(rating = rating, reason = reason)
    }

    // ── AI Judge (Multiplayer) ──────────────────────────────────────────

    /**
     * Validates all captures in a multiplayer game via AI.
     * Downloads each photo, calls the validate-photo Edge Function,
     * and inserts VoteDto records with voter_id = "ai_judge".
     * Returns the total number of captures processed.
     */
    suspend fun validateMultiplayerCaptures(
        gameId: String,
        categories: List<Category>,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ): Int {
        val captures = getCaptures(gameId)
        if (captures.isEmpty()) return 0

        val categoryMap = categories.associateBy { it.id }
        var count = 0

        // Process in parallel batches of 4 to avoid overwhelming the edge function
        for (batch in captures.chunked(4)) {
            coroutineScope {
                val jobs = batch.map { capture ->
                    launch {
                        val category = categoryMap[capture.category_id] ?: return@launch
                        val photoBytes = downloadPhoto(gameId, capture.player_id, capture.category_id)
                        val rating = if (photoBytes == null) {
                            1 // No photo available
                        } else {
                            try {
                                validateSoloPhoto(photoBytes, category.name, category.description).rating
                            } catch (e: Exception) {
                                AppLogger.e("Repo", "AI validation failed for ${capture.player_id}/${capture.category_id}", e)
                                3 // Fallback rating
                            }
                        }
                        try {
                            supabase.from("votes").insert(
                                VoteInsertDto(game_id = gameId, voter_id = "ai_judge", target_player_id = capture.player_id, category_id = capture.category_id, rating = rating)
                            )
                        } catch (e: Exception) {
                            if (!isDuplicateError(e)) AppLogger.w("Repo", "AI vote insert failed", e)
                        }
                    }
                }
                jobs.forEach { it.join() }
            }
            count += batch.size
            onProgress(count, captures.size)
        }

        return count
    }

    // ── Solo Leaderboard ────────────────────────────────────────────────

    suspend fun submitSoloScore(playerName: String, score: Int, categoriesCount: Int, timeBonus: Int, durationSeconds: Int, isOutdoor: Boolean = true, userId: String? = null): SoloScoreDto {
        val result = supabase.from("solo_scores").insert(
            SoloScoreInsertDto(
                player_name = playerName,
                user_id = userId,
                score = score,
                categories_count = categoriesCount,
                time_bonus = timeBonus,
                duration_seconds = durationSeconds,
                is_outdoor = isOutdoor,
            )
        ) { select() }.decodeSingle<SoloScoreDto>()
        soloLeaderboardCaches.values.forEach { it.invalidate() }
        return result
    }

    suspend fun getSoloLeaderboard(limit: Int = 50, isOutdoor: Boolean? = null, offset: Int = 0, createdAfter: String? = null): List<SoloScoreDto> {
        val cacheKey = "$limit:$isOutdoor:$offset:$createdAfter"
        val cache = soloLeaderboardCaches.getOrPut(cacheKey) { ResponseCache(ttlMs = 60_000L) }
        return cache.getOrFetch {
            supabase.from("solo_scores")
                .select {
                    filter {
                        if (isOutdoor != null) eq("is_outdoor", isOutdoor)
                        if (createdAfter != null) gte("created_at", createdAfter)
                    }
                    order("score", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList()
        }
    }

    suspend fun getSoloPersonalBest(playerName: String): SoloScoreDto? =
        supabase.from("solo_scores")
            .select {
                filter { eq("player_name", playerName) }
                order("score", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }
            .decodeList<SoloScoreDto>().firstOrNull()

    /**
     * Returns the approximate rank of a player's best score.
     * Uses a head+count query to avoid loading all records into memory.
     */
    suspend fun getSoloRank(playerName: String): Int? {
        val best = getSoloPersonalBest(playerName) ?: return null
        val higherCount = supabase.from("solo_scores")
            .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("player_name")) {
                filter { gt("score", best.score) }
            }
            .decodeList<SoloScoreNameDto>()
            .map { it.player_name }
            .toSet()
            .size
        return higherCount + 1
    }

    /** Total number of distinct players on the leaderboard. Uses head+count for efficiency. */
    suspend fun getSoloTotalPlayers(): Int {
        val count = supabase.from("solo_scores")
            .select {
                head = true
                count(Count.EXACT)
            }
            .countOrNull()?.toInt() ?: 0
        return count
    }

    // ── In-Game Chat ────────────────────────────────────────────────────

    @Serializable
    data class ChatMessageDto(
        val id: String = "",
        val game_id: String = "",
        val player_id: String = "",
        val player_name: String = "",
        val message: String = "",
        val created_at: String = "",
    )

    @Serializable
    private data class ChatMessageInsertDto(
        val game_id: String,
        val player_id: String,
        val player_name: String,
        val message: String,
    )

    suspend fun sendChatMessage(gameId: String, playerId: String, playerName: String, message: String) {
        val sanitizedMessage = message.take(200)
        if (sanitizedMessage.isBlank()) return
        supabase.from("chat_messages").insert(
            ChatMessageInsertDto(game_id = gameId, player_id = playerId, player_name = playerName.take(50), message = sanitizedMessage)
        )
    }

    suspend fun getChatMessages(gameId: String, limit: Int = 50): List<ChatMessageDto> =
        supabase.from("chat_messages")
            .select {
                filter { eq("game_id", gameId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                limit(limit.toLong())
            }
            .decodeList()

    // ── Activity Feed ──────────────────────────────────────────────────

    @Serializable
    data class ActivityDto(
        val id: String = "",
        val user_id: String = "",
        val event_type: String = "",
        val description: String = "",
        val metadata: String = "{}",
        val created_at: String = "",
    )

    @Serializable
    private data class ActivityInsertDto(
        val user_id: String,
        val event_type: String,
        val description: String,
        val metadata: String = "{}",
    )

    suspend fun postActivity(userId: String, eventType: String, description: String, metadata: String = "{}") {
        try {
            supabase.from("activity_feed").insert(
                ActivityInsertDto(user_id = userId, event_type = eventType, description = description, metadata = metadata)
            )
        } catch (e: Exception) {
            AppLogger.d("Repo", "Activity post failed (non-critical)", e)
        }
    }

    suspend fun getFriendsActivity(friendIds: List<String>, limit: Int = 30, offset: Int = 0): List<ActivityDto> {
        if (friendIds.isEmpty()) return emptyList()
        return supabase.from("activity_feed")
            .select {
                filter { isIn("user_id", friendIds) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }
            .decodeList()
    }

    // ── Multiplayer Leaderboard ──────────────────────────────────────────

    @Serializable
    data class MultiplayerStatsDto(
        val user_id: String = "",
        val display_name: String = "",
        val games_played: Int = 0,
        val games_won: Int = 0,
        val current_win_streak: Int = 0,
        val longest_win_streak: Int = 0,
        val total_captures: Int = 0,
        val avg_rating: Double = 0.0,
    )

    suspend fun upsertMultiplayerStats(stats: MultiplayerStatsDto) {
        supabase.from("multiplayer_stats").upsert(stats)
    }

    suspend fun getMultiplayerLeaderboard(limit: Int = 50, orderBy: String = "games_won"): List<MultiplayerStatsDto> =
        supabase.from("multiplayer_stats")
            .select {
                order(orderBy, io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList()

    /** Delete all game photos and avatars from Supabase Storage to free space. */
    suspend fun cleanupStoragePhotos(gameId: String, playerIds: List<String>) {
        try {
            // List and delete game photos
            val gameFiles = supabase.storage.from("photos").list(gameId)
            for (playerFolder in gameFiles) {
                val playerPath = "$gameId/${playerFolder.name}"
                val photos = supabase.storage.from("photos").list(playerPath)
                val paths = photos.map { "$playerPath/${it.name}" }
                if (paths.isNotEmpty()) {
                    supabase.storage.from("photos").delete(paths)
                }
            }
        } catch (e: Exception) {
            AppLogger.w("Repo", "Game photo cleanup failed for $gameId", e)
        }
        try {
            // Delete avatar photos
            val avatarPaths = playerIds.map { "avatars/$it.jpg" }
            if (avatarPaths.isNotEmpty()) {
                supabase.storage.from("photos").delete(avatarPaths)
            }
        } catch (e: Exception) {
            AppLogger.w("Repo", "Avatar cleanup failed for $gameId", e)
        }
    }
}
