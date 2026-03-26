package pg.geobingo.one.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.platform.AdManager
import pg.geobingo.one.platform.ConsentManager
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.i18n.Language
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.semanticHeading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    SystemBackHandler { nav.goBack() }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.settingsTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.current.back, tint = ColorPrimary)
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
            SettingsSection(title = S.current.general) {
                SettingsToggleRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = S.current.soundEffects,
                    subtitle = S.current.soundEffectsDesc,
                    checked = gameState.ui.soundEnabled,
                    onCheckedChange = { gameState.ui.updateSoundEnabled(it) },
                )
                HorizontalDivider(color = ColorOutlineVariant)
                SettingsToggleRow(
                    icon = Icons.Default.Vibration,
                    title = S.current.hapticFeedback,
                    subtitle = S.current.hapticFeedbackDesc,
                    checked = gameState.ui.hapticEnabled,
                    onCheckedChange = { gameState.ui.updateHapticEnabled(it) },
                )
            }

            // Advertising section — nur auf iOS/Android sichtbar
            if (AdManager.isAdSupported) {
                SettingsSection(title = S.current.advertising) {
                    SettingsClickRow(
                        icon = Icons.Default.Campaign,
                        title = S.current.adSettings,
                        subtitle = S.current.adSettingsDesc,
                        onClick = { ConsentManager.showPrivacyOptionsForm {} },
                    )
                }
            }

            // Language section
            SettingsSection(title = S.current.language) {
                var showLangDialog by remember { mutableStateOf(false) }
                SettingsClickRow(
                    icon = Icons.Default.Language,
                    title = S.current.language,
                    subtitle = S.language.displayName,
                    onClick = { showLangDialog = true },
                )
                if (showLangDialog) {
                    AlertDialog(
                        onDismissRequest = { showLangDialog = false },
                        containerColor = ColorSurface,
                        title = { Text(S.current.language, color = ColorOnSurface) },
                        text = {
                            Column {
                                Language.entries.forEach { lang ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                S.switchLanguage(lang)
                                                AppSettings.setString(SettingsKeys.LANGUAGE, lang.code)
                                                showLangDialog = false
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        RadioButton(
                                            selected = S.language == lang,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(selectedColor = ColorPrimary),
                                        )
                                        Text(lang.displayName, color = ColorOnSurface, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showLangDialog = false }) {
                                Text(S.current.close, color = ColorPrimary)
                            }
                        },
                    )
                }
            }

            // Support section
            SettingsSection(title = S.current.support) {
                SettingsClickRow(
                    icon = Icons.Default.Email,
                    title = S.current.contact,
                    subtitle = "support@katchit.app",
                    onClick = { uriHandler.openUri("mailto:support@katchit.app") },
                )
            }

            // Legal section
            SettingsSection(title = S.current.legal) {
                SettingsClickRow(
                    icon = Icons.Default.Description,
                    title = S.current.impressum,
                    onClick = { uriHandler.openUri("https://katchit.app/impressum.html") },
                )
                HorizontalDivider(color = ColorOutlineVariant)
                SettingsClickRow(
                    icon = Icons.Default.Shield,
                    title = S.current.privacyPolicy,
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
            style = AppTextStyles.sectionHeader,
            color = ColorOnSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                .semanticHeading(title),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ColorSurfaceContainer)
                .padding(horizontal = Spacing.md, vertical = Spacing.xxs),
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
