package pg.geobingo.one.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.platform.rememberShareManager
import pg.geobingo.one.ui.components.SelfiePicker
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareManager = rememberShareManager()
    SystemBackHandler { nav.goBack() }

    val isLoggedIn = AccountManager.isLoggedIn
    val currentUser = AccountManager.currentUser

    var showAuthDialog by remember { mutableStateOf(false) }
    var authLoading by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.account,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goHome() }) {
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
            if (isLoggedIn && currentUser != null) {
                // Profile section
                AccountProfileSection(snackbarHostState, scope)

                // Account info
                AccountInfoSection(AccountManager.displayEmail)

                // Account actions
                AccountActionsSection(
                    onChangeEmail = { showChangeEmailDialog = true },
                    onChangePassword = { showChangePasswordDialog = true },
                    onInviteFriends = { shareManager.shareText(S.current.inviteFriendsMessage) },
                )

                // Danger zone
                AccountDangerSection(
                    onSignOut = {
                        scope.launch {
                            AccountManager.signOut()
                            snackbarHostState.showSnackbar(S.current.signedOut)
                        }
                    },
                    onDeleteAccount = { showDeleteDialog = true },
                )
            } else {
                // Not logged in
                NotLoggedInSection(
                    onSignIn = { showAuthDialog = true },
                )
            }
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────────────
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
                if (isSignUp && AccountManager.needsProfileSetup) {
                    nav.navigateTo(Screen.PROFILE_SETUP)
                }
            },
            onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
            onShowResetPassword = {
                showAuthDialog = false
                showResetDialog = true
            },
        )
    }

    if (showResetDialog) {
        ResetPasswordDialog(
            onDismiss = { showResetDialog = false },
            onResult = { msg ->
                showResetDialog = false
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = ColorSurface,
            title = { Text(S.current.deleteAccountConfirm, color = ColorOnSurface, fontWeight = FontWeight.Bold) },
            text = { Text(S.current.deleteAccountDesc, color = ColorOnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    scope.launch {
                        val result = AccountManager.deleteAccount()
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar(S.current.accountDeleted)
                        }
                    }
                }) { Text(S.current.delete, color = ColorError, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(S.current.cancel, color = ColorOnSurfaceVariant)
                }
            },
        )
    }

    if (showChangeEmailDialog) {
        ChangeEmailDialog(
            onDismiss = { showChangeEmailDialog = false },
            onResult = { msg -> showChangeEmailDialog = false; scope.launch { snackbarHostState.showSnackbar(msg) } },
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onResult = { msg -> showChangePasswordDialog = false; scope.launch { snackbarHostState.showSnackbar(msg) } },
        )
    }
}

// ── Profile Section ─────────────────────────────────────────────────────────

@Composable
private fun AccountProfileSection(
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val profileVersion = AccountManager.profileVersion
    var isEditing by remember { mutableStateOf(false) }
    // Re-seed on profileVersion bumps so a new sign-in / cloud sync actually
    // pulls the fresh name + avatar instead of sticking with the value from
    // the previous session's first composition.
    var nameInput by remember(profileVersion) { mutableStateOf(AppSettings.getString("last_player_name", "")) }
    var avatarBytes by remember(profileVersion) { mutableStateOf<ByteArray?>(LocalPhotoStore.loadAvatar("profile")) }
    var isSaving by remember { mutableStateOf(false) }

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
                pg.geobingo.one.util.AppLogger.w("Account", "Avatar save failed", e)
            }
        }
    }

    AccountSectionCard(title = S.current.editProfile) {
        if (isEditing) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 20) nameInput = it },
                    label = { Text(S.current.displayName, color = ColorOnSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimary, unfocusedBorderColor = ColorOutline,
                        focusedTextColor = ColorOnSurface, unfocusedTextColor = ColorOnSurface,
                        focusedLabelColor = ColorPrimary, cursorColor = ColorPrimary,
                    ),
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = ColorPrimary) },
                )
                SelfiePicker(
                    avatarBytes = avatarBytes,
                    onTakePhoto = { photoCapturer.launch() },
                    onClear = {
                        avatarBytes = null
                        scope.launch { AccountManager.removeProfileAvatar() }
                    },
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { isEditing = false }) { Text(S.current.cancel, color = ColorOnSurfaceVariant) }
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
                                val result = AccountManager.updateDisplayName(name)
                                if (result.isFailure) {
                                    isSaving = false
                                    val msg = if (result.exceptionOrNull()?.message?.contains("display_name_rejected") == true)
                                        S.current.nameContainsProfanity
                                    else S.current.authError
                                    snackbarHostState.showSnackbar(msg)
                                    return@launch
                                }
                                val bytes = avatarBytes
                                if (bytes != null && bytes.isNotEmpty()) {
                                    val avatarResult = AccountManager.uploadProfileAvatar(bytes)
                                    if (avatarResult.isFailure &&
                                        avatarResult.exceptionOrNull()?.message?.contains("image_rejected") == true) {
                                        isSaving = false
                                        snackbarHostState.showSnackbar(S.current.imageRejectedByModeration)
                                        return@launch
                                    }
                                }
                                isSaving = false; isEditing = false
                                snackbarHostState.showSnackbar(S.current.profileUpdated)
                            }
                        },
                        enabled = nameInput.trim().isNotEmpty() && !isSaving,
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ColorPrimary)
                        else Text(S.current.save, color = ColorPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { isEditing = true }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PlayerAvatarViewRaw(name = nameInput.ifBlank { "?" }, color = ColorPrimary, size = 48.dp, photoBytes = avatarBytes)
                Column(modifier = Modifier.weight(1f)) {
                    Text(nameInput.ifBlank { "?" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = ColorOnSurface)
                    Text(AccountManager.displayEmail, style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant)
                }
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp), tint = ColorPrimary)
            }
        }
    }
}

