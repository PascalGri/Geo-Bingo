package pg.geobingo.one.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.PlayerCosmetics
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.components.PlayerBanner
import pg.geobingo.one.ui.components.PlayerBannerSize
import pg.geobingo.one.ui.components.rememberLocalUserCosmetics
import pg.geobingo.one.ui.components.rememberPlayerCosmeticsMap
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.util.AppLogger

private val MPGradient = listOf(Color(0xFFEC4899), Color(0xFFF59E0B))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerLeaderboardScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    var entries by remember { mutableStateOf<List<GameRepository.MultiplayerStatsDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedSort by remember { mutableStateOf(0) } // 0=wins, 1=streak, 2=games
    val currentUserId = AccountManager.currentUserId

    val sortOptions: List<Pair<String, String>> = listOf(
        Pair(S.current.wins, "games_won"),
        Pair(S.current.winStreak, "longest_win_streak"),
        Pair(S.current.gamesPlayed, "games_played"),
    )

    LaunchedEffect(selectedSort) {
        loading = true
        try {
            entries = GameRepository.getMultiplayerLeaderboard(50, sortOptions[selectedSort].second)
        } catch (e: Exception) {
            AppLogger.w("MPLeaderboard", "Load failed", e)
        }
        loading = false
    }

    SystemBackHandler { nav.goBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.multiplayerLeaderboard,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = MPGradient,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goHome() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.current.back, tint = ColorPrimary)
                    }
                },
                actions = { pg.geobingo.one.ui.components.TopBarStarsAndProfile(gameState = gameState, onNavigate = { nav.navigateTo(it) }) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Sort tabs
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sortOptions.forEachIndexed { idx, (label, _) ->
                    val selected = selectedSort == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) Brush.linearGradient(MPGradient)
                                else Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
                            )
                            .clickable { selectedSort = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) Color.White else ColorOnSurfaceVariant,
                        )
                    }
                }
            }

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MPGradient.first())
                }
            } else if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EmojiEvents, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(S.current.soloNoScoresYet, style = MaterialTheme.typography.bodyLarge, color = ColorOnSurfaceVariant)
                    }
                }
            } else {
                // Prefetch cosmetics for all visible leaderboard entries in one query
                val userIds = remember(entries) { entries.map { it.user_id }.filter { it.isNotBlank() } }
                val cosmeticsByUserId by rememberPlayerCosmeticsMap(userIds)
                val localCosmetics = rememberLocalUserCosmetics()

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(entries) { index, entry ->
                        val isMe = entry.user_id == currentUserId
                        val rankColor = when (index) {
                            0 -> Color(0xFFFBBF24)
                            1 -> Color(0xFF94A3B8)
                            2 -> Color(0xFFCD7F32)
                            else -> ColorOnSurfaceVariant
                        }
                        val sortValue = when (selectedSort) {
                            0 -> "${entry.games_won}"
                            1 -> "${entry.longest_win_streak}"
                            else -> "${entry.games_played}"
                        }

                        val playerCosmetics = if (isMe) {
                            localCosmetics
                        } else {
                            cosmeticsByUserId[entry.user_id] ?: PlayerCosmetics.NONE
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Rank chip on the left
                            Box(modifier = Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                                if (index < 3) {
                                    Icon(Icons.Default.EmojiEvents, null, tint = rankColor, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(
                                        "#${index + 1}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = rankColor,
                                    )
                                }
                            }
                            // Banner on the right
                            PlayerBanner(
                                modifier = Modifier.weight(1f),
                                name = entry.display_name.ifBlank { "Player" },
                                cosmetics = playerCosmetics,
                                avatarColor = if (isMe) ColorPrimary else MPGradient.first(),
                                size = PlayerBannerSize.Compact,
                                subtitle = "${entry.games_won} ${S.current.wins} • ${entry.games_played} ${S.current.gamesPlayed}",
                                trailing = {
                                    Text(
                                        sortValue,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index < 3) rankColor else Color.White,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
