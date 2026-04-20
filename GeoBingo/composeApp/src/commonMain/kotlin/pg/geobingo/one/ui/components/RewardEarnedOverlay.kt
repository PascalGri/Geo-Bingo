package pg.geobingo.one.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.state.RewardEvent
import pg.geobingo.one.ui.theme.ConfettiEffect
import pg.geobingo.one.ui.theme.LocalReduceMotion
import kotlin.math.cos
import kotlin.math.sin

private const val DISPLAY_MS = 2200L

// Deep matte purple-to-gold palette — premium feel without the cheap shine.
private val MatteBackdropTop = Color(0xFF1E1033)
private val MatteBackdropBottom = Color(0xFF11091F)
private val MatteAccent = Color(0xFFC9A227)         // rich deep gold
private val MatteAccentDim = Color(0xFF8C6F1A)      // darker gold for rim
private val SparkleColor = Color(0xFFFFE18A)

/**
 * Global "reward earned" overlay shown above everything when gameState.ui.pendingReward
 * is set. A dramatic staged entrance: darkened scrim fades in → reward card
 * drops in with spring scale → sparkle burst → auto-dismiss.
 */
@Composable
fun RewardEarnedOverlay(gameState: GameState) {
    val event = gameState.ui.pendingReward
    val reduceMotion = LocalReduceMotion.current

    LaunchedEffect(event?.triggerKey) {
        if (event == null) return@LaunchedEffect
        delay(DISPLAY_MS)
        if (gameState.ui.pendingReward?.triggerKey == event.triggerKey) {
            gameState.ui.pendingReward = null
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Subtle darkened scrim so the card pops off the background.
        AnimatedVisibility(
            visible = event != null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(220)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
            )
        }

        AnimatedVisibility(
            visible = event != null,
            enter = scaleIn(
                initialScale = if (reduceMotion) 1f else 0.4f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            ) + fadeIn(tween(220)),
            exit = scaleOut(
                targetScale = if (reduceMotion) 1f else 1.12f,
                animationSpec = tween(260, easing = LinearOutSlowInEasing),
            ) + fadeOut(tween(260)),
        ) {
            if (event != null) RewardCard(event)
        }

        // Full-screen particle burst for anything involving stars.
        if (event != null && event.stars > 0 && !reduceMotion) {
            SparkleBurst(triggerKey = event.triggerKey, modifier = Modifier.fillMaxSize())
            ConfettiEffect(trigger = true, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun RewardCard(event: RewardEvent) {
    val reduceMotion = LocalReduceMotion.current

    // Subtle breathing pulse on the card.
    val scale = remember { Animatable(1f) }
    LaunchedEffect(event.triggerKey) {
        if (reduceMotion) return@LaunchedEffect
        scale.snapTo(1f)
        scale.animateTo(1.06f, tween(280, easing = FastOutSlowInEasing))
        scale.animateTo(1f, tween(320, easing = LinearOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        contentAlignment = Alignment.Center,
    ) {
        // Outer rim-light: animated dim-gold stroke drawn slightly larger
        // than the card, giving a high-end "spotlight" feel without a
        // cartoony neon glow.
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(all = 0.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            MatteAccent.copy(alpha = 0.55f),
                            MatteAccentDim.copy(alpha = 0.12f),
                            MatteAccent.copy(alpha = 0.55f),
                            MatteAccentDim.copy(alpha = 0.12f),
                            MatteAccent.copy(alpha = 0.55f),
                        ),
                    )
                ),
        )

        // Main matte card — no shine, dark backdrop, gold rim border.
        Column(
            modifier = Modifier
                .padding(all = 2.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MatteBackdropTop, MatteBackdropBottom),
                    )
                )
                .border(1.dp, MatteAccent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Icon/emoji in a ringed circle for visual anchor.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MatteAccent.copy(alpha = 0.12f))
                    .border(1.dp, MatteAccent.copy(alpha = 0.6f), RoundedCornerShape(50)),
            ) {
                if (event.emoji != null) {
                    Text(event.emoji, fontSize = 28.sp)
                } else {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        tint = MatteAccent,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }

            Text(
                text = event.label,
                color = Color(0xFFF5E8C0),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )

            if (event.stars > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2B1A05),
                                    Color(0xFF1A0F03),
                                ),
                            )
                        )
                        .border(1.dp, MatteAccent.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Default.Star, null, tint = MatteAccent, modifier = Modifier.size(18.dp))
                    Text(
                        text = "+${event.stars}",
                        color = Color(0xFFF5E8C0),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

/**
 * Radial sparkle burst from screen centre. Lightweight Canvas drawing —
 * no per-frame recomposition, just a single Animatable driving the progress.
 */
@Composable
private fun BoxScope.SparkleBurst(triggerKey: Long, modifier: Modifier = Modifier) {
    val progress = remember(triggerKey) { Animatable(0f) }
    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = LinearOutSlowInEasing))
    }
    val sparkles = remember(triggerKey) {
        (0 until 18).map { i ->
            val angle = (i / 18f) * 2f * 3.14159f
            Sparkle(
                angle = angle,
                distance = 120f + (i % 4) * 40f,
                size = 6f + (i % 3) * 3f,
            )
        }
    }
    Canvas(modifier = modifier) {
        val p = progress.value
        val cx = size.width / 2f
        val cy = size.height / 2f
        sparkles.forEach { s ->
            val d = s.distance * p * 1.6f
            val x = cx + cos(s.angle) * d
            val y = cy + sin(s.angle) * d
            val alpha = (1f - p).coerceIn(0f, 1f)
            drawCircle(
                color = SparkleColor.copy(alpha = alpha * 0.9f),
                radius = s.size * (1f - p * 0.4f),
                center = Offset(x, y),
            )
        }
    }
}

private data class Sparkle(val angle: Float, val distance: Float, val size: Float)
