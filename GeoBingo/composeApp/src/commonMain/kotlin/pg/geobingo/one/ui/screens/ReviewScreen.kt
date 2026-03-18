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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.VoteKeys
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.PlayerAvatarView

@Composable
fun ReviewScreen(gameState: GameState) {
    val scope = rememberCoroutineScope()
    val gameId = gameState.gameId ?: return
    val categories = gameState.selectedCategories
    val myPlayerId = gameState.myPlayerId ?: return

    val sortedPlayers = remember(gameState.players) { gameState.players.sortedBy { it.id } }
    val numPlayers = sortedPlayers.size

    val realtime = gameState.realtime

    // Download avatar photos for all players not yet cached or tried
    LaunchedEffect(gameState.players) {
        gameState.players
            .filter { it.id !in gameState.playerAvatarBytes && it.id !in gameState.triedAvatarDownloads }
            .forEach { player ->
                scope.launch {
                    gameState.triedAvatarDownloads = gameState.triedAvatarDownloads + player.id
                    val bytes = GameRepository.downloadAvatarPhoto(player.id)
                    if (bytes != null) {
                        gameState.playerAvatarBytes = gameState.playerAvatarBytes + (player.id to bytes)
                    }
                }
            }
    }

    LaunchedEffect(gameId) {
        if (gameState.jokerMode) {
            try {
                val labels = GameRepository.getJokerLabels(gameId)
                gameState.jokerLabels = labels
                val jokerCats = labels.entries.map { (playerId, label) ->
                    Category(id = "joker_$playerId", name = "🃏 $label", emoji = "joker")
                }.filter { jokerCat -> gameState.selectedCategories.none { it.id == jokerCat.id } }
                if (jokerCats.isNotEmpty()) gameState.selectedCategories = gameState.selectedCategories + jokerCats
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    LaunchedEffect(gameState.reviewCategoryIndex) {
        try { gameState.allCaptures = GameRepository.getCaptures(gameId) } catch (e: Exception) { e.printStackTrace() }
    }

    LaunchedEffect(gameId) {
        if (realtime == null) return@LaunchedEffect
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

    // Realtime: detect vote submissions to trigger faster advancement
    LaunchedEffect(gameId) {
        if (realtime == null) return@LaunchedEffect
        realtime.voteSubmissionInserts.collect { _ ->
            // When any vote submission arrives, check if we can advance
            if (gameState.hasSubmittedCurrentCategory) {
                val currentStepIndex = gameState.reviewCategoryIndex
                val currentCatIndex = currentStepIndex / numPlayers
                val currentPlayerIdx = currentStepIndex % numPlayers
                if (currentCatIndex < categories.size && currentPlayerIdx < sortedPlayers.size) {
                    val currentStepKey = VoteKeys.stepKey(categories[currentCatIndex].id, sortedPlayers[currentPlayerIdx].id)
                    try {
                        val count = GameRepository.getVoteSubmissionCount(gameId, currentStepKey)
                        if (count >= numPlayers) {
                            val nextStep = currentStepIndex + 1
                            val totalSteps = categories.size * numPlayers
                            if (nextStep >= totalSteps) {
                                for (attempt in 0 until 3) {
                                    try {
                                        if (attempt > 0) delay(1_000L * attempt)
                                        GameRepository.setGameStatus(gameId, "results")
                                        break
                                    } catch (_: Exception) {}
                                }
                                try { gameState.allVotes = GameRepository.getVotes(gameId) } catch (_: Exception) {}
                                gameState.currentScreen = Screen.RESULTS
                            } else {
                                for (attempt in 0 until 3) {
                                    try {
                                        if (attempt > 0) delay(1_000L * attempt)
                                        GameRepository.setReviewCategoryIndex(gameId, nextStep)
                                        break
                                    } catch (_: Exception) {}
                                }
                                gameState.reviewCategoryIndex = nextStep
                                gameState.hasSubmittedCurrentCategory = false
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    LaunchedEffect(gameId) {
        while (true) {
            delay(3_000)
            try {
                val game = GameRepository.getGameById(gameId)
                val newStep = game?.review_category_index ?: 0
                if (newStep != gameState.reviewCategoryIndex) {
                    gameState.reviewCategoryIndex = newStep
                    gameState.hasSubmittedCurrentCategory = false
                }
                if (game?.status == "results" && gameState.currentScreen == Screen.REVIEW) {
                    gameState.allVotes = GameRepository.getVotes(gameId)
                    gameState.currentScreen = Screen.RESULTS
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    if (numPlayers == 0 || categories.isEmpty()) {
        Box(Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ColorPrimary) }
        return
    }

    val stepIndex = gameState.reviewCategoryIndex
    val totalSteps = categories.size * numPlayers

    if (stepIndex >= totalSteps) {
        Box(Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ColorPrimary) }
        return
    }

    val categoryIndex = stepIndex / numPlayers
    val targetPlayerIndex = stepIndex % numPlayers
    val currentCategory = categories[categoryIndex]
    val targetPlayer = sortedPlayers[targetPlayerIndex]
    val stepKey = VoteKeys.stepKey(currentCategory.id, targetPlayer.id)

    suspend fun advanceStep() {
        try {
            val submissionCount = GameRepository.getVoteSubmissionCount(gameId, stepKey)
            if (submissionCount >= numPlayers) {
                val nextStep = stepIndex + 1
                if (nextStep >= totalSteps) {
                    for (attempt in 0 until 3) {
                        try {
                            if (attempt > 0) delay(1_000L * attempt)
                            GameRepository.setGameStatus(gameId, "results")
                            break
                        } catch (_: Exception) {}
                    }
                    try { gameState.allVotes = GameRepository.getVotes(gameId) } catch (_: Exception) {}
                    gameState.currentScreen = Screen.RESULTS
                } else {
                    var serverUpdated = false
                    for (attempt in 0 until 3) {
                        try {
                            if (attempt > 0) delay(1_000L * attempt)
                            GameRepository.setReviewCategoryIndex(gameId, nextStep)
                            serverUpdated = true
                            break
                        } catch (_: Exception) {}
                    }
                    if (serverUpdated) {
                        gameState.reviewCategoryIndex = nextStep
                        gameState.hasSubmittedCurrentCategory = false
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    key(stepIndex) {
        if (gameState.hasSubmittedCurrentCategory) {
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
                onReadyToAdvance = { scope.launch { advanceStep() } },
                onForceAdvance = {
                    scope.launch {
                        try {
                            val nextStep = stepIndex + 1
                            if (nextStep >= totalSteps) GameRepository.setGameStatus(gameId, "results")
                            else GameRepository.setReviewCategoryIndex(gameId, nextStep)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                },
            )
        } else {
            DarkSinglePhotoVotingScreen(
                gameId = gameId,
                currentCategory = currentCategory,
                categoryIndex = categoryIndex,
                totalCategories = categories.size,
                targetPlayer = targetPlayer,
                targetPlayerIndex = targetPlayerIndex,
                totalPlayers = numPlayers,
                stepIndex = stepIndex,
                playerAvatarBytes = gameState.playerAvatarBytes[targetPlayer.id],
                onVote = { approved ->
                    scope.launch {
                        gameState.hasSubmittedCurrentCategory = true
                        var attempt = 0
                        while (attempt < 3) {
                            try {
                                if (attempt > 0) delay(1_000L * attempt)
                                GameRepository.submitStepVote(gameId, myPlayerId, targetPlayer.id, currentCategory.id, stepKey, approved)
                                break
                            } catch (e: Exception) {
                                e.printStackTrace()
                                attempt++
                            }
                        }
                        // Always try to advance, even if vote submission had issues
                        advanceStep()
                    }
                },
                onNoPhoto = {
                    scope.launch {
                        gameState.hasSubmittedCurrentCategory = true
                        var attempt = 0
                        while (attempt < 3) {
                            try {
                                if (attempt > 0) delay(1_000L * attempt)
                                GameRepository.submitStepSubmission(gameId, myPlayerId, stepKey)
                                break
                            } catch (e: Exception) {
                                e.printStackTrace()
                                attempt++
                            }
                        }
                        advanceStep()
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DarkSinglePhotoVotingScreen(
    gameId: String, currentCategory: Category, categoryIndex: Int, totalCategories: Int,
    targetPlayer: Player, targetPlayerIndex: Int, totalPlayers: Int, stepIndex: Int,
    playerAvatarBytes: ByteArray? = null,
    onVote: (Boolean) -> Unit, onNoPhoto: () -> Unit
) {
    var photo by remember(stepIndex) { mutableStateOf<ImageBitmap?>(null) }
    var photoLoading by remember(stepIndex) { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(stepIndex) {
        photoLoading = true
        val bytes = GameRepository.downloadPhoto(gameId, targetPlayer.id, currentCategory.id)
        if (bytes != null) {
            try { LocalPhotoStore.savePhoto(gameId, targetPlayer.id, currentCategory.id, bytes) } catch (_: Exception) {}
        }
        photo = bytes?.toImageBitmap()
        photoLoading = false
        if (photo == null) { delay(2500); onNoPhoto() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Abstimmung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ColorOnSurface)
                        Text("Kategorie ${categoryIndex + 1}/$totalCategories • Spieler ${targetPlayerIndex + 1}/$totalPlayers", style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientBorderCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, borderColors = GradientPrimary, backgroundColor = ColorPrimaryContainer) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedGradientText(text = currentCategory.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), gradientColors = GradientPrimary)
                    Text("Erfüllt dieses Bild die Kategorie?", style = MaterialTheme.typography.bodySmall, color = ColorOnPrimaryContainer.copy(alpha = 0.7f))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerAvatarView(player = targetPlayer, size = 36.dp, fontSize = 14.sp, photoBytes = playerAvatarBytes)
                Spacer(Modifier.width(8.dp))
                Text(targetPlayer.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = ColorOnSurface)
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(ColorSurfaceVariant), contentAlignment = Alignment.Center) {
                if (photoLoading) CircularProgressIndicator(color = ColorPrimary)
                else if (photo != null) Image(bitmap = photo!!, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(40.dp), tint = ColorOnSurfaceVariant)
                    Text("Kein Foto gefunden", style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onVote(false) }, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorError), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Close, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nein", fontWeight = FontWeight.Bold)
                }
                GradientButton(text = "Ja", onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onVote(true) }, modifier = Modifier.weight(1f), gradientColors = GradientPrimary, leadingIcon = { Icon(Icons.Default.Check, null, tint = Color.White) })
            }
        }
    }
}

@Composable
private fun DarkWaitingScreen(gameId: String, stepKey: String, categoryName: String, playerName: String, categoryIndex: Int, totalCategories: Int, playerIndex: Int, totalPlayers: Int, isHost: Boolean, onReadyToAdvance: () -> Unit, onForceAdvance: () -> Unit) {
    var submittedCount by remember(stepKey) { mutableStateOf(0) }
    var advanceCalled by remember(stepKey) { mutableStateOf(false) }
    LaunchedEffect(stepKey) {
        // Keep polling until the step actually changes (composable is destroyed)
        while (true) {
            try {
                submittedCount = GameRepository.getVoteSubmissionCount(gameId, stepKey)
                if (submittedCount >= totalPlayers && !advanceCalled) {
                    advanceCalled = true
                    onReadyToAdvance()
                }
            } catch (e: Exception) { e.printStackTrace() }
            delay(1_500)
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = ColorPrimary)
            AnimatedGradientText(text = "Abgestimmt!", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), gradientColors = GradientPrimary)
            Text("$submittedCount von $totalPlayers haben abgestimmt", style = MaterialTheme.typography.bodyMedium, color = ColorOnSurfaceVariant)
            if (isHost) {
                OutlinedButton(onClick = onForceAdvance, modifier = Modifier.padding(top = 16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorOnSurfaceVariant), border = BorderStroke(1.dp, ColorOutlineVariant), shape = RoundedCornerShape(20.dp)) {
                    Text("Überspringen (Host-Option)", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
