package pg.geobingo.one.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.animation.core.Animatable as AnimatableValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────
//  Animated gradient text
// ─────────────────────────────────────────────

@Composable
fun AnimatedGradientText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    gradientColors: List<Color> = GradientPrimary,
    durationMillis: Int = 5000,
) {
    val transition = rememberInfiniteTransition(label = "gradText")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 700f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "textOffset",
    )
    val brush = Brush.linearGradient(
        colors = gradientColors + gradientColors,
        start = Offset(offset, 0f),
        end = Offset(offset + 500f, 300f),
    )
    Text(
        text = text,
        style = style.copy(brush = brush),
        modifier = modifier,
    )
}

// ─────────────────────────────────────────────
//  Card with animated gradient border
// ─────────────────────────────────────────────

@Composable
fun GradientBorderCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderColors: List<Color> = GradientPrimary,
    backgroundColor: Color = ColorSurface,
    borderWidth: Dp = 1.5.dp,
    durationMillis: Int = 6000,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "cardBorder")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "borderOffset",
    )
    val gradientBrush = Brush.linearGradient(
        colors = borderColors,
        start = Offset(offset, 0f),
        end = Offset(offset + 600f, 600f),
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(gradientBrush)
            .padding(borderWidth),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(cornerRadius - borderWidth))
                .background(backgroundColor)
                .fillMaxWidth(),
            content = content,
        )
    }
}

// ─────────────────────────────────────────────
//  Full gradient background box (animated)
// ─────────────────────────────────────────────

@Composable
fun AnimatedGradientBox(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = GradientPrimary,
    durationMillis: Int = 8000,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "boxGradient")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bgOffset",
    )
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = gradientColors,
                start = Offset(offset, 0f),
                end = Offset(offset + 600f, 600f),
            )
        ),
        content = content,
    )
}

// ─────────────────────────────────────────────
//  Gradient button
// ─────────────────────────────────────────────

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradientColors: List<Color> = GradientPrimary,
    height: Dp = 56.dp,
    fontSize: TextUnit = 16.sp,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val transition = rememberInfiniteTransition(label = "btnGradient")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "btnOffset",
    )
    val brush = if (enabled) {
        Brush.linearGradient(
            colors = gradientColors,
            start = Offset(offset, 0f),
            end = Offset(offset + 400f, 200f),
        )
    } else {
        Brush.linearGradient(listOf(Color(0xFF2A2A50), Color(0xFF2A2A50)))
    }
    // Press animation: scale down to 0.95f
    val pressScale = remember { AnimatableValue(1f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale.value; scaleY = pressScale.value }
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(brush)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        scope.launch { pressScale.animateTo(0.95f, tween(80)) }
                        tryAwaitRelease()
                        scope.launch { pressScale.animateTo(1f, tween(120)) }
                    },
                    onTap = { onClick() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            leadingIcon?.invoke()
            if (leadingIcon != null) Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = fontSize,
                    color = if (enabled) Color.White else ColorOnSurfaceVariant,
                ),
            )
        }
    }
}
