package pg.geobingo.one.ui.screens.solo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.SoundPlayer
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.util.Analytics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloGameScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val solo = gameState.solo
    var pendingCategoryId by remember { mutableStateOf<String?>(null) }

    val photoCapturer = rememberPhotoCapturer { bytes ->
        if (bytes != null && pendingCategoryId != null) {
            val catId = pendingCategoryId!!
            if (catId !in solo.capturedCategories) {
                solo.capturedCategories = solo.capturedCategories + catId
                solo.captureTimestamps = solo.captureTimestamps + (catId to kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
                if (gameState.ui.soundEnabled) SoundPlayer.playCapture()
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
            // Warning sounds
            val t = solo.timeRemainingSeconds
            if (gameState.ui.soundEnabled && (t == 60 || t == 30 || t == 10)) {
                SoundPlayer.playTimerWarning()
            }
        }
        // Time's up or all captured
        if (solo.isRunning) {
            solo.isRunning = false
            if (gameState.ui.soundEnabled) SoundPlayer.playGameEnd()
            nav.replaceCurrent(Screen.SOLO_RESULTS)
        }
    }

    // Check all captured
    LaunchedEffect(solo.capturedCategories) {
        if (solo.capturedCategories.size == solo.categories.size && solo.categories.isNotEmpty() && solo.isRunning) {
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
    val timeColor = when {
        solo.timeRemainingSeconds <= 10 -> Color(0xFFEF4444)
        solo.timeRemainingSeconds <= 30 -> Color(0xFFFBBF24)
        else -> ColorPrimary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.soloMode,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        gradientColors = GradientPrimary,
                    )
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
                            style = MaterialTheme.typography.titleMedium,
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
            // Progress
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
                Text(
                    "${solo.capturedCategories.size * solo.basePointsPerCategory} ${S.current.pointsAbbrev}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            LinearProgressIndicator(
                progress = { if (total > 0) captured.toFloat() / total else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = ColorPrimary,
                trackColor = ColorSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            // Category grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(solo.categories) { category ->
                    val isCaptured = category.id in solo.capturedCategories
                    SoloCategoryCard(
                        name = category.name,
                        isCaptured = isCaptured,
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
    speed: Int?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCaptured) ColorPrimary.copy(alpha = 0.12f) else ColorSurface,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCaptured) ColorPrimary.copy(alpha = 0.4f) else ColorOutlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isCaptured) {
                Icon(Icons.Default.Check, contentDescription = null, tint = ColorPrimary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(4.dp))
                Text(name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = ColorPrimary)
                if (speed != null) {
                    Text("${speed}s", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                }
            } else {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(4.dp))
                Text(name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = ColorOnSurface)
            }
        }
    }
}
