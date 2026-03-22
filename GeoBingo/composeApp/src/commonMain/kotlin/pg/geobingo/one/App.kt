package pg.geobingo.one

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.platform.AdManager
import pg.geobingo.one.platform.ConsentManager
import pg.geobingo.one.ui.components.SyncAvatars
import pg.geobingo.one.ui.screens.*
import pg.geobingo.one.ui.screens.create.CreateGameScreen
import pg.geobingo.one.ui.screens.game.GameScreen
import pg.geobingo.one.ui.screens.results.ResultsScreen
import pg.geobingo.one.ui.screens.review.ReviewScreen
import pg.geobingo.one.ui.theme.KatchItTheme

@Composable
fun App() {
    val gameState = remember { GameState() }

    // Consent einmalig beim App-Start anfordern, danach Ads vorladen
    LaunchedEffect(Unit) {
        if (AdManager.isAdSupported) {
            ConsentManager.requestConsent {
                AdManager.preloadAds()
            }
        }
    }

    KatchItTheme {
        SyncAvatars(gameState)
        when (gameState.session.currentScreen) {
            Screen.HOME -> HomeScreen(gameState)
            Screen.HOW_TO_PLAY -> HowToPlayScreen(gameState)
            Screen.CREATE_GAME -> CreateGameScreen(gameState)
            Screen.JOIN_GAME -> JoinGameScreen(gameState)
            Screen.LOBBY -> LobbyScreen(gameState)
            Screen.GAME -> GameScreen(gameState)
            Screen.VOTE_TRANSITION -> VoteTransitionScreen(gameState)
            Screen.REVIEW -> ReviewScreen(gameState)
            Screen.RESULTS_TRANSITION -> ResultsTransitionScreen(gameState)
            Screen.RESULTS -> ResultsScreen(gameState)
            Screen.HISTORY -> HistoryScreen(gameState)
            Screen.SETTINGS -> SettingsScreen(gameState)
        }
    }
}
