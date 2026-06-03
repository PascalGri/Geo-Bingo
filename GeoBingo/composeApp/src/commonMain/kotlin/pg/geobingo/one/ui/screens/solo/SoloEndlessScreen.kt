package pg.geobingo.one.ui.screens.solo

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.endlessPool
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SoundEffect
import pg.geobingo.one.platform.SoundPlayer
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.platform.play
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.util.AppLogger

private val EndlessGradient = listOf(Color(0xFFF97316), Color(0xFFEF4444))
private val AIGradient = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
private val GoldGradient = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))

private const val BEST_STREAK_KEY = "endless_best_streak"
private const val START_SECONDS = 30
private const val SHRINK_PER_HIT = 2
private const val MIN_SECONDS = 8
private const val PASS_THRESHOLD = 3

private enum class Phase { PLAYING, BUSY, GAME_OVER }

private fun timeForRound(streak: Int): Int =
    (START_SECONDS - streak * SHRINK_PER_HIT).coerceAtLeast(MIN_SECONDS)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloEndlessScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val scope = rememberCoroutineScope()
    val aiConsentAccepted = pg.geobingo.one.platform.AiConsent.ratingAccepted

    var phase by remember { mutableStateOf(Phase.PLAYING) }
    var streak by remember { mutableStateOf(0) }
    var queue by remember { mutableStateOf(endlessPool()) }
    var current by remember { mutableStateOf<Category?>(null) }
    var roundIndex by remember { mutableStateOf(0) }
    var roundEndMillis by remember { mutableStateOf(0L) }
    var carryMillis by remember { mutableStateOf(0L) }
    var timeLeft by remember { mutableStateOf(START_SECONDS) }
    var lastRating by remember { mutableStateOf<Int?>(null) }
    var isNewRecord by remember { mutableStateOf(false) }
    val bestAtStart = remember { mutableStateOf(AppSettings.getInt(BEST_STREAK_KEY, 0)) }
    val displayBest = maxOf(streak, bestAtStart.value)

    fun nextCategory() {
        if (queue.isEmpty()) queue = endlessPool()
        current = queue.first()
        queue = queue.drop(1)
    }

    fun startRun() {
        streak = 0
        queue = endlessPool()
        carryMillis = 0L
        isNewRecord = false
        lastRating = null
        bestAtStart.value = AppSettings.getInt(BEST_STREAK_KEY, 0)
        nextCategory()
        phase = Phase.PLAYING
        roundIndex += 1
    }

    // Initial round
    LaunchedEffect(Unit) {
        if (current == null) {
            nextCategory()
            roundIndex += 1
        }
    }

    // Per-round shrinking timer (wall-clock). Paused whenever phase != PLAYING
    // (camera open / AI validating), so the clock only ticks while the player
    // is hunting for the subject.
    LaunchedEffect(roundIndex) {
        if (phase != Phase.PLAYING || current == null) return@LaunchedEffect
        val durMs = if (carryMillis > 0L) carryMillis else timeForRound(streak) * 1000L
        carryMillis = 0L
        roundEndMillis = Clock.System.now().toEpochMilliseconds() + durMs
        while (phase == Phase.PLAYING) {
            val remMs = roundEndMillis - Clock.System.now().toEpochMilliseconds()
            timeLeft = ((remMs + 999L) / 1000L).toInt().coerceAtLeast(0)
            if (remMs <= 0L) {
                phase = Phase.GAME_OVER
                break
            }
            delay(100L)
        }
    }

    // Commit best streak + sound on game over
    LaunchedEffect(phase) {
        if (phase == Phase.GAME_OVER) {
            if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.GameEnd)
            val storedBest = AppSettings.getInt(BEST_STREAK_KEY, 0)
            if (streak > storedBest) {
                AppSettings.setInt(BEST_STREAK_KEY, streak)
                isNewRecord = true
            }
        }
    }

    fun onHit(rating: Int) {
        lastRating = rating
        streak += 1
        if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.Capture)
        nextCategory()
        carryMillis = 0L
        phase = Phase.PLAYING
        roundIndex += 1
    }

    fun onMiss(rating: Int) {
        lastRating = rating
        if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.PhotoRejected)
        phase = Phase.GAME_OVER
    }

    // AI evaluation is mandatory: a round only counts once the model has actually
    // rated the photo. If the call fails (network / crash) we must NOT award it —
    // otherwise any photo would pass without being judged. Hand the round back
    // with the time that was left so a flaky network costs a retake, not the streak.
    fun onValidationError() {
        gameState.ui.pendingToast = S.current.endlessValidationFailed
        if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.PhotoRejected)
        phase = Phase.PLAYING
        roundIndex += 1
    }

    fun validate(cat: Category, bytes: ByteArray) {
        val handler = kotlinx.coroutines.CoroutineExceptionHandler { _, t ->
            AppLogger.w("SoloEndless", "Validation coroutine escaped: ${t::class.simpleName}", t)
            onValidationError()
        }
        scope.launch(handler) {
            try {
                val result = GameRepository.validateSoloPhoto(
                    imageBytes = bytes,
                    categoryName = cat.name,
                    categoryDescription = cat.description,
                )
                if (!result.safe) {
                    // Moderation rejected the shot — let the player retake with
                    // their remaining time instead of ending the run.
                    gameState.ui.pendingToast = S.current.imageRejectedByModeration
                    if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.PhotoRejected)
                    phase = Phase.PLAYING
                    roundIndex += 1
                    return@launch
                }
                if (result.rating >= PASS_THRESHOLD) onHit(result.rating) else onMiss(result.rating)
            } catch (t: Throwable) {
                AppLogger.w("SoloEndless", "Validation failed: ${t::class.simpleName}", t)
                onValidationError()
            }
        }
    }

    val photoCapturer = rememberPhotoCapturer { bytes ->
        val cat = current
        if (cat != null && bytes != null) {
            phase = Phase.BUSY
            validate(cat, bytes)
        } else {
            // Cancelled — resume the round with the time that was left.
            if (carryMillis > 0L) {
                phase = Phase.PLAYING
                roundIndex += 1
            }
        }
    }

    fun onCaptureClick() {
        if (phase != Phase.PLAYING) return
        // Pause the clock the instant the camera opens.
        carryMillis = (roundEndMillis - Clock.System.now().toEpochMilliseconds()).coerceAtLeast(0L)
        phase = Phase.BUSY
        if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.CategorySelect)
        photoCapturer.launch()
    }

    SystemBackHandler { nav.resetTo(Screen.HOME) }

    val timeColor by animateColorAsState(
        targetValue = when {
            timeLeft <= 5 -> Color(0xFFEF4444)
            timeLeft <= 10 -> Color(0xFFFBBF24)
            else -> EndlessGradient.first()
        },
        animationSpec = tween(300),
        label = "endlessTimeColor",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = EndlessGradient.first(), modifier = Modifier.size(18.dp))
                        AnimatedGradientText(
                            text = S.current.modeEndless,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            gradientColors = EndlessGradient,
                        )
                    }
                },
                actions = {
                    StatChip(Icons.Default.LocalFireDepartment, "$streak", EndlessGradient.first())
                    Spacer(Modifier.width(6.dp))
                    StatChip(Icons.Default.EmojiEvents, "$displayBest", Color(0xFFFBBF24))
                    Spacer(Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Countdown
                Text(
                    "$timeLeft",
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                    fontSize = 72.sp,
                    color = timeColor,
                )
                Text(
                    S.current.endlessStreak + ": $streak",
                    style = MaterialTheme.typography.titleMedium,
                    color = ColorOnSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(Modifier.height(32.dp))

                // Current category card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(ColorSurface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            current?.name ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnSurface,
                            textAlign = TextAlign.Center,
                        )
                        val desc = current?.description
                        if (!desc.isNullOrBlank()) {
                            Text(
                                desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ColorOnSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))

                // Capture button
                CaptureButton(busy = phase == Phase.BUSY, onClick = ::onCaptureClick)
            }

            // Game-over overlay
            AnimatedVisibility(visible = phase == Phase.GAME_OVER, enter = fadeIn(), exit = fadeOut()) {
                GameOverOverlay(
                    streak = streak,
                    best = displayBest,
                    isNewRecord = isNewRecord,
                    onTryAgain = { startRun() },
                    onHome = { nav.resetTo(Screen.HOME) },
                )
            }
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ColorSurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ColorOnSurface)
    }
}

