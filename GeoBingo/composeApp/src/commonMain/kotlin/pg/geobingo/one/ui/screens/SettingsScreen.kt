package pg.geobingo.one.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.ui.components.StarsChip
import pg.geobingo.one.ui.components.SelfiePicker
import pg.geobingo.one.platform.AdManager
import pg.geobingo.one.platform.BillingManager
import pg.geobingo.one.platform.ConsentManager
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.i18n.Language
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.platform.rememberShareManager
import pg.geobingo.one.ui.components.CollectScrollToTop
import pg.geobingo.one.ui.components.ScrollToTopTags
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val snackbarHostState = remember { SnackbarHostState() }
    SystemBackHandler { nav.goBack() }
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    CollectScrollToTop(ScrollToTopTags.SETTINGS, scrollState)

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
                    IconButton(onClick = { nav.goHome() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.current.back, tint = ColorPrimary)
                    }
                },
                actions = { pg.geobingo.one.ui.components.TopBarStarsAndProfile(gameState = gameState, onNavigate = { nav.navigateTo(it) }) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = Spacing.screenHorizontal, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AccountQuickSection(
                onNavigate = { nav.navigateTo(it) },
            )

            SoundAndHapticSection(
                soundEnabled = gameState.ui.soundEnabled,
                onSoundEnabledChange = { gameState.ui.updateSoundEnabled(it) },
                hapticEnabled = gameState.ui.hapticEnabled,
                onHapticEnabledChange = { gameState.ui.updateHapticEnabled(it) },
            )

            AdvertisingSection(gameState = gameState)

            LanguageSection()

            SupportSection(
                onContactClick = { uriHandler.openUri("mailto:support@katchit.app") },
            )

            LegalSection(
                onImpressumClick = { uriHandler.openUri("https://katchit.app/impressum.html") },
                onPrivacyClick = { uriHandler.openUri("https://katchit.app/datenschutz.html") },
            )

            VersionFooter(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

// ── Account Quick Section (navigate to account screen) ──────────────────────

@Composable
private fun AccountQuickSection(
    onNavigate: (Screen) -> Unit,
) {
    SettingsSection(title = S.current.account) {
        val isLoggedIn = AccountManager.isLoggedIn
        if (isLoggedIn) {
            val name = AppSettings.getString("last_player_name", "")
            val avatarBytes = LocalPhotoStore.loadAvatar("profile")
            SettingsClickRow(
                icon = Icons.Default.Person,
                title = name.ifBlank { S.current.account },
                subtitle = AccountManager.currentUser?.email ?: "",
                onClick = { onNavigate(Screen.ACCOUNT) },
            )
        } else {
            SettingsClickRow(
                icon = Icons.Default.PersonAdd,
                title = S.current.signIn,
                subtitle = S.current.syncDataDesc,
                onClick = { onNavigate(Screen.ACCOUNT) },
            )
        }
    }
}

// ── Sound & Haptic Section ──────────────────────────────────────────────────

@Composable
private fun SoundAndHapticSection(
    soundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    hapticEnabled: Boolean,
    onHapticEnabledChange: (Boolean) -> Unit,
) {
    SettingsSection(title = S.current.general) {
        SettingsToggleRow(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            title = S.current.soundEffects,
            subtitle = S.current.soundEffectsDesc,
            checked = soundEnabled,
            onCheckedChange = onSoundEnabledChange,
        )
        HorizontalDivider(color = ColorOutlineVariant)
        SettingsToggleRow(
            icon = Icons.Default.Vibration,
            title = S.current.hapticFeedback,
            subtitle = S.current.hapticFeedbackDesc,
            checked = hapticEnabled,
            onCheckedChange = onHapticEnabledChange,
        )
    }
}

// ── Advertising Section ─────────────────────────────────────────────────────

@Composable
private fun AdvertisingSection(gameState: GameState) {
    if (AdManager.isAdSupported) {
        SettingsSection(title = S.current.advertising) {
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
}

// ── Language Section ────────────────────────────────────────────────────────

@Composable
private fun LanguageSection() {
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
}

// ── Support Section ─────────────────────────────────────────────────────────

@Composable
private fun SupportSection(onContactClick: () -> Unit) {
    SettingsSection(title = S.current.support) {
        SettingsClickRow(
            icon = Icons.Default.Email,
            title = S.current.contact,
            subtitle = "support@katchit.app",
            onClick = onContactClick,
        )
    }
}

// ── Legal Section ───────────────────────────────────────────────────────────

@Composable
private fun LegalSection(
    onImpressumClick: () -> Unit,
    onPrivacyClick: () -> Unit,
) {
    SettingsSection(title = S.current.legal) {
        SettingsClickRow(
            icon = Icons.Default.Description,
            title = S.current.impressum,
            onClick = onImpressumClick,
        )
        HorizontalDivider(color = ColorOutlineVariant)
        SettingsClickRow(
            icon = Icons.Default.Shield,
            title = S.current.privacyPolicy,
            onClick = onPrivacyClick,
        )
    }
}

// ── Version Footer ──────────────────────────────────────────────────────────

@Composable
private fun VersionFooter(modifier: Modifier = Modifier) {
    Text(
        "KatchIt! v1.1",
        style = MaterialTheme.typography.bodySmall,
        color = ColorOutline,
        modifier = modifier.padding(top = 8.dp),
    )
}

// ── Profile Section (editable name + avatar) ─────────────────────────────────

@Composable
private fun ProfileSection(
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    var isEditing by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(AppSettings.getString("last_player_name", "")) }
    var avatarBytes by remember { mutableStateOf<ByteArray?>(LocalPhotoStore.loadAvatar("profile")) }
    var isSaving by remember { mutableStateOf(false) }

    // Download avatar from cloud if local cache is empty
    LaunchedEffect(Unit) {
        if (avatarBytes == null || avatarBytes?.isEmpty() == true) {
            val downloaded = AccountManager.downloadProfileAvatar()
            if (downloaded != null && downloaded.isNotEmpty()) {
                avatarBytes = downloaded
            }
        }
    }

    val photoCapturer = rememberPhotoCapturer { bytes ->
        if (bytes != null) {
            avatarBytes = bytes
            try { LocalPhotoStore.saveAvatar("profile", bytes) } catch (e: Exception) {
                pg.geobingo.one.util.AppLogger.w("Settings", "Avatar save failed", e)
            }
        }
    }

    SettingsSection(title = S.current.editProfile) {
        if (isEditing) {
            // Edit mode
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 20) nameInput = it },
                    label = { Text(S.current.displayName, color = ColorOnSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimary,
                        unfocusedBorderColor = ColorOutline,
                        focusedTextColor = ColorOnSurface,
                        unfocusedTextColor = ColorOnSurface,
                        focusedLabelColor = ColorPrimary,
                        cursorColor = ColorPrimary,
                    ),
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = ColorPrimary) },
                )

                SelfiePicker(
                    avatarBytes = avatarBytes,
                    onTakePhoto = { photoCapturer.launch() },
                    onClear = {
                        avatarBytes = null
                        scope.launch {
                            AccountManager.removeProfileAvatar()
                        }
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { isEditing = false }) {
                        Text(S.current.cancel, color = ColorOnSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val name = nameInput.trim()
                            if (name.isEmpty()) return@TextButton
                            if (!pg.geobingo.one.util.NameValidator.isValid(name)) {
                                scope.launch { snackbarHostState.showSnackbar(S.current.nameContainsProfanity) }
                                return@TextButton
                            }
                            isSaving = true
                            scope.launch {
                                AppSettings.setString("last_player_name", name)
                                AccountManager.updateDisplayName(name)
                                val bytes = avatarBytes
                                if (bytes != null && bytes.isNotEmpty()) {
                                    AccountManager.uploadProfileAvatar(bytes)
                                }
                                isSaving = false
                                isEditing = false
                                snackbarHostState.showSnackbar(S.current.profileUpdated)
                            }
                        },
                        enabled = nameInput.trim().isNotEmpty() && !isSaving,
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ColorPrimary)
                        } else {
                            Text(S.current.save, color = ColorPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            // Display mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditing = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PlayerAvatarViewRaw(
                    name = nameInput.ifBlank { "?" },
                    color = ColorPrimary,
                    size = 48.dp,
                    photoBytes = avatarBytes,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        nameInput.ifBlank { "?" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorOnSurface,
                    )
                    Text(
                        AccountManager.currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurfaceVariant,
                    )
                }
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp), tint = ColorPrimary)
            }
        }
    }
}

