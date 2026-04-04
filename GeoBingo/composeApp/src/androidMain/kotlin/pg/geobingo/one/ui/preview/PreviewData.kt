package pg.geobingo.one.ui.preview

import androidx.compose.ui.graphics.Color
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.GameState
import pg.geobingo.one.network.PlayerDto

fun mockGameState(): GameState = GameState().apply {
    val p1 = Player("p1", "Pascal", Color(0xFF84CC16))
    val p2 = Player("p2", "Lena", Color(0xFF38BDF8))
    val p3 = Player("p3", "Jonas", Color(0xFFFBBF24))

    gameplay.players = listOf(p1, p2, p3)
    session.myPlayerId = "p1"
    session.isHost = true
    session.gameId = "preview"
    session.gameCode = "KCH7X2"
    gameplay.gameDurationMinutes = 15
    gameplay.timeRemainingSeconds = 7 * 60 + 34
    gameplay.isGameRunning = true
    gameplay.currentPlayerIndex = 0

    gameplay.selectedCategories = listOf(
        Category("fountain", "Brunnen", "fountain"),
        Category("mural", "Wandgemälde", "mural"),
        Category("bike", "Fahrrad", "bike"),
        Category("cafe", "Café", "cafe"),
        Category("dog", "Hund", "dog"),
        Category("flower", "Blume", "flower"),
        Category("bridge", "Brücke", "bridge"),
        Category("market", "Markt", "market"),
        Category("statue", "Statue", "statue"),
    )

    gameplay.captures = mapOf(
        "p1" to setOf("fountain", "bike", "dog"),
        "p2" to setOf("mural", "cafe"),
        "p3" to emptySet(),
    )
}

fun mockLobbyGameState(): GameState = GameState().apply {
    session.gameCode = "KCH7X2"
    session.gameId = "preview"
    session.isHost = true
    gameplay.gameDurationMinutes = 15
    gameplay.selectedCategories = listOf(
        Category("fountain", "Brunnen", "fountain"),
        Category("mural", "Wandgemälde", "mural"),
        Category("bike", "Fahrrad", "bike"),
    )
    gameplay.lobbyPlayers = listOf(
        PlayerDto(id = "p1", game_id = "preview", name = "Pascal", color = "#84CC16"),
        PlayerDto(id = "p2", game_id = "preview", name = "Lena", color = "#38BDF8"),
    )
    session.myPlayerId = "p1"
}
