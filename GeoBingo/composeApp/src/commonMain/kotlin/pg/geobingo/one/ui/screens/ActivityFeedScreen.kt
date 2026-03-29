package pg.geobingo.one.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    var activities by remember { mutableStateOf<List<GameRepository.ActivityDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (!AccountManager.isLoggedIn) { loading = false; return@LaunchedEffect }
        try {
            val friends = FriendsManager.getFriends()
            val friendIds = friends.map { it.userId }
            val myId = AccountManager.currentUserId
            val allIds = if (myId != null) friendIds + myId else friendIds
            activities = GameRepository.getFriendsActivity(allIds)
        } catch (e: Exception) {
            AppLogger.w("Activity", "Load failed", e)
        }
        loading = false
    }

    SystemBackHandler { nav.goBack() }

    Scaffold(
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
                    IconButton(onClick = { nav.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.current.back, tint = ColorPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FeedGradient.first())
            }
        } else if (activities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Feed, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(S.current.noActivity, style = MaterialTheme.typography.bodyLarge, color = ColorOnSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
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
            }
        }
    }
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
