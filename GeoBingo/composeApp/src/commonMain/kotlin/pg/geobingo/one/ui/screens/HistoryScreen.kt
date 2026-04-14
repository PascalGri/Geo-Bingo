package pg.geobingo.one.ui.screens

import kotlinx.datetime.toLocalDateTime
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Style
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pg.geobingo.one.data.Player
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameHistoryEntry
import pg.geobingo.one.navigation.NavArgs
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.network.parseHexColor
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.Spacing
import pg.geobingo.one.util.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    SystemBackHandler { nav.goBack() }

    // Fade-in animation for content
    val contentAlpha = remember { Animatable(0f) }
    val contentOffset = remember { Animatable(40f) }
    LaunchedEffect(Unit) {
        launch { contentOffset.animateTo(0f, tween(400)) }
        contentAlpha.animateTo(1f, tween(400))
    }

    // Track which entry is expanded (by gameCode + date for uniqueness)
    var expandedEntryKey by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.gameHistory,
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
        if (!pg.geobingo.one.network.AccountManager.isLoggedIn) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                SignInRequiredState(
                    icon = Icons.Default.History,
                    title = S.current.signInRequired,
                    description = S.current.signInRequiredDesc,
                    signInLabel = S.current.signIn,
                    onSignIn = { nav.navigateTo(pg.geobingo.one.game.Screen.ACCOUNT) },
                )
            }
        } else if (gameState.ui.gameHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).graphicsLayer { translationY = contentOffset.value; alpha = contentAlpha.value },
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Default.History,
                    title = S.current.noGamesYet,
                    subtitle = S.current.playedGamesAppearHere,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.screenHorizontal).graphicsLayer { translationY = contentOffset.value; alpha = contentAlpha.value },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                itemsIndexed(gameState.ui.gameHistory, key = { _, entry -> entry.gameCode + entry.date }) { index, entry ->
                    val entryKey = entry.gameCode + entry.date
                    val isExpanded = expandedEntryKey == entryKey
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                if (expandedEntryKey == entryKey) expandedEntryKey = null
                                gameState.ui.gameHistory = gameState.ui.gameHistory.filterIndexed { i, _ -> i != index }
                                true
                            } else false
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) ColorError.copy(alpha = 0.3f) else Color.Transparent,
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = S.current.delete, tint = ColorError)
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                    ) {
                        HistoryEntryCard(
                            entry = entry,
                            isLatest = index == 0,
                            expanded = isExpanded,
                            onToggleExpand = {
                                expandedEntryKey = if (isExpanded) null else entryKey
                            },
                            onNavigateToDetail = if (entry.gameId.isNotEmpty()) {
                                {
                                    nav.navigateTo(pg.geobingo.one.game.Screen.MATCH_DETAIL, NavArgs.MatchDetail(gameId = entry.gameId, entry = entry))
                                }
                            } else null,
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

/** A single photo loaded from local storage, associated with a player and category. */
private data class HistoryPhoto(
    val playerId: String,
    val playerName: String,
    val playerColorHex: String,
    val categoryName: String,
    val bitmap: ImageBitmap,
)

@Composable
private fun HistoryEntryCard(
    entry: GameHistoryEntry,
    isLatest: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigateToDetail: (() -> Unit)? = null,
) {
    // Load avatars from local cache
    var avatarBytes by remember { mutableStateOf(mapOf<String, ByteArray>()) }
    LaunchedEffect(entry) {
        val loaded = mutableMapOf<String, ByteArray>()
        entry.players.forEach { hp ->
            try {
                val bytes = LocalPhotoStore.loadAvatar(hp.id)
                if (bytes != null) loaded[hp.id] = bytes
            } catch (e: Exception) {
                AppLogger.w("History", "Avatar load failed: ${hp.id}", e)
            }
        }
        if (loaded.isNotEmpty()) avatarBytes = loaded
    }

    // Load photos when expanded
    var photos by remember { mutableStateOf<List<HistoryPhoto>>(emptyList()) }
    var photosLoading by remember { mutableStateOf(false) }
    var photosLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded && !photosLoaded && entry.gameId.isNotEmpty()) {
            photosLoading = true
            val loadedPhotos = mutableListOf<HistoryPhoto>()
            // Try loading photos for each player + category combination
            val categoriesToCheck = if (entry.categories.isNotEmpty()) {
                entry.categories
            } else {
                // Fallback: try to load categories from game meta
                tryLoadCategoriesFromMeta(entry.gameId)
            }
            for (cat in categoriesToCheck) {
                for (hp in entry.players) {
                    try {
                        val bytes = withContext(Dispatchers.Default) {
                            LocalPhotoStore.loadPhoto(entry.gameId, hp.id, cat.id)
                        }
                        if (bytes != null) {
                            val bitmap = withContext(Dispatchers.Default) { bytes.toImageBitmap() }
                            if (bitmap != null) {
                                loadedPhotos.add(
                                    HistoryPhoto(
                                        playerId = hp.id,
                                        playerName = hp.name,
                                        playerColorHex = hp.colorHex,
                                        categoryName = cat.name,
                                        bitmap = bitmap,
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.w("History", "Photo load failed: ${cat.id}/${hp.id}", e)
                    }
                }
            }
            photos = loadedPhotos
            photosLoading = false
            photosLoaded = true
        }
    }

    // Chevron rotation animation
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
    )

    Card(
        modifier = Modifier.fillMaxWidth().clickable { if (onNavigateToDetail != null) onNavigateToDetail() else onToggleExpand() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isLatest) ColorPrimary.copy(alpha = 0.4f) else ColorOutlineVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (entry.date.isNotBlank()) {
                        val dateText = try {
                            val instant = kotlinx.datetime.Instant.parse(entry.date)
                            val local = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                            val day = local.dayOfMonth.toString().padStart(2, '0')
                            val month = local.monthNumber.toString().padStart(2, '0')
                            val hour = local.hour.toString().padStart(2, '0')
                            val minute = local.minute.toString().padStart(2, '0')
                            "$day.$month.${local.year}  $hour:$minute"
                        } catch (e: Exception) {
                            AppLogger.w("History", "Date parse failed", e)
                            ""
                        }
                        if (dateText.isNotEmpty()) {
                            Text(
                                dateText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorOnSurface,
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            entry.gameCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorOnSurfaceVariant,
                            fontSize = 10.sp,
                        )
                        if (entry.jokerMode) {
                            Icon(Icons.Default.Style, null, modifier = Modifier.size(14.dp), tint = ColorOnSurfaceVariant)
                        }
                        Text(
                            "${entry.totalCategories} ${S.current.categories}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorOnSurfaceVariant,
                            fontSize = 10.sp,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        entry.playerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                    Text(
                        "${entry.score} ${S.current.pointsAbbrev}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimary,
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).rotate(chevronRotation),
                    tint = ColorOnSurfaceVariant,
                )
            }

            HorizontalDivider(color = ColorOutlineVariant)

            // Rankings with avatars
            entry.players.forEachIndexed { i, hp ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val rankColor = when (i) {
                        0 -> Color(0xFFFBBF24) // Gold
                        1 -> Color(0xFF94A3B8) // Silver
                        2 -> Color(0xFFCD7F32) // Bronze
                        else -> ColorOnSurfaceVariant
                    }
                    Text(
                        "${i + 1}.",
                        fontSize = 13.sp,
                        fontWeight = if (i < 3) FontWeight.Bold else FontWeight.Normal,
                        color = rankColor,
                    )
                    PlayerAvatarView(
                        player = Player(id = hp.id, name = hp.name, color = parseHexColor(hp.colorHex)),
                        size = 24.dp,
                        fontSize = 10.sp,
                        photoBytes = avatarBytes[hp.id],
                    )
                    Text(
                        hp.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (hp.name == entry.playerName) FontWeight.Bold else FontWeight.Normal,
                        color = if (hp.name == entry.playerName) ColorPrimary else ColorOnSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${hp.score} ${S.current.pointsAbbrev}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                }
            }

            // Expandable photo gallery
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = ColorOutlineVariant)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ColorPrimary,
                        )
                        Text(
                            S.current.photos,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorOnSurface,
                        )
                    }

                    when {
                        photosLoading -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ShimmerPlaceholder(
                                    modifier = Modifier.weight(1f).aspectRatio(1f),
                                    cornerRadius = 10.dp,
                                )
                                ShimmerPlaceholder(
                                    modifier = Modifier.weight(1f).aspectRatio(1f),
                                    cornerRadius = 10.dp,
                                )
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
                            // Photo grid: 2 columns
                            val rows = photos.chunked(2)
                            rows.forEach { rowPhotos ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    rowPhotos.forEach { photo ->
                                        HistoryPhotoItem(
                                            photo = photo,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    if (rowPhotos.size == 1) {
                                        Spacer(Modifier.weight(1f))
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
private fun HistoryPhotoItem(
    photo: HistoryPhoto,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.FillWidth,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        photo.playerName,
                        color = parseHexColor(photo.playerColorHex),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        photo.categoryName,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                    )
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
        // Player + category overlay at bottom
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

/**
 * Fallback: try to parse categories from the locally saved game meta JSON.
 * Returns an empty list if nothing is found or parsing fails.
 */
private fun tryLoadCategoriesFromMeta(gameId: String): List<pg.geobingo.one.game.HistoryCategory> {
    return try {
        val json = LocalPhotoStore.loadGameMeta(gameId) ?: return emptyList()
        // Minimal manual JSON parsing for "categories":[{"id":"...","name":"..."},...]
        val catStart = json.indexOf("\"categories\":[")
        if (catStart == -1) return emptyList()
        val arrayStart = json.indexOf('[', catStart)
        val arrayEnd = json.indexOf(']', arrayStart)
        if (arrayStart == -1 || arrayEnd == -1) return emptyList()
        val arrayContent = json.substring(arrayStart + 1, arrayEnd)
        val result = mutableListOf<pg.geobingo.one.game.HistoryCategory>()
        val objectPattern = Regex("""\{[^}]*"id"\s*:\s*"([^"]*)"[^}]*"name"\s*:\s*"([^"]*)"[^}]*\}""")
        for (match in objectPattern.findAll(arrayContent)) {
            result.add(pg.geobingo.one.game.HistoryCategory(id = match.groupValues[1], name = match.groupValues[2]))
        }
        result
    } catch (e: Exception) {
        AppLogger.w("History", "Category meta parse failed", e)
        emptyList()
    }
}
