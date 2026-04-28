package pg.geobingo.one.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.network.FriendsManager
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.util.AppLogger

private val FeedGradient = listOf(Color(0xFFF59E0B), Color(0xFFEF4444))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityFeedScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val snackbarHostState = remember { SnackbarHostState() }
    var showAuthDialog by remember { mutableStateOf(false) }
    var activities by remember { mutableStateOf<List<GameRepository.ActivityDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var allIds by remember { mutableStateOf<List<String>>(emptyList()) }
    val pageSize = 30

    LaunchedEffect(Unit) {
        if (!AccountManager.isLoggedIn) { loading = false; return@LaunchedEffect }
        try {
            val friends = FriendsManager.getFriends()
            val friendIds = friends.map { it.userId }
            val myId = AccountManager.currentUserId
            allIds = if (myId != null) friendIds + myId else friendIds
            val page = GameRepository.getFriendsActivity(allIds, limit = pageSize)
            activities = page
            hasMore = page.size >= pageSize
        } catch (e: Exception) {
            AppLogger.w("Activity", "Load failed", e)
        }
        loading = false
    }

    SystemBackHandler { nav.goBack() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.activityFeed,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = FeedGradient,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goHome() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.current.back, tint = ColorPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        if (!AccountManager.isLoggedIn) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                SignInRequiredState(
                    icon = Icons.Default.RssFeed,
                    title = S.current.signInRequired,
                    description = S.current.signInRequiredDesc,
                    signInLabel = S.current.signIn,
                    onSignIn = { showAuthDialog = true },
                )
            }
            return@Scaffold
        }
        if (loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FeedGradient.first())
            }
        } else if (activities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.RssFeed, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(S.current.noActivity, style = MaterialTheme.typography.bodyLarge, color = ColorOnSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        S.current.noActivityDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        } else {
            val listState = rememberLazyListState()

            // Detect when scrolled near the bottom and load more
            val shouldLoadMore by remember {
                derivedStateOf {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val totalItems = listState.layoutInfo.totalItemsCount
                    lastVisible >= totalItems - 3 && !loadingMore && hasMore
                }
            }
            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore && activities.isNotEmpty()) {
                    loadingMore = true
                    try {
                        val page = GameRepository.getFriendsActivity(allIds, limit = pageSize, offset = activities.size)
                        activities = activities + page
                        hasMore = page.size >= pageSize
                    } catch (e: Exception) {
                        AppLogger.w("Activity", "Load more failed", e)
                    }
                    loadingMore = false
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(activities, key = { it.id }) { activity ->
                    val (icon, iconColor) = when (activity.event_type) {
                        "game_won" -> Icons.Default.EmojiEvents to Color(0xFFFBBF24)
                        "game_played" -> Icons.Default.SportsEsports to ColorPrimary
                        "joined" -> Icons.Default.PersonAdd to Color(0xFF22C55E)
                        else -> Icons.Default.Notifications to ColorOnSurfaceVariant
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ColorOutlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    activity.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorOnSurface,
                                )
                                val timeAgo = formatTimeAgo(activity.created_at)
                                Text(
                                    timeAgo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorOnSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (loadingMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = FeedGradient.first(), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }

    SignInDialogHost(
        visible = showAuthDialog,
        onDismiss = { showAuthDialog = false },
        gameState = gameState,
        snackbarHostState = snackbarHostState,
    )
}

private fun formatTimeAgo(isoTimestamp: String): String {
    return try {
        val now = kotlinx.datetime.Clock.System.now()
        val then = kotlinx.datetime.Instant.parse(isoTimestamp)
        val diff = now - then
        when {
            diff.inWholeMinutes < 1 -> "just now"
            diff.inWholeMinutes < 60 -> "${diff.inWholeMinutes}min"
            diff.inWholeHours < 24 -> "${diff.inWholeHours}h"
            else -> "${diff.inWholeDays}d"
        }
    } catch (_: Exception) { "" }
}
