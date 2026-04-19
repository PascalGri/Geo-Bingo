package pg.geobingo.one.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
            PreviewHero(
                playerName = playerName,
                avatarBytes = avatarBytes,
                cosmetics = PlayerCosmetics(
                    frameId = equippedFrameId,
                    nameEffectId = equippedNameId,
                    titleId = equippedTitleId,
                    bannerBackgroundId = equippedBannerBgId,
                ),
            )

            // ── Profile Frames ──────────────────────────────────────────
            CosmeticSectionHeader(title = S.current.profileFrames, icon = Icons.Default.AccountCircle)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            CosmeticSectionHeader(title = S.current.nameEffects, icon = Icons.Default.AutoAwesome)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            CosmeticSectionHeader(title = S.current.playerTitles, icon = Icons.Default.MilitaryTech)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            CosmeticSectionHeader(title = S.current.bannerBackgrounds, icon = Icons.Default.Wallpaper)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            CosmeticSectionHeader(title = S.current.cardDesigns, icon = Icons.Default.Palette)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

    Box(modifier = modifier) {
        GradientBorderCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            borderColors = if (isEquipped) borderColors else listOf(ColorOutlineVariant, ColorOutlineVariant),
            backgroundColor = ColorSurface,
            borderWidth = if (isEquipped) 2.dp else 1.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 148.dp).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Frame preview — slightly larger, with soft glow halo if coloured.
                Box(contentAlignment = Alignment.Center) {
                    if (frame.borderColors.any { it != Color.Transparent }) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            (frame.borderColors.firstOrNull() ?: ColorPrimary).copy(alpha = 0.35f),
                                            Color.Transparent,
                                        ),
                                    )
                                ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(
                                if (frame.borderColors.any { it != Color.Transparent }) frame.borderColors
                                else listOf(ColorSurfaceVariant, ColorSurfaceVariant)
                            )),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ColorSurface),
                        )
                    }
                }

                Text(
                    frame.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorOnSurface,
                )

                Spacer(Modifier.weight(1f))

                when {
                    isEquipped -> EquippedLabel()
                    isOwned -> EquipPill(onClick = onEquip)
                    frame.starsCost > 0 -> BuyPill(cost = frame.starsCost, onClick = onBuy)
                }
            }
        }
        if (isOwned && !isEquipped) OwnedCornerBadge()
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
    Box(modifier = modifier) {
        GradientBorderCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            borderColors = if (isEquipped && effect.gradientColors.size >= 2) effect.gradientColors else listOf(ColorOutlineVariant, ColorOutlineVariant),
            backgroundColor = ColorSurface,
            borderWidth = if (isEquipped) 2.dp else 1.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 148.dp).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(Modifier.height(6.dp))
                // Name preview with its effect applied so the user sees exactly
                // what it'll look like in-game.
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

                Spacer(Modifier.weight(1f))

                when {
                    isEquipped -> EquippedLabel()
                    isOwned -> EquipPill(onClick = onEquip)
                    effect.starsCost > 0 -> BuyPill(cost = effect.starsCost, onClick = onBuy)
                }
            }
        }
        if (isOwned && !isEquipped) OwnedCornerBadge()
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
    Box(modifier = modifier) {
        GradientBorderCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            borderColors = if (isEquipped) listOf(title.color, title.color.copy(alpha = 0.6f)) else listOf(ColorOutlineVariant, ColorOutlineVariant),
            backgroundColor = ColorSurface,
            borderWidth = if (isEquipped) 2.dp else 1.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 148.dp).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                // Bigger title chip preview
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(title.color.copy(alpha = 0.18f))
                        .border(1.dp, title.color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        title.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = title.color,
                    )
                }

                Spacer(Modifier.weight(1f))

                when {
                    isEquipped -> EquippedLabel()
                    isOwned -> EquipPill(onClick = onEquip)
                    title.starsCost > 0 -> BuyPill(cost = title.starsCost, onClick = onBuy)
                }
            }
        }
        if (isOwned && !isEquipped) OwnedCornerBadge()
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

    Box(modifier = modifier) {
        GradientBorderCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            borderColors = if (isEquipped) borderColors else listOf(ColorOutlineVariant, ColorOutlineVariant),
            backgroundColor = ColorSurface,
            borderWidth = if (isEquipped) 2.dp else 1.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Mini 3x3 bingo grid preview — makes it visually distinct from
                // BannerBackgroundCard so users see at a glance this cosmetic
                // applies to in-game cards, not the profile banner.
                val brush = if (design.backgroundColors.size >= 2)
                    Brush.linearGradient(design.backgroundColors)
                else
                    Brush.linearGradient(listOf(design.backgroundColors.first(), design.backgroundColors.first()))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 18.dp, height = 14.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(brush),
                                )
                            }
                        }
                    }
                }

                Text(
                    design.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorOnSurface,
                )

                Spacer(Modifier.weight(1f))

                when {
                    isEquipped -> EquippedLabel()
                    isOwned -> EquipPill(onClick = onEquip)
                    design.starsCost > 0 -> BuyPill(cost = design.starsCost, onClick = onBuy)
                }
            }
        }
        if (isOwned && !isEquipped) OwnedCornerBadge()
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

    Box(modifier = modifier) {
        GradientBorderCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            borderColors = if (isEquipped) borderColors else listOf(ColorOutlineVariant, ColorOutlineVariant),
            backgroundColor = ColorSurface,
            borderWidth = if (isEquipped) 2.dp else 1.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Banner gradient preview with player name — visualises what it
                // will actually look like on the profile banner.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
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
                        style = MaterialTheme.typography.labelLarge,
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

                Spacer(Modifier.weight(1f))

                when {
                    isEquipped -> EquippedLabel()
                    isOwned -> EquipPill(onClick = onEquip)
                    background.starsCost > 0 -> BuyPill(cost = background.starsCost, onClick = onBuy)
                }
            }
        }
        if (isOwned && !isEquipped) OwnedCornerBadge()
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Shared pills
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun BuyPill(cost: Int, onClick: () -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "buyPillPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (reduceMotion) 1f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "buyPillScale",
    )
    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(GradientGold))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        // Subtle top-shine overlay.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                    )
                ),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp), tint = Color.White)
            Text(
                "$cost",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EquipPill(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ColorPrimary.copy(alpha = 0.15f))
            .border(1.dp, ColorPrimary, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            S.current.equip,
            style = MaterialTheme.typography.labelSmall,
            color = ColorPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Shared section header + preview hero
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun CosmeticSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
    ) {
        Icon(icon, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(14.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = ColorOnSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun PreviewHero(
    playerName: String,
    avatarBytes: ByteArray?,
    cosmetics: PlayerCosmetics,
) {
    val reduceMotion = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "heroCosmeticShimmer")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cosmeticShimmer",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A0B2E),
                        Color(0xFF2B0F45),
                        Color(0xFF3D0A45),
                    ),
                    start = Offset(shimmer, 0f),
                    end = Offset(shimmer + 500f, 500f),
                )
            )
            .border(1.dp, ColorPrimary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = ColorPrimary, modifier = Modifier.size(14.dp))
            Text(
                "VORSCHAU",
                style = MaterialTheme.typography.labelSmall,
                color = ColorPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }
        PlayerBanner(
            name = playerName,
            cosmetics = cosmetics,
            avatarBytes = avatarBytes,
            avatarColor = ColorPrimary,
            size = PlayerBannerSize.Hero,
        )
    }
}

/** Prominent "EQUIPPED" pill — clearly distinct from Buy / Equip buttons. */
@Composable
private fun EquippedLabel() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ColorPrimary.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(Icons.Default.Check, null, tint = ColorPrimary, modifier = Modifier.size(12.dp))
        Text(
            S.current.equipped.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp),
            color = ColorPrimary,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** Small corner badge that marks "owned but not equipped" items. */
@Composable
private fun BoxScope.OwnedCornerBadge() {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .clip(CircleShape)
            .background(ColorSuccess.copy(alpha = 0.9f))
            .size(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Check,
            null,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
    }
}

