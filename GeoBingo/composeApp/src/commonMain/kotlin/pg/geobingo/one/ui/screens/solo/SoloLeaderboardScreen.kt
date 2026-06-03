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
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
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
fun SoloLeaderboardScreen(gameState: GameState, startOnDaily: Boolean = false) {
    val nav = remember { ServiceLocator.navigation }
    // Top-level board switch: 0 = standard all-time/weekly/monthly, 1 = today's daily.
    var boardMode by remember { mutableStateOf(if (startOnDaily) 1 else 0) }
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

    // Daily board: a single global list for today's UTC date, no env/cat/time
    // sub-filters. Loaded lazily the first time the player opens the Daily tab.
    var dailyScores by remember { mutableStateOf<List<SoloScoreDto>>(emptyList()) }
    var dailyLoading by remember { mutableStateOf(false) }
    var dailyError by remember { mutableStateOf(false) }
    var dailyLoaded by remember { mutableStateOf(false) }

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

    // Calendar-aligned windows: the weekly board covers the current ISO week
    // (Mon 00:00 → next Mon 00:00, i.e. it resets Sunday night) and the monthly
    // board the current calendar month; all-time has no lower bound. This
    // replaces the old rolling now-7d / now-30d windows, which never reset on a
    // fixed boundary the player could anticipate.
    val window = remember(selectedTimePeriod) { leaderboardWindow(selectedTimePeriod, Clock.System.now()) }
    val createdAfter: String? = window.startInclusive?.toString()

    // The daily board "resets" at the next UTC midnight, when the date key rolls
    // over and a fresh daily set goes live worldwide.
    val dailyDateKey = pg.geobingo.one.data.dailyRunDateKey()
    val dailyResetAt: Instant = remember(dailyDateKey) {
        val tz = TimeZone.UTC
        Clock.System.now().toLocalDateTime(tz).date.plus(DatePeriod(days = 1)).atStartOfDayIn(tz)
    }

    // Ticks once per second so the visible reset countdown stays live.
    var nowTick by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(selectedTimePeriod) {
        while (true) {
            nowTick = Clock.System.now()
            delay(1000)
        }
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

    // Reload all variants when time period changes (standard board only)
    LaunchedEffect(selectedTimePeriod, boardMode) {
        if (boardMode != 0) return@LaunchedEffect
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
        try {
            val outdoorCount = loadPage(isOutdoor = true, offset = 0, createdAfter = createdAfter)
            val indoorCount = loadPage(isOutdoor = false, offset = 0, createdAfter = createdAfter)
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

    // Load today's global daily board the first time the Daily tab is opened.
    LaunchedEffect(boardMode) {
        if (boardMode != 1 || dailyLoaded) return@LaunchedEffect
        dailyLoading = true
        dailyError = false
        try {
            dailyScores = deduplicateScores(
                GameRepository.getSoloLeaderboard(limit = 200, mode = "daily", dailyDate = dailyDateKey)
            )
            dailyLoaded = true
        } catch (e: Exception) {
            AppLogger.w("Leaderboard", "Daily load failed", e)
            dailyError = true
        }
        dailyLoading = false
    }

    SystemBackHandler { nav.goBack() }

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
            BoardModeTabs(boardMode = boardMode, onSelect = { boardMode = it })

            if (boardMode == 1) {
                DailyBoard(
                    scores = dailyScores,
                    loading = dailyLoading,
                    error = dailyError,
                    resetAt = dailyResetAt,
                    now = nowTick,
                    currentUserId = currentUserId,
                    playerName = playerName,
                )
            } else if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorPrimary)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
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

                LeaderboardResetCountdown(
                    period = selectedTimePeriod,
                    resetAt = window.resetAt,
                    now = nowTick,
                )

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
                                val currentOffset = when {
                                    isOutdoor && selectedCatCount == 0 -> rawOffset5Outdoor
                                    isOutdoor && selectedCatCount == 1 -> rawOffset10Outdoor
                                    !isOutdoor && selectedCatCount == 0 -> rawOffset5Indoor
                                    else -> rawOffset10Indoor
                                }
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

                    // Prefetch cosmetics for all visible leaderboard users in one query
                    val scoreUserIds = remember(scores) { scores.mapNotNull { it.user_id?.takeIf { id -> id.isNotBlank() } } }
                    val cosmeticsByUserIdSolo by pg.geobingo.one.ui.components.rememberPlayerCosmeticsMap(scoreUserIds)
                    val localCosmeticsSolo = pg.geobingo.one.ui.components.rememberLocalUserCosmetics()

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(scores) { index, score ->
                            val isMe = isOwnScore(score, currentUserId, playerName)
                            val rowCosmetics = when {
                                isMe -> localCosmeticsSolo
                                score.user_id != null -> cosmeticsByUserIdSolo[score.user_id] ?: pg.geobingo.one.network.PlayerCosmetics.NONE
                                else -> pg.geobingo.one.network.PlayerCosmetics.NONE
                            }
                            LeaderboardRow(
                                rank = index + 1,
                                score = score,
                                isCurrentPlayer = isMe,
                                cosmetics = rowCosmetics,
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
}


@Composable
private fun LeaderboardRow(
    rank: Int,
    score: SoloScoreDto,
    isCurrentPlayer: Boolean,
    cosmetics: pg.geobingo.one.network.PlayerCosmetics = pg.geobingo.one.network.PlayerCosmetics.NONE,
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFBBF24) // gold
        2 -> Color(0xFF94A3B8) // silver
        3 -> Color(0xFFCD7F32) // bronze
        else -> ColorOnSurfaceVariant
    }
    val subtitle = "${score.categories_count} ${S.current.categories}" +
            if (score.time_bonus > 0) " | +${score.time_bonus}s bonus" else ""

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.width(34.dp), contentAlignment = Alignment.Center) {
            if (rank <= 3) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = rankColor,
                    modifier = Modifier.size(24.dp),
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

        pg.geobingo.one.ui.components.PlayerBanner(
            modifier = Modifier.weight(1f),
            // Some older rows have a blank player_name (pre-guest-flow or
            // submission race). Fall back so the row always has a label.
            name = score.player_name.ifBlank { if (isCurrentPlayer) "Du" else "Spieler" },
            cosmetics = cosmetics,
            avatarColor = if (isCurrentPlayer) ColorPrimary else Color(0xFF6366F1),
            size = pg.geobingo.one.ui.components.PlayerBannerSize.Compact,
            subtitle = subtitle,
            trailing = {
                Text(
                    "${score.score}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (rank <= 3) rankColor else Color.White,
                )
            },
        )
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

private data class LeaderboardWindow(val startInclusive: Instant?, val resetAt: Instant?)

/**
 * Calendar-aligned bounds for a leaderboard time filter.
 * period 1 = current ISO week (Mon 00:00 .. next Mon 00:00 → resets Sunday night),
 * period 2 = current calendar month (1st .. 1st of next month),
 * anything else = all-time (no bounds, never resets).
 */
private fun leaderboardWindow(period: Int, now: Instant): LeaderboardWindow {
    if (period != 1 && period != 2) return LeaderboardWindow(null, null)
    val tz = TimeZone.currentSystemDefault()
    val today = now.toLocalDateTime(tz).date
    return if (period == 1) {
        val weekStart = today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1))
        LeaderboardWindow(
            startInclusive = weekStart.atStartOfDayIn(tz),
            resetAt = weekStart.plus(DatePeriod(days = 7)).atStartOfDayIn(tz),
        )
    } else {
        val monthStart = LocalDate(today.year, today.monthNumber, 1)
        LeaderboardWindow(
            startInclusive = monthStart.atStartOfDayIn(tz),
            resetAt = monthStart.plus(DatePeriod(months = 1)).atStartOfDayIn(tz),
        )
    }
}

/** Small live row telling the player when this leaderboard view next resets. */
@Composable
private fun LeaderboardResetCountdown(period: Int, resetAt: Instant?, now: Instant) {
    val isAllTime = period != 1 && period != 2
    val label = when {
        isAllTime -> S.current.leaderboardNeverResets
        resetAt != null -> {
            val secs = (resetAt - now).inWholeSeconds.coerceAtLeast(0)
            val d = secs / 86_400
            val h = (secs % 86_400) / 3_600
            val m = (secs % 3_600) / 60
            val s = secs % 60
            val dur = when {
                d > 0 -> "${d}d ${h}h ${m}m"
                h > 0 -> "${h}h ${m}m ${s}s"
                else -> "${m}m ${s}s"
            }
            "${S.current.leaderboardResetIn} $dur"
        }
        else -> return
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isAllTime) Icons.Default.AllInclusive else Icons.Default.Timer,
            contentDescription = null,
            tint = ColorOnSurfaceVariant,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = ColorOnSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Top-level segmented switch between the all-time Standard board and today's global Daily board. */
@Composable
private fun BoardModeTabs(boardMode: Int, onSelect: (Int) -> Unit) {
    val gradient = listOf(Color(0xFF22D3EE), Color(0xFF6366F1))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            0 to (Icons.Default.Leaderboard to S.current.leaderboardTabStandard),
            1 to (Icons.Default.Today to S.current.leaderboardTabDaily),
        ).forEach { (idx, iconLabel) ->
            val (icon, label) = iconLabel
            val selected = boardMode == idx
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) Brush.linearGradient(gradient)
                        else Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) Color.Transparent else ColorOutline,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(idx) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = if (selected) Color.White else ColorOnSurfaceVariant,
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else ColorOnSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Today's global daily board. One flat ranked list (no env/cat/time sub-filters)
 * scoped to the current UTC date key; it naturally "resets" when a new daily set
 * goes live at midnight UTC.
 */
@Composable
private fun ColumnScope.DailyBoard(
    scores: List<SoloScoreDto>,
    loading: Boolean,
    error: Boolean,
    resetAt: Instant,
    now: Instant,
    currentUserId: String?,
    playerName: String,
) {
    LeaderboardResetCountdown(period = 1, resetAt = resetAt, now = now)
    Spacer(Modifier.height(2.dp))

    when {
        loading -> {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorPrimary)
            }
        }
        scores.isEmpty() -> {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (error) Icons.Default.CloudOff else Icons.Default.Today,
                        null,
                        tint = ColorOnSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (error) S.current.error else S.current.soloNoScoresYet,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorOnSurfaceVariant,
                    )
                }
            }
        }
        else -> {
            val scoreUserIds = remember(scores) { scores.mapNotNull { it.user_id?.takeIf { id -> id.isNotBlank() } } }
            val cosmeticsByUserId by pg.geobingo.one.ui.components.rememberPlayerCosmeticsMap(scoreUserIds)
            val localCosmetics = pg.geobingo.one.ui.components.rememberLocalUserCosmetics()
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(scores) { index, score ->
                    val isMe = isOwnScore(score, currentUserId, playerName)
                    val rowCosmetics = when {
                        isMe -> localCosmetics
                        score.user_id != null -> cosmeticsByUserId[score.user_id] ?: pg.geobingo.one.network.PlayerCosmetics.NONE
                        else -> pg.geobingo.one.network.PlayerCosmetics.NONE
                    }
                    LeaderboardRow(
                        rank = index + 1,
                        score = score,
                        isCurrentPlayer = isMe,
                        cosmetics = rowCosmetics,
                    )
                }
            }
        }
    }
}