// ── Account Section (auth + providers + password reset + delete) ──────────────

@Composable
private fun AccountSection(
    isLoggedIn: Boolean,
    currentUser: io.github.jan.supabase.auth.user.UserInfo?,
    gameState: GameState,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    nav: pg.geobingo.one.navigation.NavigationManager,
) {
    var showAuthDialog by remember { mutableStateOf(false) }
    var authLoading by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    val shareManager = rememberShareManager()

    SettingsSection(title = S.current.account) {
        if (isLoggedIn && currentUser != null) {
            // Change email
            SettingsClickRow(
                icon = Icons.Default.Email,
                title = S.current.changeEmail,
                subtitle = S.current.changeEmailDesc,
                onClick = { showChangeEmailDialog = true },
            )
            HorizontalDivider(color = ColorOutlineVariant)
            // Change password
            SettingsClickRow(
                icon = Icons.Default.Lock,
                title = S.current.changePassword,
                subtitle = S.current.changePasswordDesc,
                onClick = { showChangePasswordDialog = true },
            )
            HorizontalDivider(color = ColorOutlineVariant)
            // Invite friends
            SettingsClickRow(
                icon = Icons.Default.Share,
                title = S.current.inviteFriends,
                subtitle = S.current.inviteFriendsDesc,
                onClick = { shareManager.shareText(S.current.inviteFriendsMessage) },
            )
            HorizontalDivider(color = ColorOutlineVariant)
            // Sign out
            SettingsClickRow(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = S.current.signOut,
                onClick = {
                    scope.launch {
                        AccountManager.signOut()
                        snackbarHostState.showSnackbar(S.current.signedOut)
                    }
                },
            )
            HorizontalDivider(color = ColorOutlineVariant)
            // Delete account
            SettingsClickRow(
                icon = Icons.Default.DeleteForever,
                title = S.current.deleteAccount,
                subtitle = S.current.deleteAccountDesc,
                onClick = { showDeleteDialog = true },
            )
        } else {
            // Not logged in - show sign in option
            SettingsClickRow(
                icon = Icons.Default.PersonAdd,
                title = S.current.signIn,
                subtitle = S.current.syncDataDesc,
                onClick = { showAuthDialog = true },
            )
        }
    }

    // ── Auth Dialog (OAuth + Email) ──────────────────────────────────────
    if (showAuthDialog) {
        AuthDialog(
            onDismiss = { if (!authLoading) showAuthDialog = false },
            authLoading = authLoading,
            onAuthLoadingChange = { authLoading = it },
            onSuccess = { isSignUp ->
                showAuthDialog = false
                gameState.stars.reload()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (isSignUp) S.current.accountCreated else S.current.signedIn
                    )
                }
                // Navigate to profile setup after sign-up if name not set
                if (isSignUp && AccountManager.needsProfileSetup) {
                    nav.navigateTo(Screen.PROFILE_SETUP)
                }
            },
            onError = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
            onShowResetPassword = {
                showAuthDialog = false
                showResetDialog = true
            },
        )
    }

    // ── Password Reset Dialog ────────────────────────────────────────────
    if (showResetDialog) {
        ResetPasswordDialog(
            onDismiss = { showResetDialog = false },
            onResult = { msg ->
                showResetDialog = false
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
        )
    }

    // ── Delete Account Dialog ────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = ColorSurface,
            title = {
                Text(S.current.deleteAccountConfirm, color = ColorOnSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(S.current.deleteAccountDesc, color = ColorOnSurfaceVariant)
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    scope.launch {
                        val result = AccountManager.deleteAccount()
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar(S.current.accountDeleted)
                        }
                    }
                }) {
                    Text(S.current.delete, color = ColorError, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(S.current.cancel, color = ColorOnSurfaceVariant)
                }
            },
        )
    }

    // ── Change Email Dialog ─────────────────────────────────────────────
    if (showChangeEmailDialog) {
        ChangeEmailDialog(
            onDismiss = { showChangeEmailDialog = false },
            onResult = { msg ->
                showChangeEmailDialog = false
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
        )
    }

    // ── Change Password Dialog ──────────────────────────────────────────
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onResult = { msg ->
                showChangePasswordDialog = false
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
        )
    }
}

