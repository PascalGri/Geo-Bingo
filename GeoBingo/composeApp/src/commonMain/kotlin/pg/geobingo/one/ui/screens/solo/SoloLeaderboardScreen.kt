package pg.geobingo.one.ui.screens.solo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.SoloScoreDto
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.util.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloLeaderboardScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    var selectedEnvironment by remember { mutableStateOf(0) } // 0 = outdoor, 1 = indoor
    var selectedCatCount by remember { mutableStateOf(0) } // 0 = 5 categories, 1 = 10 categories
    var selectedTimePeriod by remember { mutableStateOf(0) } // 0 = all-time, 1 = this week, 2 = this month

    // Cache for all 4 leaderboard variants
    var scores5Outdoor by remember { mutableStateOf<List<SoloScoreDto>>(emptyList()) }
    var scores5Indoor by remember { mutableStateOf<List<SoloScoreDto>>(emptyList()) }
    var scores10Outdoor by remember { mutableStateOf<List<SoloScoreDto>>(emptyList()) }
    var scores10Indoor by remember { mutableStateOf<List<SoloScoreDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    // Pagination state per variant: raw offset tracks how many raw rows have been fetched from DB
    var rawOffset5Outdoor by remember { mutableStateOf(0) }
    var rawOffset5Indoor by remember { mutableStateOf(0) }
    var rawOffset10Outdoor by remember { mutableStateOf(0) }
    var rawOffset10Indoor by remember { mutableStateOf(0) }
    var hasMore5Outdoor by remember { mutableStateOf(true) }
    var hasMore5Indoor by remember { mutableStateOf(true) }
    var hasMore10Outdoor by remember { mutableStateOf(true) }
    var hasMore10Indoor by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    val pageSize = 100 // fetch in chunks from DB, then deduplicate + filter client-side

    val currentUserId = AccountManager.currentUserId
    val playerName = gameState.solo.playerName

    val createdAfter: String? = when (selectedTimePeriod) {
        1 -> (Clock.System.now() - 7.days).toString()
        2 -> (Clock.System.now() - 30.days).toString()
        else -> null
    }

    val scores = when {
        selectedCatCount == 0 && selectedEnvironment == 0 -> scores5Outdoor
        selectedCatCount == 0 && selectedEnvironment == 1 -> scores5Indoor
        selectedCatCount == 1 && selectedEnvironment == 0 -> scores10Outdoor
        else -> scores10Indoor
    }
    val hasMore = when {
        selectedCatCount == 0 && selectedEnvironment == 0 -> hasMore5Outdoor
        selectedCatCount == 0 && selectedEnvironment == 1 -> hasMore5Indoor
        selectedCatCount == 1 && selectedEnvironment == 0 -> hasMore10Outdoor
        else -> hasMore10Indoor
    }

    /** Load a page of scores for the given outdoor flag and append to the correct variant lists. */
    suspend fun loadPage(isOutdoor: Boolean, offset: Int, createdAfter: String? = null): Int {
        val raw = GameRepository.getSoloLeaderboard(pageSize, isOutdoor = isOutdoor, offset = offset, createdAfter = createdAfter)
        val raw5 = raw.filter { it.categories_count <= 5 }
        val raw10 = raw.filter { it.categories_count > 5 }
        if (isOutdoor) {
            scores5Outdoor = deduplicateScores(scores5Outdoor + raw5)
            scores10Outdoor = deduplicateScores(scores10Outdoor + raw10)
            rawOffset5Outdoor = offset + raw.size
            rawOffset10Outdoor = offset + raw.size
        } else {
            scores5Indoor = deduplicateScores(scores5Indoor + raw5)
            scores10Indoor = deduplicateScores(scores10Indoor + raw10)
            rawOffset5Indoor = offset + raw.size
            rawOffset10Indoor = offset + raw.size
        }
        return raw.size
    }

    // Reload all variants when time period changes
    LaunchedEffect(selectedTimePeriod) {
        loading = true
        error = false
        scores5Outdoor = emptyList()
        scores5Indoor = emptyList()
        scores10Outdoor = emptyList()
        scores10Indoor = emptyList()
        rawOffset5Outdoor = 0
        rawOffset5Indoor = 0
        rawOffset10Outdoor = 0
        rawOffset10Indoor = 0
        hasMore5Outdoor = true
        hasMore5Indoor = true
        hasMore10Outdoor = true
        hasMore10Indoor = true
        val periodFilter: String? = when (selectedTimePeriod) {
            1 -> (Clock.System.now() - 7.days).toString()
            2 -> (Clock.System.now() - 30.days).toString()
            else -> null
        }
        try {
            val outdoorCount = loadPage(isOutdoor = true, offset = 0, createdAfter = periodFilter)
            val indoorCount = loadPage(isOutdoor = false, offset = 0, createdAfter = periodFilter)
            hasMore5Outdoor = outdoorCount >= pageSize
            hasMore10Outdoor = outdoorCount >= pageSize
            hasMore5Indoor = indoorCount >= pageSize
            hasMore10Indoor = indoorCount >= pageSize
            loading = false
        } catch (e: Exception) {
            AppLogger.w("Leaderboard", "Failed to load", e)
            loading = false
            error = true
        }
    }

    SystemBackHandler { nav.goBack() }

    val isInTop50 = scores.any { isOwnScore(it, currentUserId, playerName) }
    val tabGradient = listOf(Color(0xFF22D3EE), Color(0xFF6366F1))

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
                actions = { pg.geobingo.one.ui.components.TopBarStarsAndProfile(gameState = gameState, onNavigate = { nav.navigateTo(it) }) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorPrimary)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Category count tabs (5 / 10)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        0 to "5 ${S.current.categories}",
                        1 to "10 ${S.current.categories}",
                    ).forEach { (idx, label) ->
                        val selected = selectedCatCount == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) Brush.linearGradient(tabGradient)
                                    else Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Color.Transparent else ColorOutline,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { selectedCatCount = idx }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (idx == 0) Icons.Default.GridView else Icons.Default.GridOn,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (selected) Color.White else ColorOnSurfaceVariant,
                                )
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) Color.White else ColorOnSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Outdoor/Indoor tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        0 to S.current.outdoor,
                        1 to S.current.indoor,
                    ).forEach { (idx, label) ->
                        val selected = selectedEnvironment == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) Brush.linearGradient(tabGradient.map { it.copy(alpha = 0.7f) })
                                    else Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Color.Transparent else ColorOutline,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { selectedEnvironment = idx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (idx == 0) Icons.Default.WbSunny else Icons.Default.House,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (selected) Color.White else ColorOnSurfaceVariant,
                                )
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) Color.White else ColorOnSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Time period tabs (All-time / This Week / This Month)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        0 to S.current.leaderboardAllTime,
                        1 to S.current.leaderboardWeekly,
                        2 to S.current.leaderboardMonthly,
                    ).forEach { (idx, label) ->
                        val selected = selectedTimePeriod == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) Brush.linearGradient(tabGradient.map { it.copy(alpha = 0.55f) })
                                    else Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Color.Transparent else ColorOutline,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable { selectedTimePeriod = idx }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) Color.White else ColorOnSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                if (scores.isEmpty() && !loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    val listState = rememberLazyListState()

                    // Detect when scrolled near the bottom and load more
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val totalItems = listState.layoutInfo.totalItemsCount
                            lastVisible >= totalItems - 3 && !loadingMore && hasMore
                        }
                    }
                    LaunchedEffect(shouldLoadMore, selectedEnvironment, selectedCatCount) {
                        if (shouldLoadMore && scores.isNotEmpty()) {
                            loadingMore = true
                            try {
                                val isOutdoor = selectedEnvironment == 0
                                val currentOffset = if (isOutdoor) rawOffset5Outdoor else rawOffset5Indoor
                                val count = loadPage(isOutdoor = isOutdoor, offset = currentOffset, createdAfter = createdAfter)
                                val noMore = count < pageSize
                                if (isOutdoor) {
                                    hasMore5Outdoor = !noMore
                                    hasMore10Outdoor = !noMore
                                } else {
                                    hasMore5Indoor = !noMore
                                    hasMore10Indoor = !noMore
                                }
                            } catch (e: Exception) {
                                AppLogger.w("Leaderboard", "Load more failed", e)
                            }
                            loadingMore = false
                        }
                    }

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(scores) { index, score ->
                            LeaderboardRow(
                                rank = index + 1,
                                score = score,
                                isCurrentPlayer = isOwnScore(score, currentUserId, playerName),
                            )
                        }

                        if (loadingMore) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = ColorPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
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
                if (isCurrentPlayer) {
                    pg.geobingo.one.ui.components.CosmeticPlayerName(
                        name = score.player_name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fallbackColor = ColorPrimary,
                    )
                } else {
                    Text(
                        score.player_name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = ColorOnSurface,
                    )
                }
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

/** Check if a score belongs to the current user: prefer user_id match, fall back to name for guests. */
private fun isOwnScore(score: SoloScoreDto, currentUserId: String?, playerName: String): Boolean {
    if (currentUserId != null && score.user_id != null) return score.user_id == currentUserId
    if (currentUserId != null && score.user_id == null) return false
    return score.player_name == playerName
}

/** Deduplicate scores: keep best per user_id (if set), otherwise per player_name. */
private fun deduplicateScores(scores: List<SoloScoreDto>): List<SoloScoreDto> {
    val seen = mutableSetOf<String>()
    return scores.filter { score ->
        val key = score.user_id ?: "name:${score.player_name}"
        seen.add(key)
    }
}
