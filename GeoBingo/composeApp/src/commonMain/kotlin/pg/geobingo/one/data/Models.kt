package pg.geobingo.one.data

import androidx.compose.ui.graphics.Color

data class Player(
    val id: String,
    val name: String,
    val color: Color,
    val avatar: String = "" // emoji character or empty → falls back to first letter
)

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String = ""
)
