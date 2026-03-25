package pg.geobingo.one.ui.screens.create

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.ui.theme.GradientBorderCard

@Composable
internal fun SpeedBonusCard(
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val color = gradientColors.first()
    GradientBorderCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        borderColors = gradientColors,
        backgroundColor = Color(0xFF0C0B15),
        borderWidth = 1.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Bolt, null, modifier = Modifier.size(18.dp), tint = color)
                Text(
                    "Schnelligkeitsbonus",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Wer eine Kategorie als Erster fotografiert, bekommt +1 Tempopunkt.",
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.85f),
                lineHeight = 17.sp,
                modifier = Modifier.padding(start = 24.dp),
            )
        }
    }
}
