package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.*
import pg.geobingo.one.network.CaptureDto
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.generateCode
import pg.geobingo.one.network.toHex
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.saveImageToDevice
import pg.geobingo.one.platform.rememberShareManager
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.PlayerAvatarView
import pg.geobingo.one.ui.theme.ConfettiEffect
import pg.geobingo.one.ui.theme.ShimmerPlaceholder
import pg.geobingo.one.ui.theme.Spacing
import pg.geobingo.one.ui.theme.rememberStaggeredAnimation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(gameState: GameState) {
    val scope = rememberCoroutineScope()
    val ranked = gameState.getRankedPlayers()
    val winner = ranked.firstOrNull()?.first
    val shareManager = rememberShareManager()
    var showConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600)
        showConfetti = true
    }

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
        if (gameState.jokerMode) {
            val gid = gameState.gameId ?: return@LaunchedEffect
            try {
                val labels = GameRepository.getJokerLabels(gid)
                gameState.jokerLabels = labels
                val jokerCats = labels.entries.map { (playerId, label) ->
                    Category(id = "joker_$playerId", name = label, emoji = "joker")
                }.filter { jokerCat -> gameState.selectedCategories.none { it.id == jokerCat.id } }
                if (jokerCats.isNotEmpty()) gameState.selectedCategories = gameState.selectedCategories + jokerCats
            } catch (_: Exception) {}
            // Refresh captures to include joker captures
            try { gameState.allCaptures = GameRepository.getCaptures(gid) } catch (_: Exception) {}
        }
    }

    // Save to history once on entry, then cleanup server storage
    LaunchedEffect(Unit) {
        gameState.saveToHistory()
        // Save game metadata locally
        val gid = gameState.gameId
        if (gid != null) {
            try {
                val metaJson = buildString {
                    append("{")
                    append("\"gameCode\":\"${gameState.gameCode ?: ""}\",")
                    append("\"date\":\"${kotlinx.datetime.Clock.System.now()}\",")
                    append("\"jokerMode\":${gameState.jokerMode},")
                    append("\"players\":[")
                    append(ranked.joinToString(",") { (p, s) -> "{\"name\":\"${p.name}\",\"id\":\"${p.id}\",\"score\":$s,\"color\":\"${p.color.toHex()}\"}" })
                    append("],")
                    append("\"categories\":[")
                    append(gameState.selectedCategories.joinToString(",") { "{\"id\":\"${it.id}\",\"name\":\"${it.name}\"}" })
                    append("]}")
                }
                LocalPhotoStore.saveGameMeta(gid, metaJson)
            } catch (_: Exception) {}
        }
        // Host cleans up server storage after a delay (let other players download photos first)
        if (gameState.isHost && gid != null) {
            kotlinx.coroutines.delay(10_000)
            try {
                GameRepository.cleanupStoragePhotos(gid, gameState.players.map { it.id })
            } catch (_: Exception) {}
        }
    }

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

    SystemBackHandler { gameState.resetGame() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = "Ergebnisse",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        gradientColors = GradientPrimary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = ColorSurface, modifier = Modifier.graphicsLayer { translationY = btnOffset.value; alpha = btnAlpha.value }) {
                var rematchLoading by remember { mutableStateOf(false) }
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (gameState.isHost) {
                        OutlinedButton(
                            onClick = {
                                if (!rematchLoading) {
                                    rematchLoading = true
                                    scope.launch {
                                        try {
                                            val myPlayer = gameState.players.find { it.id == gameState.myPlayerId }
                                            val name = myPlayer?.name ?: "Host"
                                            val colorHex = myPlayer?.color?.toHex() ?: "#4CAF50"
                                            val newCode = generateCode()
                                            val newGame = GameRepository.createGame(newCode, gameState.gameDurationMinutes * 60)
                                            val newPlayer = GameRepository.addPlayer(newGame.id, name, colorHex)
                                            GameRepository.addCategories(newGame.id, gameState.selectedCategories)
                                            gameState.resetForRematch(newGame.id, newCode, newPlayer.id)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            rematchLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, ColorPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorPrimary),
                            enabled = !rematchLoading,
                        ) {
                            if (rematchLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ColorPrimary)
                            } else {
                                Icon(Icons.Default.Replay, null, modifier = Modifier.size(16.dp), tint = ColorPrimary)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Rematch (gleiche Kategorien)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorPrimary,
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
                                    append("$medal ${player.name}: $score Pkt.\n")
                                }
                                append("\nZeig, was du kannst und spiele KatchIt!")
                            }
                            shareManager.shareText(text)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = GradientCool,
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    GradientButton(
                        text = "Neues Spiel",
                        onClick = { gameState.resetGame() },
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = GradientPrimary,
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
                    DarkPodiumSection(ranked = ranked.take(3), playerAvatarBytes = gameState.playerAvatarBytes)
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
                    val capturedCount = gameState.allCaptures.count { it.player_id == player.id }
                    val speedBonus = gameState.getSpeedBonusCount(player.id)
                    DarkRankCard(
                        rank = index + 1,
                        player = player,
                        score = score,
                        capturedCount = capturedCount,
                        totalCategories = gameState.selectedCategories.size,
                        captures = gameState.getPlayerCaptures(player.id).map { it.name },
                        isWinner = index == 0,
                        speedBonus = speedBonus,
                        photoBytes = gameState.playerAvatarBytes[player.id],
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Photo Gallery
            if (gameState.allCaptures.isNotEmpty()) {
                val gameId = gameState.gameId
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
                        val rows = gameState.allCaptures.chunked(2)
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
                                        players = gameState.players,
                                        categories = gameState.selectedCategories,
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

@Composable
private fun DarkPodiumSection(ranked: List<Pair<Player, Int>>, playerAvatarBytes: Map<String, ByteArray>) {
    val heights = listOf(100.dp, 72.dp, 56.dp)
    val podiumOrder = when (ranked.size) {
        1 -> listOf(ranked[0] to 0)
        2 -> listOf(ranked[1] to 1, ranked[0] to 0)
        else -> listOf(ranked[1] to 1, ranked[0] to 0, ranked[2] to 2)
    }

    // Podium grow animations
    val podiumHeights = podiumOrder.map { (_, rank) ->
        val anim = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(rank * 200L + 300L)
            anim.animateTo(1f, tween(600))
        }
        anim
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        podiumOrder.forEachIndexed { i, (playerScore, rank) ->
            val (player, score) = playerScore
            val rankColor = when (rank) {
                0 -> Color(0xFFFBBF24).copy(alpha = 0.7f)
                1 -> Color(0xFF94A3B8).copy(alpha = 0.5f)
                else -> Color(0xFFCD7F32).copy(alpha = 0.5f)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = when (rank) { 0 -> "1." ; 1 -> "2."; else -> "3." },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = rankColor,
                )
                Spacer(Modifier.height(4.dp))
                PlayerAvatarView(player = player, size = 40.dp, fontSize = 16.sp, photoBytes = playerAvatarBytes[player.id])
                Spacer(Modifier.height(4.dp))
                Text(
                    player.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = ColorOnBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "$score Pkt.",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = player.color,
                )
                Spacer(Modifier.height(4.dp))
                // Animated podium bar growing from bottom
                val targetHeight = heights[rank]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(targetHeight * podiumHeights[i].value)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(ColorSurfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun DarkRankCard(
    rank: Int,
    player: Player,
    score: Int,
    capturedCount: Int,
    totalCategories: Int,
    captures: List<String>,
    isWinner: Boolean,
    speedBonus: Int = 0,
    photoBytes: ByteArray? = null,
) {
    val cardBg = if (isWinner) ColorPrimaryContainer else ColorSurface
    val borderColor = if (isWinner) ColorPrimary.copy(alpha = 0.5f) else ColorOutlineVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(if (isWinner) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Rank number
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> Color(0xFF3A3010) // Muted gold bg
                            2 -> Color(0xFF2A2E34) // Muted silver bg
                            3 -> Color(0xFF2E2218) // Muted bronze bg
                            else -> ColorSurfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (rank) {
                        1 -> Color(0xFFFBBF24)
                        2 -> Color(0xFF94A3B8)
                        3 -> Color(0xFFCD7F32)
                        else -> ColorOnSurfaceVariant
                    },
                )
            }

            Spacer(Modifier.width(10.dp))

            // Avatar
            PlayerAvatarView(player = player, size = 38.dp, fontSize = 15.sp, photoBytes = photoBytes)

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    player.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorOnSurface,
                )
                if (captures.isNotEmpty()) {
                    Text(
                        captures.take(3).joinToString(", ") + if (captures.size > 3) " …" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            // Score
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$score",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = player.color,
                )
                Text(
                    "Pkt.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                )
                if (capturedCount > score) {
                    Text(
                        "$capturedCount gefunden",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                if (speedBonus > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, null, modifier = Modifier.size(12.dp), tint = Color(0xFFFBBF24))
                        Text(
                            " +$speedBonus schnell",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFBBF24),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryPhotoItem(
    modifier: Modifier = Modifier,
    gameId: String,
    capture: CaptureDto,
    players: List<Player>,
    categories: List<Category>,
) {
    var photo by remember(capture.id) { mutableStateOf<ImageBitmap?>(null) }
    var photoBytes by remember(capture.id) { mutableStateOf<ByteArray?>(null) }
    var loading by remember(capture.id) { mutableStateOf(true) }
    var showFullscreen by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(capture.id) {
        loading = true
        // Try local first, then server
        var bytes = LocalPhotoStore.loadPhoto(gameId, capture.player_id, capture.category_id)
        if (bytes == null) {
            bytes = GameRepository.downloadPhoto(gameId, capture.player_id, capture.category_id)
            if (bytes != null) {
                try { LocalPhotoStore.savePhoto(gameId, capture.player_id, capture.category_id, bytes) } catch (_: Exception) {}
            }
        }
        photoBytes = bytes
        photo = bytes?.toImageBitmap()
        loading = false
    }

    val player = players.find { it.id == capture.player_id }
    val category = categories.find { it.id == capture.category_id }

    if (showFullscreen && photo != null) {
        AlertDialog(
            onDismissRequest = { showFullscreen = false },
            containerColor = Color(0xFF0A0A0A),
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = photo!!,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.FillWidth,
                    )
                    if (player != null || category != null) {
                        Spacer(Modifier.height(8.dp))
                        if (player != null) {
                            Text(player.name, color = player.color, fontWeight = FontWeight.Bold)
                        }
                        if (category != null) {
                            Text(category.name, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    if (photoBytes != null) {
                        TextButton(onClick = {
                            scope.launch {
                                val filename = "${player?.name ?: "foto"}_${category?.name ?: ""}.jpg"
                                val ok = saveImageToDevice(photoBytes!!, filename)
                                saveSuccess = ok
                            }
                        }) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (saveSuccess == true) "Gespeichert" else "Speichern")
                        }
                    }
                    TextButton(onClick = { showFullscreen = false }) { Text("Schließen") }
                }
            },
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(ColorSurface)
            .clickable { if (photo != null) showFullscreen = true },
    ) {
        when {
            loading -> {
                ShimmerPlaceholder(modifier = Modifier.fillMaxSize(), cornerRadius = 10.dp)
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = ColorOnSurfaceVariant,
                    )
                }
            }
        }

        // Player + category overlay at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 5.dp, vertical = 3.dp),
        ) {
            Column {
                if (player != null) {
                    Text(
                        player.name,
                        color = player.color,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                if (category != null) {
                    Text(
                        category.name,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
