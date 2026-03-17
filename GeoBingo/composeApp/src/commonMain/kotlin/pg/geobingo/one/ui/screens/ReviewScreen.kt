package pg.geobingo.one.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.network.CaptureDto
import pg.geobingo.one.network.GameRealtimeManager
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.ui.theme.*

@Composable
fun ReviewScreen(gameState: GameState) {
    val scope = rememberCoroutineScope()
    val gameId = gameState.gameId ?: return
    val categories = gameState.selectedCategories
    val myPlayerId = gameState.myPlayerId ?: return

    // Consistent player order across all clients
    val sortedPlayers = remember(gameState.players) { gameState.players.sortedBy { it.id } }
    val numPlayers = sortedPlayers.size

    val realtime = remember(gameId) { GameRealtimeManager(gameId) }

    LaunchedEffect(gameId) {
        try { gameState.allCaptures = GameRepository.getCaptures(gameId) } catch (_: Exception) {}
    }

    // Realtime: react to step advances and results phase
    LaunchedEffect(gameId) {
        realtime.gameUpdates.collect { game ->
            val newStep = game.review_category_index
            if (newStep != gameState.reviewCategoryIndex) {
                gameState.reviewCategoryIndex = newStep
                gameState.hasSubmittedCurrentCategory = false
            }
            if (game.status == "results") {
                try { gameState.allVotes = GameRepository.getVotes(gameId) } catch (_: Exception) {}
                gameState.currentScreen = Screen.RESULTS
            }
        }
    }

    // Polling fallback every 3 seconds
    LaunchedEffect(gameId) {
        try { realtime.subscribe() } catch (_: Exception) {}
        while (true) {
            delay(3_000)
            try {
                val game = GameRepository.getGameById(gameId)
                val newStep = game?.review_category_index ?: 0
                if (newStep != gameState.reviewCategoryIndex) {
                    gameState.reviewCategoryIndex = newStep
                    gameState.hasSubmittedCurrentCategory = false
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

    if (numPlayers == 0 || categories.isEmpty()) {
        Box(Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ColorPrimary)
        }
        return
    }

    val stepIndex = gameState.reviewCategoryIndex
    val totalSteps = categories.size * numPlayers

    if (stepIndex >= totalSteps) {
        Box(Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ColorPrimary)
        }
        return
    }

    val categoryIndex = stepIndex / numPlayers
    val targetPlayerIndex = stepIndex % numPlayers
    val currentCategory = categories[categoryIndex]
    val targetPlayer = sortedPlayers[targetPlayerIndex]
    // Unique key per step for vote_submissions tracking
    val stepKey = "${currentCategory.id}__${targetPlayer.id}"

    val targetCapture = gameState.allCaptures.find {
        it.player_id == targetPlayer.id && it.category_id == currentCategory.id
    }

    suspend fun advanceStep() {
        try {
            val submissionCount = GameRepository.getVoteSubmissionCount(gameId, stepKey)
            if (submissionCount >= numPlayers) {
                val nextStep = stepIndex + 1
                if (nextStep >= totalSteps) {
                    GameRepository.setGameStatus(gameId, "results")
                } else {
                    GameRepository.setReviewCategoryIndex(gameId, nextStep)
                    gameState.reviewCategoryIndex = nextStep
                    gameState.hasSubmittedCurrentCategory = false
                }
            }
        } catch (_: Exception) {}
    }

    when {
        gameState.hasSubmittedCurrentCategory -> {
            DarkWaitingScreen(
                gameId = gameId,
                stepKey = stepKey,
                categoryName = currentCategory.name,
                playerName = targetPlayer.name,
                categoryIndex = categoryIndex,
                totalCategories = categories.size,
                playerIndex = targetPlayerIndex,
                totalPlayers = numPlayers,
                isHost = gameState.isHost,
                onForceAdvance = {
                    scope.launch {
                        try {
                            val nextStep = stepIndex + 1
                            if (nextStep >= totalSteps) {
                                GameRepository.setGameStatus(gameId, "results")
                            } else {
                                GameRepository.setReviewCategoryIndex(gameId, nextStep)
                                gameState.reviewCategoryIndex = nextStep
                                gameState.hasSubmittedCurrentCategory = false
                            }
                        } catch (_: Exception) {}
                    }
                },
            )
        }
        targetCapture == null -> {
            DarkNoPhotoScreen(
                playerName = targetPlayer.name,
                playerColor = targetPlayer.color,
                categoryName = currentCategory.name,
                categoryIndex = categoryIndex,
                totalCategories = categories.size,
                stepIndex = stepIndex,
                onAutoAdvance = {
                    scope.launch {
                        try {
                            GameRepository.submitStepSubmission(gameId, myPlayerId, stepKey)
                            gameState.hasSubmittedCurrentCategory = true
                            advanceStep()
                        } catch (_: Exception) {}
                    }
                },
            )
        }
        else -> {
            DarkSinglePhotoVotingScreen(
                gameId = gameId,
                currentCategory = currentCategory,
                categoryIndex = categoryIndex,
                totalCategories = categories.size,
                targetPlayer = targetPlayer,
                targetPlayerIndex = targetPlayerIndex,
                totalPlayers = numPlayers,
                stepIndex = stepIndex,
                capture = targetCapture,
                onVote = { approved ->
                    scope.launch {
                        try {
                            GameRepository.submitStepVote(
                                gameId = gameId,
                                voterId = myPlayerId,
                                targetPlayerId = targetPlayer.id,
                                categoryId = currentCategory.id,
                                stepKey = stepKey,
                                approved = approved,
                            )
                            gameState.hasSubmittedCurrentCategory = true
                            advanceStep()
                        } catch (_: Exception) {}
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DarkSinglePhotoVotingScreen(
    gameId: String,
    currentCategory: Category,
    categoryIndex: Int,
    totalCategories: Int,
    targetPlayer: Player,
    targetPlayerIndex: Int,
    totalPlayers: Int,
    stepIndex: Int,
    capture: CaptureDto,
    onVote: (Boolean) -> Unit,
) {
    var photo by remember(stepIndex) { mutableStateOf<ImageBitmap?>(null) }
    var photoLoading by remember(stepIndex) { mutableStateOf(true) }

    LaunchedEffect(stepIndex) {
        photoLoading = true
        photo = null
        val bytes = GameRepository.downloadPhoto(gameId, capture.player_id, capture.category_id)
        photo = bytes?.toImageBitmap()
        photoLoading = false
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
                            "Kategorie ${categoryIndex + 1} / $totalCategories  •  Spieler ${targetPlayerIndex + 1} / $totalPlayers",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Category header
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
                        "Erfüllt dieses Bild die Kategorie?",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Player name row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(targetPlayer.color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        targetPlayer.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    targetPlayer.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorOnSurface,
                )
            }

            // Photo area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ColorSurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    photoLoading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.dp,
                                color = ColorPrimary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Foto lädt...",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorOnSurfaceVariant,
                            )
                        }
                    }
                    photo != null -> {
                        Image(
                            bitmap = photo!!,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = ColorOnSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Foto konnte nicht geladen werden",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorOnSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            // Vote buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onVote(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorError),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nein", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                GradientButton(
                    text = "Ja",
                    onClick = { onVote(true) },
                    modifier = Modifier.weight(1f),
                    gradientColors = GradientPrimary,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun DarkNoPhotoScreen(
    playerName: String,
    playerColor: Color,
    categoryName: String,
    categoryIndex: Int,
    totalCategories: Int,
    stepIndex: Int,
    onAutoAdvance: () -> Unit,
) {
    var countdown by remember(stepIndex) { mutableStateOf(4) }

    LaunchedEffect(stepIndex) {
        repeat(4) {
            delay(1_000)
            countdown--
        }
        onAutoAdvance()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(ColorBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                "Kategorie ${categoryIndex + 1} / $totalCategories: $categoryName",
                style = MaterialTheme.typography.bodySmall,
                color = ColorOnSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(playerColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    playerName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                playerName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ColorOnSurface,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ColorErrorContainer),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ColorError.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = ColorError,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        "Kein Bild gefunden",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorError,
                    )
                    Text(
                        "Keine Punkte für $playerName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorError.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Text(
                "Weiter in $countdown...",
                style = MaterialTheme.typography.bodySmall,
                color = ColorOnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DarkWaitingScreen(
    gameId: String,
    stepKey: String,
    categoryName: String,
    playerName: String,
    categoryIndex: Int,
    totalCategories: Int,
    playerIndex: Int,
    totalPlayers: Int,
    isHost: Boolean,
    onForceAdvance: () -> Unit,
) {
    var submittedCount by remember(stepKey) { mutableStateOf(0) }
    var waitSeconds by remember(stepKey) { mutableStateOf(0) }

    LaunchedEffect(stepKey) {
        while (true) {
            delay(2_000)
            waitSeconds += 2
            try {
                submittedCount = GameRepository.getVoteSubmissionCount(gameId, stepKey)
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(ColorBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp), color = ColorPrimary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ColorPrimary,
                    modifier = Modifier.size(28.dp),
                )
                AnimatedGradientText(
                    text = "Abgestimmt!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    gradientColors = GradientPrimary,
                    durationMillis = 2000,
                )
            }
            Text(
                "$submittedCount von $totalPlayers haben abgestimmt",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorOnSurfaceVariant,
            )
            Text(
                "Kategorie ${categoryIndex + 1} / $totalCategories: $categoryName  •  $playerName",
                style = MaterialTheme.typography.bodySmall,
                color = ColorOnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (isHost && waitSeconds >= 30) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onForceAdvance,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorOnSurfaceVariant),
                    border = BorderStroke(1.dp, ColorOutlineVariant),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        "Nächstes Bild (Spieler überspringen)",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurfaceVariant,
                    )
                }
            }
        }
    }
}
