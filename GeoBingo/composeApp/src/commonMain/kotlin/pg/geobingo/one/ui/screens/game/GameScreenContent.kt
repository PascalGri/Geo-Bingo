package pg.geobingo.one.ui.screens.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pg.geobingo.one.data.Player
import pg.geobingo.one.game.*
import pg.geobingo.one.game.GameMode
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*

@Composable
fun GameScreenContent(
    gameState: GameState,
    finishCountdownSeconds: Int? = null,
    uploadSuccessCategory: String? = null,
    onJokerClick: () -> Unit = {},
    onVoteToEnd: () -> Unit = {},
    onCameraClick: (String, String) -> Unit = { _, _ -> },
) {
    // Fade-in animation
    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { contentAlpha.animateTo(1f, tween(400)) }

    val modeGradient = when (gameState.session.gameMode) {
        GameMode.CLASSIC     -> GradientPrimary
        GameMode.BLIND_BINGO -> GradientCool
        GameMode.WEIRD_CORE  -> GradientWeird
        GameMode.QUICK_START -> GradientQuickStart
    }
    val modeColor = modeGradient.first()

    val isLow = gameState.gameplay.timeRemainingSeconds in 1..60
    val isCritical = gameState.gameplay.timeRemainingSeconds in 1..30
    val timerColor by animateColorAsState(
        targetValue = if (isLow) ColorError else modeColor,
        animationSpec = tween(500),
    )

    // Timer pulse animation when <30s
    val pulseTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseDuration = when {
        gameState.gameplay.timeRemainingSeconds in 1..10 -> 500
        gameState.gameplay.timeRemainingSeconds in 11..20 -> 1000
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

    val myPlayer = gameState.gameplay.players.find { it.id == gameState.session.myPlayerId }

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
                                    text = gameState.formatTime(gameState.gameplay.timeRemainingSeconds),
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
                                    text = gameState.formatTime(gameState.gameplay.timeRemainingSeconds),
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
                                        if (gameState.review.allCategoriesCaptured) S.current.youFoundAll
                                        else S.current.someoneFoundAll,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        S.current.secondsRemaining(finishCountdownSeconds),
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
                            gameState.gameplay.players.forEach { player ->
                                val isMe = player.id == gameState.session.myPlayerId
                                val captured = gameState.gameplay.captures[player.id]?.size ?: 0
                                GamePlayerTab(
                                    player = player,
                                    isActive = isMe,
                                    captureCount = captured,
                                    totalCategories = gameState.gameplay.selectedCategories.size,
                                    photoBytes = gameState.photo.playerAvatarBytes[player.id],
                                    accentColor = modeColor,
                                    teamNumber = if (gameState.gameplay.teamModeEnabled) gameState.gameplay.teamAssignments[player.id] else null,
                                    onClick = {},
                                )
                            }
                        }
                    }
                }

                // Player info + controls
                if (myPlayer != null) {
                    val isTeamMode = gameState.gameplay.teamModeEnabled
                    val myTeam = if (isTeamMode) gameState.getMyTeamNumber() else null
                    val myCount = if (isTeamMode && myTeam != null)
                        gameState.getTeamCaptures(myTeam).size
                    else
                        gameState.gameplay.captures[myPlayer.id]?.size ?: 0
                    val totalCats = gameState.gameplay.selectedCategories.size
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
                                PlayerAvatarView(player = myPlayer, size = 30.dp, fontSize = 12.sp, photoBytes = gameState.photo.playerAvatarBytes[myPlayer.id])
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                val displayName = if (isTeamMode && myTeam != null)
                                    gameState.gameplay.teamNames[myTeam] ?: myPlayer.name
                                else myPlayer.name
                                Text(displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ColorOnSurface)
                                Text(
                                    if (isTeamMode) S.current.teamFoundCount(myCount, totalCats)
                                    else S.current.foundCount(myCount, totalCats),
                                    style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant,
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (gameState.joker.jokerMode && !gameState.joker.myJokerUsed) {
                                OutlinedButton(
                                    onClick = onJokerClick,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = modeColor),
                                    border = BorderStroke(1.dp, modeColor.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Icon(Icons.Default.Style, null, modifier = Modifier.size(14.dp), tint = modeColor)
                                    Spacer(Modifier.width(4.dp))
                                    Text(S.current.joker, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            OutlinedButton(
                                onClick = onVoteToEnd,
                                enabled = !gameState.review.hasVotedToEnd,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorError),
                                border = BorderStroke(1.dp, ColorError.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                val needed = gameState.gameplay.players.size
                                Text(
                                    if (gameState.review.hasVotedToEnd) S.current.votedToEnd(gameState.review.endVoteCount, needed)
                                    else S.current.voteToEnd(gameState.review.endVoteCount, needed),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = ColorOutlineVariant)
                }

                // Bingo grid
                if (myPlayer != null) {
                    val isBlindBingo = gameState.session.gameMode == GameMode.BLIND_BINGO
                    val totalSeconds = gameState.gameplay.gameDurationMinutes * 60
                    val elapsed = totalSeconds - gameState.gameplay.timeRemainingSeconds
                    val totalCats = gameState.gameplay.selectedCategories.size
                    val revealedCount = if (isBlindBingo && totalCats > 0 && totalSeconds > 0) {
                        (elapsed * totalCats / totalSeconds + 1).coerceIn(1, totalCats)
                    } else {
                        totalCats
                    }

                    val cols = when {
                        totalCats <= 4 -> 2
                        totalCats <= 9 -> 3
                        else -> 4
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(cols),
                        modifier = Modifier.weight(1f).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(gameState.gameplay.selectedCategories.size) { index ->
                            val category = gameState.gameplay.selectedCategories[index]
                            val isRevealed = index < revealedCount
                            val isNextToReveal = isBlindBingo && index == revealedCount

                            AnimatedContent(
                                targetState = isRevealed,
                                transitionSpec = {
                                    (fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.8f))
                                        .togetherWith(fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.8f))
                                },
                                label = "categoryReveal",
                            ) { revealed ->
                                if (!revealed) {
                                    BlindBingoLockedCard(
                                        revealIndex = index + 1,
                                        isNextToReveal = isNextToReveal,
                                        glowColors = modeGradient,
                                    )
                                } else {
                                    // Team mode: check team captures; Solo: check player captures
                                    val captured: Boolean
                                    val photoBytes: ByteArray?
                                    val otherCapturers: List<Player>

                                    if (isTeamMode && myTeam != null) {
                                        captured = gameState.isTeamCaptured(myTeam, category.id)
                                        val capturer = gameState.getTeamCapturer(myTeam, category.id)
                                        photoBytes = if (capturer != null) gameState.getPhoto(capturer.id, category.id) else null
                                        // Show other teams' capture counts
                                        otherCapturers = gameState.getTeamNumbers()
                                            .filter { it != myTeam }
                                            .flatMap { otherTeam ->
                                                if (gameState.isTeamCaptured(otherTeam, category.id))
                                                    listOfNotNull(gameState.getTeamCapturer(otherTeam, category.id))
                                                else emptyList()
                                            }
                                    } else {
                                        captured = gameState.isCaptured(myPlayer.id, category.id)
                                        photoBytes = gameState.getPhoto(myPlayer.id, category.id)
                                        otherCapturers = gameState.gameplay.players.filter { p ->
                                            p.id != myPlayer.id && (gameState.gameplay.captures[p.id]?.contains(category.id) == true)
                                        }
                                    }

                                    var thumbnail by remember(photoBytes) { mutableStateOf<ImageBitmap?>(null) }
                                    LaunchedEffect(photoBytes) {
                                        thumbnail = if (photoBytes != null) withContext(Dispatchers.Default) { photoBytes.toImageBitmap() } else null
                                    }
                                    val isUploading = gameState.photo.isUploading(category.id)
                                    val showUploadSuccess = uploadSuccessCategory == category.id
                                    DarkBingoCategoryCard(
                                        category = category,
                                        isCaptured = captured,
                                        isUploading = isUploading,
                                        showUploadSuccess = showUploadSuccess,
                                        playerColor = myPlayer.color,
                                        thumbnail = thumbnail,
                                        otherCapturingPlayers = otherCapturers,
                                        onCameraClick = { onCameraClick(myPlayer.id, category.id) },
                                    )
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
internal fun BlindBingoLockedCard(
    revealIndex: Int,
    isNextToReveal: Boolean = false,
    glowColors: List<Color> = emptyList(),
) {
    val transition = rememberInfiniteTransition(label = "lockedPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lockedAlpha",
    )

    // Pulsing glow border when this card is the next to be revealed
    val glowAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    val borderStroke = if (isNextToReveal && glowColors.size >= 2) {
        BorderStroke(
            2.dp,
            Brush.linearGradient(glowColors.map { it.copy(alpha = glowAlpha) }),
        )
    } else {
        BorderStroke(1.dp, ColorOutlineVariant)
    }

    Card(
        modifier = Modifier.aspectRatio(0.9f).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceVariant),
        border = borderStroke,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).graphicsLayer { this.alpha = alpha },
                    tint = if (isNextToReveal && glowColors.isNotEmpty())
                        glowColors.first().copy(alpha = alpha)
                    else ColorOnSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "#$revealIndex",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isNextToReveal && glowColors.isNotEmpty())
                        glowColors.first().copy(alpha = alpha)
                    else ColorOnSurfaceVariant.copy(alpha = alpha),
                )
            }
        }
    }
}

@Composable
internal fun GamePlayerTab(player: Player, isActive: Boolean, captureCount: Int, totalCategories: Int, photoBytes: ByteArray? = null, accentColor: Color = ColorPrimary, teamNumber: Int? = null, onClick: () -> Unit) {
    val bg = if (isActive)
        Brush.linearGradient(listOf(player.color.copy(alpha = 0.2f), player.color.copy(alpha = 0.1f)))
    else
        Brush.linearGradient(listOf(ColorSurfaceVariant, ColorSurfaceVariant))

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(if (isActive) Modifier.border(1.dp, player.color.copy(alpha = 0.4f), RoundedCornerShape(16.dp)) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (teamNumber != null) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (teamNumber == 1) accentColor.copy(alpha = 0.7f) else ColorOnSurfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$teamNumber", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(4.dp))
            }
            PlayerAvatarView(player = player, size = 18.dp, fontSize = 8.sp, photoBytes = photoBytes)
            Spacer(Modifier.width(6.dp))
            Text(player.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (isActive) ColorOnSurface else ColorOnSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(
                "$captureCount/$totalCategories",
                fontSize = 11.sp,
                color = if (captureCount >= totalCategories) accentColor
                    else (if (isActive) player.color else ColorOnSurfaceVariant).copy(alpha = 0.8f),
                fontWeight = if (captureCount >= totalCategories) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
