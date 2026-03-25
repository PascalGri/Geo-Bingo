package pg.geobingo.one

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.platform.AdManager
import pg.geobingo.one.platform.ConsentManager
import pg.geobingo.one.platform.rememberConnectivityState
import pg.geobingo.one.ui.components.SyncAvatars
import pg.geobingo.one.ui.screens.*
import pg.geobingo.one.ui.screens.ModeSelectScreen
import pg.geobingo.one.ui.screens.create.CreateGameScreen
import pg.geobingo.one.ui.screens.game.GameScreen
import pg.geobingo.one.ui.screens.results.ResultsScreen
import pg.geobingo.one.ui.screens.review.ReviewScreen
import pg.geobingo.one.ui.theme.KatchItTheme

@Composable
fun App() {
    val gameState = remember { GameState() }
    val isConnected by rememberConnectivityState()

    // Consent einmalig beim App-Start anfordern, danach Ads vorladen
    LaunchedEffect(Unit) {
        if (AdManager.isAdSupported) {
            ConsentManager.requestConsent {
                AdManager.preloadAds()
            }
        }
    }

    KatchItTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            // Offline banner
            AnimatedVisibility(
                visible = !isConnected,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFB71C1C))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Keine Internetverbindung",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                SyncAvatars(gameState)
                when (gameState.session.currentScreen) {
                    Screen.HOME -> HomeScreen(gameState)
                    Screen.HOW_TO_PLAY -> HowToPlayScreen(gameState)
                    Screen.SELECT_MODE -> ModeSelectScreen(gameState)
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
    }
}
