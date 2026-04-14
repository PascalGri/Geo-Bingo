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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.state.BannerBackground
import pg.geobingo.one.game.state.CardDesign
import pg.geobingo.one.game.state.CosmeticsManager
import pg.geobingo.one.game.state.NameEffect
import pg.geobingo.one.game.state.PlayerTitle
import pg.geobingo.one.game.state.ProfileFrame
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.network.PlayerCosmetics
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.LocalPhotoStore
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.components.CollectScrollToTop
import pg.geobingo.one.ui.components.CosmeticPlayerName
import pg.geobingo.one.ui.components.FramedAvatar
import pg.geobingo.one.ui.components.MiniShopPopup
import pg.geobingo.one.ui.components.PlayerBanner
import pg.geobingo.one.ui.components.PlayerBannerSize
import pg.geobingo.one.ui.components.ScrollToTopTags
import pg.geobingo.one.ui.components.ShopTabSwitcher
import pg.geobingo.one.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosmeticShopScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    var showMiniShop by remember { mutableStateOf(false) }
    var miniShopNeeded by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()
    val purchaseScope = rememberCoroutineScope()
    CollectScrollToTop(ScrollToTopTags.SHOP_COSMETICS, scrollState)

    // Unified purchase flow: server-authoritative when logged in (via purchase_cosmetic RPC),
    // local-only for guests. Caller only supplies item ID + fallback cost.
    fun tryPurchase(
        cosmeticId: String,
        localCost: Int,
        onAcquired: () -> Unit,
    ) {
        if (AccountManager.isLoggedIn) {
            purchaseScope.launch {
                when (val res = CosmeticsManager.purchaseCosmeticCloud(cosmeticId)) {
                    is CosmeticsManager.PurchaseResult.Success -> {
                        gameState.stars.setBalance(res.newStarBalance)
                        onAcquired()
                    }
                    is CosmeticsManager.PurchaseResult.InsufficientStars -> {
                        miniShopNeeded = localCost
                        showMiniShop = true
                    }
                    is CosmeticsManager.PurchaseResult.UnknownCosmetic,
                    is CosmeticsManager.PurchaseResult.NotAuthenticated,
                    is CosmeticsManager.PurchaseResult.Error -> {
                        // Fall back to local flow on transient errors
                        if (gameState.stars.spend(localCost)) {
                            CosmeticsManager.purchase(cosmeticId)
                            onAcquired()
                        } else {
                            miniShopNeeded = localCost
                            showMiniShop = true
                        }
                    }
                }
            }
        } else {
            if (gameState.stars.spend(localCost)) {
                CosmeticsManager.purchase(cosmeticId)
                onAcquired()
            } else {
                miniShopNeeded = localCost
                showMiniShop = true
            }
        }
    }

    // Force recomposition on purchase
    var purchaseCounter by remember { mutableStateOf(0) }
    val equippedFrameId = remember(purchaseCounter) { CosmeticsManager.getEquippedFrameId() }
    val equippedNameId = remember(purchaseCounter) { CosmeticsManager.getEquippedNameEffectId() }
    val equippedTitleId = remember(purchaseCounter) { CosmeticsManager.getEquippedTitleId() }
    val equippedCardDesignId = remember(purchaseCounter) { CosmeticsManager.getEquippedCardDesignId() }
    val equippedBannerBgId = remember(purchaseCounter) { CosmeticsManager.getEquippedBannerBackgroundId() }

    val profileVersion = pg.geobingo.one.network.AccountManager.profileVersion
    val playerName = remember(profileVersion) { AppSettings.getString("last_player_name", "Player") }
    val avatarBytes = remember(profileVersion) { LocalPhotoStore.loadAvatar("profile") }

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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Stars / Cosmetics Tab Switcher ──────────────────────────
            ShopTabSwitcher(activeScreen = pg.geobingo.one.game.Screen.COSMETIC_SHOP)

            // ── Live preview banner (Rocket-League-style) ───────────────
            Text(
                "Vorschau",
                style = MaterialTheme.typography.labelMedium,
                color = ColorOnSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp),
            )
            PlayerBanner(
                name = playerName,
                cosmetics = PlayerCosmetics(
                    frameId = equippedFrameId,
                    nameEffectId = equippedNameId,
                    titleId = equippedTitleId,
                    bannerBackgroundId = equippedBannerBgId,
                ),
                avatarBytes = avatarBytes,
                avatarColor = ColorPrimary,
                size = PlayerBannerSize.Hero,
            )

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
                                    tryPurchase(frame.id, frame.starsCost) {
                                        CosmeticsManager.setEquippedFrame(frame.id)
                                        purchaseCounter++
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
                                    tryPurchase(effect.id, effect.starsCost) {
                                        CosmeticsManager.setEquippedNameEffect(effect.id)
                                        purchaseCounter++
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

            // ── Player Titles ────────────────────────────────────────────
            Text(
                S.current.playerTitles,
                style = MaterialTheme.typography.labelMedium,
                color = ColorOnSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CosmeticsManager.ALL_TITLES.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { title ->
                            PlayerTitleCard(
                                title = title,
                                isOwned = CosmeticsManager.isOwned(title.id),
                                isEquipped = equippedTitleId == title.id,
                                onBuy = {
                                    tryPurchase(title.id, title.starsCost) {
                                        CosmeticsManager.setEquippedTitle(title.id)
                                        purchaseCounter++
                                    }
                                },
                                onEquip = {
                                    CosmeticsManager.setEquippedTitle(title.id)
                                    purchaseCounter++
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            // ── Banner Backgrounds (NEW in v1.3) ─────────────────────────
            Text(
                S.current.bannerBackgrounds,
                style = MaterialTheme.typography.labelMedium,
                color = ColorOnSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CosmeticsManager.ALL_BANNER_BACKGROUNDS.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { bg ->
                            BannerBackgroundCard(
                                background = bg,
                                playerName = playerName,
                                isOwned = CosmeticsManager.isOwned(bg.id),
                                isEquipped = equippedBannerBgId == bg.id,
                                onBuy = {
                                    tryPurchase(bg.id, bg.starsCost) {
                                        CosmeticsManager.setEquippedBannerBackground(bg.id)
                                        purchaseCounter++
                                    }
                                },
                                onEquip = {
                                    CosmeticsManager.setEquippedBannerBackground(bg.id)
                                    purchaseCounter++
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            // ── Card Designs ─────────────────────────────────────────────
            Text(
                S.current.cardDesigns,
                style = MaterialTheme.typography.labelMedium,
                color = ColorOnSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CosmeticsManager.ALL_CARD_DESIGNS.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { design ->
                            CardDesignCard(
                                design = design,
                                isOwned = CosmeticsManager.isOwned(design.id),
                                isEquipped = equippedCardDesignId == design.id,
                                onBuy = {
                                    tryPurchase(design.id, design.starsCost) {
                                        CosmeticsManager.setEquippedCardDesign(design.id)
                                        purchaseCounter++
                                    }
                                },
                                onEquip = {
                                    CosmeticsManager.setEquippedCardDesign(design.id)
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

@Composable
private fun PlayerTitleCard(
    title: PlayerTitle,
    isOwned: Boolean,
    isEquipped: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GradientBorderCard(
        modifier = modifier,
        cornerRadius = 14.dp,
        borderColors = if (isEquipped) listOf(title.color, title.color.copy(alpha = 0.6f)) else listOf(ColorOutlineVariant, ColorOutlineVariant),
        backgroundColor = ColorSurface,
        borderWidth = if (isEquipped) 1.5.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Title preview badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(title.color.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    title.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = title.color,
                )
            }

            Text(
                title.name,
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
                title.starsCost > 0 -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(GradientGold))
                            .clickable { onBuy() }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Text("${title.starsCost}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardDesignCard(
    design: CardDesign,
    isOwned: Boolean,
    isEquipped: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColors = if (design.backgroundColors.size >= 2) design.backgroundColors else GradientPrimary

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
            // Gradient preview rectangle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (design.backgroundColors.size >= 2)
                            Brush.linearGradient(design.backgroundColors)
                        else
                            Brush.linearGradient(listOf(design.backgroundColors.first(), design.backgroundColors.first()))
                    ),
            )

            Text(
                design.name,
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
                design.starsCost > 0 -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(GradientGold))
                            .clickable { onBuy() }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Text("${design.starsCost}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerBackgroundCard(
    background: BannerBackground,
    playerName: String,
    isOwned: Boolean,
    isEquipped: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColors = if (background.gradientColors.size >= 2) background.gradientColors else GradientPrimary

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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Banner gradient preview rectangle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (background.gradientColors.size >= 2)
                            Brush.linearGradient(background.gradientColors)
                        else
                            Brush.linearGradient(
                                listOf(background.gradientColors.first(), background.gradientColors.first()),
                            )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = playerName.take(8),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Text(
                background.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = ColorOnSurface,
            )

            when {
                isEquipped -> {
                    Text(
                        S.current.equipped,
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                isOwned -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ColorPrimary.copy(alpha = 0.1f))
                            .clickable { onEquip() }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            S.current.equip,
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                background.starsCost > 0 -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(GradientGold))
                            .clickable { onBuy() }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Text(
                                "${background.starsCost}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
