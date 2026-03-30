package pg.geobingo.one.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.state.CosmeticsManager
import pg.geobingo.one.game.state.NameEffect
import pg.geobingo.one.game.state.ProfileFrame
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.components.CosmeticPlayerName
import pg.geobingo.one.ui.components.FramedAvatar
import pg.geobingo.one.ui.components.MiniShopPopup
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosmeticShopScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    var showMiniShop by remember { mutableStateOf(false) }
    var miniShopNeeded by remember { mutableStateOf(0) }

    // Force recomposition on purchase
    var purchaseCounter by remember { mutableStateOf(0) }
    val equippedFrameId = remember(purchaseCounter) { CosmeticsManager.getEquippedFrameId() }
    val equippedNameId = remember(purchaseCounter) { CosmeticsManager.getEquippedNameEffectId() }

    val playerName = AppSettings.getString("last_player_name", "Player")
    val avatarBytes = LocalPhotoStore.loadAvatar("profile")

    SystemBackHandler { nav.goBack() }

    if (showMiniShop) {
        MiniShopPopup(
            gameState = gameState,
            neededStars = miniShopNeeded,
            onDismiss = { showMiniShop = false },
            onPurchased = { showMiniShop = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.cosmeticShop,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        gradientColors = GradientPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.goBack() }) {
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Preview card
            GradientBorderCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                borderColors = GradientPrimary,
                backgroundColor = ColorSurface,
                borderWidth = 1.5.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FramedAvatar(frameId = equippedFrameId, size = 56.dp) {
                        PlayerAvatarViewRaw(
                            name = playerName,
                            color = ColorPrimary,
                            size = 48.dp,
                            photoBytes = avatarBytes,
                        )
                    }
                    Column {
                        Text("Vorschau", style = MaterialTheme.typography.labelSmall, color = ColorOnSurfaceVariant)
                        CosmeticPlayerName(
                            name = playerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            nameEffectId = equippedNameId,
                        )
                    }
                }
            }

            // ── Profile Frames ──────────────────────────────────────────
            Text(
                S.current.profileFrames,
                style = MaterialTheme.typography.labelMedium,
                color = ColorOnSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CosmeticsManager.ALL_FRAMES.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { frame ->
                            FrameCard(
                                frame = frame,
                                isOwned = CosmeticsManager.isOwned(frame.id),
                                isEquipped = equippedFrameId == frame.id,
                                onBuy = {
                                    if (gameState.stars.spend(frame.starsCost)) {
                                        CosmeticsManager.purchase(frame.id)
                                        CosmeticsManager.setEquippedFrame(frame.id)
                                        purchaseCounter++
                                    } else {
                                        miniShopNeeded = frame.starsCost
                                        showMiniShop = true
                                    }
                                },
                                onEquip = {
                                    CosmeticsManager.setEquippedFrame(frame.id)
                                    purchaseCounter++
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            // ── Name Effects ────────────────────────────────────────────
            Text(
                S.current.nameEffects,
                style = MaterialTheme.typography.labelMedium,
                color = ColorOnSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CosmeticsManager.ALL_NAME_EFFECTS.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { effect ->
                            NameEffectCard(
                                effect = effect,
                                playerName = playerName,
                                isOwned = CosmeticsManager.isOwned(effect.id),
                                isEquipped = equippedNameId == effect.id,
                                onBuy = {
                                    if (gameState.stars.spend(effect.starsCost)) {
                                        CosmeticsManager.purchase(effect.id)
                                        CosmeticsManager.setEquippedNameEffect(effect.id)
                                        purchaseCounter++
                                    } else {
                                        miniShopNeeded = effect.starsCost
                                        showMiniShop = true
                                    }
                                },
                                onEquip = {
                                    CosmeticsManager.setEquippedNameEffect(effect.id)
                                    purchaseCounter++
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FrameCard(
    frame: ProfileFrame,
    isOwned: Boolean,
    isEquipped: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColors = if (frame.borderColors.any { it != Color.Transparent } && frame.borderColors.size >= 2)
        frame.borderColors else GradientPrimary

    GradientBorderCard(
        modifier = modifier,
        cornerRadius = 14.dp,
        borderColors = if (isEquipped) borderColors else listOf(ColorOutlineVariant, ColorOutlineVariant),
        backgroundColor = ColorSurface,
        borderWidth = if (isEquipped) 1.5.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Frame preview circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(
                        if (frame.borderColors.any { it != Color.Transparent }) frame.borderColors
                        else listOf(ColorSurfaceVariant, ColorSurfaceVariant)
                    )),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ColorSurface),
                )
            }

            Text(
                frame.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = ColorOnSurface,
            )

            when {
                isEquipped -> {
                    Text(S.current.equipped, style = MaterialTheme.typography.labelSmall, color = ColorPrimary, fontWeight = FontWeight.Bold)
                }
                isOwned -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ColorPrimary.copy(alpha = 0.1f))
                            .clickable { onEquip() }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(S.current.equip, style = MaterialTheme.typography.labelSmall, color = ColorPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                frame.starsCost > 0 -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(GradientGold))
                            .clickable { onBuy() }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Text("${frame.starsCost}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NameEffectCard(
    effect: NameEffect,
    playerName: String,
    isOwned: Boolean,
    isEquipped: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GradientBorderCard(
        modifier = modifier,
        cornerRadius = 14.dp,
        borderColors = if (isEquipped && effect.gradientColors.size >= 2) effect.gradientColors else listOf(ColorOutlineVariant, ColorOutlineVariant),
        backgroundColor = ColorSurface,
        borderWidth = if (isEquipped) 1.5.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Name preview
            CosmeticPlayerName(
                name = playerName.take(8),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                nameEffectId = effect.id,
            )

            Text(
                effect.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = ColorOnSurface,
            )

            when {
                isEquipped -> {
                    Text(S.current.equipped, style = MaterialTheme.typography.labelSmall, color = ColorPrimary, fontWeight = FontWeight.Bold)
                }
                isOwned -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ColorPrimary.copy(alpha = 0.1f))
                            .clickable { onEquip() }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(S.current.equip, style = MaterialTheme.typography.labelSmall, color = ColorPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                effect.starsCost > 0 -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(GradientGold))
                            .clickable { onBuy() }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Text("${effect.starsCost}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
