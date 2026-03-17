package pg.geobingo.one.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.game.*
import pg.geobingo.one.ui.theme.*

@Composable
fun HomeScreen(gameState: GameState) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(gameState.pendingToast) {
        val msg = gameState.pendingToast ?: return@LaunchedEffect
        gameState.pendingToast = null
        snackbarHostState.showSnackbar(msg)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ColorBackground,
    ) { _ ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
    ) {
        // Top glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1A3A10).copy(alpha = 0.6f), Color.Transparent),
                        radius = 600f,
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))

            // Title
            AnimatedGradientText(
                text = "KatchIt!",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 56.sp,
                ),
                gradientColors = GradientPrimary,
                durationMillis = 2500,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Sei schneller als deine Freunde!",
                style = MaterialTheme.typography.bodyLarge,
                color = ColorOnSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(36.dp))

            // Three compact mechanic pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MechanicPill(
                    icon = Icons.Default.GridView,
                    label = "Kategorien\nwählen",
                    modifier = Modifier.weight(1f),
                )
                MechanicPill(
                    icon = Icons.Default.CameraAlt,
                    label = "Motive\nfotografieren",
                    modifier = Modifier.weight(1f),
                )
                MechanicPill(
                    icon = Icons.Default.EmojiEvents,
                    label = "Abstimmen\n& gewinnen",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(24.dp))

            // Datenschutz-Hinweis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1C2A1A))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Default.GppMaybe,
                    contentDescription = null,
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(18.dp).padding(top = 1.dp),
                )
                Text(
                    text = "Fotografiere keine Personen ohne deren ausdrückliche Zustimmung. " +
                           "Das Recht am eigenen Bild (§ 22 KUG) schützt jede Person vor unerlaubter " +
                           "Aufnahme und Weitergabe. Die Verantwortung für rechtmäßige Aufnahmen " +
                           "liegt ausschließlich beim jeweiligen Nutzer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAED581),
                    lineHeight = 17.sp,
                )
            }

            Spacer(Modifier.weight(1f))

            GradientButton(
                text = "Runde erstellen",
                onClick = { gameState.currentScreen = Screen.CREATE_GAME },
                modifier = Modifier.fillMaxWidth(),
                gradientColors = GradientPrimary,
                leadingIcon = {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp), tint = Color.White)
                },
            )

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { gameState.currentScreen = Screen.JOIN_GAME },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.5.dp, ColorPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorPrimary),
            ) {
                Icon(Icons.Default.Login, null, modifier = Modifier.size(20.dp), tint = ColorPrimary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Runde beitreten",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = ColorPrimary,
                )
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = { gameState.currentScreen = Screen.HOW_TO_PLAY }) {
                Icon(
                    Icons.Default.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = ColorOnSurfaceVariant,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "Wie funktioniert's?",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurfaceVariant,
                )
            }

            Text("KatchIt! v1.0", style = MaterialTheme.typography.bodySmall, color = ColorOutline)
            Spacer(Modifier.height(32.dp))
        }
    }
    } // end Scaffold
}

@Composable
private fun MechanicPill(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ColorSurface)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(GradientPrimary)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = ColorOnSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
        )
    }
}
