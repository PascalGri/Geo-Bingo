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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.network.GameRealtimeManager
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.VoteKeys
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

    val realtime = remember(gameId) { GameRealtimeManager(gameId, "review") }

    // On first load: fetch joker labels and append virtual joker categories
    LaunchedEffect(gameId) {
        if (gameState.jokerMode) {
            try {
                val labels = GameRepository.getJokerLabels(gameId)
                gameState.jokerLabels = labels
                val jokerCats = labels.entries.map { (playerId, label) ->
                    Category(
                        id = "joker_$playerId",
                        name = "🃏 $label",
                        emoji = "joker",
                    )
                }.filter { jokerCat -> gameState.selectedCategories.none { it.id == jokerCat.id } }
                if (jokerCats.isNotEmpty()) {
                    gameState.selectedCategories = gameState.selectedCategories + jokerCats
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // Re-fetch captures on each step change (used for scoring in ResultsScreen)
    LaunchedEffect(gameState.reviewCategoryIndex) {
        try { gameState.allCaptures = GameRepository.getCaptures(gameId) } catch (e: Exception) { e.printStackTrace() }
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
                try { gameState.allVotes = GameRepository.getVotes(gameId) } catch (e: Exception) { e.printStackTrace() }
                gameState.currentScreen = Screen.RESULTS
            }
        }
    }

    // Polling fallback every 3 seconds
    LaunchedEffect(gameId) {
        try { realtime.subscribe() } catch (e: Exception) { e.printStackTrace() }
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
                gameState.consecutiveNetworkErrors = 0
            } catch (e: Exception) {
                e.printStackTrace()
                gameState.consecutiveNetworkErrors++
            }
        }
    }

    DisposableEffect(gameId) {
        onDispose {
            val cleanupScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
            cleanupScope.launch {
                try { realtime.unsubscribe() } catch (e: Exception) { e.printStackTrace() }
                cleanupScope.cancel()
            }
        }
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
    val stepKey = VoteKeys.stepKey(currentCategory.id, targetPlayer.id)

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
        } catch (e: Exception) { e.printStackTrace() }
    }

    val noPhotoAction: () -> Unit = {
        scope.launch {
            gameState.hasSubmittedCurrentCategory = true
            var submitted = false
            repeat(2) {
                if (!submitted) {
                    try {
                        GameRepository.submitStepSubmission(gameId, myPlayerId, stepKey)
                        submitted = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        delay(1_500)
                    }
                }
            }
            advanceStep()
        }
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
                isOffline = gameState.consecutiveNetworkErrors >= 3,
                onReadyToAdvance = { scope.launch { advanceStep() } },
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
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                },
            )
        }
        else -> {
            // Always attempt to load from storage — bypasses captures-table RLS issues
            DarkSinglePhotoVotingScreen(
                gameId = gameId,
                currentCategory = currentCategory,
                categoryIndex = categoryIndex,
                totalCategories = categories.size,
                targetPlayer = targetPlayer,
                targetPlayerIndex = targetPlayerIndex,
                totalPlayers = numPlayers,
                stepIndex = stepIndex,
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
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                },
                onNoPhoto = noPhotoAction,
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
    onVote: (Boolean) -> Unit,
    onNoPhoto: () -> Unit,
) {
    var photo by remember(stepIndex) { mutableStateOf<ImageBitmap?>(null) }
    var photoLoading by remember(stepIndex) { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(stepIndex) {
        photoLoading = true
        photo = null
        val bytes = GameRepository.downloadPhoto(gameId, targetPlayer.id, currentCategory.id)
        photo = bytes?.toImageBitmap()
        photoLoading = false
        if (photo == null) {
            // No photo in storage → auto-advance after brief display
            delay(3_000)
            onNoPhoto()
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
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onVote(false) 
                    },
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
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onVote(true) 
                    },
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
    isOffline: Boolean = false,
    onReadyToAdvance: () -> Unit,
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
                if (submittedCount >= totalPlayers) {
                    onReadyToAdvance()
                    break
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ColorBackground)) {
        if (isOffline) {
            Surface(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = ColorError.copy(alpha = 0.15f),
            ) {
                Text(
                    "Keine Verbindung – versuche erneut zu verbinden…",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorError,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
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
