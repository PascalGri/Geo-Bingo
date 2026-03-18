package pg.geobingo.one.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlayScreen(gameState: GameState) {
    SystemBackHandler { gameState.currentScreen = Screen.HOME }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "So geht's",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorOnSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { gameState.currentScreen = Screen.HOME }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = ColorPrimary)
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
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
                modifier = Modifier.padding(vertical = 8.dp),
            )

            // Steps
            StepCard(
                number = "1",
                icon = Icons.Default.GroupAdd,
                title = "Runde erstellen",
                body = "Eine Person erstellt eine Runde und wählt Kategorien – z.B. \"Roter Porsche\" oder \"Straßenmusiker\". Die anderen treten per Code bei.",
            )
            StepCard(
                number = "2",
                icon = Icons.Default.DirectionsWalk,
                title = "Raus in die Stadt",
                body = "Sobald alle dabei sind, startet der Host das Spiel. Jetzt habt ihr eine festgelegte Zeit, um so viele Kategorien wie möglich zu fotografieren.",
            )
            StepCard(
                number = "3",
                icon = Icons.Default.CameraAlt,
                title = "Fotos aufnehmen",
                body = "Findet ihr ein Motiv, tippt auf die Kategorie und macht ein Foto. Mehrere Spieler können dieselbe Kategorie fotografieren – das Beste gewinnt.",
            )
            StepCard(
                number = "4",
                icon = Icons.Default.HowToVote,
                title = "Abstimmen",
                body = "Nach der Zeit seht ihr alle Fotos. Jeder stimmt pro Kategorie ab, welches Foto am besten passt. Schnell, fair, lustig.",
            )
            StepCard(
                number = "5",
                icon = Icons.Default.EmojiEvents,
                title = "Sieger küren",
                body = "Wer die meisten Votes sammelt, gewinnt. Am Ende seht ihr das Ranking aller Spieler und die besten Bilder der Runde.",
            )

            // Speed bonus info card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1A2E),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.4f)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Bolt, null, modifier = Modifier.size(20.dp), tint = Color(0xFFFBBF24))
                        Text(
                            "Schnelligkeitsbonus",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24),
                        )
                    }
                    Text(
                        "Wer als Erster eine Kategorie fotografiert, bekommt automatisch +1 Bonuspunkt – zusätzlich zu den Abstimmungspunkten. Schnell sein lohnt sich!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFBBF24).copy(alpha = 0.85f),
                        lineHeight = 18.sp,
                    )
                }
            }

            // Tips box
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ColorPrimaryContainer,
                modifier = Modifier.fillMaxWidth(),
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
                onClick = { gameState.currentScreen = Screen.CREATE_GAME },
                modifier = Modifier.fillMaxWidth(),
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
private fun StepCard(number: String, icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
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
