package pg.geobingo.one.ui.screens.create

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import pg.geobingo.one.data.getCategoryIcon
import pg.geobingo.one.data.getCategoryIconRotation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.data.Category
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DarkCategorySelectCard(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showInfo by remember { mutableStateOf(false) }

    // Scale bounce on selection change
    val scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        scaleAnim.animateTo(1.12f, tween(100))
        scaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            containerColor = ColorSurface,
            icon = {
                Icon(
                    imageVector = getCategoryIcon(category.id),
                    contentDescription = null,
                    tint = if (isSelected) ColorPrimary else ColorOnSurfaceVariant,
                    modifier = Modifier.size(32.dp).rotate(getCategoryIconRotation(category.id)),
                )
            },
            title = {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnSurface,
                    textAlign = TextAlign.Center,
                )
            },
            text = if (category.description.isNotBlank()) {
                {
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorOnSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else null,
            confirmButton = {
                TextButton(onClick = { showInfo = false; onClick() }) {
                    Icon(
                        if (isSelected) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                        null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSelected) "Abwählen" else "Auswählen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("Schließen")
                }
            },
        )
    }

    if (isSelected) {
        GradientBorderCard(
            modifier = modifier
                .scale(scaleAnim.value)
                .aspectRatio(0.85f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showInfo = true },
                ),
            cornerRadius = 10.dp,
            borderColors = GradientPrimary,
            backgroundColor = ColorPrimaryContainer,
            borderWidth = 1.5.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = getCategoryIcon(category.id),
                    contentDescription = category.name,
                    modifier = Modifier.size(26.dp).rotate(getCategoryIconRotation(category.id)),
                    tint = ColorPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = ColorOnPrimaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp,
                )
            }
        }
    } else {
        Card(
            modifier = modifier
                .scale(scaleAnim.value)
                .aspectRatio(0.85f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showInfo = true },
                ),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ColorSurfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = getCategoryIcon(category.id),
                    contentDescription = category.name,
                    modifier = Modifier.size(26.dp).rotate(getCategoryIconRotation(category.id)),
                    tint = ColorOnSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = ColorOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp,
                )
            }
        }
    }
}
