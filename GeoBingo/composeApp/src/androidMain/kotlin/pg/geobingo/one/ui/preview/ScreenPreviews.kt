package pg.geobingo.one.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import pg.geobingo.one.game.Screen
import pg.geobingo.one.ui.screens.game.GameScreenContent
import pg.geobingo.one.ui.screens.HomeScreen
import pg.geobingo.one.ui.screens.LobbyScreen
import pg.geobingo.one.ui.theme.KatchItTheme

// ── GameScreen ────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF060E08, name = "Game – Normal")
@Composable
fun PreviewGameScreen() {
    KatchItTheme {
        GameScreenContent(gameState = mockGameState())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060E08, name = "Game – Timer kritisch")
@Composable
fun PreviewGameScreenTimerLow() {
    KatchItTheme {
        GameScreenContent(gameState = mockGameState().apply {
            gameplay.timeRemainingSeconds = 42
        })
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060E08, name = "Game – Spieler 2 aktiv")
@Composable
fun PreviewGameScreenPlayer2() {
    KatchItTheme {
        GameScreenContent(gameState = mockGameState().apply {
            gameplay.currentPlayerIndex = 1
        })
    }
}

// ── HomeScreen ────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF060E08, name = "Home")
@Composable
fun PreviewHomeScreen() {
    KatchItTheme {
        HomeScreen(gameState = mockGameState().apply { session.currentScreen = Screen.HOME })
    }
}

// ── LobbyScreen ───────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF060E08, name = "Lobby – Host")
@Composable
fun PreviewLobbyScreen() {
    KatchItTheme {
        LobbyScreen(gameState = mockLobbyGameState())
    }
}
