package pg.geobingo.one.game.state

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pg.geobingo.one.data.Category
import pg.geobingo.one.platform.AppSettings

private const val KEY_FAVORITE_CATEGORY_SETS = "favorite_category_sets"
private const val MAX_FAVORITES = 10

@Serializable
data class FavoriteCategoryRef(
    val id: String,
    val name: String,
    val emoji: String,
)

@Serializable
data class FavoriteSet(
    val id: String,
    val name: String,
    val categories: List<FavoriteCategoryRef>,
)

private val json = Json { ignoreUnknownKeys = true }

object FavoriteCategoriesManager {

    fun getFavorites(): List<FavoriteSet> {
        val raw = AppSettings.getString(KEY_FAVORITE_CATEGORY_SETS, "")
        if (raw.isBlank()) return emptyList()
        return try {
            json.decodeFromString<List<FavoriteSet>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveFavorite(name: String, categories: List<Category>) {
        val current = getFavorites().toMutableList()
        if (current.size >= MAX_FAVORITES) return
        val id = "fav_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}"
        val refs = categories.map { FavoriteCategoryRef(id = it.id, name = it.name, emoji = it.emoji) }
        current.add(FavoriteSet(id = id, name = name, categories = refs))
        AppSettings.setString(KEY_FAVORITE_CATEGORY_SETS, json.encodeToString(current))
    }

    fun deleteFavorite(id: String) {
        val updated = getFavorites().filter { it.id != id }
        AppSettings.setString(KEY_FAVORITE_CATEGORY_SETS, json.encodeToString(updated))
    }
}
