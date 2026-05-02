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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.platform.SoundEffect
import pg.geobingo.one.platform.SoundPlayer
import pg.geobingo.one.platform.play
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.ui.components.MiniShopPopup
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.util.AppLogger

private val SoloGradient = listOf(Color(0xFF22D3EE), Color(0xFF6366F1))
private val AIGradient = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))

private const val EXTRA_TIME_COST = 15
private const val RETAKE_COST = 5
private const val SWAP_COST = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloGameScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val solo = gameState.solo
    val scope = rememberCoroutineScope()
    var pendingCategoryId by remember { mutableStateOf<String?>(null) }
    var isRetake by remember { mutableStateOf(false) }

    // Mini shop popup state
    var showMiniShop by remember { mutableStateOf(false) }
    var miniShopNeeded by remember { mutableStateOf(0) }

    // Swap category dialog
    var showSwapDialog by remember { mutableStateOf<String?>(null) }

    // AI consent state
    var aiConsentAccepted by remember { mutableStateOf(AppSettings.getBoolean(SettingsKeys.AI_CONSENT_ACCEPTED, false)) }
    var showAiConsentDialog by remember { mutableStateOf(false) }
    // Buffer photo bytes + catId while waiting for consent

    fun trySpend(cost: Int, onSuccess: () -> Unit) {
        if (gameState.stars.spend(cost)) {
            onSuccess()
        } else {
            miniShopNeeded = cost
            showMiniShop = true
        }
    }

    // Shared validation logic to avoid duplicating the API call code.
    // Catches Throwable (not just Exception) because Kotlin/Native can bridge
    // ObjC NSExceptions and other non-Exception Throwables that would otherwise
    // crash the app as "unhandled coroutine exception". The extra
    // CoroutineExceptionHandler is a second-line defense for anything that
    // escapes even the Throwable catch.
    fun validatePhoto(catId: String, bytes: ByteArray, fallbackOnError: Boolean) {
        val category = solo.categories.find { it.id == catId } ?: return
        solo.validatingCategories = solo.validatingCategories + catId
        solo.captureTimestamps = solo.captureTimestamps + (catId to kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
        val handler = kotlinx.coroutines.CoroutineExceptionHandler { _, t ->
            AppLogger.w("SoloGame", "Photo validation coroutine escaped: ${t::class.simpleName}", t)
            solo.validatingCategories = solo.validatingCategories - catId
            if (fallbackOnError) {
                solo.categoryRatings = solo.categoryRatings + (catId to 5)
                solo.categoryReasons = solo.categoryReasons + (catId to "")
            }
        }
        scope.launch(handler) {
            try {
                val result = GameRepository.validateSoloPhoto(
                    imageBytes = bytes,
                    categoryName = category.name,
                    categoryDescription = category.description,
                )
                // Server-side moderation flagged the photo as unsafe — undo
                // the capture, clear the stored photo, and surface a toast.
                if (!result.safe) {
                    solo.capturedCategories = solo.capturedCategories - catId
                    solo.categoryRatings = solo.categoryRatings - catId
                    solo.categoryReasons = solo.categoryReasons - catId
                    solo.captureTimestamps = solo.captureTimestamps - catId
                    gameState.ui.pendingToast = pg.geobingo.one.i18n.S.current.imageRejectedByModeration
                    if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.PhotoRejected)
                    return@launch
                }
                solo.categoryRatings = solo.categoryRatings + (catId to result.rating)
                solo.categoryReasons = solo.categoryReasons + (catId to result.reason)
                if (gameState.ui.soundEnabled) {
                    if (result.rating >= 3) SoundPlayer.play(SoundEffect.PhotoValidated)
                    else SoundPlayer.play(SoundEffect.PhotoRejected)
                }
            } catch (t: Throwable) {
                AppLogger.w("SoloGame", "Photo validation failed: ${t::class.simpleName}", t)
                if (fallbackOnError) {
                    solo.categoryRatings = solo.categoryRatings + (catId to 5)
                    solo.categoryReasons = solo.categoryReasons + (catId to "")
                }
            } finally {
                solo.validatingCategories = solo.validatingCategories - catId
            }
        }
    }

    // Consent is resolved at screen entry via the LaunchedEffect below, so
    // by the time a photo is captured the user has either accepted (use AI)
    // or declined (use a default rating). No more mid-game dialog.
    fun handleCapturedPhoto(catId: String, bytes: ByteArray, retake: Boolean) {
        if (retake) {
            if (aiConsentAccepted) {
                validatePhoto(catId, bytes, fallbackOnError = false)
            }
        } else if (catId !in solo.capturedCategories) {
            solo.capturedCategories = solo.capturedCategories + catId
            if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.Capture)
            if (aiConsentAccepted) {
                validatePhoto(catId, bytes, fallbackOnError = true)
            } else {
                solo.categoryRatings = solo.categoryRatings + (catId to 3)
                solo.categoryReasons = solo.categoryReasons + (catId to "")
            }
        }
    }

    // Ask for AI consent on screen entry, before any photo is taken. This
    // both matches the DSGVO expectation (consent before processing) and
    // avoids the Compose iOS crash where a Dialog mount raced the
    // UIImagePickerController dismiss animation.
    LaunchedEffect(Unit) {
        if (!aiConsentAccepted) {
            showAiConsentDialog = true
        }
    }

    val photoCapturer = rememberPhotoCapturer { bytes ->
        val catId = pendingCategoryId
        if (catId != null) {
            if (bytes != null) {
                handleCapturedPhoto(catId, bytes, isRetake)
            } else {
                AppLogger.w("SoloGame", "Photo capture returned null bytes for catId=$catId")
                gameState.ui.pendingToast = pg.geobingo.one.i18n.S.current.uploadFailed
            }
            isRetake = false
            pendingCategoryId = null
        }
    }

    // Timer — uses wall-clock to avoid drift from imprecise delay()
    // endTimeMillis is mutable so extra-time power-ups can extend it
    var endTimeMillis by remember { mutableStateOf(0L) }
    LaunchedEffect(solo.isRunning) {
        if (!solo.isRunning) return@LaunchedEffect
        // On a fresh run start startTimeMillis is 0 → stamp it with now.
        // On a rejoin the restored value points to the ORIGINAL start so
        // capture-speed calculations stay correct; don't overwrite it.
        if (solo.startTimeMillis == 0L) {
            solo.startTimeMillis = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        }
        endTimeMillis = solo.startTimeMillis + solo.totalDurationSeconds * 1000L
        while (solo.isRunning && solo.timeRemainingSeconds > 0) {
            delay(250L)
            val remaining = ((endTimeMillis - kotlinx.datetime.Clock.System.now().toEpochMilliseconds()) / 1000).toInt().coerceAtLeast(0)
            val prev = solo.timeRemainingSeconds
            solo.timeRemainingSeconds = remaining
            // Timer warning sounds at threshold crossings
            if (gameState.ui.soundEnabled) {
                for (threshold in listOf(60, 30, 10)) {
                    if (prev > threshold && remaining <= threshold) {
                        SoundPlayer.play(SoundEffect.TimerWarning)
                    }
                }
            }
        }
        if (solo.isRunning) {
            solo.isRunning = false
            if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.GameEnd)
            nav.replaceCurrent(Screen.SOLO_RESULTS)
        }
    }

    // Persist a snapshot so the user can rejoin the same run if the app is
    // killed / they switch apps. Keyed on capture + ratings so every scoring
    // change triggers a new write.
    LaunchedEffect(solo.capturedCategories, solo.categoryRatings, solo.isRunning) {
        if (solo.isRunning) pg.geobingo.one.game.ActiveSession.saveSolo(solo)
    }

    // Check all captured
    LaunchedEffect(solo.capturedCategories, solo.validatingCategories) {
        if (solo.capturedCategories.size == solo.categories.size
            && solo.categories.isNotEmpty()
            && solo.isRunning
            && solo.validatingCategories.isEmpty()
        ) {
            solo.isRunning = false
            if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.Success)
            nav.replaceCurrent(Screen.SOLO_RESULTS)
        }
    }

    SystemBackHandler {
        // Manual leave → discard the rejoin snapshot; user chose to abandon.
        pg.geobingo.one.game.ActiveSession.clearSolo()
        solo.reset()
        nav.resetTo(Screen.HOME)
    }

    // Timer display values derived without triggering parent recomposition
    val timeRemainingSeconds = solo.timeRemainingSeconds
    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val timeText = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    val isLow = timeRemainingSeconds <= 30
    val isCritical = timeRemainingSeconds <= 10
    val timeColor by animateColorAsState(
        targetValue = when {
            isCritical -> Color(0xFFEF4444)
            isLow -> Color(0xFFFBBF24)
            else -> SoloGradient.first()
        },
        animationSpec = tween(400),
    )

    // Mini shop popup
    if (showMiniShop) {
        MiniShopPopup(
            gameState = gameState,
            neededStars = miniShopNeeded,
            onDismiss = { showMiniShop = false },
            onPurchased = { showMiniShop = false },
        )
    }

    // AI consent dialog
    if (showAiConsentDialog) {
        AlertDialog(
            onDismissRequest = { /* Block outside-tap dismiss — force explicit choice */ },
            icon = { Icon(Icons.Default.PhotoCamera, null, tint = AIGradient.first(), modifier = Modifier.size(28.dp)) },
            title = { Text(S.current.aiConsentTitle, fontWeight = FontWeight.Bold) },
            text = { pg.geobingo.one.ui.components.AiConsentDialogText() },
            confirmButton = {
                TextButton(onClick = {
                    AppSettings.setBoolean(SettingsKeys.AI_CONSENT_ACCEPTED, true)
                    aiConsentAccepted = true
                    showAiConsentDialog = false
                }) {
                    Text(S.current.aiConsentAccept)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Decline: keep aiConsentAccepted=false; photos get default rating.
                    // Not persisted so we can ask again next session.
                    showAiConsentDialog = false
                }) {
                    Text(S.current.aiConsentDecline)
                }
            },
        )
    }

    // Swap category confirmation dialog
    showSwapDialog?.let { catId ->
        val category = solo.categories.find { it.id == catId }
        AlertDialog(
            onDismissRequest = { showSwapDialog = null },
            icon = { Icon(Icons.Default.SwapHoriz, null, tint = SoloGradient.first(), modifier = Modifier.size(28.dp)) },
            title = { Text(S.current.swapCategory, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    S.current.swapCategoryCost(SWAP_COST),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorOnSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    trySpend(SWAP_COST) {
                        // Replace the category with a fresh one from the pool
                        val pool = pg.geobingo.one.data.soloCategories(solo.isOutdoor, solo.categories.size + 5)
                        val existingIds = solo.categories.map { it.id }.toSet()
                        val replacement = pool.firstOrNull { it.id !in existingIds }
                        if (replacement != null) {
                            solo.categories = solo.categories.map { if (it.id == catId) replacement else it }
                            if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.PowerUp)
                        }
                    }
                    showSwapDialog = null
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp), tint = Color(0xFFFBBF24))
                        Text("$SWAP_COST ${S.current.stars}")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwapDialog = null }) { Text(S.current.cancel) }
            },
        )
    }

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
                    // Stars display
                    pg.geobingo.one.ui.components.StarsChip(
                        count = gameState.stars.starCount,
                        onClick = { miniShopNeeded = 0; showMiniShop = true },
                    )
                    Spacer(Modifier.width(4.dp))
                    // Timer
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
            // Progress + score + power-up buttons
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
                // Power-up: Extra Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Extra time button
                    SuggestionChip(
                        onClick = {
                            trySpend(EXTRA_TIME_COST) {
                                solo.timeRemainingSeconds += 60
                                endTimeMillis += 60_000L
                                gameState.ui.pendingToast = "+60s"
                                if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.PowerUp)
                            }
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.AddAlarm, null, modifier = Modifier.size(14.dp), tint = SoloGradient.first())
                                Text("+60s", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        },
                        icon = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                Icon(Icons.Default.Star, null, modifier = Modifier.size(10.dp), tint = Color(0xFFFBBF24))
                                Text("$EXTRA_TIME_COST", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFFFBBF24))
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                    )
                    // Score display
                    AnimatedContent(
                        targetState = solo.starScore,
                        transitionSpec = {
                            (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f))
                                .togetherWith(fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 1.2f))
                        },
                        label = "scoreAnim",
                    ) { score ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                            Text(
                                "$score ${S.current.pointsAbbrev}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = SoloGradient.first(),
                                fontWeight = FontWeight.Bold,
                            )
                        }
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
                            if (!isCaptured && !isValidating) {
                                if (gameState.ui.soundEnabled) SoundPlayer.play(SoundEffect.CategorySelect)
                                pendingCategoryId = category.id
                                isRetake = false
                                photoCapturer.launch()
                            }
                        },
                        onRetake = if (isCaptured && rating != null && rating <= 2 && !isValidating) {
                            {
                                trySpend(RETAKE_COST) {
                                    pendingCategoryId = category.id
                                    isRetake = true
                                    photoCapturer.launch()
                                }
                            }
                        } else null,
                        onSwap = if (!isCaptured && !isValidating) {
                            { showSwapDialog = category.id }
                        } else null,
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
    onRetake: (() -> Unit)? = null,
    onSwap: (() -> Unit)? = null,
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(isCaptured) {
        if (isCaptured) {
            scale.animateTo(0.92f, tween(100))
            scale.animateTo(1.05f, tween(200))
            scale.animateTo(1f, tween(150))
        }
    }

    val ratingColor = when {
        rating == null -> ColorOnSurfaceVariant
        rating >= 4 -> Color(0xFF22C55E)
        rating >= 3 -> Color(0xFFFBBF24)
        else -> Color(0xFFEF4444)
    }

    val borderColors = when {
        isValidating -> AIGradient
        rating != null && rating >= 4 -> listOf(Color(0xFF22C55E), Color(0xFF10B981))
        rating != null && rating >= 3 -> listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
        rating != null -> listOf(Color(0xFFEF4444), Color(0xFFDC2626))
        isCaptured -> SoloGradient
        else -> listOf(ColorOutlineVariant, ColorOutlineVariant)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cardPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer",
    )

    val borderAlpha = if (isValidating) pulseAlpha else 1f

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCaptured) 1.5.dp else 1.dp,
            brush = Brush.linearGradient(borderColors.map { it.copy(alpha = borderAlpha) }),
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isCaptured && rating != null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(ratingColor.copy(alpha = 0.06f), Color.Transparent))
                    ),
                )
            }
            if (isValidating) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, AIGradient.first().copy(alpha = 0.08f), AIGradient.last().copy(alpha = 0.08f), Color.Transparent),
                            startX = shimmerOffset * 300f, endX = (shimmerOffset + 1f) * 300f,
                        )
                    ),
                )
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(10.dp),
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
                            Icon(Icons.Default.CameraAlt, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = ColorOnSurface, textAlign = TextAlign.Center)
                        } else if (validating) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = AIGradient.first())
                                AnimatedGradientText(text = "AI", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), gradientColors = AIGradient)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text("Analysiert...", style = MaterialTheme.typography.labelSmall, color = AIGradient.first().copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text(name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = SoloGradient.first(), textAlign = TextAlign.Center)
                        } else if (rtg != null) {
                            val rc = when { rtg >= 4 -> Color(0xFF22C55E); rtg >= 3 -> Color(0xFFFBBF24); else -> Color(0xFFEF4444) }
                            StaggeredStarRating(rating = rtg, color = rc)
                            Spacer(Modifier.height(2.dp))
                            Text(name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = ColorOnSurface, textAlign = TextAlign.Center, maxLines = 2)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (speed != null) Text("${speed}s", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                                Text("AI bewertet", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8B5CF6).copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Retake button for low-rated photos
            if (onRetake != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.9f))
                        .clickable { onRetake() }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(10.dp), tint = Color.White)
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(8.dp), tint = Color(0xFFFBBF24))
                        Text("$RETAKE_COST", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Swap button for uncaptured categories
            if (onSwap != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SoloGradient.first().copy(alpha = 0.9f))
                        .clickable { onSwap() }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(10.dp), tint = Color.White)
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(8.dp), tint = Color(0xFFFBBF24))
                        Text("$SWAP_COST", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StaggeredStarRating(rating: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(5) { i ->
            val starAlpha = remember { Animatable(0f) }
            val starScale = remember { Animatable(0.5f) }
            LaunchedEffect(Unit) {
                delay(i * 80L)
                launch { starAlpha.animateTo(1f, tween(200)) }
                launch { starScale.animateTo(1.3f, tween(150)); starScale.animateTo(1f, tween(100)) }
            }
            Icon(
                if (i < rating) Icons.Default.Star else Icons.Default.StarBorder,
                null,
                tint = if (i < rating) color.copy(alpha = starAlpha.value) else color.copy(alpha = 0.3f * starAlpha.value),
                modifier = Modifier.size(14.dp).graphicsLayer { scaleX = starScale.value; scaleY = starScale.value },
            )
        }
    }
}
