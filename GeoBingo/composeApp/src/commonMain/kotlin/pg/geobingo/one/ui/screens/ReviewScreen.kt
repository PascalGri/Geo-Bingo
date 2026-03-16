package pg.geobingo.one.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.data.Category
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.network.CaptureDto
import pg.geobingo.one.network.GameRealtimeManager
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(gameState: GameState) {
    val scope = rememberCoroutineScope()
    val gameId = gameState.gameId ?: return
    val categories = gameState.selectedCategories
    val totalPlayers = gameState.players.size
    val myPlayerId = gameState.myPlayerId ?: return

    val categoryIndex = gameState.reviewCategoryIndex

    val realtime = remember(gameId) { GameRealtimeManager(gameId) }

    LaunchedEffect(gameId) {
        try { gameState.allCaptures = GameRepository.getCaptures(gameId) } catch (_: Exception) {}
    }

    // Realtime: react to category index advances and results phase
    LaunchedEffect(gameId) {
        realtime.gameUpdates.collect { game ->
            val newIndex = game.review_category_index
            if (newIndex != gameState.reviewCategoryIndex) {
                gameState.reviewCategoryIndex = newIndex
                gameState.hasSubmittedCurrentCategory = false
                gameState.categoryVotes = emptyMap()
            }
            if (game.status == "results") {
                try { gameState.allVotes = GameRepository.getVotes(gameId) } catch (_: Exception) {}
                gameState.currentScreen = Screen.RESULTS
            }
        }
    }

    LaunchedEffect(gameId) {
        try { realtime.subscribe() } catch (_: Exception) {}
        // Fallback poll every 10s
        while (true) {
            delay(10_000)
            try {
                val game = GameRepository.getGameById(gameId)
                val newIndex = game?.review_category_index ?: 0
                if (newIndex != gameState.reviewCategoryIndex) {
                    gameState.reviewCategoryIndex = newIndex
                    gameState.hasSubmittedCurrentCategory = false
                    gameState.categoryVotes = emptyMap()
                }
                if (game?.status == "results") {
                    gameState.allVotes = GameRepository.getVotes(gameId)
                    gameState.currentScreen = Screen.RESULTS
                }
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(gameId) {
        onDispose { scope.launch { try { realtime.unsubscribe() } catch (_: Exception) {} } }
    }

    if (categoryIndex >= categories.size) {
        Box(Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ColorPrimary)
        }
        return
    }

    val currentCategory = categories[categoryIndex]
    val capturesForCategory = gameState.allCaptures.filter { it.category_id == currentCategory.id }
    val othersCaptures = capturesForCategory.filter { it.player_id != myPlayerId }

    if (gameState.hasSubmittedCurrentCategory) {
        DarkWaitingScreen(
            categoryName = currentCategory.name,
            categoryIndex = categoryIndex,
            totalCategories = categories.size,
        )
    } else {
        DarkVotingScreen(
            gameState = gameState,
            currentCategory = currentCategory,
            categoryIndex = categoryIndex,
            totalCategories = categories.size,
            othersCaptures = othersCaptures,
            totalPlayers = totalPlayers,
            onSubmit = { votes ->
                scope.launch {
                    try {
                        GameRepository.submitCategoryVotes(
                            gameId = gameId,
                            voterId = myPlayerId,
                            categoryId = currentCategory.id,
                            votes = votes.toList(),
                        )
                        gameState.hasSubmittedCurrentCategory = true
                        gameState.categoryVotes = emptyMap()

                        val submissionCount = GameRepository.getVoteSubmissionCount(gameId, currentCategory.id)
                        if (submissionCount >= totalPlayers) {
                            val nextIndex = categoryIndex + 1
                            if (nextIndex >= categories.size) {
                                GameRepository.setGameStatus(gameId, "results")
                            } else {
                                GameRepository.setReviewCategoryIndex(gameId, nextIndex)
                                gameState.reviewCategoryIndex = nextIndex
                                gameState.hasSubmittedCurrentCategory = false
                                gameState.categoryVotes = emptyMap()
                            }
                        }
                    } catch (_: Exception) {}
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DarkVotingScreen(
    gameState: GameState,
    currentCategory: Category,
    categoryIndex: Int,
    totalCategories: Int,
    othersCaptures: List<CaptureDto>,
    totalPlayers: Int,
    onSubmit: (Map<String, Boolean>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val gameId = gameState.gameId ?: return
    var votes by remember(categoryIndex) { mutableStateOf(mapOf<String, Boolean>()) }
    var photoCache by remember { mutableStateOf(mapOf<String, ImageBitmap?>()) }

    LaunchedEffect(othersCaptures) {
        othersCaptures.forEach { capture ->
            if (capture.player_id !in photoCache) {
                scope.launch {
                    val bytes = GameRepository.downloadPhoto(gameId, capture.player_id, capture.category_id)
                    photoCache = photoCache + (capture.player_id to bytes?.toImageBitmap())
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Abstimmung",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorOnSurface,
                        )
                        Text(
                            "Kategorie ${categoryIndex + 1} / $totalCategories",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = ColorSurface) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    GradientButton(
                        text = "Abstimmung abschicken",
                        onClick = { onSubmit(votes) },
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = GradientPrimary,
                    )
                }
            }
        },
        containerColor = ColorBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Category header with gradient border
            GradientBorderCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                borderColors = GradientPrimary,
                backgroundColor = ColorPrimaryContainer,
                durationMillis = 3000,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimatedGradientText(
                        text = currentCategory.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        gradientColors = GradientPrimary,
                        durationMillis = 2000,
                    )
                    Text(
                        "Haben andere Spieler das wirklich gefunden?",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (othersCaptures.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("\uD83D\uDE05", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Niemand hat diese Kategorie fotografiert",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorOnSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                othersCaptures.forEach { capture ->
                    val player = gameState.players.find { it.id == capture.player_id }
                    val photo = photoCache[capture.player_id]
                    val isApproved = votes[capture.player_id]

                    DarkPlayerPhotoVoteCard(
                        playerName = player?.name ?: "Spieler",
                        playerColor = player?.color ?: ColorOnSurfaceVariant,
                        photo = photo,
                        isApproved = isApproved,
                        onVote = { approved ->
                            votes = votes + (capture.player_id to approved)
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DarkPlayerPhotoVoteCard(
    playerName: String,
    playerColor: Color,
    photo: ImageBitmap?,
    isApproved: Boolean?,
    onVote: (Boolean) -> Unit,
) {
    val cardBg = when (isApproved) {
        true -> ColorPrimaryContainer
        false -> ColorErrorContainer
        null -> ColorSurface
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(
            1.dp,
            when (isApproved) {
                true -> ColorPrimary.copy(alpha = 0.5f)
                false -> ColorError.copy(alpha = 0.5f)
                null -> ColorOutlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(playerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(playerName.take(1).uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    playerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorOnSurface,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ColorSurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (photo != null) {
                    Image(
                        bitmap = photo,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = ColorPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text("Foto lädt...", style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Reject button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isApproved == false)
                                Brush.linearGradient(listOf(ColorError.copy(alpha = 0.3f), ColorError.copy(alpha = 0.3f)))
                            else
                                Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
                        )
                        .border(
                            if (isApproved == false) 2.dp else 1.dp,
                            if (isApproved == false) ColorError else ColorOutline,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { onVote(false) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = ColorError, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nein", color = ColorError, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Approve button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isApproved == true)
                                Brush.linearGradient(GradientPrimary)
                            else
                                Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
                        )
                        .then(
                            if (isApproved != true)
                                Modifier.border(1.dp, ColorOutline, RoundedCornerShape(12.dp))
                            else
                                Modifier
                        )
                        .clickable { onVote(true) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isApproved == true) Color.White else ColorOnSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Ja",
                            color = if (isApproved == true) Color.White else ColorOnSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkWaitingScreen(
    categoryName: String,
    categoryIndex: Int,
    totalCategories: Int,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(ColorBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp), color = ColorPrimary)
            AnimatedGradientText(
                text = "✅ Abgestimmt!",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                gradientColors = GradientPrimary,
                durationMillis = 2000,
            )
            Text(
                "Warte auf andere Spieler...",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorOnSurfaceVariant,
            )
            Text(
                "Kategorie ${categoryIndex + 1} / $totalCategories: $categoryName",
                style = MaterialTheme.typography.bodySmall,
                color = ColorOnSurfaceVariant,
            )
        }
    }
}
