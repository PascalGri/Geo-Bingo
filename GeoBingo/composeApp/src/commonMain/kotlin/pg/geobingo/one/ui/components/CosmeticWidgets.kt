package pg.geobingo.one.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pg.geobingo.one.game.state.CosmeticsManager
import pg.geobingo.one.game.state.NameEffect
import pg.geobingo.one.game.state.ProfileFrame
import pg.geobingo.one.ui.theme.AnimatedGradientText
import pg.geobingo.one.ui.theme.ColorOnSurface

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

    if (effect != null && effect.id != "name_none" && effect.animated && effect.gradientColors.size >= 2) {
        AnimatedGradientText(
            text = name,
            modifier = modifier,
            style = style.copy(fontWeight = fontWeight),
            gradientColors = effect.gradientColors,
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

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(title.color.copy(alpha = 0.15f))
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
        Box(
            modifier = modifier
                .clip(CircleShape)
                .border(
                    width = frame.borderWidth.dp,
                    brush = Brush.linearGradient(frame.borderColors),
                    shape = CircleShape,
                )
                .padding(frame.borderWidth.dp),
        ) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}
