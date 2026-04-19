package pg.geobingo.one.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pg.geobingo.one.game.state.CosmeticsManager
import pg.geobingo.one.game.state.NameEffect
import pg.geobingo.one.game.state.ProfileFrame
import pg.geobingo.one.ui.theme.AnimatedGradientText
import pg.geobingo.one.ui.theme.ColorOnSurface
import pg.geobingo.one.ui.theme.LocalReduceMotion

/**
 * Displays a player name with the equipped cosmetic name effect.
 * Falls back to plain Text if no effect is equipped.
 */
@Composable
fun CosmeticPlayerName(
    name: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight = FontWeight.Medium,
    nameEffectId: String? = null,
    fallbackColor: Color = ColorOnSurface,
) {
    val effect = if (nameEffectId != null) {
        CosmeticsManager.ALL_NAME_EFFECTS.find { it.id == nameEffectId }
    } else {
        CosmeticsManager.getEquippedNameEffect()
    }

    if (effect != null && effect.id != "name_none" && effect.gradientColors.size >= 2) {
        // Shimmer across the gradient for any 2+ colour effect; AnimatedGradientText honours LocalReduceMotion.
        AnimatedGradientText(
            text = name,
            modifier = modifier,
            style = style.copy(fontWeight = fontWeight),
            gradientColors = effect.gradientColors,
            durationMillis = 4000,
        )
    } else {
        Text(
            text = name,
            modifier = modifier,
            style = style,
            fontWeight = fontWeight,
            color = fallbackColor,
        )
    }
}

/**
 * Displays the equipped player title as a small colored text badge.
 * Shows nothing if titleId is "title_none" or not found.
 */
@Composable
fun PlayerTitleBadge(
    titleId: String,
    modifier: Modifier = Modifier,
) {
    if (titleId == "title_none") return
    val title = CosmeticsManager.ALL_TITLES.find { it.id == titleId } ?: return

    val reduceMotion = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "titlePulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = if (reduceMotion) 0.15f else 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "titlePulseA",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(title.color.copy(alpha = pulseAlpha))
            .border(0.5.dp, title.color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = title.name,
            style = MaterialTheme.typography.labelSmall,
            color = title.color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Wraps content (typically an avatar) with the equipped profile frame border.
 */
@Composable
fun FramedAvatar(
    modifier: Modifier = Modifier,
    frameId: String? = null,
    size: Dp = 40.dp,
    content: @Composable () -> Unit,
) {
    val frame = if (frameId != null) {
        CosmeticsManager.ALL_FRAMES.find { it.id == frameId }
    } else {
        CosmeticsManager.getEquippedFrame()
    }

    if (frame != null && frame.id != "frame_none" && frame.borderColors.any { it != Color.Transparent }) {
        val reduceMotion = LocalReduceMotion.current
        val hasMultiColor = frame.borderColors.size >= 2
        val transition = rememberInfiniteTransition(label = "frameBorder")
        val rotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = if (reduceMotion || !hasMultiColor) 0f else 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "frameRot",
        )
        val borderBrush = if (hasMultiColor) {
            // Doubling colours so rotation remains continuous.
            Brush.sweepGradient(frame.borderColors + frame.borderColors.first())
        } else {
            Brush.linearGradient(frame.borderColors)
        }

        val isPremium = hasMultiColor && frame.borderColors.any { it.red > 0.9f && it.green > 0.7f }

        Box(
            modifier = modifier
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Rotating border ring.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { rotationZ = rotation }
                    .clip(CircleShape)
                    .border(
                        width = frame.borderWidth.dp,
                        brush = borderBrush,
                        shape = CircleShape,
                    ),
            )
            Box(modifier = Modifier.padding(frame.borderWidth.dp)) {
                content()
            }
            if (isPremium && !reduceMotion) {
                SparkleOverlay(transition = transition)
            }
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

@Composable
private fun BoxScope.SparkleOverlay(
    transition: androidx.compose.animation.core.InfiniteTransition,
) {
    val a by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sparkleA",
    )
    val b by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sparkleB",
    )
    Box(
        modifier = Modifier
            .size(4.dp)
            .align(Alignment.TopEnd)
            .offset(x = (-4).dp, y = 4.dp)
            .graphicsLayer { alpha = a }
            .clip(CircleShape)
            .background(Color.White),
    )
    Box(
        modifier = Modifier
            .size(4.dp)
            .align(Alignment.BottomStart)
            .offset(x = 4.dp, y = (-4).dp)
            .graphicsLayer { alpha = b }
            .clip(CircleShape)
            .background(Color.White),
    )
}
