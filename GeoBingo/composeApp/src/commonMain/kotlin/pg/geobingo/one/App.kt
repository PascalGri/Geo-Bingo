package pg.geobingo.one

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.ui.screens.*
import pg.geobingo.one.ui.theme.GeoBingoTheme

@Composable
fun App() {
    val gameState = remember { GameState() }

    GeoBingoTheme {
        when (gameState.currentScreen) {
            Screen.HOME -> HomeScreen(gameState)
            Screen.CREATE_GAME -> CreateGameScreen(gameState)
            Screen.GAME -> GameScreen(gameState)
            Screen.REVIEW -> ReviewScreen(gameState)
            Screen.RESULTS -> ResultsScreen(gameState)
        }
    }
}