// ── Auth Dialog with OAuth + Email ───────────────────────────────────────────

@Composable
internal fun AuthDialog(
    onDismiss: () -> Unit,
    authLoading: Boolean,
    onAuthLoadingChange: (Boolean) -> Unit,
    onSuccess: (isSignUp: Boolean) -> Unit,
    onError: (String) -> Unit,
    onShowResetPassword: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
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
                // OAuth Buttons
                OAuthButton(
                    text = S.current.continueWithGoogle,
                    icon = Icons.Default.Public,
                    onClick = {
                        onAuthLoadingChange(true)
                        scope.launch {
                            val result = AccountManager.signInWithGoogle()
                            onAuthLoadingChange(false)
                            if (result.isSuccess) {
                                onSuccess(false)
                            } else {
                                errorMsg = S.current.authError
                            }
                        }
                    },
                    enabled = !authLoading,
                )

                OAuthButton(
                    text = S.current.continueWithApple,
                    icon = Icons.Default.PhoneIphone,
                    onClick = {
                        onAuthLoadingChange(true)
                        scope.launch {
                            val result = AccountManager.signInWithApple()
                            onAuthLoadingChange(false)
                            if (result.isSuccess) {
                                onSuccess(false)
                            } else {
                                errorMsg = S.current.authError
                            }
                        }
                    },
                    enabled = !authLoading,
                )

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = ColorOutlineVariant)
                    Text(
                        S.current.orContinueWithEmail,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurfaceVariant,
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = ColorOutlineVariant)
                }

                // Email input
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
                // Password input
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
                    Text(errorMsg ?: "", style = MaterialTheme.typography.bodySmall, color = ColorError)
                }

                // Toggle sign in / sign up + forgot password
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { isSignUp = !isSignUp }) {
                        Text(
                            if (isSignUp) S.current.signIn else S.current.signUp,
                            color = ColorPrimary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    if (!isSignUp) {
                        TextButton(onClick = onShowResetPassword) {
                            Text(
                                S.current.forgotPassword,
                                color = ColorOnSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!email.contains("@") || !email.contains(".")) {
                        errorMsg = S.current.authEmailInvalid
                        return@TextButton
                    }
                    if (password.length < 6) {
                        errorMsg = S.current.passwordTooShort
                        return@TextButton
                    }
                    onAuthLoadingChange(true)
                    errorMsg = null
                    scope.launch {
                        val result = if (isSignUp)
                            AccountManager.signUp(email, password)
                        else
                            AccountManager.signIn(email, password)

                        onAuthLoadingChange(false)
                        if (result.isSuccess) {
                            onSuccess(isSignUp)
                        } else {
                            val msg = result.exceptionOrNull()?.message?.lowercase() ?: ""
                            errorMsg = when {
                                msg.contains("already registered") || msg.contains("already exists") ->
                                    S.current.authEmailAlreadyUsed
                                msg.contains("invalid") ->
                                    S.current.authEmailInvalid
                                msg.contains("network") || msg.contains("socket") ||
                                    msg.contains("timeout") || msg.contains("unreachable") ||
                                    msg.contains("connect") ->
                                    S.current.authNetworkError
                                else ->
                                    S.current.authError
                            }
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
            TextButton(onClick = onDismiss, enabled = !authLoading) {
                Text(S.current.cancel, color = ColorOnSurfaceVariant)
            }
        },
    )
}

@Composable
internal fun OAuthButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = ColorOnSurface,
            disabledContentColor = ColorOnSurfaceVariant,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, ColorOutline),
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Medium)
    }
}

