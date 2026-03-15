package pg.geobingo.one.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import pg.geobingo.one.ui.theme.AnimatedGradientText
import pg.geobingo.one.ui.theme.ColorBackground
import pg.geobingo.one.ui.theme.ColorOnSurface
import pg.geobingo.one.ui.theme.ColorOnSurfaceVariant
import pg.geobingo.one.ui.theme.ColorOutline
import pg.geobingo.one.ui.theme.ColorOutlineVariant
import pg.geobingo.one.ui.theme.ColorPrimary
import pg.geobingo.one.ui.theme.ColorPrimaryContainer
import pg.geobingo.one.ui.theme.ColorSurface
import pg.geobingo.one.ui.theme.GradientBorderCard
import pg.geobingo.one.ui.theme.GradientButton
import pg.geobingo.one.ui.theme.GradientCool
import pg.geobingo.one.ui.theme.GradientPrimary

@Composable
fun HomeScreen(gameState: GameState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
    ) {
        // Subtle radial glow at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2D1B6B).copy(alpha = 0.5f),
                            Color.Transparent,
                        ),
                        radius = 500f,
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(80.dp))

            // App icon with gradient background
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(GradientPrimary)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PinDrop,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = Color.White,
                )
            }

            Spacer(Modifier.height(28.dp))

            AnimatedGradientText(
                text = "Gotcha!",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp,
                ),
                gradientColors = GradientPrimary,
                durationMillis = 2500,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Erkunde die Stadt mit Freunden",
                style = MaterialTheme.typography.bodyLarge,
                color = ColorOnSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(48.dp))

            // Feature card with animated gradient border
            GradientBorderCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                borderColors = GradientCool,
                backgroundColor = ColorSurface,
                durationMillis = 4000,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    HomeFeatureItem(Icons.Default.Group, "2–8 Spieler in einer Runde")
                    HorizontalDivider(color = ColorOutlineVariant)
                    HomeFeatureItem(Icons.Default.GridView, "Kategorien frei wählen")
                    HorizontalDivider(color = ColorOutlineVariant)
                    HomeFeatureItem(Icons.Default.CameraAlt, "Fotos mit der Kamera aufnehmen")
                    HorizontalDivider(color = ColorOutlineVariant)
                    HomeFeatureItem(Icons.Default.HowToVote, "Abstimmen & Punkte zählen")
                }
            }

            Spacer(Modifier.weight(1f))

            GradientButton(
                text = "Runde erstellen",
                onClick = { gameState.currentScreen = Screen.CREATE_GAME },
                modifier = Modifier.fillMaxWidth(),
                gradientColors = GradientPrimary,
                leadingIcon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White,
                    )
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
                Icon(
                    Icons.Default.Login,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = ColorPrimary,
                )
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
            Text(
                "Gotcha! v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = ColorOutline,
            )
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun HomeFeatureItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ColorPrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = ColorPrimary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = ColorOnSurface,
        )
    }
}
