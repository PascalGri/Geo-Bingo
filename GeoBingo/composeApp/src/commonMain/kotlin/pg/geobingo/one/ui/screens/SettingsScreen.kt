package pg.geobingo.one.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(gameState: GameState) {
    SystemBackHandler { gameState.currentScreen = Screen.HOME }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = "Einstellungen",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientPrimary,
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
                .padding(horizontal = Spacing.screenHorizontal, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Sound & Haptic section
            SettingsSection(title = "Allgemein") {
                SettingsToggleRow(
                    icon = Icons.Default.VolumeUp,
                    title = "Soundeffekte",
                    subtitle = "Töne bei Aktionen abspielen",
                    checked = gameState.soundEnabled,
                    onCheckedChange = { gameState.soundEnabled = it },
                )
                HorizontalDivider(color = ColorOutlineVariant)
                SettingsToggleRow(
                    icon = Icons.Default.Vibration,
                    title = "Haptisches Feedback",
                    subtitle = "Vibrationen bei Interaktionen",
                    checked = gameState.hapticEnabled,
                    onCheckedChange = { gameState.hapticEnabled = it },
                )
            }

            // Support section
            SettingsSection(title = "Support") {
                SettingsClickRow(
                    icon = Icons.Default.Email,
                    title = "Kontakt",
                    subtitle = "support@katchit.app",
                    onClick = { uriHandler.openUri("mailto:support@katchit.app") },
                )
            }

            // Legal section
            SettingsSection(title = "Rechtliches") {
                SettingsClickRow(
                    icon = Icons.Default.Description,
                    title = "Impressum",
                    onClick = { uriHandler.openUri("https://katchit.app/impressum.html") },
                )
                HorizontalDivider(color = ColorOutlineVariant)
                SettingsClickRow(
                    icon = Icons.Default.Shield,
                    title = "Datenschutzerklärung",
                    onClick = { uriHandler.openUri("https://katchit.app/datenschutz.html") },
                )
            }

            // Version
            Text(
                "KatchIt! v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = ColorOutline,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = ColorOnSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ColorSurface)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            content = content,
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = ColorPrimary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = ColorOnSurface, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant, fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ColorOnPrimary,
                checkedTrackColor = ColorPrimary,
                uncheckedThumbColor = ColorOnSurfaceVariant,
                uncheckedTrackColor = ColorSurfaceVariant,
            ),
        )
    }
}

@Composable
private fun SettingsClickRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = ColorPrimary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = ColorOnSurface, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant, fontSize = 12.sp)
            }
        }
        IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ColorOnSurfaceVariant)
        }
    }
}
