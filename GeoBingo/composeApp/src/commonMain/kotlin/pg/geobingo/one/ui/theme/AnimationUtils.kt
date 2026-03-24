package pg.geobingo.one.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─────────────────────────────────────────────
//  Staggered entrance animation utility
// ─────────────────────────────────────────────

class StaggeredAnimationState(
    val offsets: List<Animatable<Float, AnimationVector1D>>,
    val alphas: List<Animatable<Float, AnimationVector1D>>,
) {
    fun modifier(index: Int): Modifier = Modifier.graphicsLayer {
        translationY = offsets[index].value
        alpha = alphas[index].value
    }
}

@Composable
fun rememberStaggeredAnimation(
    count: Int,
    initialOffset: Float = 40f,
    staggerDelay: Long = 60L,
    animDuration: Int = 400,
): StaggeredAnimationState {
    val offsets = (0 until count).map { remember { Animatable(initialOffset) } }
    val alphas = (0 until count).map { remember { Animatable(0f) } }
    LaunchedEffect(Unit) {
        for (i in offsets.indices) {
            launch {
                delay(i * staggerDelay)
                launch { offsets[i].animateTo(0f, tween(animDuration)) }
                alphas[i].animateTo(1f, tween(animDuration))
            }
        }
    }
    return remember { StaggeredAnimationState(offsets, alphas) }
}

// ─────────────────────────────────────────────
//  Shimmer placeholder for loading images
// ─────────────────────────────────────────────

@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ColorSurfaceVariant,
                        ColorSurfaceVariant.copy(alpha = 0.5f),
                        ColorOutline.copy(alpha = 0.3f),
                        ColorSurfaceVariant.copy(alpha = 0.5f),
                        ColorSurfaceVariant,
                    ),
                    start = Offset(offset, 0f),
                    end = Offset(offset + 400f, 200f),
                )
            ),
    )
}

// ─────────────────────────────────────────────
//  Konfetti / particle effect
// ─────────────────────────────────────────────

data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val color: Color,
    val size: Float,
    val angle: Float,
    val speed: Float,
    val rotationSpeed: Float,
)

@Composable
fun ConfettiEffect(
    trigger: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!trigger) return

    val particles = remember {
        val colors = listOf(
            Color(0xFFF43F5E), // Rose
            Color(0xFFD946EF), // Fuchsia
            Color(0xFFA855F7), // Purple
            Color(0xFF7C3AED), // Violet
            Color(0xFFE879F9), // Fuchsia light
            Color(0xFF22D3EE), // Cyan
            Color(0xFFFF6B6B), // Coral
        )
        (0 until 60).map {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.5f,
                color = colors.random(),
                size = Random.nextFloat() * 8f + 4f,
                angle = Random.nextFloat() * 360f,
                speed = Random.nextFloat() * 3f + 1f,
                rotationSpeed = Random.nextFloat() * 10f - 5f,
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.animateTo(1f, tween(2500, easing = LinearEasing))
    }

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val p = progress.value

        particles.forEach { particle ->
            val gravity = 2f
            val t = p * particle.speed
            val px = particle.x * w + sin(particle.angle * PI.toFloat() / 180f) * t * 40f
            val py = particle.y * h + t * h * 0.5f + gravity * t * t * 20f
            val rotation = particle.angle + particle.rotationSpeed * p * 360f
            val alpha = (1f - p).coerceIn(0f, 1f)

            if (py < h * 1.2f) {
                drawContext.canvas.save()
                drawContext.transform.translate(px, py)
                drawContext.transform.rotate(rotation)
                drawRect(
                    color = particle.color.copy(alpha = alpha),
                    topLeft = Offset(-particle.size / 2, -particle.size / 2),
                    size = androidx.compose.ui.geometry.Size(particle.size, particle.size * 0.6f),
                )
                drawContext.canvas.restore()
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Spacing constants
// ─────────────────────────────────────────────

object Spacing {
    val screenHorizontal = 20.dp
    val cardPadding = 16.dp
    val sectionGap = 12.dp
    val elementGap = 8.dp
}
