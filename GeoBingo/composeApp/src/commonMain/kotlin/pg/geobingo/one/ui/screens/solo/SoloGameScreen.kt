package pg.geobingo.one.ui.screens.solo

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
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
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.platform.SoundPlayer
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.util.AppLogger

private val SoloGradient = listOf(Color(0xFF22D3EE), Color(0xFF6366F1))
private val AIGradient = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloGameScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val solo = gameState.solo
    val scope = rememberCoroutineScope()
    var pendingCategoryId by remember { mutableStateOf<String?>(null) }

    val photoCapturer = rememberPhotoCapturer { bytes ->
        if (bytes != null && pendingCategoryId != null) {
            val catId = pendingCategoryId!!
            if (catId !in solo.capturedCategories) {
                solo.capturedCategories = solo.capturedCategories + catId
                solo.captureTimestamps = solo.captureTimestamps + (catId to kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
                if (gameState.ui.soundEnabled) SoundPlayer.playCapture()

                val category = solo.categories.find { it.id == catId }
                if (category != null) {
                    solo.validatingCategories = solo.validatingCategories + catId
                    scope.launch {
                        try {
                            val result = GameRepository.validateSoloPhoto(
                                imageBytes = bytes,
                                categoryName = category.name,
                                categoryDescription = category.description,
                            )
                            solo.categoryRatings = solo.categoryRatings + (catId to result.rating)
                            solo.categoryReasons = solo.categoryReasons + (catId to result.reason)
                        } catch (e: Exception) {
                            AppLogger.w("SoloGame", "Photo validation failed", e)
                            solo.categoryRatings = solo.categoryRatings + (catId to 5)
                            solo.categoryReasons = solo.categoryReasons + (catId to "")
                        } finally {
                            solo.validatingCategories = solo.validatingCategories - catId
                        }
                    }
                }
            }
            pendingCategoryId = null
        }
    }

    // Timer
    LaunchedEffect(solo.isRunning) {
        if (!solo.isRunning) return@LaunchedEffect
        solo.startTimeMillis = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        while (solo.isRunning && solo.timeRemainingSeconds > 0) {
            delay(1000L)
            solo.timeRemainingSeconds--
            val t = solo.timeRemainingSeconds
            if (gameState.ui.soundEnabled && (t == 60 || t == 30 || t == 10)) {
                SoundPlayer.playTimerWarning()
            }
        }
        if (solo.isRunning) {
            solo.isRunning = false
            if (gameState.ui.soundEnabled) SoundPlayer.playGameEnd()
            nav.replaceCurrent(Screen.SOLO_RESULTS)
        }
    }

    // Check all captured – wait for all validations to finish
    LaunchedEffect(solo.capturedCategories, solo.validatingCategories) {
        if (solo.capturedCategories.size == solo.categories.size
            && solo.categories.isNotEmpty()
            && solo.isRunning
            && solo.validatingCategories.isEmpty()
        ) {
            solo.isRunning = false
            if (gameState.ui.soundEnabled) SoundPlayer.playSuccess()
            nav.replaceCurrent(Screen.SOLO_RESULTS)
        }
    }

    SystemBackHandler {
        solo.reset()
        nav.resetTo(Screen.HOME)
    }

    val minutes = solo.timeRemainingSeconds / 60
    val seconds = solo.timeRemainingSeconds % 60
    val timeText = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    val isLow = solo.timeRemainingSeconds <= 30
    val isCritical = solo.timeRemainingSeconds <= 10
    val timeColor by animateColorAsState(
        targetValue = when {
            isCritical -> Color(0xFFEF4444)
            isLow -> Color(0xFFFBBF24)
            else -> SoloGradient.first()
        },
        animationSpec = tween(400),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = AIGradient.first(), modifier = Modifier.size(18.dp))
                        AnimatedGradientText(
                            text = "KatchIt! AI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            gradientColors = AIGradient,
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = timeColor, modifier = Modifier.size(20.dp))
                        Text(
                            timeText,
                            style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.sp),
                            fontWeight = FontWeight.Bold,
                            color = timeColor,
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
                .padding(horizontal = 16.dp),
        ) {
            // Progress bar with AI branding
            val captured = solo.capturedCategories.size
            val total = solo.categories.size
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "$captured / $total",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnSurface,
                )
                // Animated AI score
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                    AnimatedContent(
                        targetState = solo.ratingScore,
                        transitionSpec = {
                            (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f))
                                .togetherWith(fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 1.2f))
                        },
                        label = "scoreAnim",
                    ) { score ->
                        Text(
                            "$score ${S.current.pointsAbbrev}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = SoloGradient.first(),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Gradient progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ColorSurfaceVariant),
            ) {
                val progress = if (total > 0) captured.toFloat() / total else 0f
                val animatedProgress = remember { Animatable(0f) }
                LaunchedEffect(progress) { animatedProgress.animateTo(progress, tween(500)) }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress.value)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.linearGradient(SoloGradient)),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Category grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(solo.categories) { category ->
                    val isCaptured = category.id in solo.capturedCategories
                    val isValidating = category.id in solo.validatingCategories
                    val rating = solo.categoryRatings[category.id]
                    SoloCategoryCard(
                        name = category.name,
                        isCaptured = isCaptured,
                        isValidating = isValidating,
                        rating = rating,
                        speed = if (isCaptured) solo.getCaptureSpeed(category.id) else null,
                        onClick = {
                            if (!isCaptured) {
                                pendingCategoryId = category.id
                                photoCapturer.launch()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SoloCategoryCard(
    name: String,
    isCaptured: Boolean,
    isValidating: Boolean,
    rating: Int?,
    speed: Int?,
    onClick: () -> Unit,
) {
    // Animate card appearance on capture
    val scale = remember { Animatable(1f) }
    LaunchedEffect(isCaptured) {
        if (isCaptured) {
            scale.animateTo(0.92f, tween(100))
            scale.animateTo(1.05f, tween(200))
            scale.animateTo(1f, tween(150))
        }
    }

    // Rating reveal animation
    val ratingAlpha = remember { Animatable(0f) }
    LaunchedEffect(rating) {
        if (rating != null) {
            ratingAlpha.animateTo(1f, tween(400))
        }
    }

    val ratingColor = when {
        rating == null -> ColorOnSurfaceVariant
        rating >= 8 -> Color(0xFF22C55E)
        rating >= 5 -> Color(0xFFFBBF24)
        else -> Color(0xFFEF4444)
    }

    val borderColors = when {
        isValidating -> AIGradient
        rating != null && rating >= 8 -> listOf(Color(0xFF22C55E), Color(0xFF10B981))
        rating != null && rating >= 5 -> listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
        rating != null -> listOf(Color(0xFFEF4444), Color(0xFFDC2626))
        isCaptured -> SoloGradient
        else -> listOf(ColorOutlineVariant, ColorOutlineVariant)
    }

    // Pulsing border animation for validating state
    val infiniteTransition = rememberInfiniteTransition(label = "cardPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    val borderAlpha = if (isValidating) pulseAlpha else 1f

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCaptured) ColorSurface else ColorSurface,
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCaptured) 1.5.dp else 1.dp,
            brush = Brush.linearGradient(borderColors.map { it.copy(alpha = borderAlpha) }),
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle gradient background for captured cards
            if (isCaptured && rating != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ratingColor.copy(alpha = 0.06f),
                                    Color.Transparent,
                                )
                            )
                        ),
                )
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedContent(
                    targetState = Triple(isCaptured, isValidating, rating),
                    transitionSpec = {
                        (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.85f))
                            .togetherWith(fadeOut(tween(200)))
                    },
                    label = "cardContent",
                ) { (captured, validating, rtg) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (!captured) {
                            // Uncaptured state
                            Icon(Icons.Default.CameraAlt, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = ColorOnSurface,
                                textAlign = TextAlign.Center,
                            )
                        } else if (validating) {
                            // AI validating state
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AIGradient.first(),
                                )
                                AnimatedGradientText(
                                    text = "AI",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    gradientColors = AIGradient,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SoloGradient.first(),
                                textAlign = TextAlign.Center,
                            )
                        } else if (rtg != null) {
                            // Rating revealed
                            val rc = when {
                                rtg >= 8 -> Color(0xFF22C55E)
                                rtg >= 5 -> Color(0xFFFBBF24)
                                else -> Color(0xFFEF4444)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(Icons.Default.Star, null, tint = rc, modifier = Modifier.size(18.dp))
                                Text(
                                    "$rtg",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = rc,
                                )
                                Text(
                                    "/10",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = rc.copy(alpha = 0.6f),
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorOnSurface,
                                textAlign = TextAlign.Center,
                            )
                            if (speed != null) {
                                Text("${speed}s", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
