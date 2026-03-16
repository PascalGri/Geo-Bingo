package pg.geobingo.one.ui.preview

import androidx.compose.ui.graphics.Color
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.network.PlayerDto

fun mockGameState(): GameState = GameState().apply {
    currentScreen = Screen.GAME

    val p1 = Player("p1", "Pascal", Color(0xFF84CC16))
    val p2 = Player("p2", "Lena", Color(0xFF38BDF8))
    val p3 = Player("p3", "Jonas", Color(0xFFFBBF24))

    players = listOf(p1, p2, p3)
    myPlayerId = "p1"
    isHost = true
    gameId = "preview"
    gameCode = "KCH7X2"
    gameDurationMinutes = 15
    timeRemainingSeconds = 7 * 60 + 34
    isGameRunning = true
    currentPlayerIndex = 0

    selectedCategories = listOf(
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

    captures = mapOf(
        "p1" to setOf("fountain", "bike", "dog"),
        "p2" to setOf("mural", "cafe"),
        "p3" to emptySet(),
    )

    photos = mapOf(
        "p1" to emptyMap(),
        "p2" to emptyMap(),
        "p3" to emptyMap(),
    )
}

fun mockLobbyGameState(): GameState = GameState().apply {
    currentScreen = Screen.LOBBY
    gameCode = "KCH7X2"
    gameId = "preview"
    isHost = true
    gameDurationMinutes = 15
    selectedCategories = listOf(
        Category("fountain", "Brunnen", "fountain"),
        Category("mural", "Wandgemälde", "mural"),
        Category("bike", "Fahrrad", "bike"),
    )
    lobbyPlayers = listOf(
        PlayerDto(id = "p1", game_id = "preview", name = "Pascal", color = "#84CC16"),
        PlayerDto(id = "p2", game_id = "preview", name = "Lena", color = "#38BDF8"),
    )
    myPlayerId = "p1"
}
