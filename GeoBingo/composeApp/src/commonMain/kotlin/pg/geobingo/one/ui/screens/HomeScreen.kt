package pg.geobingo.one.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import geobingo.composeapp.generated.resources.Res
import geobingo.composeapp.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource
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

            // App icon
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop,
            )

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

            // Game flow card
            GradientBorderCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                borderColors = GradientCool,
                backgroundColor = ColorSurface,
                durationMillis = 4000,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GameFlowStep(icon = Icons.Default.DirectionsWalk, label = "Erkunden")
                    GameFlowArrow()
                    GameFlowStep(icon = Icons.Default.CameraAlt, label = "Fotografieren")
                    GameFlowArrow()
                    GameFlowStep(icon = Icons.Default.HowToVote, label = "Abstimmen")
                    GameFlowArrow()
                    GameFlowStep(icon = Icons.Default.EmojiEvents, label = "Gewinnen")
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
private fun GameFlowStep(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(GradientPrimary)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = ColorOnSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GameFlowArrow() {
    Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = ColorPrimary.copy(alpha = 0.5f),
    )
}
