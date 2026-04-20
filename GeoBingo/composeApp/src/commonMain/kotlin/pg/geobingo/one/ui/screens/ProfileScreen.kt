package pg.geobingo.one.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.game.state.CosmeticsManager
import pg.geobingo.one.game.state.SoloStatsManager
import pg.geobingo.one.i18n.S
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.platform.rememberPhotoCapturer
import pg.geobingo.one.platform.toImageBitmap
import pg.geobingo.one.ui.components.PlayerBanner
import pg.geobingo.one.ui.components.PlayerBannerSize
import pg.geobingo.one.ui.components.rememberLocalUserCosmetics
import pg.geobingo.one.ui.theme.*

private val ProfileGradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val profileVersion = AccountManager.profileVersion
    val isLoggedIn = AccountManager.isLoggedIn
    val scope = rememberCoroutineScope()

    val playerName = remember(profileVersion) { AppSettings.getString("last_player_name", "Player") }
    val avatarBytes = remember(profileVersion) { LocalPhotoStore.loadAvatar("profile") }
    val localCosmetics = rememberLocalUserCosmetics()

    // Multiplayer stats (AppSettings-backed, cloud-synced for signed-in users)
    val gamesPlayedMP = AppSettings.getInt(SettingsKeys.GAMES_PLAYED, 0)
    val gamesWonMP = AppSettings.getInt(SettingsKeys.GAMES_WON, 0)
    val winRateMP = if (gamesPlayedMP > 0) (gamesWonMP * 100 / gamesPlayedMP) else 0
    val longestStreak = AppSettings.getInt(SettingsKeys.LONGEST_WIN_STREAK, 0)

    // Solo stats (separately tracked via SoloStatsManager)
    val soloStats = remember(profileVersion) { SoloStatsManager.getStats() }
    val totalGames = gamesPlayedMP + soloStats.gamesPlayed

    var showEditDialog by remember { mutableStateOf(false) }

    SystemBackHandler { nav.goBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.profile,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = ProfileGradient,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goHome() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.current.back, tint = ColorPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { nav.navigateTo(Screen.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = S.current.settingsTitle, tint = ColorPrimary)
                    }
                    pg.geobingo.one.ui.components.TopBarStarsAndProfile(gameState = gameState, onNavigate = { nav.navigateTo(it) })
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // Banner + edit button overlay — tapping the pencil opens an
            // inline dialog for name + avatar changes without leaving the
            // profile screen.
            Box(modifier = Modifier.fillMaxWidth()) {
                PlayerBanner(
                    name = playerName,
                    cosmetics = localCosmetics,
                    avatarBytes = avatarBytes,
                    avatarColor = ProfileGradient.first(),
                    size = PlayerBannerSize.Hero,
                    subtitle = if (isLoggedIn) AccountManager.displayEmail.ifBlank { null } else null,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable { showEditDialog = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Edit, contentDescription = S.current.editProfile, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Stats (meaningful mix of solo + multiplayer) ───────────
            if (isLoggedIn) {
                // Combined totals first
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileStatCard(Icons.Default.SportsEsports, "$totalGames", S.current.gamesPlayed, ProfileGradient, Modifier.weight(1f))
                    ProfileStatCard(Icons.Default.Star, "${soloStats.totalStars + (gamesWonMP * 5)}", S.current.starsEarnedLifetime, ProfileGradient, Modifier.weight(1f))
                }

                // Solo-specific stats (always relevant since Solo is the primary mode)
                if (soloStats.gamesPlayed > 0) {
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileStatCard(Icons.Default.MilitaryTech, "${soloStats.bestScore}", S.current.bestScore, ProfileGradient, Modifier.weight(1f))
                        ProfileStatCard(Icons.Default.EmojiEvents, "${soloStats.perfectGames}", S.current.perfectGames, ProfileGradient, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileStatCard(Icons.Default.CameraAlt, "${soloStats.totalCaptures}", S.current.categoriesCaptured, ProfileGradient, Modifier.weight(1f))
                        ProfileStatCard(Icons.Default.AutoAwesome, "${soloStats.allCapturedCount}", S.current.fullBingos, ProfileGradient, Modifier.weight(1f))
                    }
                }

                // Multiplayer-specific stats — only when the user has actually
                // played multiplayer, so solo-only players don't see confusing
                // 0% winrate.
                if (gamesPlayedMP > 0) {
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileStatCard(Icons.Default.Groups, "$gamesPlayedMP", S.current.multiplayerGames, ProfileGradient, Modifier.weight(1f))
                        ProfileStatCard(Icons.Default.Percent, "$winRateMP%", S.current.winRate, ProfileGradient, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileStatCard(Icons.Default.LocalFireDepartment, "$longestStreak", S.current.winStreak, ProfileGradient, Modifier.weight(1f))
                        ProfileStatCard(Icons.Default.EmojiEvents, "$gamesWonMP", S.current.wins, ProfileGradient, Modifier.weight(1f))
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    S.current.signInRequiredDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Owned cosmetics collection ────────────────────────────
            OwnedCosmeticsSection(onOpenShop = { nav.navigateTo(Screen.COSMETIC_SHOP) })

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { nav.navigateTo(Screen.HISTORY) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ProfileGradient.first().copy(alpha = 0.5f)),
            ) {
                Icon(Icons.Default.History, null, tint = ProfileGradient.first(), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(S.current.history, color = ProfileGradient.first())
            }

            if (!isLoggedIn) {
                Spacer(Modifier.height(16.dp))
                GradientButton(
                    text = S.current.signIn,
                    onClick = { nav.navigateTo(Screen.ACCOUNT) },
                    gradientColors = ProfileGradient,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentName = playerName,
            currentAvatar = avatarBytes,
            onDismiss = { showEditDialog = false },
            onSaved = { newName ->
                scope.launch {
                    // Persist locally and sync to cloud via AccountManager so
                    // display name updates across the signed-in profile.
                    AppSettings.setString("last_player_name", newName)
                    if (AccountManager.isLoggedIn) {
                        AccountManager.updateDisplayName(newName)
                    } else {
                        AccountManager.bumpProfileVersion()
                    }
                    showEditDialog = false
                }
            },
        )
    }
}

@Composable
private fun EditProfileDialog(
    currentName: String,
    currentAvatar: ByteArray?,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit,
) {
    var nameInput by remember { mutableStateOf(currentName) }
    var previewAvatar by remember { mutableStateOf(currentAvatar) }

    val photoCapturer = rememberPhotoCapturer { bytes ->
        if (bytes != null) {
            previewAvatar = bytes
            try {
                LocalPhotoStore.saveAvatar("profile", bytes)
                AccountManager.bumpProfileVersion()
            } catch (_: Exception) { /* ignore save errors */ }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorSurface,
        title = { Text(S.current.editProfile, fontWeight = FontWeight.Bold, color = ColorOnSurface) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Tappable avatar — replaces the picture immediately via the
                // shared rememberPhotoCapturer flow.
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(ColorSurfaceVariant)
                        .border(2.dp, ProfileGradient.first(), CircleShape)
                        .clickable { photoCapturer.launch() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (previewAvatar != null) {
                        val img = remember(previewAvatar) {
                            try { previewAvatar!!.toImageBitmap() } catch (_: Exception) { null }
                        }
                        if (img != null) {
                            androidx.compose.foundation.Image(
                                bitmap = img,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp).clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            )
                        } else {
                            Icon(Icons.Default.Person, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(48.dp))
                        }
                    } else {
                        Icon(Icons.Default.PhotoCamera, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(36.dp))
                    }
                }
                Text(
                    S.current.tapAvatarToChange,
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 24) nameInput = it },
                    label = { Text(S.current.displayName) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (nameInput.isNotBlank()) onSaved(nameInput.trim()) },
                enabled = nameInput.isNotBlank(),
            ) {
                Text(S.current.save, color = ProfileGradient.first(), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(S.current.cancel, color = ColorOnSurfaceVariant)
            }
        },
    )
}

/**
 * Horizontal strips showing all owned cosmetics grouped by category. Tapping
 * an item equips it instantly; the profile banner above updates via the
 * shared equippedRevision flow so the change is visible immediately.
 */
@Composable
private fun OwnedCosmeticsSection(onOpenShop: () -> Unit) {
    val rev by CosmeticsManager.equippedRevision.collectAsState()

    val ownedFrames = remember(rev) { CosmeticsManager.ALL_FRAMES.filter { CosmeticsManager.isOwned(it.id) || it.starsCost == 0 } }
    val ownedEffects = remember(rev) { CosmeticsManager.ALL_NAME_EFFECTS.filter { CosmeticsManager.isOwned(it.id) || it.starsCost == 0 } }
    val ownedTitles = remember(rev) { CosmeticsManager.ALL_TITLES.filter { CosmeticsManager.isOwned(it.id) || it.starsCost == 0 } }
    val ownedBanners = remember(rev) { CosmeticsManager.ALL_BANNER_BACKGROUNDS.filter { CosmeticsManager.isOwned(it.id) || it.starsCost == 0 } }
    val ownedCards = remember(rev) { CosmeticsManager.ALL_CARD_DESIGNS.filter { CosmeticsManager.isOwned(it.id) || it.starsCost == 0 } }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Inventory2, null, tint = ProfileGradient.first(), modifier = Modifier.size(18.dp))
                Text(
                    S.current.myCollection,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnSurface,
                )
            }
            TextButton(onClick = onOpenShop) {
                Text(S.current.shop, color = ProfileGradient.first())
                Spacer(Modifier.width(2.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = ProfileGradient.first(), modifier = Modifier.size(14.dp).graphicsLayerFlipX())
            }
        }

        CosmeticRow(
            label = S.current.profileFrames,
            items = ownedFrames.map { CosmeticChip(it.id, it.name, it.borderColors, equippedId = CosmeticsManager.getEquippedFrameId()) },
            onEquip = { CosmeticsManager.setEquippedFrame(it) },
        )
        CosmeticRow(
            label = S.current.nameEffects,
            items = ownedEffects.map { CosmeticChip(it.id, it.name, it.gradientColors, equippedId = CosmeticsManager.getEquippedNameEffectId()) },
            onEquip = { CosmeticsManager.setEquippedNameEffect(it) },
        )
        CosmeticRow(
            label = S.current.playerTitles,
            items = ownedTitles.map { CosmeticChip(it.id, it.name, listOf(it.color, it.color.copy(alpha = 0.6f)), equippedId = CosmeticsManager.getEquippedTitleId()) },
            onEquip = { CosmeticsManager.setEquippedTitle(it) },
        )
        CosmeticRow(
            label = S.current.bannerBackgrounds,
            items = ownedBanners.map { CosmeticChip(it.id, it.name, it.gradientColors, equippedId = CosmeticsManager.getEquippedBannerBackgroundId()) },
            onEquip = { CosmeticsManager.setEquippedBannerBackground(it) },
        )
        CosmeticRow(
            label = S.current.cardDesigns,
            items = ownedCards.map { CosmeticChip(it.id, it.name, it.backgroundColors, equippedId = CosmeticsManager.getEquippedCardDesignId()) },
            onEquip = { CosmeticsManager.setEquippedCardDesign(it) },
        )
    }
}

private data class CosmeticChip(
    val id: String,
    val name: String,
    val colors: List<Color>,
    val equippedId: String?,
)

@Composable
private fun CosmeticRow(
    label: String,
    items: List<CosmeticChip>,
    onEquip: (String) -> Unit,
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = ColorOnSurfaceVariant,
            letterSpacing = 0.5.sp,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { chip ->
                val equipped = chip.id == chip.equippedId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (chip.colors.size >= 2) Brush.linearGradient(chip.colors)
                            else Brush.linearGradient(listOf(chip.colors.firstOrNull() ?: ColorSurfaceVariant, chip.colors.firstOrNull() ?: ColorSurfaceVariant))
                        )
                        .border(
                            width = if (equipped) 2.dp else 1.dp,
                            color = if (equipped) Color.White else Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { onEquip(chip.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (equipped) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Text(chip.name, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    GradientBorderCard(
        modifier = modifier,
        cornerRadius = 14.dp,
        borderColors = gradientColors,
        backgroundColor = ColorSurface,
        borderWidth = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = gradientColors.first(), modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ColorOnSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
        }
    }
}

private fun Modifier.graphicsLayerFlipX(): Modifier = this.then(
    Modifier.graphicsLayer { scaleX = -1f }
)
