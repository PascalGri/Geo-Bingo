package pg.geobingo.one.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.components.SelfiePicker
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.util.NameValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var nameInput by remember { mutableStateOf(AppSettings.getString("last_player_name", "")) }
    var avatarBytes by remember { mutableStateOf<ByteArray?>(LocalPhotoStore.loadAvatar("profile")) }
    var isLoading by remember { mutableStateOf(false) }

    val photoCapturer = rememberPhotoCapturer { bytes ->
        if (bytes != null) {
            avatarBytes = bytes
            try { LocalPhotoStore.saveAvatar("profile", bytes) } catch (e: Exception) {
                pg.geobingo.one.util.AppLogger.w("ProfileSetup", "Avatar save failed", e)
            }
        }
    }

    SystemBackHandler { /* block back during setup */ }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TextButton(onClick = {
                        // Skip profile setup, go home
                        nav.resetTo(Screen.HOME)
                    }) {
                        Text(S.current.profileSetupSkip, color = ColorOnSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorBackground),
            )
        },
        containerColor = ColorBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            AnimatedGradientText(
                text = S.current.profileSetupTitle,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                gradientColors = GradientPrimary,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                S.current.profileSetupSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = ColorOnSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            // Avatar picker
            SelfiePicker(
                avatarBytes = avatarBytes,
                onTakePhoto = { photoCapturer.launch() },
                onClear = {
                    avatarBytes = null
                    try { LocalPhotoStore.deleteAvatar("profile") } catch (e: Exception) {
                        pg.geobingo.one.util.AppLogger.w("ProfileSetup", "Avatar clear failed", e)
                    }
                },
            )

            Spacer(Modifier.height(32.dp))

            // Name input
            OutlinedTextField(
                value = nameInput,
                onValueChange = { if (it.length <= 20) nameInput = it },
                label = { Text(S.current.profileSetupNameLabel, color = ColorOnSurfaceVariant) },
                placeholder = { Text(S.current.namePlaceholder, color = ColorOutline) },
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
                    focusedContainerColor = ColorSurface,
                    unfocusedContainerColor = ColorSurface,
                ),
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = ColorPrimary)
                },
            )

            Spacer(Modifier.height(40.dp))

            // Complete button
            GradientButton(
                text = if (isLoading) S.current.loading else S.current.profileSetupComplete,
                onClick = {
                    val name = nameInput.trim()
                    if (name.isEmpty()) {
                        scope.launch { snackbarHostState.showSnackbar(S.current.profileSetupNameRequired) }
                        return@GradientButton
                    }
                    if (!NameValidator.isValid(name)) {
                        scope.launch { snackbarHostState.showSnackbar(S.current.nameContainsProfanity) }
                        return@GradientButton
                    }
                    isLoading = true
                    scope.launch {
                        // Sync to cloud first — server-side moderation may reject
                        // the name; only persist locally if the DB accepts it.
                        val result = AccountManager.updateDisplayName(name)
                        if (result.isFailure) {
                            isLoading = false
                            val msg = if (result.exceptionOrNull()?.message?.contains("display_name_rejected") == true)
                                S.current.nameContainsProfanity else S.current.authError
                            snackbarHostState.showSnackbar(msg)
                            return@launch
                        }
                        // Upload avatar if present
                        val bytes = avatarBytes
                        if (bytes != null && bytes.isNotEmpty()) {
                            AccountManager.uploadProfileAvatar(bytes)
                        }
                        isLoading = false
                        nav.resetTo(Screen.HOME)
                    }
                },
                enabled = nameInput.trim().isNotEmpty() && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                gradientColors = GradientPrimary,
                leadingIcon = if (isLoading) {
                    { CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp) }
                } else {
                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp), tint = androidx.compose.ui.graphics.Color.White) }
                },
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}
