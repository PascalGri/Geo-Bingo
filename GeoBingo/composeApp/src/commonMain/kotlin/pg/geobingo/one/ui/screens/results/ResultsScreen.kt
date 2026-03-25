package pg.geobingo.one.ui.screens.results

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pg.geobingo.one.data.Category
import pg.geobingo.one.game.GameConstants
import pg.geobingo.one.game.*
import pg.geobingo.one.util.AppLogger
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.generateCode
import pg.geobingo.one.network.toHex
import pg.geobingo.one.platform.AdManager
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.platform.rememberShareManager
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*

internal fun formatRating(value: Double): String {
    val rounded = (value * 10).toInt()
    return "${rounded / 10}.${rounded % 10}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(gameState: GameState) {
    val scope = rememberCoroutineScope()
    val ranked = remember(gameState.gameplay.players, gameState.review.allVotes, gameState.review.allCaptures) {
        gameState.rankedPlayers
    }
    val winner = ranked.firstOrNull()?.first
    val shareManager = rememberShareManager()
    var showConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600)
        showConfetti = true
    }

    val modeGradient = when (gameState.session.gameMode) {
        GameMode.CLASSIC     -> GradientPrimary
        GameMode.BLIND_BINGO -> GradientCool
        GameMode.WEIRD_CORE  -> GradientWeird
        GameMode.QUICK_START -> GradientQuickStart
    }
    val modeColor = modeGradient.first()

    val anim = rememberStaggeredAnimation(count = 4, staggerDelay = 80L)
    val btnOffset = remember { Animatable(80f) }
    val btnAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            kotlinx.coroutines.delay(250L)
            launch { btnOffset.animateTo(0f, tween(450)) }
            btnAlpha.animateTo(1f, tween(450))
        }
    }

    fun Modifier.staggered(index: Int): Modifier = this.then(anim.modifier(index))

    // Load joker categories if not yet present
    LaunchedEffect(Unit) {
        if (gameState.joker.jokerMode) {
            val gid = gameState.session.gameId ?: return@LaunchedEffect
            try {
                val labels = GameRepository.getJokerLabels(gid)
                gameState.joker.jokerLabels = labels
                val jokerCats = labels.entries.map { (playerId, label) ->
                    Category(id = "joker_$playerId", name = label, emoji = "joker")
                }.filter { jokerCat -> gameState.gameplay.selectedCategories.none { it.id == jokerCat.id } }
                if (jokerCats.isNotEmpty()) gameState.gameplay.selectedCategories = gameState.gameplay.selectedCategories + jokerCats
            } catch (e: Exception) { AppLogger.w("Results", "Joker labels fetch failed", e) }
            // Refresh captures to include joker captures
            try { gameState.review.allCaptures = GameRepository.getCaptures(gid) } catch (e: Exception) { AppLogger.w("Results", "Captures fetch failed", e) }
        }
    }

    // Save to history once on entry, then cleanup server storage
    LaunchedEffect(Unit) {
        gameState.saveToHistory()
        // Save game metadata locally
        val gid = gameState.session.gameId
        if (gid != null) {
            try {
                val metaJson = buildString {
                    append("{")
                    append("\"gameCode\":\"${gameState.session.gameCode ?: ""}\",")
                    append("\"date\":\"${kotlinx.datetime.Clock.System.now()}\",")
                    append("\"jokerMode\":${gameState.joker.jokerMode},")
                    append("\"players\":[")
                    append(ranked.joinToString(",") { (p, s) -> "{\"name\":\"${p.name}\",\"id\":\"${p.id}\",\"score\":$s,\"color\":\"${p.color.toHex()}\"}" })
                    append("],")
                    append("\"categories\":[")
                    append(gameState.gameplay.selectedCategories.joinToString(",") { "{\"id\":\"${it.id}\",\"name\":\"${it.name}\"}" })
                    append("]}")
                }
                LocalPhotoStore.saveGameMeta(gid, metaJson)
            } catch (e: Exception) { AppLogger.d("Results", "Game meta save failed", e) }
        }
        // Host cleans up server storage after a delay (let other players download photos first)
        if (gameState.session.isHost && gid != null) {
            kotlinx.coroutines.delay(GameConstants.RESULTS_CLEANUP_DELAY_MS)
            try {
                GameRepository.cleanupStoragePhotos(gid, gameState.gameplay.players.map { it.id })
            } catch (e: Exception) { AppLogger.w("Results", "Storage cleanup failed", e) }
        }
    }

    SystemBackHandler { gameState.resetGame() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = "Ergebnisse",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        gradientColors = modeGradient,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = ColorSurface, modifier = Modifier.graphicsLayer { translationY = btnOffset.value; alpha = btnAlpha.value }) {
                var rematchLoading by remember { mutableStateOf(false) }
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (gameState.session.isHost) {
                        OutlinedButton(
                            onClick = {
                                if (!rematchLoading) {
                                    rematchLoading = true
                                    scope.launch {
                                        try {
                                            val myPlayer = gameState.gameplay.players.find { it.id == gameState.session.myPlayerId }
                                            val name = myPlayer?.name ?: "Host"
                                            val colorHex = myPlayer?.color?.toHex() ?: "#4CAF50"
                                            val newCode = generateCode()
                                            val newGame = GameRepository.createGame(newCode, gameState.gameplay.gameDurationMinutes * 60)
                                            val newPlayer = GameRepository.addPlayer(newGame.id, name, colorHex)
                                            GameRepository.addCategories(newGame.id, gameState.gameplay.selectedCategories)
                                            gameState.resetForRematch(newGame.id, newCode, newPlayer.id)
                                        } catch (e: Exception) {
                                            AppLogger.e("Results", "Rematch creation failed", e)
                                            rematchLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, modeColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = modeColor),
                            enabled = !rematchLoading,
                        ) {
                            if (rematchLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = modeColor)
                            } else {
                                Icon(Icons.Default.Replay, null, modifier = Modifier.size(16.dp), tint = modeColor)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Rematch (gleiche Kategorien)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = modeColor,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    GradientButton(
                        text = "Teilen",
                        onClick = {
                            val text = buildString {
                                append("KatchIt! Runde beendet\n\n")
                                ranked.take(3).forEachIndexed { index, (player, score) ->
                                    val medal = when(index) { 0 -> "#1"; 1 -> "#2"; 2 -> "#3"; else -> "" }
                                    val avg = gameState.getPlayerAverageRating(player.id)
                                    val starText = if (avg != null) " (${formatRating(avg)})" else ""
                                    append("$medal ${player.name}: $score Pkt.$starText\n")
                                }
                                append("\nZeig, was du kannst und spiele KatchIt!")
                            }
                            shareManager.shareText(text)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = modeGradient,
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        },
                    )
                    if (AdManager.isAdSupported) {
                        Spacer(Modifier.height(8.dp))
                        var rewardedAdLoading by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = {
                                if (!rewardedAdLoading) {
                                    rewardedAdLoading = true
                                    AdManager.showRewardedAd(
                                        onReward = { /* Hier spaeter Belohnung vergeben */ },
                                        onDismiss = { rewardedAdLoading = false }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, ColorOutlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorOnSurface),
                            enabled = !rewardedAdLoading,
                        ) {
                            if (rewardedAdLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ColorOnSurface)
                            } else {
                                Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Bonus ansehen", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    GradientButton(
                        text = "Neues Spiel",
                        onClick = {
                            // Interstitial Ad max 1x pro 3 Spiele
                            if (AdManager.isAdSupported) {
                                val count = AppSettings.getInt(SettingsKeys.INTERSTITIAL_GAME_COUNT, 0) + 1
                                AppSettings.setInt(SettingsKeys.INTERSTITIAL_GAME_COUNT, count)
                                if (count % 3 == 0) {
                                    AdManager.showInterstitialAd { gameState.resetGame() }
                                } else {
                                    gameState.resetGame()
                                }
                            } else {
                                gameState.resetGame()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = modeGradient,
                        leadingIcon = {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        },
                    )
                }
            }
        },
        containerColor = ColorBackground,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Winner banner
            if (winner != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggered(0)
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = Color(0xFFFBBF24),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        winner.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorOnBackground,
                    )
                    Text(
                        "gewinnt!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorOnSurfaceVariant,
                    )
                }
            }

            // Podium
            if (ranked.size >= 2) {
                Spacer(Modifier.height(20.dp))
                Box(modifier = Modifier.staggered(1)) {
                    DarkPodiumSection(ranked = ranked.take(3), playerAvatarBytes = gameState.photo.playerAvatarBytes, gameState = gameState)
                }
            }

            // Full ranking
            Column(
                modifier = Modifier.padding(16.dp).staggered(2),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Alle Ergebnisse",
                    style = MaterialTheme.typography.labelLarge,
                    color = ColorOnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                ranked.forEachIndexed { index, (player, score) ->
                    val capturedCount = gameState.review.allCaptures.count { it.player_id == player.id }
                    val speedBonus = gameState.getSpeedBonusCount(player.id)
                    val avgRating = gameState.getPlayerAverageRating(player.id)
                    DarkRankCard(
                        rank = index + 1,
                        player = player,
                        score = score,
                        capturedCount = capturedCount,
                        totalCategories = gameState.gameplay.selectedCategories.size,
                        captures = gameState.getPlayerCaptures(player.id).map { it.name },
                        isWinner = index == 0,
                        speedBonus = speedBonus,
                        averageRating = avgRating,
                        photoBytes = gameState.photo.playerAvatarBytes[player.id],
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Best Photo Highlight
            if (gameState.review.allCaptures.isNotEmpty() && gameState.review.allVotes.isNotEmpty()) {
                val gameId = gameState.session.gameId
                if (gameId != null) {
                    // Find the capture with highest average rating
                    val bestCapture = gameState.review.allCaptures.maxByOrNull { capture ->
                        gameState.getCategoryAverageRating(capture.player_id, capture.category_id) ?: 0.0
                    }
                    val bestRating = bestCapture?.let {
                        gameState.getCategoryAverageRating(it.player_id, it.category_id)
                    }
                    if (bestCapture != null && bestRating != null && bestRating >= 1.0) {
                        val bestPlayer = gameState.gameplay.players.find { it.id == bestCapture.player_id }
                        val bestCategory = gameState.gameplay.selectedCategories.find { it.id == bestCapture.category_id }
                        Column(
                            modifier = Modifier.padding(16.dp).staggered(3),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFFBBF24),
                                )
                                Spacer(Modifier.width(6.dp))
                                AnimatedGradientText(
                                    text = "Best Photo",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    gradientColors = GradientGold,
                                )
                            }
                            GradientBorderCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 14.dp,
                                borderColors = GradientGold,
                                backgroundColor = ColorSurface,
                                borderWidth = 2.dp,
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    GalleryPhotoItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        gameId = gameId,
                                        capture = bestCapture,
                                        players = gameState.gameplay.players,
                                        categories = gameState.gameplay.selectedCategories,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column {
                                            if (bestPlayer != null) {
                                                Text(
                                                    bestPlayer.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = bestPlayer.color,
                                                )
                                            }
                                            if (bestCategory != null) {
                                                Text(
                                                    bestCategory.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ColorOnSurfaceVariant,
                                                )
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp), tint = Color(0xFFFBBF24))
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                formatRating(bestRating),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFBBF24),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Photo Gallery
            if (gameState.review.allCaptures.isNotEmpty()) {
                val gameId = gameState.session.gameId
                if (gameId != null) {
                    Column(
                        modifier = Modifier.padding(16.dp).staggered(3),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Alle Fotos",
                            style = MaterialTheme.typography.labelLarge,
                            color = ColorOnSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        val rows = gameState.review.allCaptures.chunked(2)
                        rows.forEach { rowCaptures ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowCaptures.forEach { capture ->
                                    GalleryPhotoItem(
                                        modifier = Modifier.weight(1f),
                                        gameId = gameId,
                                        capture = capture,
                                        players = gameState.gameplay.players,
                                        categories = gameState.gameplay.selectedCategories,
                                    )
                                }
                                if (rowCaptures.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
        // Winner confetti overlay
        if (winner != null) {
            ConfettiEffect(trigger = showConfetti, modifier = Modifier.fillMaxSize())
        }
        } // end Box
    }
}
