package pg.geobingo.one.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.ui.theme.*

/**
 * Two big segment buttons (Stars / Cosmetics) for switching between the
 * Star Shop and Cosmetic Shop. Rendered at the top of both shop screens.
 */
@Composable
fun ShopTabSwitcher(
    activeScreen: Screen,
    modifier: Modifier = Modifier,
) {
    val nav = remember { ServiceLocator.navigation }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShopTabButton(
            modifier = Modifier.weight(1f),
            label = S.current.shopTabStars,
            icon = Icons.Default.Star,
            selected = activeScreen == Screen.SHOP,
            gradient = GradientGold,
            onClick = {
                if (activeScreen != Screen.SHOP) nav.replaceCurrent(Screen.SHOP)
            },
        )
        ShopTabButton(
            modifier = Modifier.weight(1f),
            label = S.current.shopTabCosmetics,
            icon = Icons.Default.AutoAwesome,
            selected = activeScreen == Screen.COSMETIC_SHOP,
            gradient = GradientPrimary,
            onClick = {
                if (activeScreen != Screen.COSMETIC_SHOP) nav.replaceCurrent(Screen.COSMETIC_SHOP)
            },
        )
    }
}

@Composable
private fun ShopTabButton(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    gradient: List<Color>,
    onClick: () -> Unit,
) {
    val borderAlpha by animateColorAsState(
        targetValue = if (selected) gradient.first() else ColorOutline.copy(alpha = 0.4f),
        animationSpec = tween(220),
        label = "shopTabBorder",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) ColorOnSurface else ColorOnSurfaceVariant.copy(alpha = 0.75f),
        animationSpec = tween(220),
        label = "shopTabLabel",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (selected) {
                    Modifier.background(
                        Brush.horizontalGradient(gradient.map { it.copy(alpha = 0.18f) }),
                    )
                } else {
                    Modifier.background(ColorSurface)
                },
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderAlpha,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (selected) gradient.first() else ColorOnSurfaceVariant.copy(alpha = 0.7f),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = labelColor,
            )
        }
    }
}
