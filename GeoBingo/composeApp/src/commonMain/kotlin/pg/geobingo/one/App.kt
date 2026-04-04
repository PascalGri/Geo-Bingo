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
import pg.geobingo.one.i18n.S
import pg.geobingo.one.navigation.NavArgs
import pg.geobingo.one.platform.DeepLinkHandler
import pg.geobingo.one.platform.rememberConnectivityState
import pg.geobingo.one.ui.components.BottomNavBar
import pg.geobingo.one.ui.components.NotificationBanners
import pg.geobingo.one.ui.components.SyncAvatars
import pg.geobingo.one.ui.components.showsBottomNav
import pg.geobingo.one.ui.screens.*
import pg.geobingo.one.ui.screens.ModeSelectScreen
import pg.geobingo.one.ui.screens.ProfileSetupScreen
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
    val gameState = remember { ServiceLocator.gameState }
    val nav = remember { ServiceLocator.navigation }
    val isConnected by rememberConnectivityState()

    // One-time app initialization
    LaunchedEffect(Unit) {
        AppInitializer.initialize(gameState)
    }

    // Online presence heartbeat (every 2 minutes)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(120_000L)
            AppInitializer.updatePresence()
        }
    }

    // Deep link handler: navigate to JoinGameScreen with code
    LaunchedEffect(DeepLinkHandler.pendingGameCode) {
        val code = DeepLinkHandler.pendingGameCode ?: return@LaunchedEffect
        DeepLinkHandler.pendingGameCode = null
        nav.navigateTo(Screen.JOIN_GAME, NavArgs.JoinGame(inviteCode = code))
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

            // Game invite + friend request banners
            NotificationBanners(gameState, nav)

            Box(modifier = Modifier.weight(1f)) {
                SyncAvatars(gameState)
                ScreenRouter(nav.currentScreen, gameState)
            }

            // Global bottom navigation bar (hidden during games/transitions)
            if (nav.currentScreen.showsBottomNav()) {
                BottomNavBar(
                    currentScreen = nav.currentScreen,
                    onTabSelected = { screen ->
                        if (screen != nav.currentScreen) {
                            nav.resetTo(screen)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ScreenRouter(screen: Screen, gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    when (screen) {
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
        Screen.SHOP -> ShopScreen(gameState)
        Screen.COSMETIC_SHOP -> CosmeticShopScreen(gameState)
        Screen.PROFILE_SETUP -> ProfileSetupScreen(gameState)
        Screen.ACCOUNT -> AccountScreen(gameState)
        Screen.FRIENDS -> FriendsScreen(gameState)
        Screen.MP_LEADERBOARD -> { nav.resetTo(Screen.HOME) }
        Screen.PROFILE -> ProfileScreen(gameState)
        Screen.ACTIVITY_FEED -> ActivityFeedScreen(gameState)
        Screen.ACHIEVEMENTS -> AchievementScreen(gameState)
        Screen.DIRECT_MESSAGE -> DirectMessageScreen(gameState)
        Screen.MATCH_DETAIL -> MatchDetailScreen(gameState)
    }
}
