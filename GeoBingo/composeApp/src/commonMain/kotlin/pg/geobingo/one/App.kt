package pg.geobingo.one

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.ActiveSession
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.Language
import pg.geobingo.one.i18n.S
import kotlinx.coroutines.launch
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.network.FriendsManager
import pg.geobingo.one.platform.AdManager
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.BillingManager
import pg.geobingo.one.platform.ConsentManager
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.platform.rememberConnectivityState
import pg.geobingo.one.ui.components.SyncAvatars
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
import pg.geobingo.one.util.Analytics

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

    // Initialize billing + restore purchases
    LaunchedEffect(Unit) {
        if (BillingManager.isBillingSupported) {
            BillingManager.initialize()
            BillingManager.restorePurchases(
                onRestored = { products ->
                    if ("pg.geobingo.one.no_ads" in products) {
                        gameState.stars.updateNoAdsPurchased(true)
                    }
                },
                onError = {},
            )
        }
    }

    // Track app open
    LaunchedEffect(Unit) {
        Analytics.track(Analytics.APP_OPENED)
    }

    // Auto-sync with cloud when logged in
    LaunchedEffect(Unit) {
        val userId = AccountManager.currentUserId
        if (userId != null) {
            AccountManager.syncLocalToCloud(userId)
        }
    }

    // Online presence heartbeat (update last_seen every 2 minutes)
    LaunchedEffect(Unit) {
        while (true) {
            FriendsManager.updateLastSeen()
            kotlinx.coroutines.delay(120_000L)
        }
    }

    // Daily login bonus + daily challenge reset
    LaunchedEffect(Unit) {
        gameState.stars.resetDailyChallengeIfNewDay()
        val bonusGranted = gameState.stars.checkDailyLoginBonus()
        if (bonusGranted) {
            gameState.ui.pendingToast = "${S.current.dailyLoginBonus}: +5 ${S.current.stars}"
        }
    }

    // Sync NavigationManager -> SessionState for backward compatibility
    // (screens that still read session.currentScreen)
    LaunchedEffect(nav.currentScreen) {
        gameState.session.currentScreen = nav.currentScreen
    }

    // Game invite banner state
    var pendingInviteBanner by remember { mutableStateOf<Pair<pg.geobingo.one.network.GameInviteDto, pg.geobingo.one.network.UserProfile>?>(null) }
    // Friend request banner state
    var pendingFriendBanner by remember { mutableStateOf<Pair<pg.geobingo.one.network.FriendshipDto, pg.geobingo.one.network.UserProfile>?>(null) }
    val bannerScope = rememberCoroutineScope()

    // Poll for pending game invites and friend requests (every 10s when logged in)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10_000L)
            if (AccountManager.isLoggedIn) {
                try {
                    if (pendingInviteBanner == null) {
                        val invites = FriendsManager.getPendingInvites()
                        if (invites.isNotEmpty()) {
                            pendingInviteBanner = invites.first()
                        }
                    }
                    if (pendingFriendBanner == null && pendingInviteBanner == null) {
                        val requests = FriendsManager.getPendingRequests()
                        if (requests.isNotEmpty()) {
                            pendingFriendBanner = requests.first()
                        }
                    }
                } catch (_: Exception) {}
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
                OfflineBanner(message = S.current.noInternet)
            }

            // Game invite banner
            AnimatedVisibility(
                visible = pendingInviteBanner != null,
                enter = slideInVertically { -it } + expandVertically(),
                exit = slideOutVertically { -it } + shrinkVertically(),
            ) {
                pendingInviteBanner?.let { (invite, profile) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899))))
                            .clickable {
                                // Accept and navigate to join
                                bannerScope.launch {
                                    FriendsManager.respondToInvite(invite.id, true)
                                    gameState.ui.pendingGameInviteCode = invite.game_code
                                    pendingInviteBanner = null
                                    nav.navigateTo(Screen.JOIN_GAME)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.SportsEsports, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${S.current.gameInviteFrom} ${profile.display_name.ifBlank { "Player" }}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    "${S.current.join} - Code: ${invite.game_code}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                            }
                            IconButton(
                                onClick = {
                                    bannerScope.launch {
                                        FriendsManager.respondToInvite(invite.id, false)
                                        pendingInviteBanner = null
                                    }
                                },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Friend request banner
            AnimatedVisibility(
                visible = pendingFriendBanner != null && pendingInviteBanner == null,
                enter = slideInVertically { -it } + expandVertically(),
                exit = slideOutVertically { -it } + shrinkVertically(),
            ) {
                pendingFriendBanner?.let { (friendship, profile) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF22D3EE))))
                            .clickable {
                                // Accept and navigate to friends
                                bannerScope.launch {
                                    FriendsManager.acceptFriendRequest(friendship.id)
                                    pendingFriendBanner = null
                                    nav.navigateTo(Screen.FRIENDS)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.People, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    S.current.friendRequestReceived,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    profile.display_name.ifBlank { "Player" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                            }
                            // Accept button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable {
                                        bannerScope.launch {
                                            FriendsManager.acceptFriendRequest(friendship.id)
                                            pendingFriendBanner = null
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(S.current.accept, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    pendingFriendBanner = null
                                },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
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
                    Screen.SHOP -> ShopScreen(gameState)
                    Screen.PROFILE_SETUP -> ProfileSetupScreen(gameState)
                    Screen.ACCOUNT -> AccountScreen(gameState)
                    Screen.FRIENDS -> FriendsScreen(gameState)
                }
            }
        }
    }
}
