package pg.geobingo.one.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val StarGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFD97706)),
    start = Offset(0f, 0f),
    end = Offset(30f, 30f),
)

@Composable
fun StarsChip(
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val base = if (onClick != null) modifier.clickable(onClick = onClick) else modifier

    Row(
        modifier = base.padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color(0xFFFBBF24),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = Color.White,
        )
    }
}

@Composable
fun SkipCardsChip(
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val base = if (onClick != null) modifier.clickable(onClick = onClick) else modifier

    Row(
        modifier = base.padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            Icons.Default.CreditCard,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = Color(0xFF94A3B8),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Color(0xFFCBD5E1),
        )
    }
}
