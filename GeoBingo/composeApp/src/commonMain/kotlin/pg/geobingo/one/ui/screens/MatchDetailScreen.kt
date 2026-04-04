package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pg.geobingo.one.data.Player
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.network.CaptureDto
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.VoteDto
import pg.geobingo.one.network.parseHexColor
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.Spacing
import pg.geobingo.one.util.AppLogger

/** Data loaded from the network for one match. */
private data class MatchDetailData(
    val captures: List<CaptureDto>,
    val votes: List<VoteDto>,
    val players: List<pg.geobingo.one.network.PlayerDto>,
    val categories: List<pg.geobingo.one.network.CategoryDto>,
)

/** Resolved photo for the photo grid section. */
private data class MatchPhoto(
    val playerId: String,
    val playerName: String,
    val playerColorHex: String,
    val categoryName: String,
    val bitmap: ImageBitmap,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    SystemBackHandler { nav.goBack() }

    val gameId = gameState.ui.selectedMatchGameId
    val entry = gameState.ui.selectedMatchEntry

    // Fade/slide in
    val contentAlpha = remember { Animatable(0f) }
    val contentOffset = remember { Animatable(30f) }
    LaunchedEffect(Unit) {
        launch { contentOffset.animateTo(0f, tween(380)) }
        contentAlpha.animateTo(1f, tween(380))
    }

    // Remote data loading
    var detailData by remember { mutableStateOf<MatchDetailData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    LaunchedEffect(gameId) {
        if (gameId.isNullOrEmpty()) {
            loading = false
            loadError = true
            return@LaunchedEffect
        }
        loading = true
        loadError = false
        try {
            val captures = GameRepository.getCaptures(gameId)
            val votes = GameRepository.getVotes(gameId)
            val players = GameRepository.getPlayers(gameId)
            val categories = GameRepository.getCategories(gameId)
            detailData = MatchDetailData(captures, votes, players, categories)
        } catch (e: Exception) {
            AppLogger.w("MatchDetail", "Failed to load match data for $gameId", e)
            loadError = true
        }
        loading = false
    }

    // Photos loaded from local cache
    var photos by remember { mutableStateOf<List<MatchPhoto>>(emptyList()) }
    var photosLoading by remember { mutableStateOf(false) }

    LaunchedEffect(detailData) {
        val data = detailData ?: return@LaunchedEffect
        if (gameId.isNullOrEmpty()) return@LaunchedEffect
        photosLoading = true
        val loaded = mutableListOf<MatchPhoto>()
        val playerMap = data.players.associateBy { it.id }
        val categoryMap = data.categories.associateBy { it.id }

        // Use captures as the source of truth for what photos might exist
        for (capture in data.captures) {
            val player = playerMap[capture.player_id] ?: continue
            val category = categoryMap[capture.category_id] ?: continue
            try {
                val bytes = withContext(Dispatchers.Default) {
                    LocalPhotoStore.loadPhoto(gameId, capture.player_id, capture.category_id)
                        ?: GameRepository.downloadPhoto(gameId, capture.player_id, capture.category_id)
                }
                if (bytes != null) {
                    val bitmap = withContext(Dispatchers.Default) { bytes.toImageBitmap() }
                    if (bitmap != null) {
                        loaded.add(
                            MatchPhoto(
                                playerId = capture.player_id,
                                playerName = player.name,
                                playerColorHex = player.color,
                                categoryName = category.label,
                                bitmap = bitmap,
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("MatchDetail", "Photo load failed: ${capture.category_id}/${capture.player_id}", e)
            }
        }
        photos = loaded
        photosLoading = false
    }

    // Format the date for the top bar
    val dateText = remember(entry?.date) {
        val raw = entry?.date ?: return@remember ""
        try {
            val instant = Instant.parse(raw)
            val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val day = local.dayOfMonth.toString().padStart(2, '0')
            val month = local.monthNumber.toString().padStart(2, '0')
            val hour = local.hour.toString().padStart(2, '0')
            val minute = local.minute.toString().padStart(2, '0')
            "$day.$month.${local.year}  $hour:$minute"
        } catch (e: Exception) {
            ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        AnimatedGradientText(
                            text = S.current.matchDetails,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            gradientColors = GradientPrimary,
                        )
                        if (dateText.isNotEmpty() || entry?.gameCode?.isNotEmpty() == true) {
                            Text(
                                text = buildString {
                                    if (dateText.isNotEmpty()) append(dateText)
                                    if (dateText.isNotEmpty() && entry?.gameCode?.isNotEmpty() == true) append("  ")
                                    if (entry?.gameCode?.isNotEmpty() == true) append(entry.gameCode)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorOnSurfaceVariant,
                                fontSize = 10.sp,
                            )
                        }
                    }
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
        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = ColorPrimary)
                }
            }
            loadError || detailData == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            S.current.noDataAvailable,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorOnSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            else -> {
                val data = detailData!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = Spacing.screenHorizontal)
                        .graphicsLayer { translationY = contentOffset.value; alpha = contentAlpha.value },
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { Spacer(Modifier.height(4.dp)) }

                    // ── Player Rankings ──────────────────────────────────────────────
                    item {
                        PlayerRankingsSection(
                            data = data,
                            entry = entry,
                        )
                    }

                    // ── Category Breakdown ───────────────────────────────────────────
                    item {
                        CategoryBreakdownSection(
                            data = data,
                            gameId = gameId ?: "",
                        )
                    }

                    // ── Photos ───────────────────────────────────────────────────────
                    item {
                        PhotosSection(
                            photos = photos,
                            photosLoading = photosLoading,
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ── Player Rankings ──────────────────────────────────────────────────────────

@Composable
private fun PlayerRankingsSection(
    data: MatchDetailData,
    entry: pg.geobingo.one.game.GameHistoryEntry?,
) {
    // Build player scores from votes: sum of avg ratings per capture + speed bonuses
    val playerScores = computePlayerScores(data)
    val rankedPlayers = data.players.sortedByDescending { playerScores[it.id] ?: 0.0 }

    GradientBorderCard(
        modifier = Modifier.fillMaxWidth(),
        borderColors = GradientPrimary,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp), tint = ColorPrimary)
                Text(
                    S.current.results,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorOnSurface,
                )
            }
            HorizontalDivider(color = ColorOutlineVariant)
            rankedPlayers.forEachIndexed { i, player ->
                val score = playerScores[player.id] ?: 0.0
                val rankColor = when (i) {
                    0 -> Color(0xFFFBBF24)
                    1 -> Color(0xFF94A3B8)
                    2 -> Color(0xFFCD7F32)
                    else -> ColorOnSurfaceVariant
                }
                val isMe = player.name == entry?.playerName
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "${i + 1}.",
                        fontSize = 13.sp,
                        fontWeight = if (i < 3) FontWeight.Bold else FontWeight.Normal,
                        color = rankColor,
                    )
                    PlayerAvatarView(
                        player = Player(id = player.id, name = player.name, color = parseHexColor(player.color)),
                        size = 26.dp,
                        fontSize = 10.sp,
                    )
                    Text(
                        player.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                        color = if (isMe) ColorPrimary else ColorOnSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Capture count badge
                    val captureCount = data.captures.count { it.player_id == player.id }
                    if (captureCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ColorSurfaceVariant,
                        ) {
                            Text(
                                "$captureCount",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorOnSurfaceVariant,
                                fontSize = 10.sp,
                            )
                        }
                    }
                    Text(
                        "${score.toInt()} ${S.current.pointsAbbrev}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Category Breakdown ───────────────────────────────────────────────────────

@Composable
private fun CategoryBreakdownSection(
    data: MatchDetailData,
    gameId: String,
) {
    // Determine first capturers per category (speed bonus)
    val firstCapturerPerCategory = data.captures
        .groupBy { it.category_id }
        .mapValues { (_, captures) ->
            captures.minByOrNull { it.created_at }?.player_id
        }

    val playerMap = data.players.associateBy { it.id }

    GradientBorderCard(
        modifier = Modifier.fillMaxWidth(),
        borderColors = GradientCool,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = ColorSecondary)
                Text(
                    S.current.categoryBreakdownDetail,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorOnSurface,
                )
            }
            HorizontalDivider(color = ColorOutlineVariant)

            if (data.categories.isEmpty()) {
                Text(
                    S.current.noDataAvailable,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurfaceVariant,
                )
            } else {
                data.categories.forEach { category ->
                    val capturesForCategory = data.captures.filter { it.category_id == category.id }
                    val firstCapturerId = firstCapturerPerCategory[category.id]

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Category name header
                        Text(
                            category.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorOnSurface,
                        )

                        if (capturesForCategory.isEmpty()) {
                            Text(
                                S.current.noDataAvailable,
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorOnSurfaceVariant,
                            )
                        } else {
                            capturesForCategory.forEach { capture ->
                                val player = playerMap[capture.player_id]
                                val isFirst = capture.player_id == firstCapturerId
                                val avgRating = computeAvgRating(data.votes, capture.player_id, capture.category_id)

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    // Small player color dot
                                    if (player != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColor(player.color)),
                                        )
                                        Text(
                                            player.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = parseHexColor(player.color),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }

                                    // Speed bonus indicator
                                    if (isFirst) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ColorWarning.copy(alpha = 0.15f),
                                        ) {
                                            Text(
                                                S.current.firstCapture,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ColorWarning,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }

                                    // Star rating display
                                    if (avgRating != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                                        ) {
                                            MiniStarRating(avgRating)
                                        }
                                        val ratingText = ((avgRating * 10).toInt() / 10.0).let {
                                            val whole = it.toInt()
                                            val dec = ((it - whole) * 10).toInt()
                                            "$whole.$dec"
                                        }
                                        Text(
                                            ratingText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ColorOnSurfaceVariant,
                                            fontSize = 10.sp,
                                        )
                                    }
                                }
                            }
                        }

                        if (category != data.categories.last()) {
                            HorizontalDivider(color = ColorOutlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

// ── Star rating display ───────────────────────────────────────────────────────

@Composable
private fun MiniStarRating(rating: Double, maxStars: Int = 5) {
    val full = rating.toInt()
    val half = (rating - full) >= 0.4
    for (i in 1..maxStars) {
        val iconSize = 10.dp
        when {
            i <= full -> Icon(Icons.Default.Star, null, modifier = Modifier.size(iconSize), tint = ColorWarning)
            i == full + 1 && half -> Icon(Icons.Default.StarHalf, null, modifier = Modifier.size(iconSize), tint = ColorWarning)
            else -> Icon(Icons.Default.StarBorder, null, modifier = Modifier.size(iconSize), tint = ColorOnSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}

// ── Photos section ────────────────────────────────────────────────────────────

@Composable
private fun PhotosSection(
    photos: List<MatchPhoto>,
    photosLoading: Boolean,
) {
    GradientBorderCard(
        modifier = Modifier.fillMaxWidth(),
        borderColors = GradientWarm,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp), tint = ColorTertiary)
                Text(
                    S.current.photos,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorOnSurface,
                )
            }
            HorizontalDivider(color = ColorOutlineVariant)

            when {
                photosLoading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ShimmerPlaceholder(modifier = Modifier.weight(1f).aspectRatio(1f), cornerRadius = 10.dp)
                        ShimmerPlaceholder(modifier = Modifier.weight(1f).aspectRatio(1f), cornerRadius = 10.dp)
                    }
                }
                photos.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ColorBackground)
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = ColorOnSurfaceVariant.copy(alpha = 0.5f),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                S.current.noPhotosAvailable,
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorOnSurfaceVariant,
                            )
                        }
                    }
                }
                else -> {
                    val rows = photos.chunked(2)
                    rows.forEach { rowPhotos ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowPhotos.forEach { photo ->
                                MatchPhotoItem(photo = photo, modifier = Modifier.weight(1f))
                            }
                            if (rowPhotos.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ── Individual photo item ─────────────────────────────────────────────────────

@Composable
private fun MatchPhotoItem(
    photo: MatchPhoto,
    modifier: Modifier = Modifier,
) {
    var showFullscreen by remember { mutableStateOf(false) }

    if (showFullscreen) {
        AlertDialog(
            onDismissRequest = { showFullscreen = false },
            containerColor = Color(0xFF0A0A0A),
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = photo.bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.FillWidth,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(photo.playerName, color = parseHexColor(photo.playerColorHex), fontWeight = FontWeight.Bold)
                    Text(photo.categoryName, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showFullscreen = false }) {
                    Text(S.current.close)
                }
            },
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(ColorSurface)
            .clickable { showFullscreen = true },
    ) {
        Image(
            bitmap = photo.bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 5.dp, vertical = 3.dp),
        ) {
            Column {
                Text(
                    photo.playerName,
                    color = parseHexColor(photo.playerColorHex),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    photo.categoryName,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 8.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Compute a simplified score per player:
 *   - Sum of average star ratings across all their captures
 *   - +1 bonus point for each category where they were first capturer
 */
private fun computePlayerScores(data: MatchDetailData): Map<String, Double> {
    val firstCapturerPerCategory = data.captures
        .groupBy { it.category_id }
        .mapValues { (_, captures) -> captures.minByOrNull { it.created_at }?.player_id }

    val scores = mutableMapOf<String, Double>()
    for (player in data.players) {
        var total = 0.0
        val playerCaptures = data.captures.filter { it.player_id == player.id }
        for (capture in playerCaptures) {
            val avg = computeAvgRating(data.votes, player.id, capture.category_id)
            if (avg != null) total += avg
            if (firstCapturerPerCategory[capture.category_id] == player.id) total += 1.0
        }
        scores[player.id] = total
    }
    return scores
}

private fun computeAvgRating(votes: List<VoteDto>, playerId: String, categoryId: String): Double? {
    val relevant = votes.filter { it.target_player_id == playerId && it.category_id == categoryId && it.rating > 0 }
    if (relevant.isEmpty()) return null
    return relevant.sumOf { it.rating.toDouble() } / relevant.size
}