// ── Account Info ────────────────────────────────────────────────────────────

@Composable
private fun AccountInfoSection(email: String) {
    AccountSectionCard(title = S.current.account) {
        AccountRow(icon = Icons.Default.Email, title = "E-Mail", subtitle = email)
    }
}

// ── Account Actions ─────────────────────────────────────────────────────────

@Composable
private fun AccountActionsSection(
    onChangeEmail: () -> Unit,
    onChangePassword: () -> Unit,
    onInviteFriends: () -> Unit,
) {
    AccountSectionCard(title = S.current.general) {
        AccountClickRow(icon = Icons.Default.Email, title = S.current.changeEmail, subtitle = S.current.changeEmailDesc, onClick = onChangeEmail)
        HorizontalDivider(color = ColorOutlineVariant)
        AccountClickRow(icon = Icons.Default.Lock, title = S.current.changePassword, subtitle = S.current.changePasswordDesc, onClick = onChangePassword)
        HorizontalDivider(color = ColorOutlineVariant)
        AccountClickRow(icon = Icons.Default.Share, title = S.current.inviteFriends, subtitle = S.current.inviteFriendsDesc, onClick = onInviteFriends)
    }
}

// ── Danger Zone ─────────────────────────────────────────────────────────────

@Composable
private fun AccountDangerSection(onSignOut: () -> Unit, onDeleteAccount: () -> Unit) {
    AccountSectionCard(title = "") {
        AccountClickRow(icon = Icons.AutoMirrored.Filled.ExitToApp, title = S.current.signOut, onClick = onSignOut)
        HorizontalDivider(color = ColorOutlineVariant)
        AccountClickRow(icon = Icons.Default.DeleteForever, title = S.current.deleteAccount, subtitle = S.current.deleteAccountDesc, onClick = onDeleteAccount)
    }
}

// ── Not Logged In ───────────────────────────────────────────────────────────

@Composable
private fun NotLoggedInSection(onSignIn: () -> Unit) {
    AccountSectionCard(title = S.current.account) {
        AccountClickRow(icon = Icons.Default.PersonAdd, title = S.current.signIn, subtitle = S.current.syncDataDesc, onClick = onSignIn)
    }
}

// ── Reusable Components ─────────────────────────────────────────────────────

@Composable
private fun AccountSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        if (title.isNotBlank()) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = ColorOnSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ColorSurface).padding(horizontal = 16.dp, vertical = 4.dp),
            content = content,
        )
    }
}

@Composable
private fun AccountRow(icon: ImageVector, title: String, subtitle: String? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, tint = ColorPrimary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = ColorOnSurface, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AccountClickRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, tint = ColorPrimary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = ColorOnSurface, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ColorOnSurfaceVariant, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp), tint = ColorOnSurfaceVariant)
    }
}