// ── Password Reset Dialog ────────────────────────────────────────────────────

@Composable
internal fun ResetPasswordDialog(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = ColorSurface,
        title = {
            Text(S.current.resetPassword, color = ColorOnSurface, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(S.current.resetPasswordDesc, style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant)
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isLoading = true
                    scope.launch {
                        val result = AccountManager.resetPassword(email)
                        isLoading = false
                        onResult(
                            if (result.isSuccess) S.current.resetPasswordSent
                            else S.current.resetPasswordError
                        )
                    }
                },
                enabled = email.isNotBlank() && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ColorPrimary)
                } else {
                    Text(S.current.resetPassword, color = ColorPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(S.current.cancel, color = ColorOnSurfaceVariant)
            }
        },
    )
}

// ── Change Email Dialog ─────────────────────────────────────────────────────

@Composable
internal fun ChangeEmailDialog(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
) {
    var newEmail by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = ColorSurface,
        title = {
            Text(S.current.changeEmail, color = ColorOnSurface, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(S.current.changeEmailDesc, style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant)
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    placeholder = { Text(S.current.newEmail) },
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
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = ColorPrimary) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isLoading = true
                    scope.launch {
                        val result = AccountManager.changeEmail(newEmail.trim())
                        isLoading = false
                        onResult(
                            if (result.isSuccess) S.current.emailChanged
                            else S.current.emailChangeError
                        )
                    }
                },
                enabled = newEmail.isNotBlank() && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ColorPrimary)
                } else {
                    Text(S.current.save, color = ColorPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(S.current.cancel, color = ColorOnSurfaceVariant)
            }
        },
    )
}

