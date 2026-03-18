package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    // Staggered entrance animations (8 elements)
    val animOffsets = (0..7).map { remember { Animatable(40f) } }
    val animAlphas = (0..7).map { remember { Animatable(0f) } }
    LaunchedEffect(Unit) {
        for (i in animOffsets.indices) {
            launch {
                delay(i * 60L)
                launch { animOffsets[i].animateTo(0f, tween(400)) }
                animAlphas[i].animateTo(1f, tween(400))
            }
        }
    }

    fun Modifier.staggered(index: Int): Modifier = this
        .graphicsLayer {
            translationY = animOffsets[index].value
            alpha = animAlphas[index].value
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
        // Top glow - subtle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ColorPrimary.copy(alpha = 0.08f), Color.Transparent),
                        radius = 800f,
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(80.dp))

            // Title
            AnimatedGradientText(
                text = "KatchIt!",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 52.sp,
                    letterSpacing = (-1).sp,
                ),
                gradientColors = GradientPrimary,
                durationMillis = 2500,
                modifier = Modifier.staggered(0),
            )

            Spacer(Modifier.height(6.dp))

            AnimatedGradientText(
                text = "Foto-Schnitzeljagd mit Freunden",
                style = MaterialTheme.typography.bodyLarge,
                gradientColors = GradientCool,
                durationMillis = 3000,
                modifier = Modifier.staggered(1),
            )

            Spacer(Modifier.height(40.dp))

            // How it works - vertical steps
            Column(
                modifier = Modifier
                    .staggered(2)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ColorSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HomeStep(
                    icon = Icons.Default.GridView,
                    number = "1",
                    text = "Kategorien w\u00E4hlen und Freunde einladen",
                )
                HomeStep(
                    icon = Icons.Default.CameraAlt,
                    number = "2",
                    text = "Raus in die Stadt und Motive fotografieren",
                )
                HomeStep(
                    icon = Icons.Default.HowToVote,
                    number = "3",
                    text = "Abstimmen, wer die besten Fotos hat",
                )
            }

            Spacer(Modifier.height(20.dp))

            // Datenschutz-Hinweis
            Row(
                modifier = Modifier
                    .staggered(3)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ColorSurface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Default.GppMaybe,
                    contentDescription = null,
                    tint = ColorOnSurfaceVariant,
                    modifier = Modifier.size(16.dp).padding(top = 1.dp),
                )
                Text(
                    text = "Fotografiere keine Personen ohne deren Zustimmung. " +
                           "Das Recht am eigenen Bild (\u00A7 22 KUG) sch\u00FCtzt jede Person. " +
                           "Die Verantwortung liegt beim jeweiligen Nutzer.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                    lineHeight = 15.sp,
                )
            }

            Spacer(Modifier.weight(1f))

            GradientButton(
                text = "Runde erstellen",
                onClick = { gameState.currentScreen = Screen.CREATE_GAME },
                modifier = Modifier.fillMaxWidth().staggered(4),
                gradientColors = GradientPrimary,
                leadingIcon = {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp), tint = Color.White)
                },
            )

            Spacer(Modifier.height(10.dp))

            GradientButton(
                text = "Runde beitreten",
                onClick = { gameState.currentScreen = Screen.JOIN_GAME },
                modifier = Modifier.fillMaxWidth().staggered(5),
                gradientColors = GradientCool,
                leadingIcon = {
                    Icon(Icons.Default.Login, null, modifier = Modifier.size(18.dp), tint = Color.White)
                },
            )

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.staggered(6),
            ) {
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
                if (gameState.gameHistory.isNotEmpty()) {
                    TextButton(onClick = { gameState.currentScreen = Screen.HISTORY }) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = ColorOnSurfaceVariant,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Verlauf",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                "KatchIt! v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = ColorOutline,
                modifier = Modifier.staggered(7),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
    } // end Scaffold
}

@Composable
private fun HomeStep(icon: ImageVector, number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedGradientBox(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp)),
            gradientColors = GradientPrimary,
            durationMillis = 3000,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = ColorOnSurface,
            )
        }
    }
}
