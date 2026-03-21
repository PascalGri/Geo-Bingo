package pg.geobingo.one.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.data.getCategoryIcon
import pg.geobingo.one.data.getCategoryIconRotation
import pg.geobingo.one.data.getRandomReplacementCategory
import pg.geobingo.one.game.*
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.VoteKeys
import pg.geobingo.one.network.toCategory
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.RequestLocationPermission
import pg.geobingo.one.platform.getCurrentLocation
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.PlayerAvatarView
import pg.geobingo.one.ui.theme.Spacing

@Composable
fun GameScreen(gameState: GameState) {
    // Request location permission once when entering the game
    RequestLocationPermission()

    val scope = rememberCoroutineScope()
    val gameId = gameState.gameId
    val realtime = gameId?.let { gameState.realtime }

    var photoTargetPlayerId by remember { mutableStateOf("") }
    var photoTargetCategoryId by remember { mutableStateOf("") }
    var jokerDialogVisible by remember { mutableStateOf(false) }
    var jokerLabelInput by remember { mutableStateOf("") }
    var uploadSuccessCategory by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    // ── Mode-specific state ─────────────────────────────────────────────────
    // Kategorie-Tausch
    var swapDialogCategory by remember { mutableStateOf<Category?>(null) }

    // Sabotage
    var sabotageTargetPickerVisible by remember { mutableStateOf(false) }
    var sabotageSelectedTarget by remember { mutableStateOf<Player?>(null) }
    var sabotageCategoryPickerVisible by remember { mutableStateOf(false) }
    var sabotageNotification by remember { mutableStateOf<String?>(null) }

    // Elimination
    var eliminationRoundEndDialogVisible by remember { mutableStateOf(false) }

    val photoCapturer = rememberPhotoCapturer { bytes ->
        if (bytes != null) {
            gameState.addPhoto(photoTargetPlayerId, photoTargetCategoryId, bytes)
            val pid = photoTargetPlayerId
            val cid = photoTargetCategoryId
            val isJoker = cid.startsWith("joker_")

            // Save locally for game history
            if (gameId != null) {
                try { LocalPhotoStore.savePhoto(gameId, pid, cid, bytes) } catch (_: Exception) {}
            }

            gameState.uploadingCategories = gameState.uploadingCategories + cid

            if (gameId != null) {
                scope.launch {
                    // Get GPS location (best-effort, don't block on failure)
                    val location = try { getCurrentLocation() } catch (_: Exception) { null }

                    // Upload capture with retries
                    var attempt = 0
                    var captureSuccess = false
                    while (attempt < 3 && !captureSuccess) {
                        try {
                            if (attempt > 0) delay(2_000L * attempt)
                            GameRepository.recordCapture(gameId, pid, cid, bytes, latitude = location?.latitude, longitude = location?.longitude)
                            captureSuccess = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            attempt++
                        }
                    }
                    // Save joker label separately (so capture retry doesn't block it)
                    if (isJoker) {
                        var jokerAttempt = 0
                        while (jokerAttempt < 3) {
                            try {
                                if (jokerAttempt > 0) delay(1_000L * jokerAttempt)
                                GameRepository.setJokerLabel(gameId, pid, jokerLabelInput.trim())
                                break
                            } catch (e: Exception) {
                                e.printStackTrace()
                                jokerAttempt++
                            }
                        }
                    }
                    if (captureSuccess) {
                        if (gameState.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        uploadSuccessCategory = cid
                        delay(1500)
                        uploadSuccessCategory = null
                    }
                    gameState.uploadingCategories = gameState.uploadingCategories - cid
                }
            } else {
                gameState.uploadingCategories = gameState.uploadingCategories - cid
            }
            if (isJoker) gameState.myJokerUsed = true
        }
    }

    if (jokerDialogVisible) {
        AlertDialog(
            onDismissRequest = { jokerDialogVisible = false },
            containerColor = ColorSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Style, null, modifier = Modifier.size(20.dp), tint = ColorPrimary)
                    Text(
                        "Joker verwenden",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorOnSurface,
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Gib ein Thema für dein Joker-Foto ein:",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = jokerLabelInput,
                        onValueChange = { if (it.length <= 40) jokerLabelInput = it },
                        placeholder = { Text("z.B. Rote Tür", color = ColorOnSurfaceVariant) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimary,
                            unfocusedBorderColor = ColorOutline,
                            focusedTextColor = ColorOnSurface,
                            unfocusedTextColor = ColorOnSurface,
                            cursorColor = ColorPrimary,
                        ),
                    )
                }
            },
            confirmButton = {
                GradientButton(
                    text = "Foto machen",
                    onClick = {
                        jokerDialogVisible = false
                        val myId = gameState.myPlayerId ?: return@GradientButton
                        photoTargetPlayerId = myId
                        photoTargetCategoryId = "joker_$myId"
                        val label = jokerLabelInput.trim().ifEmpty { "Joker" }
                        jokerLabelInput = label
                        val existingJoker = gameState.selectedCategories.find { it.id == "joker_$myId" }
                        if (existingJoker == null) {
                            gameState.selectedCategories = gameState.selectedCategories + Category(
                                id = "joker_$myId",
                                name = label,
                                emoji = "joker",
                            )
                        }
                        photoCapturer.launch()
                    },
                    enabled = jokerLabelInput.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            dismissButton = {
                TextButton(onClick = { jokerDialogVisible = false }) {
                    Text("Abbrechen")
                }
            },
        )
    }

    // ── Kategorie-Tausch Dialog ────────────────────────────────────────────
    if (swapDialogCategory != null && gameState.gameMode == "kategorie_tausch") {
        val catToSwap = swapDialogCategory!!
        AlertDialog(
            onDismissRequest = { swapDialogCategory = null },
            containerColor = ColorSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(20.dp), tint = Color(0xFF3B82F6))
                    Text("Kategorie tauschen?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorOnSurface)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "\"${catToSwap.name}\" gegen eine zufällige neue Kategorie tauschen?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorOnSurfaceVariant,
                    )
                    Text(
                        "Du hast nur 1 Tausch pro Spiel!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = ColorError,
                    )
                }
            },
            confirmButton = {
                GradientButton(
                    text = "Tauschen",
                    onClick = {
                        val cat = swapDialogCategory ?: return@GradientButton
                        swapDialogCategory = null
                        gameState.mySwapUsed = true
                        val existingIds = gameState.selectedCategories.map { it.emoji }.toSet()
                        val newCat = getRandomReplacementCategory(existingIds)
                        // Replace locally
                        gameState.selectedCategories = gameState.selectedCategories.map {
                            if (it.id == cat.id) newCat else it
                        }
                        // Record on server
                        if (gameId != null) {
                            val pid = gameState.myPlayerId ?: return@GradientButton
                            scope.launch {
                                try {
                                    GameRepository.recordCategorySwap(
                                        gameId, pid, cat.id, newCat.id, newCat.name, newCat.emoji
                                    )
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            dismissButton = {
                TextButton(onClick = { swapDialogCategory = null }) { Text("Abbrechen") }
            },
        )
    }

    // ── Sabotage: Target-Picker Dialog ──────────────────────────────────────
    if (sabotageTargetPickerVisible && gameState.gameMode == "sabotage") {
        val myId = gameState.myPlayerId
        val targets = gameState.players.filter { it.id != myId }
        AlertDialog(
            onDismissRequest = { sabotageTargetPickerVisible = false },
            containerColor = ColorSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Block, null, modifier = Modifier.size(20.dp), tint = Color(0xFFEF4444))
                    Text("Sabotage!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorOnSurface)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Welchen Spieler willst du sabotieren?", style = MaterialTheme.typography.bodyMedium, color = ColorOnSurfaceVariant)
                    targets.forEach { target ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (sabotageSelectedTarget?.id == target.id) target.color.copy(alpha = 0.15f) else ColorSurfaceVariant,
                            border = BorderStroke(1.dp, if (sabotageSelectedTarget?.id == target.id) target.color.copy(alpha = 0.5f) else ColorOutlineVariant),
                            modifier = Modifier.fillMaxWidth().clickable { sabotageSelectedTarget = target },
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                PlayerAvatarView(player = target, size = 28.dp, fontSize = 12.sp, photoBytes = gameState.playerAvatarBytes[target.id])
                                Spacer(Modifier.width(10.dp))
                                Text(target.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = ColorOnSurface)
                                if (sabotageSelectedTarget?.id == target.id) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp), tint = target.color)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                GradientButton(
                    text = "Weiter",
                    onClick = {
                        sabotageTargetPickerVisible = false
                        sabotageCategoryPickerVisible = true
                    },
                    enabled = sabotageSelectedTarget != null,
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
                )
            },
            dismissButton = {
                TextButton(onClick = { sabotageTargetPickerVisible = false; sabotageSelectedTarget = null }) { Text("Abbrechen") }
            },
        )
    }

    // ── Sabotage: Category-Picker Dialog ────────────────────────────────────
    if (sabotageCategoryPickerVisible && sabotageSelectedTarget != null && gameState.gameMode == "sabotage") {
        val target = sabotageSelectedTarget!!
        // Show only categories the target has NOT yet captured
        val targetCaptures = gameState.captures[target.id] ?: emptySet()
        val blockableCategories = gameState.selectedCategories.filter { it.id !in targetCaptures && !it.id.startsWith("joker_") }
        var selectedBlockCat by remember { mutableStateOf<Category?>(null) }
        AlertDialog(
            onDismissRequest = { sabotageCategoryPickerVisible = false },
            containerColor = ColorSurface,
            title = {
                Text("Welche Kategorie sperren?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorOnSurface)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Wähle eine Kategorie von ${target.name} zum Sperren:", style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant)
                    if (blockableCategories.isEmpty()) {
                        Text("Keine Kategorien zum Sperren verfügbar.", style = MaterialTheme.typography.bodyMedium, color = ColorError)
                    }
                    blockableCategories.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedBlockCat?.id == cat.id) Color(0xFFEF4444).copy(alpha = 0.12f) else ColorSurfaceVariant,
                            border = BorderStroke(1.dp, if (selectedBlockCat?.id == cat.id) Color(0xFFEF4444).copy(alpha = 0.5f) else ColorOutlineVariant),
                            modifier = Modifier.fillMaxWidth().clickable { selectedBlockCat = cat },
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(getCategoryIcon(cat.id), null, modifier = Modifier.size(18.dp).rotate(getCategoryIconRotation(cat.id)), tint = ColorOnSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Text(cat.name, style = MaterialTheme.typography.bodyMedium, color = ColorOnSurface, modifier = Modifier.weight(1f))
                                if (selectedBlockCat?.id == cat.id) {
                                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                GradientButton(
                    text = "Sabotieren!",
                    onClick = {
                        val blockCat = selectedBlockCat ?: return@GradientButton
                        sabotageCategoryPickerVisible = false
                        sabotageSelectedTarget = null
                        gameState.mySabotageUsed = true

                        val existingIds = gameState.selectedCategories.map { it.emoji }.toSet()
                        val replacementCat = getRandomReplacementCategory(existingIds)

                        // Update local sabotage state
                        val targetSabotages = (gameState.sabotages[target.id] ?: emptyMap()).toMutableMap()
                        targetSabotages[blockCat.id] = replacementCat
                        gameState.sabotages = gameState.sabotages + (target.id to targetSabotages)

                        // If the target is me, update blocked categories
                        if (target.id == gameState.myPlayerId) {
                            gameState.myBlockedCategories = gameState.myBlockedCategories + blockCat.id
                        }

                        // Record on server
                        if (gameId != null) {
                            val saboteurId = gameState.myPlayerId ?: return@GradientButton
                            scope.launch {
                                try {
                                    GameRepository.recordSabotage(
                                        gameId, saboteurId, target.id,
                                        blockCat.id, replacementCat.id,
                                        replacementCat.name, replacementCat.emoji
                                    )
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                    },
                    enabled = selectedBlockCat != null,
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
                )
            },
            dismissButton = {
                TextButton(onClick = { sabotageCategoryPickerVisible = false }) { Text("Abbrechen") }
            },
        )
    }

    // ── Sabotage notification popup ─────────────────────────────────────────
    if (sabotageNotification != null) {
        AlertDialog(
            onDismissRequest = { sabotageNotification = null },
            containerColor = ColorSurface,
            icon = { Icon(Icons.Default.Block, null, modifier = Modifier.size(32.dp), tint = Color(0xFFEF4444)) },
            title = { Text("Sabotage!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
            text = { Text(sabotageNotification!!, style = MaterialTheme.typography.bodyMedium, color = ColorOnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { sabotageNotification = null }) { Text("OK") }
            },
        )
    }

    // ── Elimination round-end screen ────────────────────────────────────────
    if (gameState.showEliminationScreen && gameState.gameMode == "elimination") {
        val eliminatedPlayer = gameState.lastEliminatedPlayerId?.let { pid -> gameState.players.find { it.id == pid } }
        val isMyElimination = eliminatedPlayer?.id == gameState.myPlayerId
        AlertDialog(
            onDismissRequest = {},
            containerColor = ColorSurface,
            icon = { Icon(Icons.Default.PersonOff, null, modifier = Modifier.size(40.dp), tint = Color(0xFFF59E0B)) },
            title = {
                Text(
                    if (isMyElimination) "Du wurdest eliminiert!" else "Elimination!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isMyElimination) ColorError else Color(0xFFF59E0B),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (eliminatedPlayer != null) {
                        PlayerAvatarView(player = eliminatedPlayer, size = 48.dp, fontSize = 20.sp, photoBytes = gameState.playerAvatarBytes[eliminatedPlayer.id])
                        Text(
                            "${eliminatedPlayer.name} wurde eliminiert!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = ColorOnSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                    val activePlayers = gameState.getActivePlayers()
                    Text(
                        "Runde ${gameState.eliminationRound} abgeschlossen",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                    Text(
                        "${activePlayers.size} Spieler verbleibend",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorPrimary,
                    )
                }
            },
            confirmButton = {
                if (gameState.isHost) {
                    val activePlayers = gameState.getActivePlayers()
                    GradientButton(
                        text = if (activePlayers.size <= 2) "Finale starten" else "Nächste Runde",
                        onClick = {
                            gameState.showEliminationScreen = false
                            // Start next round or end game
                            scope.launch {
                                if (gameId == null) return@launch
                                val active = gameState.getActivePlayers()
                                if (active.size <= 1) {
                                    // Game over - only 1 player left
                                    try { GameRepository.endGameAsVoting(gameId) } catch (_: Exception) {}
                                    gameState.reviewCategoryIndex = 0
                                    gameState.currentScreen = Screen.VOTE_TRANSITION
                                } else {
                                    // Reset for next round
                                    val nextRound = gameState.eliminationRound + 1
                                    gameState.eliminationRound = nextRound
                                    try { GameRepository.setEliminationRound(gameId, nextRound) } catch (_: Exception) {}

                                    // Reset captures for active players
                                    gameState.captures = gameState.players.associate { it.id to emptySet() }
                                    gameState.photos = gameState.players.associate { it.id to emptyMap() }
                                    gameState.allCategoriesCaptured = false
                                    gameState.finishSignalDetected = false
                                    gameState.hasVotedToEnd = false
                                    gameState.endVoteCount = 0

                                    // Pick new categories for this round (2-3 categories)
                                    val numCats = if (active.size <= 3) 1 else 2
                                    val existingIds = gameState.selectedCategories.map { it.emoji }.toSet()
                                    val newCats = (1..numCats).map { getRandomReplacementCategory(existingIds) }
                                    gameState.selectedCategories = newCats

                                    // Upload new categories to DB
                                    try {
                                        val catDtos = GameRepository.addCategories(gameId, newCats)
                                        gameState.selectedCategories = catDtos.map { it.toCategory() }
                                    } catch (e: Exception) { e.printStackTrace() }

                                    // Reset timer for short round
                                    gameState.timeRemainingSeconds = 120 // 2 minutes per elimination round
                                    gameState.isGameRunning = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = if (activePlayers.size <= 2) listOf(Color(0xFFF59E0B), Color(0xFFEF4444)) else GradientPrimary,
                    )
                } else {
                    Text("Warte auf den Host...", style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant)
                }
            },
        )
    }

    // ── Realtime: sabotage events ───────────────────────────────────────────
    LaunchedEffect(gameId) {
        if (realtime == null || gameState.gameMode != "sabotage") return@LaunchedEffect
        realtime.sabotageInserts.collect { sabotage ->
            // Update local sabotage state
            val targetSabotages = (gameState.sabotages[sabotage.target_id] ?: emptyMap()).toMutableMap()
            val replacementCat = Category(
                id = sabotage.replacement_category_id,
                name = sabotage.replacement_label,
                emoji = sabotage.replacement_icon_id,
            )
            targetSabotages[sabotage.blocked_category_id] = replacementCat
            gameState.sabotages = gameState.sabotages + (sabotage.target_id to targetSabotages)
            gameState.sabotageSource = gameState.sabotageSource + (sabotage.target_id to sabotage.saboteur_id)

            // If I'm the target, show notification and update my categories
            if (sabotage.target_id == gameState.myPlayerId) {
                gameState.myBlockedCategories = gameState.myBlockedCategories + sabotage.blocked_category_id
                val blockedCatName = gameState.selectedCategories.find { it.id == sabotage.blocked_category_id }?.name ?: "?"
                val saboteurName = gameState.players.find { it.id == sabotage.saboteur_id }?.name ?: "?"
                // Replace the blocked category with the replacement
                gameState.selectedCategories = gameState.selectedCategories.map {
                    if (it.id == sabotage.blocked_category_id) replacementCat else it
                }
                sabotageNotification = "$saboteurName hat deine Kategorie \"$blockedCatName\" gesperrt! Neue Kategorie: \"${sabotage.replacement_label}\""
            }
        }
    }

    // ── Realtime: elimination events ────────────────────────────────────────
    LaunchedEffect(gameId) {
        if (realtime == null || gameState.gameMode != "elimination") return@LaunchedEffect
        realtime.eliminationInserts.collect { elimination ->
            gameState.isGameRunning = false
            gameState.eliminatedPlayerIds = gameState.eliminatedPlayerIds + elimination.player_id
            gameState.lastEliminatedPlayerId = elimination.player_id
            gameState.showEliminationScreen = true
        }
    }

    // ── Realtime: game updates for elimination round changes ────────────────
    LaunchedEffect(gameId) {
        if (realtime == null || gameState.gameMode != "elimination") return@LaunchedEffect
        realtime.gameUpdates.collect { game ->
            if (game.elimination_round > gameState.eliminationRound && !gameState.isHost) {
                // Non-host: sync round change
                gameState.eliminationRound = game.elimination_round
                gameState.showEliminationScreen = false
                // Reload categories for new round
                if (gameId != null) {
                    try {
                        val cats = GameRepository.getCategories(gameId)
                        gameState.selectedCategories = cats.map { it.toCategory() }
                    } catch (_: Exception) {}
                }
                // Reset local state for new round
                gameState.captures = gameState.players.associate { it.id to emptySet() }
                gameState.photos = gameState.players.associate { it.id to emptyMap() }
                gameState.allCategoriesCaptured = false
                gameState.finishSignalDetected = false
                gameState.hasVotedToEnd = false
                gameState.endVoteCount = 0
                gameState.timeRemainingSeconds = 120
                gameState.isGameRunning = true
            }
        }
    }

    // ── Elimination: timer end triggers round evaluation (host only) ────────
    LaunchedEffect(gameState.timeRemainingSeconds, gameState.gameMode) {
        if (gameState.gameMode != "elimination") return@LaunchedEffect
        if (!gameState.isHost) return@LaunchedEffect
        if (gameState.timeRemainingSeconds > 0) return@LaunchedEffect
        if (!gameState.isGameRunning) return@LaunchedEffect
        if (gameState.showEliminationScreen) return@LaunchedEffect

        val activePlayers = gameState.getActivePlayers()
        if (activePlayers.size <= 1) {
            // Game over
            if (gameId != null) {
                try { GameRepository.endGameAsVoting(gameId) } catch (_: Exception) {}
            }
            gameState.reviewCategoryIndex = 0
            gameState.currentScreen = Screen.VOTE_TRANSITION
            return@LaunchedEffect
        }

        // Find the player with fewest captures in this round
        val ranked = activePlayers.map { p ->
            p to (gameState.captures[p.id]?.size ?: 0)
        }.sortedBy { it.second }

        val worstPlayer = ranked.first().first
        gameState.isGameRunning = false

        // Record elimination
        if (gameId != null) {
            try {
                GameRepository.recordElimination(gameId, worstPlayer.id, gameState.eliminationRound)
            } catch (e: Exception) { e.printStackTrace() }
        }
        gameState.eliminatedPlayerIds = gameState.eliminatedPlayerIds + worstPlayer.id
        gameState.lastEliminatedPlayerId = worstPlayer.id
        gameState.showEliminationScreen = true
    }

    // ── Sabotage: poll for sabotages (fallback) ─────────────────────────────
    LaunchedEffect(gameId) {
        if (gameId == null || gameState.gameMode != "sabotage") return@LaunchedEffect
        while (gameState.isGameRunning) {
            delay(5_000)
            try {
                val allSabotages = GameRepository.getSabotages(gameId)
                allSabotages.forEach { sabotage ->
                    val existing = gameState.sabotages[sabotage.target_id] ?: emptyMap()
                    if (sabotage.blocked_category_id !in existing) {
                        val replacementCat = Category(
                            id = sabotage.replacement_category_id,
                            name = sabotage.replacement_label,
                            emoji = sabotage.replacement_icon_id,
                        )
                        val updated = existing.toMutableMap()
                        updated[sabotage.blocked_category_id] = replacementCat
                        gameState.sabotages = gameState.sabotages + (sabotage.target_id to updated)

                        if (sabotage.target_id == gameState.myPlayerId && sabotage.blocked_category_id !in gameState.myBlockedCategories) {
                            gameState.myBlockedCategories = gameState.myBlockedCategories + sabotage.blocked_category_id
                            gameState.selectedCategories = gameState.selectedCategories.map {
                                if (it.id == sabotage.blocked_category_id) replacementCat else it
                            }
                            val blockedName = gameState.selectedCategories.find { it.id == sabotage.blocked_category_id }?.name ?: "?"
                            val saboteurName = gameState.players.find { it.id == sabotage.saboteur_id }?.name ?: "?"
                            sabotageNotification = "$saboteurName hat deine Kategorie \"$blockedName\" gesperrt! Neue Kategorie: \"${sabotage.replacement_label}\""
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Realtime: game status and end-votes
    LaunchedEffect(gameId) {
        if (realtime == null) return@LaunchedEffect
        realtime.gameUpdates.collect { game ->
            if (game.status == "voting" && gameState.currentScreen == Screen.GAME) {
                gameState.isGameRunning = false
                gameState.reviewCategoryIndex = game.review_category_index
                gameState.currentScreen = Screen.VOTE_TRANSITION
            }
        }
    }

    LaunchedEffect(gameId) {
        if (realtime == null || gameId == null) return@LaunchedEffect
        val gid = gameId
        realtime.voteSubmissionInserts.collect { submission ->
            if (submission.category_id == VoteKeys.END_VOTE) {
                try {
                    val count = GameRepository.getEndVoteCount(gid)
                    gameState.endVoteCount = count
                    if (count >= gameState.players.size && gameState.players.isNotEmpty()) {
                        gameState.isGameRunning = false
                        GameRepository.endGameAsVoting(gameId)
                        gameState.reviewCategoryIndex = 0
                        gameState.currentScreen = Screen.VOTE_TRANSITION
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            // Also detect finish signal via realtime
            if (submission.category_id == VoteKeys.ALL_CAPTURED && !gameState.finishSignalDetected && !gameState.allCategoriesCaptured) {
                gameState.finishSignalDetected = true
            }
        }
    }

    // Realtime: update other players' capture counts
    LaunchedEffect(gameId) {
        if (realtime == null) return@LaunchedEffect
        realtime.captureInserts.collect { capture ->
            if (capture.player_id != gameState.myPlayerId) {
                val current = gameState.captures[capture.player_id] ?: emptySet()
                gameState.captures = gameState.captures + (capture.player_id to current + capture.category_id)
            }
        }
    }

    // Fallback polling
    LaunchedEffect(gameId) {
        if (gameId == null) return@LaunchedEffect
        while (true) {
            delay(3_000)
            try {
                val game = GameRepository.getGameById(gameId)
                if (game?.status == "voting" && gameState.currentScreen == Screen.GAME) {
                    gameState.isGameRunning = false
                    gameState.reviewCategoryIndex = game.review_category_index
                    gameState.currentScreen = Screen.VOTE_TRANSITION
                }
                if (gameState.isGameRunning) {
                    // Poll other players' captures
                    try {
                        val allCaptures = GameRepository.getCaptures(gameId)
                        val updatedCaptures = gameState.captures.toMutableMap()
                        allCaptures.forEach { capture ->
                            val current = updatedCaptures[capture.player_id] ?: emptySet()
                            updatedCaptures[capture.player_id] = current + capture.category_id
                        }
                        gameState.captures = updatedCaptures
                    } catch (_: Exception) {}

                    val count = GameRepository.getEndVoteCount(gameId)
                    gameState.endVoteCount = count
                    if (count >= gameState.players.size && gameState.players.isNotEmpty()) {
                        gameState.isGameRunning = false
                        GameRepository.endGameAsVoting(gameId)
                        gameState.reviewCategoryIndex = 0
                        gameState.currentScreen = Screen.VOTE_TRANSITION
                    }

                    // Check if any player completed all categories (for countdown)
                    if (!gameState.finishSignalDetected && !gameState.allCategoriesCaptured) {
                        try {
                            if (GameRepository.hasAllCapturedSignal(gameId)) {
                                gameState.finishSignalDetected = true
                            }
                        } catch (_: Exception) {}
                    }
                }
                gameState.consecutiveNetworkErrors = 0
            } catch (e: Exception) {
                e.printStackTrace()
                gameState.consecutiveNetworkErrors++
            }
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

    // Retry avatar downloads for players still missing avatars (handles iOS failures)
    LaunchedEffect(gameId) {
        var retries = 0
        while (retries < 5) {
            delay(5_000)
            val missing = gameState.players.filter { it.id !in gameState.playerAvatarBytes }
            if (missing.isEmpty()) break
            missing.forEach { player ->
                scope.launch {
                    val bytes = GameRepository.downloadAvatarPhoto(player.id)
                    if (bytes != null) {
                        gameState.playerAvatarBytes = gameState.playerAvatarBytes + (player.id to bytes)
                    }
                }
            }
            retries++
        }
    }

    // Timer logic – keyed on eliminationRound so it restarts each round in elimination mode
    LaunchedEffect(Unit, gameState.eliminationRound) {
        while (gameState.isGameRunning && gameState.timeRemainingSeconds > 0 && !gameState.allCategoriesCaptured) {
            delay(1000L)
            if (gameState.isGameRunning && !gameState.allCategoriesCaptured) {
                gameState.timeRemainingSeconds--
            }
        }
        if (!gameState.allCategoriesCaptured && gameState.timeRemainingSeconds <= 0 && gameState.isGameRunning) {
            // In elimination mode, timer end is handled by the elimination LaunchedEffect
            if (gameState.gameMode == "elimination") return@LaunchedEffect
            if (gameId != null) {
                try { GameRepository.endGameAsVoting(gameId) } catch (e: Exception) { e.printStackTrace() }
            }
            gameState.reviewCategoryIndex = 0
            gameState.currentScreen = Screen.VOTE_TRANSITION
        }
    }

    // Detect remote "all captured" signal from other players (skip in elimination mode)
    LaunchedEffect(gameId) {
        if (gameId == null) return@LaunchedEffect
        if (gameState.gameMode == "elimination") return@LaunchedEffect
        while (gameState.isGameRunning && !gameState.allCategoriesCaptured && !gameState.finishSignalDetected) {
            delay(2_000)
            try {
                if (GameRepository.hasAllCapturedSignal(gameId)) {
                    gameState.finishSignalDetected = true
                }
            } catch (_: Exception) {}
        }
    }

    // Finish countdown – triggered by local completion OR remote signal (skip in elimination mode)
    var finishCountdownSeconds by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        if (gameState.gameMode == "elimination") return@LaunchedEffect
        // Wait until either the local player or a remote player signals "all captured"
        snapshotFlow { gameState.allCategoriesCaptured || gameState.finishSignalDetected }
            .first { it }
        // Signal to server if we were the one who captured all
        if (gameState.allCategoriesCaptured && gameId != null) {
            val pid = gameState.myPlayerId ?: ""
            for (attempt in 0 until 3) {
                try {
                    if (attempt > 0) delay(1_000L * attempt)
                    GameRepository.signalAllCaptured(gameId, pid)
                    break
                } catch (_: Exception) {}
            }
        }
        finishCountdownSeconds = 30
        repeat(30) {
            delay(1000L)
            finishCountdownSeconds = (finishCountdownSeconds ?: 1) - 1
        }
        if (gameState.isGameRunning) {
            gameState.isGameRunning = false
            if (gameId != null) {
                try { GameRepository.endGameAsVoting(gameId) } catch (_: Exception) {}
            }
            gameState.reviewCategoryIndex = 0
            gameState.currentScreen = Screen.VOTE_TRANSITION
        }
    }

    GameScreenContent(
        gameState = gameState,
        finishCountdownSeconds = finishCountdownSeconds,
        uploadSuccessCategory = uploadSuccessCategory,
        onJokerClick = { jokerDialogVisible = true },
        onSwapCategory = { category -> swapDialogCategory = category },
        onSabotageClick = { sabotageTargetPickerVisible = true },
        onVoteToEnd = {
            scope.launch {
                if (gameId != null && !gameState.hasVotedToEnd) {
                    val pid = gameState.myPlayerId ?: return@launch
                    gameState.hasVotedToEnd = true
                    gameState.endVoteCount = gameState.endVoteCount + 1
                    var attempt = 0
                    var success = false
                    while (attempt < 3) {
                        try {
                            if (attempt > 0) delay(1_000L * attempt)
                            GameRepository.submitEndVote(gameId, pid)
                            success = true
                            break
                        } catch (e: Exception) {
                            e.printStackTrace()
                            attempt++
                        }
                    }
                    if (!success) {
                        gameState.hasVotedToEnd = false
                        gameState.endVoteCount = (gameState.endVoteCount - 1).coerceAtLeast(0)
                    } else {
                        // Check if majority reached after own vote
                        val count = gameState.endVoteCount
                        if (count * 2 > gameState.players.size && gameState.players.isNotEmpty()) {
                            gameState.isGameRunning = false
                            try { GameRepository.endGameAsVoting(gameId) } catch (_: Exception) {}
                            gameState.reviewCategoryIndex = 0
                            gameState.currentScreen = Screen.VOTE_TRANSITION
                        }
                    }
                }
            }
        },
        onCameraClick = { playerId, catId ->
            photoTargetPlayerId = playerId
            photoTargetCategoryId = catId
            photoCapturer.launch()
        },
    )
}

@Composable
fun GameScreenContent(
    gameState: GameState,
    finishCountdownSeconds: Int? = null,
    uploadSuccessCategory: String? = null,
    onJokerClick: () -> Unit = {},
    onSwapCategory: (Category) -> Unit = {},
    onSabotageClick: () -> Unit = {},
    onVoteToEnd: () -> Unit = {},
    onCameraClick: (String, String) -> Unit = { _, _ -> },
) {
    // Fade-in animation
    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { contentAlpha.animateTo(1f, tween(400)) }

    val isLow = gameState.timeRemainingSeconds in 1..60
    val isCritical = gameState.timeRemainingSeconds in 1..30
    val timerColor by animateColorAsState(
        targetValue = if (isLow) ColorError else ColorPrimary,
        animationSpec = tween(500),
    )

    // Timer pulse animation when <30s
    val pulseTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseDuration = when {
        gameState.timeRemainingSeconds in 1..10 -> 500
        gameState.timeRemainingSeconds in 11..20 -> 1000
        else -> 1500
    }
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = if (isCritical) 0.4f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isCritical) 1.03f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val myPlayer = gameState.players.find { it.id == gameState.myPlayerId }

    Scaffold(containerColor = ColorBackground) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).graphicsLayer { alpha = contentAlpha.value }) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar: timer
                Surface(
                    color = ColorSurface,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, ColorOutlineVariant),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            },
                        ) {
                            // Pulsing glow behind timer when critical
                            if (isCritical) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ColorError.copy(alpha = pulseAlpha)),
                                )
                            }
                            if (isLow) {
                                AnimatedGradientText(
                                    text = gameState.formatTime(gameState.timeRemainingSeconds),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 3.sp,
                                        fontSize = 40.sp,
                                    ),
                                    gradientColors = GradientHot,
                                    durationMillis = 600,
                                )
                            } else {
                                Text(
                                    text = gameState.formatTime(gameState.timeRemainingSeconds),
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = timerColor,
                                    letterSpacing = 3.sp,
                                )
                            }
                        }

                        if (finishCountdownSeconds != null) {
                            Spacer(Modifier.height(10.dp))
                            AnimatedGradientBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                gradientColors = GradientHot,
                                durationMillis = 800,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        if (gameState.allCategoriesCaptured) "Du hast alle gefunden!"
                                        else "Jemand hat alle gefunden!",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "Noch ${finishCountdownSeconds}s",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            gameState.players.forEach { player ->
                                val isMe = player.id == gameState.myPlayerId
                                val captured = gameState.captures[player.id]?.size ?: 0
                                val isEliminated = gameState.isEliminated(player.id)
                                GamePlayerTab(
                                    player = player,
                                    isActive = isMe,
                                    captureCount = captured,
                                    totalCategories = gameState.selectedCategories.size,
                                    photoBytes = gameState.playerAvatarBytes[player.id],
                                    isEliminated = isEliminated,
                                    onClick = {},
                                )
                            }
                        }
                    }
                }

                // Player info + controls
                if (myPlayer != null) {
                    val myCount = gameState.captures[myPlayer.id]?.size ?: 0
                    val totalCats = gameState.selectedCategories.size
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.screenHorizontal),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Animated progress ring around avatar
                            val ringProgress = remember { Animatable(0f) }
                            val targetProgress = if (totalCats > 0) myCount.toFloat() / totalCats else 0f
                            LaunchedEffect(myCount) {
                                ringProgress.animateTo(targetProgress, tween(500))
                            }
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { ringProgress.value },
                                    modifier = Modifier.size(40.dp),
                                    color = myPlayer.color,
                                    trackColor = ColorSurfaceVariant,
                                    strokeWidth = 3.dp,
                                )
                                PlayerAvatarView(player = myPlayer, size = 30.dp, fontSize = 12.sp, photoBytes = gameState.playerAvatarBytes[myPlayer.id])
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(myPlayer.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ColorOnSurface)
                                Text("$myCount/$totalCats gefunden", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (gameState.jokerMode && !gameState.myJokerUsed) {
                                OutlinedButton(
                                    onClick = onJokerClick,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorPrimary),
                                    border = BorderStroke(1.dp, ColorPrimary.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Icon(Icons.Default.Style, null, modifier = Modifier.size(14.dp), tint = ColorPrimary)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Joker", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            // Sabotage button
                            if (gameState.gameMode == "sabotage" && !gameState.mySabotageUsed && !gameState.isEliminated(myPlayer.id)) {
                                val totalTime = gameState.gameDurationMinutes * 60
                                val elapsed = totalTime - gameState.timeRemainingSeconds
                                val protectionOver = elapsed >= totalTime * 0.25
                                OutlinedButton(
                                    onClick = onSabotageClick,
                                    enabled = protectionOver,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = if (protectionOver) 0.6f else 0.2f)),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Icon(Icons.Default.Block, null, modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444).copy(alpha = if (protectionOver) 1f else 0.4f))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (protectionOver) "Sabotage" else "Sabotage (Schutz)",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                            // Elimination round indicator
                            if (gameState.gameMode == "elimination") {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PersonOff, null, modifier = Modifier.size(14.dp), tint = Color(0xFFF59E0B))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Runde ${gameState.eliminationRound + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFFF59E0B),
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = onVoteToEnd,
                                enabled = !gameState.hasVotedToEnd,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorError),
                                border = BorderStroke(1.dp, ColorError.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                val needed = gameState.players.size
                                Text(
                                    if (gameState.hasVotedToEnd) "Abgestimmt (${gameState.endVoteCount}/$needed)"
                                    else "Vorzeitig beenden (${gameState.endVoteCount}/$needed)",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = ColorOutlineVariant)
                }

                // Bingo grid
                if (myPlayer != null) {
                    val cols = when {
                        gameState.selectedCategories.size <= 4 -> 2
                        gameState.selectedCategories.size <= 9 -> 3
                        else -> 4
                    }
                    // For elimination mode: show if player is eliminated
                    if (gameState.gameMode == "elimination" && gameState.isEliminated(myPlayer.id)) {
                        Box(
                            modifier = Modifier.weight(1f).padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.PersonOff, null, modifier = Modifier.size(48.dp), tint = ColorOnSurfaceVariant.copy(alpha = 0.5f))
                                Text(
                                    "Du wurdest eliminiert",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ColorOnSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "Du kannst das Spiel noch beobachten.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorOnSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(cols),
                            modifier = Modifier.weight(1f).padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(gameState.selectedCategories) { category ->
                                val captured = gameState.isCaptured(myPlayer.id, category.id)
                                val photoBytes = gameState.getPhoto(myPlayer.id, category.id)
                                val thumbnail: ImageBitmap? = remember(photoBytes) { photoBytes?.toImageBitmap() }
                                val otherCapturers = gameState.players.filter { p ->
                                    p.id != myPlayer.id && (gameState.captures[p.id]?.contains(category.id) == true)
                                }
                                val isUploading = category.id in gameState.uploadingCategories
                                val showUploadSuccess = uploadSuccessCategory == category.id
                                val canSwap = gameState.gameMode == "kategorie_tausch" && !gameState.mySwapUsed && !captured
                                DarkBingoCategoryCard(
                                    category = category,
                                    isCaptured = captured,
                                    isUploading = isUploading,
                                    showUploadSuccess = showUploadSuccess,
                                    playerColor = myPlayer.color,
                                    thumbnail = thumbnail,
                                    otherCapturingPlayers = otherCapturers,
                                    onCameraClick = { onCameraClick(myPlayer.id, category.id) },
                                    showSwapButton = canSwap,
                                    onSwapClick = { onSwapCategory(category) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GamePlayerTab(player: Player, isActive: Boolean, captureCount: Int, totalCategories: Int, photoBytes: ByteArray? = null, isEliminated: Boolean = false, onClick: () -> Unit) {
    val bg = when {
        isEliminated -> Brush.linearGradient(listOf(ColorSurfaceVariant.copy(alpha = 0.5f), ColorSurfaceVariant.copy(alpha = 0.3f)))
        isActive -> Brush.linearGradient(listOf(player.color.copy(alpha = 0.2f), player.color.copy(alpha = 0.1f)))
        else -> Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(if (isActive && !isEliminated) Modifier.border(1.dp, player.color.copy(alpha = 0.4f), RoundedCornerShape(16.dp)) else Modifier)
            .graphicsLayer { alpha = if (isEliminated) 0.5f else 1f }
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayerAvatarView(player = player, size = 18.dp, fontSize = 8.sp, photoBytes = photoBytes)
            Spacer(Modifier.width(6.dp))
            Text(player.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (isEliminated) ColorOnSurfaceVariant.copy(alpha = 0.5f) else if (isActive) ColorOnSurface else ColorOnSurfaceVariant)
            if (isEliminated) {
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Default.PersonOff, null, modifier = Modifier.size(10.dp), tint = ColorError.copy(alpha = 0.6f))
            } else {
                Spacer(Modifier.width(4.dp))
                Text(
                    "$captureCount/$totalCategories",
                    fontSize = 11.sp,
                    color = if (captureCount >= totalCategories) ColorPrimary
                        else (if (isActive) player.color else ColorOnSurfaceVariant).copy(alpha = 0.8f),
                    fontWeight = if (captureCount >= totalCategories) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DarkBingoCategoryCard(
    category: Category,
    isCaptured: Boolean,
    isUploading: Boolean,
    showUploadSuccess: Boolean = false,
    playerColor: Color,
    thumbnail: ImageBitmap?,
    otherCapturingPlayers: List<Player> = emptyList(),
    onCameraClick: () -> Unit,
    showSwapButton: Boolean = false,
    onSwapClick: () -> Unit = {},
) {
    var showInfo by remember { mutableStateOf(false) }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            containerColor = ColorSurface,
            title = { Text(category.name, fontWeight = FontWeight.Bold) },
            text = { Text(category.description) },
            confirmButton = {
                TextButton(onClick = { showInfo = false; onCameraClick() }) {
                    Text(if (isCaptured) "Neu aufnehmen" else "Foto machen")
                }
            },
            dismissButton = { TextButton(onClick = { showInfo = false }) { Text("Schließen") } }
        )
    }

    val containerColor by animateColorAsState(if (isCaptured) playerColor.copy(alpha = 0.15f) else ColorSurface)
    val borderColor = if (isCaptured) playerColor.copy(alpha = 0.5f) else ColorOutlineVariant

    // Upload success checkmark scale animation
    val checkScale = remember { Animatable(0f) }
    LaunchedEffect(showUploadSuccess) {
        if (showUploadSuccess) {
            checkScale.animateTo(1.2f, tween(200))
            checkScale.animateTo(1f, tween(150))
        } else {
            checkScale.snapTo(0f)
        }
    }

    Card(
        modifier = Modifier.aspectRatio(0.9f).fillMaxWidth().combinedClickable(onClick = { onCameraClick() }, onLongClick = { showInfo = true }),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (thumbnail != null && !isUploading) {
                // Photo fills the entire card
                Image(
                    bitmap = thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // Gradient scrim at bottom for text readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(0.45f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        ),
                )
                // Category name overlay at bottom
                Text(
                    category.name,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
                // Checkmark badge top-right
                if (isCaptured) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(playerColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = Color.White)
                    }
                }
            } else {
                // No photo yet or uploading — show icon/spinner layout
                Column(
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = playerColor)
                    } else {
                        Icon(imageVector = getCategoryIcon(category.id), contentDescription = null, modifier = Modifier.size(26.dp).rotate(getCategoryIconRotation(category.id)), tint = if (isCaptured) playerColor else ColorOnSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(category.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 12.sp)
                    if (isCaptured) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = playerColor)
                }
            }
            // Swap button (Kategorie-Tausch mode)
            if (showSwapButton) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(3.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.9f))
                        .clickable { onSwapClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(14.dp), tint = Color.White)
                }
            }
            // Upload success overlay with animated checkmark
            if (showUploadSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(playerColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer {
                                scaleX = checkScale.value
                                scaleY = checkScale.value
                            },
                        tint = playerColor,
                    )
                }
            }
        }
    }
}
