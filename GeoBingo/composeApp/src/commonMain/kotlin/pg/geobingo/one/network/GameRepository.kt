package pg.geobingo.one.network

import androidx.compose.ui.graphics.Color
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable
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
    val duration_s: Int = 300
)

@Serializable
data class PlayerDto(
    val id: String = "",
    val game_id: String = "",
    val name: String = "",
    val color: String = ""
)

@Serializable
data class CategoryDto(
    val id: String = "",
    val game_id: String = "",
    val label: String = "",
    val icon_id: String = "",
    val sort_order: Int = 0
)

fun PlayerDto.toPlayer(): Player = Player(
    id = id,
    name = name,
    color = parseHexColor(color)
)

fun CategoryDto.toCategory(): Category = Category(
    id = id,
    name = label,
    emoji = icon_id
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

object GameRepository {

    suspend fun createGame(code: String, durationSeconds: Int): GameDto =
        supabase.from("games").insert(
            GameDto(code = code, duration_s = durationSeconds)
        ) { select() }.decodeSingle()

    suspend fun addPlayer(gameId: String, name: String, color: String): PlayerDto =
        supabase.from("players").insert(
            PlayerDto(game_id = gameId, name = name, color = color)
        ) { select() }.decodeSingle()

    suspend fun addCategories(gameId: String, categories: List<Category>): List<CategoryDto> {
        val dtos = categories.mapIndexed { i, cat ->
            CategoryDto(game_id = gameId, label = cat.name, icon_id = cat.id, sort_order = i)
        }
        return supabase.from("categories").insert(dtos) { select() }.decodeList()
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
            .decodeList()

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
}
