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
import kotlinx.coroutines.launch
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
import pg.geobingo.one.ui.components.StarsChip
import pg.geobingo.one.platform.AdManager
import pg.geobingo.one.platform.BillingManager
import pg.geobingo.one.platform.ConsentManager
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.i18n.Language
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    SystemBackHandler { nav.goBack() }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                actions = { StarsChip(count = gameState.stars.starCount, onClick = { nav.navigateTo(Screen.SHOP) }) },
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
            // Account section
            SettingsSection(title = S.current.account) {
                val isLoggedIn = pg.geobingo.one.network.AccountManager.isLoggedIn
                val currentUser = pg.geobingo.one.network.AccountManager.currentUser
                var showAuthDialog by remember { mutableStateOf(false) }
                var authLoading by remember { mutableStateOf(false) }

                if (isLoggedIn && currentUser != null) {
                    SettingsClickRow(
                        icon = Icons.Default.Person,
                        title = "${S.current.loggedInAs} ${currentUser.email ?: ""}",
                        subtitle = S.current.syncDataDesc,
                        onClick = {
                            scope.launch {
                                val userId = pg.geobingo.one.network.AccountManager.currentUserId ?: return@launch
                                pg.geobingo.one.network.AccountManager.syncLocalToCloud(userId)
                                snackbarHostState.showSnackbar(S.current.syncData)
                            }
                        },
                    )
                    HorizontalDivider(color = ColorOutlineVariant)
                    SettingsClickRow(
                        icon = Icons.Default.ExitToApp,
                        title = S.current.signOut,
                        onClick = {
                            scope.launch {
                                pg.geobingo.one.network.AccountManager.signOut()
                                snackbarHostState.showSnackbar(S.current.signedOut)
                            }
                        },
                    )
                } else {
                    SettingsClickRow(
                        icon = Icons.Default.PersonAdd,
                        title = S.current.signIn,
                        subtitle = S.current.syncDataDesc,
                        onClick = { showAuthDialog = true },
                    )
                }

                if (showAuthDialog) {
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    var isSignUp by remember { mutableStateOf(false) }
                    var errorMsg by remember { mutableStateOf<String?>(null) }

                    AlertDialog(
                        onDismissRequest = { if (!authLoading) showAuthDialog = false },
                        containerColor = ColorSurface,
                        title = {
                            Text(
                                if (isSignUp) S.current.signUp else S.current.signIn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorOnSurface,
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    placeholder = { Text(S.current.emailPlaceholder) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ColorPrimary,
                                        unfocusedBorderColor = ColorOutline,
                                        focusedTextColor = ColorOnSurface,
                                        unfocusedTextColor = ColorOnSurface,
                                        cursorColor = ColorPrimary,
                                    ),
                                )
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    placeholder = { Text(S.current.passwordPlaceholder) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ColorPrimary,
                                        unfocusedBorderColor = ColorOutline,
                                        focusedTextColor = ColorOnSurface,
                                        unfocusedTextColor = ColorOnSurface,
                                        cursorColor = ColorPrimary,
                                    ),
                                )
                                if (errorMsg != null) {
                                    Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = ColorError)
                                }
                                TextButton(onClick = { isSignUp = !isSignUp }) {
                                    Text(
                                        if (isSignUp) S.current.signIn else S.current.signUp,
                                        color = ColorPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (password.length < 6) {
                                        errorMsg = S.current.passwordTooShort
                                        return@TextButton
                                    }
                                    authLoading = true
                                    errorMsg = null
                                    scope.launch {
                                        val result = if (isSignUp)
                                            pg.geobingo.one.network.AccountManager.signUp(email, password)
                                        else
                                            pg.geobingo.one.network.AccountManager.signIn(email, password)

                                        authLoading = false
                                        if (result.isSuccess) {
                                            showAuthDialog = false
                                            // Reload stars from cloud
                                            gameState.stars.reload()
                                            snackbarHostState.showSnackbar(
                                                if (isSignUp) S.current.accountCreated else S.current.signedIn
                                            )
                                        } else {
                                            errorMsg = result.exceptionOrNull()?.message ?: S.current.authError
                                        }
                                    }
                                },
                                enabled = email.isNotBlank() && password.isNotBlank() && !authLoading,
                            ) {
                                if (authLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ColorPrimary)
                                } else {
                                    Text(
                                        if (isSignUp) S.current.signUp else S.current.signIn,
                                        color = ColorPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAuthDialog = false }, enabled = !authLoading) {
                                Text(S.current.cancel, color = ColorOnSurfaceVariant)
                            }
                        },
                    )
                }
            }

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
                    // Remove Ads IAP
                    if (gameState.stars.noAdsPurchased) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorSuccess, modifier = Modifier.size(22.dp))
                            Text(S.current.adsRemoved, style = MaterialTheme.typography.bodyMedium, color = ColorSuccess, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        var purchaseLoading by remember { mutableStateOf(false) }
                        SettingsClickRow(
                            icon = Icons.Default.RemoveCircle,
                            title = S.current.removeAds,
                            subtitle = "${S.current.removeAdsPrice} - ${S.current.removeAdsDesc}",
                            onClick = {
                                if (!purchaseLoading) {
                                    purchaseLoading = true
                                    BillingManager.purchaseProduct(
                                        productId = "pg.geobingo.one.no_ads",
                                        onSuccess = {
                                            gameState.stars.updateNoAdsPurchased(true)
                                            purchaseLoading = false
                                        },
                                        onError = {
                                            purchaseLoading = false
                                        },
                                    )
                                }
                            },
                        )
                    }
                    HorizontalDivider(color = ColorOutlineVariant)
                    SettingsClickRow(
                        icon = Icons.Default.Campaign,
                        title = S.current.adSettings,
                        subtitle = S.current.adSettingsDesc,
                        onClick = { ConsentManager.showPrivacyOptionsForm {} },
                    )
                    HorizontalDivider(color = ColorOutlineVariant)
                    // Restore Purchases
                    SettingsClickRow(
                        icon = Icons.Default.Refresh,
                        title = S.current.restorePurchases,
                        onClick = {
                            BillingManager.restorePurchases(
                                onRestored = { products ->
                                    if ("pg.geobingo.one.no_ads" in products) {
                                        gameState.stars.updateNoAdsPurchased(true)
                                    }
                                },
                                onError = {},
                            )
                        },
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
                "KatchIt! v1.1",
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