@Composable
private fun CaptureButton(busy: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "captureGlow")
    val glow by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "captureGlowScale",
    )
    Box(
        modifier = Modifier
            .size(104.dp)
            .graphicsLayer { if (!busy) { scaleX = glow; scaleY = glow } }
            .clip(CircleShape)
            .background(Brush.linearGradient(if (busy) AIGradient else EndlessGradient))
            .clickable(enabled = !busy, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp, color = Color.White)
        } else {
            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
    }
}

@Composable
private fun GameOverOverlay(
    streak: Int,
    best: Int,
    isNewRecord: Boolean,
    onTryAgain: () -> Unit,
    onHome: () -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    val accent = if (isNewRecord) GoldGradient else EndlessGradient

    // One-shot entrance: the card scales + fades in. Gated by reduce-motion (our
    // wasmJs / low-end escape hatch) — when on, it simply renders settled.
    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduceMotion) appear.animateTo(1f, tween(380)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.92f),
                        accent.last().copy(alpha = 0.26f),
                        Color.Black.copy(alpha = 0.94f),
                    )
                )
            )
            // Swallow taps so the paused game underneath can't be poked.
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        GradientBorderCard(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .widthIn(max = 380.dp)
                .graphicsLayer {
                    val s = 0.9f + 0.1f * appear.value
                    scaleX = s
                    scaleY = s
                    alpha = appear.value
                },
            cornerRadius = 28.dp,
            borderColors = accent,
            borderWidth = 1.5.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Streak medal: a gradient disc inside a soft, low-alpha halo.
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(accent.map { it.copy(alpha = 0.18f) })),
                    )
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(accent)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (isNewRecord) Icons.Default.EmojiEvents else Icons.Default.LocalFireDepartment,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                "$streak",
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                                color = Color.White,
                            )
                        }
                    }
                }

                AnimatedGradientText(
                    text = if (isNewRecord) S.current.endlessNewRecord else S.current.endlessRunOver,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    gradientColors = accent,
                )
                Text(
                    S.current.endlessYouReached(streak),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )

                // Best-streak pill. On a fresh record this equals the streak just
                // set, so it doubles as the celebratory headline stat.
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                    Text(
                        "${S.current.endlessBest}: $best",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Primary action. Static gradient (no per-frame animation) so the
                // overlay stays cheap and respects reduce-motion.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(EndlessGradient))
                        .clickable(onClick = onTryAgain)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Replay, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text(S.current.endlessTryAgain, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onHome)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(S.current.close, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Celebrate a personal best with confetti. Auto-skips under reduce-motion.
        ConfettiEffect(trigger = isNewRecord, modifier = Modifier.fillMaxSize())
    }
}
