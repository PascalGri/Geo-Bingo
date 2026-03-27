package pg.geobingo.one

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.Language
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.AdManager
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.ConsentManager
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.platform.rememberConnectivityState
import pg.geobingo.one.ui.components.SyncAvatars
import pg.geobingo.one.ui.screens.*
import pg.geobingo.one.ui.screens.ModeSelectScreen
import pg.geobingo.one.ui.screens.create.CreateGameScreen
import pg.geobingo.one.ui.screens.game.GameScreen
import pg.geobingo.one.ui.screens.results.ResultsScreen
import pg.geobingo.one.ui.screens.review.ReviewScreen
import pg.geobingo.one.ui.screens.solo.SoloGameScreen
import pg.geobingo.one.ui.screens.solo.SoloLeaderboardScreen
import pg.geobingo.one.ui.screens.solo.SoloResultsScreen
import pg.geobingo.one.ui.screens.solo.SoloStartTransitionScreen
import pg.geobingo.one.ui.theme.KatchItTheme
import pg.geobingo.one.ui.theme.OfflineBanner

@Composable
fun App() {
    val gameState = remember { GameState() }
    val nav = remember { ServiceLocator.navigation }
    val isConnected by rememberConnectivityState()

    // Initialize language from saved preference
    LaunchedEffect(Unit) {
        val savedLang = AppSettings.getString(SettingsKeys.LANGUAGE, "de")
        val lang = Language.entries.find { it.code == savedLang } ?: Language.DE
        S.switchLanguage(lang)
    }

    // Consent einmalig beim App-Start anfordern, danach Ads vorladen
    LaunchedEffect(Unit) {
        if (AdManager.isAdSupported) {
            ConsentManager.requestConsent {
                AdManager.preloadAds()
            }
        }
    }

    // Sync NavigationManager -> SessionState for backward compatibility
    // (screens that still read session.currentScreen)
    LaunchedEffect(nav.currentScreen) {
        gameState.session.currentScreen = nav.currentScreen
    }

    KatchItTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            // Offline banner
            AnimatedVisibility(
                visible = !isConnected,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                OfflineBanner(message = S.current.noInternet)
            }

            Box(modifier = Modifier.weight(1f)) {
                SyncAvatars(gameState)
                when (nav.currentScreen) {
                    Screen.ONBOARDING -> OnboardingScreen(gameState)
                    Screen.HOME -> HomeScreen(gameState)
                    Screen.HOW_TO_PLAY -> HowToPlayScreen(gameState)
                    Screen.SELECT_MODE -> ModeSelectScreen(gameState)
                    Screen.CREATE_GAME -> CreateGameScreen(gameState)
                    Screen.JOIN_GAME -> JoinGameScreen(gameState)
                    Screen.LOBBY -> LobbyScreen(gameState)
                    Screen.GAME_START_TRANSITION -> GameStartTransitionScreen(gameState)
                    Screen.GAME -> GameScreen(gameState)
                    Screen.VOTE_TRANSITION -> VoteTransitionScreen(gameState)
                    Screen.REVIEW -> ReviewScreen(gameState)
                    Screen.RESULTS_TRANSITION -> ResultsTransitionScreen(gameState)
                    Screen.RESULTS -> ResultsScreen(gameState)
                    Screen.HISTORY -> HistoryScreen(gameState)
                    Screen.SETTINGS -> SettingsScreen(gameState)
                    Screen.STATS -> StatsScreen(gameState)
                    Screen.SOLO_START_TRANSITION -> SoloStartTransitionScreen(gameState)
                    Screen.SOLO_GAME -> SoloGameScreen(gameState)
                    Screen.SOLO_RESULTS -> SoloResultsScreen(gameState)
                    Screen.SOLO_LEADERBOARD -> SoloLeaderboardScreen(gameState)
                }
            }
        }
    }
}
