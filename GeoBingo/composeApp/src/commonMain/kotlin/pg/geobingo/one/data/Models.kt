package pg.geobingo.one.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class Player(
    val id: String,
    val name: String,
    val color: Color,
    val avatar: String = "" // emoji character or empty → falls back to first letter
)

@Immutable
data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String = ""
)
