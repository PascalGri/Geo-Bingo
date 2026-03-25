package pg.geobingo.one.ui.screens.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.ui.theme.AnimatedGradientText
import pg.geobingo.one.ui.theme.ColorOnSurfaceVariant
import pg.geobingo.one.ui.theme.ColorSurface
import pg.geobingo.one.ui.theme.GradientBorderCard

@Composable
internal fun GameCodeCard(
    gameCode: String,
    modeLabel: String,
    modeIcon: ImageVector,
    modeGradient: List<Color>,
    modifier: Modifier = Modifier,
) {
    GradientBorderCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        borderColors = modeGradient,
        backgroundColor = ColorSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Mode badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(modeGradient.first().copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Icon(
                    modeIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = modeGradient.first(),
                )
                Text(
                    modeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = modeGradient.first(),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Rundencode",
                style = MaterialTheme.typography.labelMedium,
                color = ColorOnSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            AnimatedGradientText(
                text = gameCode,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                ),
                gradientColors = modeGradient,
                durationMillis = 2000,
            )
        }
    }
}
