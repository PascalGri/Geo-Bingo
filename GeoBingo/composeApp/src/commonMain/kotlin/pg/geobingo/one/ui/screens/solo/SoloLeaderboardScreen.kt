package pg.geobingo.one.ui.screens.solo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.SoloScoreDto
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.util.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloLeaderboardScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    var scores by remember { mutableStateOf<List<SoloScoreDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var myRank by remember { mutableStateOf<Int?>(null) }
    var totalPlayers by remember { mutableStateOf(0) }
    var myBest by remember { mutableStateOf<SoloScoreDto?>(null) }

    val playerName = gameState.solo.playerName

    LaunchedEffect(Unit) {
        try {
            scores = GameRepository.getSoloLeaderboard(50)
            loading = false
        } catch (e: Exception) {
            AppLogger.w("Leaderboard", "Failed to load", e)
            loading = false
            error = true
        }
        // Load own rank in parallel
        if (playerName.isNotBlank()) {
            try {
                myRank = GameRepository.getSoloRank(playerName)
                totalPlayers = GameRepository.getSoloTotalPlayers()
                myBest = GameRepository.getSoloPersonalBest(playerName)
            } catch (e: Exception) {
                AppLogger.d("Leaderboard", "Rank lookup failed", e)
            }
        }
    }

    SystemBackHandler { nav.goBack() }

    val isInTop50 = scores.any { it.player_name == playerName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.soloLeaderboard,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.current.back, tint = ColorPrimary)
                    }
                },
                actions = { pg.geobingo.one.ui.components.StarsChip(count = gameState.stars.starCount, onClick = { nav.navigateTo(pg.geobingo.one.game.Screen.SHOP) }) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorPrimary)
            }
        } else if (error || scores.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Star, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (error) S.current.error else S.current.soloNoScoresYet,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorOnSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Own rank banner if NOT in top 50
                if (!isInTop50 && myRank != null && myBest != null) {
                    item {
                        MyRankBanner(
                            rank = myRank!!,
                            totalPlayers = totalPlayers,
                            bestScore = myBest!!,
                            playerName = playerName,
                        )
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = ColorOutlineVariant)
                        Spacer(Modifier.height(4.dp))
                    }
                }

                // Top 50
                itemsIndexed(scores) { index, score ->
                    LeaderboardRow(
                        rank = index + 1,
                        score = score,
                        isCurrentPlayer = score.player_name == playerName,
                    )
                }

                // Footer with own rank if in top 50
                if (isInTop50 && myRank != null) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            S.current.soloRankDisplay(myRank!!, totalPlayers),
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorOnSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyRankBanner(rank: Int, totalPlayers: Int, bestScore: SoloScoreDto, playerName: String) {
    val bracket = when {
        rank <= 10 -> "Top 10"
        rank <= 50 -> "Top 50"
        rank <= 100 -> "Top 100"
        rank <= 500 -> "Top 500"
        rank <= 1000 -> "Top 1000"
        else -> "Top ${((rank / 1000) + 1) * 1000}"
    }

    GradientBorderCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        borderColors = GradientPrimary,
        backgroundColor = ColorPrimary.copy(alpha = 0.06f),
        borderWidth = 1.5.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        S.current.soloYourRank,
                        style = MaterialTheme.typography.labelMedium,
                        color = ColorOnSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "#$rank",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorPrimary,
                        )
                        Text(
                            "/ $totalPlayers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorOnSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        S.current.soloApproxRank(bracket),
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${bestScore.score} ${S.current.pointsAbbrev}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorOnSurface,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Progress bar showing rough position
            val progress = if (totalPlayers > 0) 1f - (rank.toFloat() / totalPlayers) else 0f
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = ColorPrimary,
                trackColor = ColorSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("#1", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant, fontSize = 10.sp)
                Text("#$totalPlayers", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, score: SoloScoreDto, isCurrentPlayer: Boolean) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFBBF24) // gold
        2 -> Color(0xFF94A3B8) // silver
        3 -> Color(0xFFCD7F32) // bronze
        else -> ColorOnSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlayer) ColorPrimary.copy(alpha = 0.08f) else ColorSurface,
        ),
        border = if (isCurrentPlayer) BorderStroke(1.dp, ColorPrimary.copy(alpha = 0.3f))
        else BorderStroke(1.dp, ColorOutlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Rank
            Box(modifier = Modifier.width(30.dp), contentAlignment = Alignment.Center) {
                if (rank <= 3) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = rankColor,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Text(
                        "#$rank",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = rankColor,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))

            // Name + details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    score.player_name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrentPlayer) ColorPrimary else ColorOnSurface,
                )
                Text(
                    "${score.categories_count} ${S.current.categories}" +
                            if (score.time_bonus > 0) " | +${score.time_bonus}s bonus" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                )
            }

            // Score
            Text(
                "${score.score}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) rankColor else ColorOnSurface,
            )
        }
    }
}
