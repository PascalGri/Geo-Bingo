package pg.geobingo.one.data

import androidx.compose.ui.graphics.Color

data class Player(
    val id: String,
    val name: String,
    val color: Color
)

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String = ""
)
