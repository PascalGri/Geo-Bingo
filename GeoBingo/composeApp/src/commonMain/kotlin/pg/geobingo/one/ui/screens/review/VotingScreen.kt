package pg.geobingo.one.ui.screens.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.data.Category
import pg.geobingo.one.data.Player
import pg.geobingo.one.network.GameRepository
import pg.geobingo.one.network.ModerationManager
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.platform.SoundEffect
import pg.geobingo.one.platform.SoundPlayer
import pg.geobingo.one.platform.play

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DarkSinglePhotoVotingScreen(
    gameId: String, currentCategory: Category, categoryIndex: Int, totalCategories: Int,
    targetPlayer: Player, targetPlayerIndex: Int, totalPlayers: Int, stepIndex: Int,
    playerAvatarBytes: ByteArray? = null,
    hapticEnabled: Boolean = true,
    soundEnabled: Boolean = true,
    teamName: String? = null,
    modeGradient: List<Color> = GradientPrimary,
    onVote: (Int) -> Unit, onNoPhoto: () -> Unit
) {
    var photo by remember(stepIndex) { mutableStateOf<ImageBitmap?>(null) }
    var photoLoading by remember(stepIndex) { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Floating reaction state
    var floatingReaction by remember(stepIndex) { mutableStateOf<String?>(null) }

    // Star rating state
    var selectedRating by remember(stepIndex) { mutableStateOf(0) }
    var submitted by remember(stepIndex) { mutableStateOf(false) }

    // Star scale animations (one per star)
    val starScales = remember(stepIndex) { List(5) { Animatable(1f) } }
    // Submission animation
    val submitScale = remember(stepIndex) { Animatable(1f) }
    val submitAlpha = remember(stepIndex) { Animatable(1f) }

    // Photo-report dialog state — required by App-Store Guideline 1.2 for
    // every piece of user-generated content visible to other users.
    var showReportDialog by remember(stepIndex) { mutableStateOf(false) }
    var photoReportToast by remember(stepIndex) { mutableStateOf<String?>(null) }
    if (photoReportToast != null) {
        LaunchedEffect(photoReportToast) {
            delay(2200L)
            photoReportToast = null
        }
    }

    // Slide-in from right per step
    val stepAlpha = remember(stepIndex) { Animatable(0f) }
    val stepSlideX = remember(stepIndex) { Animatable(300f) }
    LaunchedEffect(stepIndex) {
        launch { stepSlideX.animateTo(0f, tween(350)) }
        stepAlpha.animateTo(1f, tween(350))
    }

    LaunchedEffect(stepIndex) {
        photoLoading = true
        val bytes = GameRepository.downloadPhoto(gameId, targetPlayer.id, currentCategory.id)
        photo = if (bytes != null) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { bytes.toImageBitmap() } else null
        photoLoading = false
        if (photo == null) { delay(2500); onNoPhoto() }
    }

    fun animateStarSelection(rating: Int) {
        scope.launch {
            for (i in 0 until 5) {
                launch {
                    if (i < rating) {
                        delay(i * 60L)
                        starScales[i].animateTo(1.4f, tween(100))
                        starScales[i].animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                    } else {
                        starScales[i].snapTo(1f)
                    }
                }
            }
        }
    }

    fun animateSubmission(rating: Int) {
        submitted = true
        scope.launch {
            // All stars pulse together
            for (i in 0 until rating) {
                launch {
                    delay(i * 40L)
                    starScales[i].animateTo(1.3f, tween(120))
                    starScales[i].animateTo(1f, tween(100))
                }
            }
            delay(300)
            // Fade out rating section
            launch { submitScale.animateTo(0.8f, tween(200)) }
            submitAlpha.animateTo(0f, tween(200))
            onVote(rating)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        AnimatedGradientText(
                            text = S.current.voting,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            gradientColors = modeGradient,
                        )
                        val entityLabel = if (teamName != null) S.current.teamOfTotal(targetPlayerIndex + 1, totalPlayers)
                            else S.current.playerOfTotal(targetPlayerIndex + 1, totalPlayers)
                        Text("${S.current.categoryOfTotal(categoryIndex + 1, totalCategories)} • $entityLabel", style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.screenHorizontal).graphicsLayer { alpha = stepAlpha.value; translationX = stepSlideX.value }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientBorderCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, borderColors = modeGradient, backgroundColor = ColorPrimaryContainer) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedGradientText(text = currentCategory.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), gradientColors = modeGradient)
                    Text(S.current.howWellDoesItFit, style = MaterialTheme.typography.bodySmall, color = ColorOnPrimaryContainer.copy(alpha = 0.7f))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerAvatarView(player = targetPlayer, size = 36.dp, fontSize = 14.sp, photoBytes = playerAvatarBytes)
                Spacer(Modifier.width(8.dp))
                if (teamName != null) {
                    Column {
                        Text(teamName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = ColorOnSurface)
                        Text(S.current.capturedBy(targetPlayer.name), style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                    }
                } else {
                    Text(targetPlayer.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = ColorOnSurface)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(ColorSurfaceVariant), contentAlignment = Alignment.Center) {
                    if (photoLoading) ShimmerPlaceholder(modifier = Modifier.fillMaxSize(), cornerRadius = 14.dp)
                    else if (photo != null) Image(bitmap = photo ?: return@Box, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(40.dp), tint = ColorOnSurfaceVariant)
                        Text(S.current.noPhotoFound, style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant)
                    }
                }
                // Report-photo flag (top-right overlay). Visible whenever a photo is
                // loaded. Tapping opens a confirm dialog → writes to content_reports.
                if (!photoLoading && photo != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { showReportDialog = true }
                            .padding(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = S.current.reportPhoto,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                // Floating reaction overlay
                if (floatingReaction != null) {
                    Text(
                        text = floatingReaction ?: "",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(
                                color = Color.Black.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }
            // Reaction chips row
            if (!photoLoading && photo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    val reactionItems = listOf(
                        S.current.niceShot,
                        S.current.funny,
                        S.current.wow,
                    )
                    reactionItems.forEach { label ->
                        OutlinedButton(
                            onClick = {
                                floatingReaction = label
                                scope.launch {
                                    delay(1500)
                                    floatingReaction = null
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                modeGradient.first().copy(alpha = 0.4f),
                            ),
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = modeGradient.first(),
                            )
                        }
                    }
                }
            }
            if (!photoLoading && photo == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).background(ColorError.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(S.current.noSubmissionSkipping, color = ColorError, fontWeight = FontWeight.Bold)
                }
            } else {
                // Star rating section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = submitScale.value; scaleY = submitScale.value; alpha = submitAlpha.value },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Star row with drag support
                    val starPositions = remember { FloatArray(5) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(submitted) {
                                if (submitted) return@pointerInput
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        val newRating = starPositions.indexOfLast { offset.x >= it }.coerceIn(0, 4) + 1
                                        if (newRating != selectedRating) {
                                            selectedRating = newRating
                                            animateStarSelection(newRating)
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (soundEnabled) SoundPlayer.play(SoundEffect.Tap)
                                        }
                                    },
                                    onHorizontalDrag = { change, _ ->
                                        change.consume()
                                        val x = change.position.x
                                        val newRating = (starPositions.indexOfLast { x >= it }.coerceIn(0, 4) + 1)
                                        if (newRating != selectedRating) {
                                            selectedRating = newRating
                                            animateStarSelection(newRating)
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (soundEnabled) SoundPlayer.play(SoundEffect.Tap)
                                        }
                                    },
                                )
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (i in 1..5) {
                            val isSelected = i <= selectedRating
                            val starColor = if (isSelected) Color(0xFFFBBF24) else ColorOnSurfaceVariant.copy(alpha = 0.3f)
                            val glowAlpha = if (isSelected) (starScales[i - 1].value - 1f).coerceIn(0f, 0.4f) + 0.2f else 0f
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "$i Sterne",
                                tint = starColor,
                                modifier = Modifier
                                    .size(48.dp)
                                    .onGloballyPositioned { coords ->
                                        starPositions[i - 1] = coords.positionInParent().x
                                    }
                                    .graphicsLayer { scaleX = starScales[i - 1].value; scaleY = starScales[i - 1].value }
                                    .drawBehind {
                                        if (isSelected) {
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFFFBBF24).copy(alpha = glowAlpha),
                                                        Color.Transparent,
                                                    ),
                                                ),
                                                radius = size.minDimension * 0.8f,
                                            )
                                        }
                                    }
                                    .clickable(enabled = !submitted) {
                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (soundEnabled) SoundPlayer.play(SoundEffect.Tap)
                                        selectedRating = i
                                        animateStarSelection(i)
                                    },
                            )
                        }
                    }
                    // Rating label
                    Text(
                        text = when (selectedRating) {
                            0 -> S.current.tapTheStars
                            1 -> S.current.doesntFitAtAll
                            2 -> S.current.barelyFits
                            3 -> S.current.fitsOkay
                            4 -> S.current.fitsWell
                            5 -> S.current.perfect
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedRating > 0) Color(0xFFFBBF24) else ColorOnSurfaceVariant,
                        fontWeight = if (selectedRating > 0) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    // Confirm button
                    GradientButton(
                        text = S.current.rate,
                        onClick = {
                            if (selectedRating > 0 && !submitted) {
                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (soundEnabled) SoundPlayer.play(SoundEffect.Vote)
                                animateSubmission(selectedRating)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        enabled = selectedRating > 0 && !submitted,
                        gradientColors = modeGradient,
                        leadingIcon = { Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
        }
    }

    // ── Photo report dialog (Guideline 1.2) ────────────────────────────
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = ColorSurface,
            title = {
                Text(S.current.reportPhotoTitle, style = MaterialTheme.typography.titleMedium, color = ColorOnSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(S.current.reportPhotoBody(targetPlayer.name), style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant)
            },
            confirmButton = {
                TextButton(onClick = {
                    showReportDialog = false
                    scope.launch {
                        ModerationManager.reportContent(
                            contentType = "photo",
                            contentId = "${gameId}/${targetPlayer.id}/${currentCategory.id}",
                            contentSnapshot = "player=${targetPlayer.name} category=${currentCategory.name}",
                            reportedUserId = targetPlayer.userId,
                        )
                        photoReportToast = S.current.reportSubmitted
                    }
                }) {
                    Text(S.current.reportMessage, color = ColorError, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text(S.current.cancel, color = ColorOnSurfaceVariant)
                }
            },
        )
    }

    // Toast after report submit.
    photoReportToast?.let { msg ->
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ColorSurface,
                shadowElevation = 6.dp,
            ) {
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}