// ── Change Password Dialog ──────────────────────────────────────────────────

@Composable
internal fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = ColorSurface,
        title = {
            Text(S.current.changePassword, color = ColorOnSurface, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(S.current.changePasswordDesc, style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant)
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMsg = null },
                    placeholder = { Text(S.current.newPassword) },
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
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = ColorPrimary) },
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMsg = null },
                    placeholder = { Text(S.current.confirmPassword) },
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
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = ColorPrimary) },
                )
                if (errorMsg != null) {
                    Text(errorMsg ?: "", style = MaterialTheme.typography.bodySmall, color = ColorError)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newPassword.length < 6) {
                        errorMsg = S.current.passwordTooShort
                        return@TextButton
                    }
                    if (newPassword != confirmPassword) {
                        errorMsg = S.current.passwordsDoNotMatch
                        return@TextButton
                    }
                    isLoading = true
                    scope.launch {
                        val result = AccountManager.changePassword(newPassword)
                        isLoading = false
                        onResult(
                            if (result.isSuccess) S.current.passwordChanged
                            else S.current.passwordChangeError
                        )
                    }
                },
                enabled = newPassword.isNotBlank() && confirmPassword.isNotBlank() && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ColorPrimary)
                } else {
                    Text(S.current.save, color = ColorPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(S.current.cancel, color = ColorOnSurfaceVariant)
            }
        },
    )
}

// ── Reusable Setting Components ──────────────────────────────────────────────

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
            .clickable(onClick = onClick)
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
        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = ColorOnSurfaceVariant)
    }
}
