package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.Spacing
import pg.geobingo.one.ui.theme.rememberStaggeredAnimation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlayScreen(gameState: GameState) {
    SystemBackHandler { gameState.session.currentScreen = Screen.HOME }

    val anim = rememberStaggeredAnimation(count = 9)
    fun Modifier.staggered(index: Int): Modifier = this.then(anim.modifier(index))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = "So geht's",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { gameState.session.currentScreen = Screen.HOME }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = ColorPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // Hero tagline
            Text(
                text = "Erkundet gemeinsam\neure Stadt.",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    brush = Brush.linearGradient(GradientPrimary),
                ),
                lineHeight = 34.sp,
                modifier = Modifier.padding(vertical = 8.dp).staggered(0),
            )

            // Steps
            StepCard(
                number = "1",
                icon = Icons.Default.GroupAdd,
                title = "Runde erstellen",
                body = "Eine Person erstellt eine Runde und wählt Kategorien – z.B. \"Roter Porsche\" oder \"Straßenmusiker\". Die anderen treten per Code bei.",
                modifier = Modifier.staggered(1),
            )
            StepCard(
                number = "2",
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                title = "Raus in die Stadt",
                body = "Sobald alle dabei sind, startet der Host das Spiel. Jetzt habt ihr eine festgelegte Zeit, um so viele Kategorien wie möglich zu fotografieren.",
                modifier = Modifier.staggered(2),
            )
            StepCard(
                number = "3",
                icon = Icons.Default.CameraAlt,
                title = "Fotos aufnehmen",
                body = "Findet ihr ein Motiv, tippt auf die Kategorie und macht ein Foto. Jeder Spieler kann jede Kategorie fotografieren.",
                modifier = Modifier.staggered(3),
            )
            StepCard(
                number = "4",
                icon = Icons.Default.HowToVote,
                title = "Bewerten",
                body = "Nach der Zeit bewertet ihr alle Fotos mit 1–5 Sternen. Je besser das Bild zur Kategorie passt, desto mehr Sterne – und jeder Stern zählt als ein Punkt.",
                modifier = Modifier.staggered(4),
            )
            StepCard(
                number = "5",
                icon = Icons.Default.EmojiEvents,
                title = "Sieger küren",
                body = "Die Sterne aller Mitspieler werden pro Kategorie gemittelt und ergeben deine Punkte. Wer insgesamt die meisten Punkte sammelt, gewinnt!",
                modifier = Modifier.staggered(5),
            )

            // Speed bonus info card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0C0B15),
                modifier = Modifier.fillMaxWidth().staggered(6),
                border = BorderStroke(1.dp, Color(0xFFD946EF).copy(alpha = 0.4f)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Bolt, null, modifier = Modifier.size(20.dp), tint = Color(0xFFD946EF))
                        Text(
                            "Schnelligkeitsbonus",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD946EF),
                        )
                    }
                    Text(
                        "Wer als Erster eine Kategorie fotografiert, bekommt +1 Bonuspunkt obendrauf. Schnell sein lohnt sich!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD946EF).copy(alpha = 0.85f),
                        lineHeight = 18.sp,
                    )
                }
            }

            // Tips box
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ColorPrimaryContainer,
                modifier = Modifier.fillMaxWidth().staggered(7),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lightbulb, null, tint = ColorPrimary, modifier = Modifier.size(18.dp))
                        Text(
                            "Tipps",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnPrimaryContainer,
                        )
                    }
                    TipItem("Spielt in einer neuen Stadt oder Gegend – macht es spannender.")
                    TipItem("Je mehr Spieler, desto lustiger die Abstimmung.")
                    TipItem("15–30 Minuten sind ideal. Bei mehr Zeit mehr Kategorien wählen.")
                }
            }

            Spacer(Modifier.height(8.dp))

            GradientButton(
                text = "Runde erstellen",
                onClick = { gameState.session.currentScreen = Screen.CREATE_GAME },
                modifier = Modifier.fillMaxWidth().staggered(8),
                gradientColors = GradientPrimary,
                leadingIcon = {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp), tint = Color.White)
                },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepCard(number: String, icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorSurface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Number + icon stack
        Box {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(GradientPrimary)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ColorBackground),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = ColorPrimary,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = ColorOnSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = ColorOnSurfaceVariant,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun TipItem(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("·", color = ColorPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text, style = MaterialTheme.typography.bodySmall, color = ColorOnPrimaryContainer, lineHeight = 18.sp)
    }
}
