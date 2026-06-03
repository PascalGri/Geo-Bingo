package pg.geobingo.one.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
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
import pg.geobingo.one.ui.components.MiniShopPopup
import pg.geobingo.one.ui.components.PlayerBanner
import pg.geobingo.one.ui.components.PlayerBannerSize
import pg.geobingo.one.ui.components.ScrollToTopTags
import pg.geobingo.one.ui.components.ShopTabSwitcher
import pg.geobingo.one.ui.theme.*

// ──────────────────────────────────────────────────────────────────────
//  Filter categories + rarity tiers
// ──────────────────────────────────────────────────────────────────────

private enum class CosmeticCategory { ALL, FRAMES, NAMES, TITLES, BANNERS, CARDS }

/** Visual rarity derived purely from star price — no separate catalog field. */
private enum class Rarity { STANDARD, RARE, EPIC, LEGENDARY, ULTIMATE }

private fun rarityFor(cost: Int): Rarity = when {
    CosmeticsManager.isUltimate(cost) -> Rarity.ULTIMATE
    cost >= 200 -> Rarity.LEGENDARY
    cost >= 100 -> Rarity.EPIC
    cost >= 50 -> Rarity.RARE
    else -> Rarity.STANDARD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosmeticShopScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    var showMiniShop by remember { mutableStateOf(false) }
    var miniShopNeeded by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()
    val purchaseScope = rememberCoroutineScope()
    CollectScrollToTop(ScrollToTopTags.SHOP_COSMETICS, scrollState)

    // Start every new cosmetic's 7-day NEU window from the first time this
    // user opens the shop (stored per-id in AppSettings as a yyyy-MM-dd date).
    LaunchedEffect(Unit) { CosmeticsManager.markNewCosmeticsSeen() }

    var selectedCategory by remember { mutableStateOf(CosmeticCategory.ALL) }

    // Reading starCount here subscribes the screen to balance changes so the
    // affordability hint ("-X") refreshes the moment the balance updates.
    val starBalance = gameState.stars.starCount

    // Unified purchase flow: server-authoritative when logged in (via purchase_cosmetic RPC),
    // local-only for guests. Caller only supplies item ID + fallback cost.
    // Resolves the cosmetic's display name across all categories so the
    // reward overlay can show "Galaxie freigeschaltet" instead of a bare ID.
    fun resolveCosmeticName(id: String): String {
        CosmeticsManager.ALL_FRAMES.firstOrNull { it.id == id }?.let { return it.name }
        CosmeticsManager.ALL_NAME_EFFECTS.firstOrNull { it.id == id }?.let { return it.name }
        CosmeticsManager.ALL_TITLES.firstOrNull { it.id == id }?.let { return it.name }
        CosmeticsManager.ALL_BANNER_BACKGROUNDS.firstOrNull { it.id == id }?.let { return it.name }
        CosmeticsManager.ALL_CARD_DESIGNS.firstOrNull { it.id == id }?.let { return it.name }
        return id
    }

    fun tryPurchase(
        cosmeticId: String,
        localCost: Int,
        onAcquired: () -> Unit,
    ) {
        val fireRewardOverlay = {
            gameState.ui.pendingReward = pg.geobingo.one.game.state.RewardEvent(
                label = resolveCosmeticName(cosmeticId),
                stars = 0,
                emoji = "\u2728", // sparkles
            )
        }
        if (AccountManager.isLoggedIn) {
            purchaseScope.launch {
                // Push local star count to the server FIRST so daily bonuses,
                // ad rewards and challenge payouts that only updated locally
                // are reflected before the purchase_cosmetic RPC runs its
                // `star_count >= cost` check. Without this, a user who's
                // earned stars offline sees "insufficient stars" even when
                // their visible balance is enough.
                AccountManager.currentUserId?.let { AccountManager.syncLocalToCloud(it) }
                when (val res = CosmeticsManager.purchaseCosmeticCloud(cosmeticId)) {
                    is CosmeticsManager.PurchaseResult.Success -> {
                        gameState.stars.setBalance(res.newStarBalance)
                        onAcquired()
                        fireRewardOverlay()
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
                            fireRewardOverlay()
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
                fireRewardOverlay()
            } else {
                miniShopNeeded = localCost
                showMiniShop = true
            }
        }
    }

    // Force recomposition on purchase / equip
    var purchaseCounter by remember { mutableStateOf(0) }
    val equippedFrameId = remember(purchaseCounter) { CosmeticsManager.getEquippedFrameId() }
    val equippedNameId = remember(purchaseCounter) { CosmeticsManager.getEquippedNameEffectId() }
    val equippedTitleId = remember(purchaseCounter) { CosmeticsManager.getEquippedTitleId() }
    val equippedCardDesignId = remember(purchaseCounter) { CosmeticsManager.getEquippedCardDesignId() }
    val equippedBannerBgId = remember(purchaseCounter) { CosmeticsManager.getEquippedBannerBackgroundId() }

    // Collection progress across every *purchasable* cosmetic (free defaults excluded).
    val purchasableIds = remember {
        (CosmeticsManager.ALL_FRAMES.map { it.id to it.starsCost } +
            CosmeticsManager.ALL_NAME_EFFECTS.map { it.id to it.starsCost } +
            CosmeticsManager.ALL_TITLES.map { it.id to it.starsCost } +
            CosmeticsManager.ALL_BANNER_BACKGROUNDS.map { it.id to it.starsCost } +
            CosmeticsManager.ALL_CARD_DESIGNS.map { it.id to it.starsCost })
            .filter { it.second > 0 }
            .map { it.first }
    }
    val totalCount = purchasableIds.size
    val ownedCount = remember(purchaseCounter) { purchasableIds.count { CosmeticsManager.isOwned(it) } }

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

    val visible: (CosmeticCategory) -> Boolean = {
        selectedCategory == CosmeticCategory.ALL || selectedCategory == it
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
        // Layout: tab switcher, live preview, collection progress and the
        // category filter are pinned under the TopAppBar so the user always
        // sees their loadout + can switch filters — only the lists scroll.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ShopTabSwitcher(activeScreen = Screen.COSMETIC_SHOP)
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
                CollectionProgress(owned = ownedCount, total = totalCount)
                CategoryChipRow(selected = selectedCategory, onSelect = { selectedCategory = it })
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = Spacing.screenHorizontal)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // ── Profile Frames ──────────────────────────────────────────
                if (visible(CosmeticCategory.FRAMES)) {
                    CosmeticSectionHeader(title = S.current.profileFrames, icon = Icons.Default.AccountCircle)
                    TwoColumnGrid(CosmeticsManager.ALL_FRAMES.sortedBy { it.starsCost }) { frame ->
                        CosmeticCard(
                            name = frame.name,
                            cost = frame.starsCost,
                            isOwned = CosmeticsManager.isOwned(frame.id),
                            isEquipped = equippedFrameId == frame.id,
                            isNew = CosmeticsManager.isNew(frame.id),
                            rarity = rarityFor(frame.starsCost),
                            starBalance = starBalance,
                            accentColors = frame.borderColors
                                .takeIf { it.size >= 2 && it.any { c -> c != Color.Transparent } } ?: GradientPrimary,
                            onBuy = {
                                tryPurchase(frame.id, frame.starsCost) {
                                    CosmeticsManager.setEquippedFrame(frame.id); purchaseCounter++
                                }
                            },
                            onEquip = { CosmeticsManager.setEquippedFrame(frame.id); purchaseCounter++ },
                            modifier = Modifier.weight(1f),
                        ) { FramePreview(frame) }
                    }
                }

                // ── Name Effects ────────────────────────────────────────────
                if (visible(CosmeticCategory.NAMES)) {
                    CosmeticSectionHeader(title = S.current.nameEffects, icon = Icons.Default.AutoAwesome)
                    TwoColumnGrid(CosmeticsManager.ALL_NAME_EFFECTS.sortedBy { it.starsCost }) { effect ->
                        CosmeticCard(
                            name = effect.name,
                            cost = effect.starsCost,
                            isOwned = CosmeticsManager.isOwned(effect.id),
                            isEquipped = equippedNameId == effect.id,
                            isNew = CosmeticsManager.isNew(effect.id),
                            rarity = rarityFor(effect.starsCost),
                            starBalance = starBalance,
                            accentColors = effect.gradientColors.takeIf { it.size >= 2 } ?: GradientPrimary,
                            onBuy = {
                                tryPurchase(effect.id, effect.starsCost) {
                                    CosmeticsManager.setEquippedNameEffect(effect.id); purchaseCounter++
                                }
                            },
                            onEquip = { CosmeticsManager.setEquippedNameEffect(effect.id); purchaseCounter++ },
                            modifier = Modifier.weight(1f),
                        ) { NamePreview(effect, playerName) }
                    }
                }

                // ── Player Titles ────────────────────────────────────────────
                if (visible(CosmeticCategory.TITLES)) {
                    CosmeticSectionHeader(title = S.current.playerTitles, icon = Icons.Default.MilitaryTech)
                    TwoColumnGrid(CosmeticsManager.ALL_TITLES.sortedBy { it.starsCost }) { title ->
                        CosmeticCard(
                            name = title.name,
                            cost = title.starsCost,
                            isOwned = CosmeticsManager.isOwned(title.id),
                            isEquipped = equippedTitleId == title.id,
                            isNew = CosmeticsManager.isNew(title.id),
                            rarity = rarityFor(title.starsCost),
                            starBalance = starBalance,
                            accentColors = listOf(title.color, title.color.copy(alpha = 0.6f)),
                            onBuy = {
                                tryPurchase(title.id, title.starsCost) {
                                    CosmeticsManager.setEquippedTitle(title.id); purchaseCounter++
                                }
                            },
                            onEquip = { CosmeticsManager.setEquippedTitle(title.id); purchaseCounter++ },
                            modifier = Modifier.weight(1f),
                            showName = false, // the title chip already shows its name
                        ) { TitlePreview(title) }
                    }
                }

                // ── Banner Backgrounds ───────────────────────────────────────
                if (visible(CosmeticCategory.BANNERS)) {
                    CosmeticSectionHeader(title = S.current.bannerBackgrounds, icon = Icons.Default.Wallpaper)
                    TwoColumnGrid(CosmeticsManager.ALL_BANNER_BACKGROUNDS.sortedBy { it.starsCost }) { bg ->
                        CosmeticCard(
                            name = bg.name,
                            cost = bg.starsCost,
                            isOwned = CosmeticsManager.isOwned(bg.id),
                            isEquipped = equippedBannerBgId == bg.id,
                            isNew = CosmeticsManager.isNew(bg.id),
                            rarity = rarityFor(bg.starsCost),
                            starBalance = starBalance,
                            accentColors = bg.gradientColors.takeIf { it.size >= 2 } ?: GradientPrimary,
                            minHeight = 160.dp,
                            onBuy = {
                                tryPurchase(bg.id, bg.starsCost) {
                                    CosmeticsManager.setEquippedBannerBackground(bg.id); purchaseCounter++
                                }
                            },
                            onEquip = { CosmeticsManager.setEquippedBannerBackground(bg.id); purchaseCounter++ },
                            modifier = Modifier.weight(1f),
                        ) { BannerPreview(bg, playerName) }
                    }
                }

                // ── Card Designs ─────────────────────────────────────────────
                if (visible(CosmeticCategory.CARDS)) {
                    CosmeticSectionHeader(title = S.current.cardDesigns, icon = Icons.Default.Palette)
                    TwoColumnGrid(CosmeticsManager.ALL_CARD_DESIGNS.sortedBy { it.starsCost }) { design ->
                        CosmeticCard(
                            name = design.name,
                            cost = design.starsCost,
                            isOwned = CosmeticsManager.isOwned(design.id),
                            isEquipped = equippedCardDesignId == design.id,
                            isNew = CosmeticsManager.isNew(design.id),
                            rarity = rarityFor(design.starsCost),
                            starBalance = starBalance,
                            accentColors = design.backgroundColors.takeIf { it.size >= 2 } ?: GradientPrimary,
                            minHeight = 160.dp,
                            onBuy = {
                                tryPurchase(design.id, design.starsCost) {
                                    CosmeticsManager.setEquippedCardDesign(design.id); purchaseCounter++
                                }
                            },
                            onEquip = { CosmeticsManager.setEquippedCardDesign(design.id); purchaseCounter++ },
                            modifier = Modifier.weight(1f),
                        ) { CardDesignPreview(design) }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Generic 2-column grid — lets call sites apply Modifier.weight(1f) on
//  each card (RowScope receiver) so a trailing single card stays half-width.
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun <T> TwoColumnGrid(items: List<T>, content: @Composable RowScope.(T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { content(it) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Generic cosmetic card shell — one composable for all five categories.
//  The category-specific visual goes in the [preview] slot.
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun CosmeticCard(
    name: String,
    cost: Int,
    isOwned: Boolean,
    isEquipped: Boolean,
    isNew: Boolean,
    rarity: Rarity,
    starBalance: Int,
    accentColors: List<Color>,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 152.dp,
    showName: Boolean = true,
    preview: @Composable () -> Unit,
) {
    val affordable = isOwned || cost == 0 || starBalance >= cost
    val borderColors = when {
        isEquipped -> accentColors
        rarity == Rarity.ULTIMATE -> UltimateBorder
        rarity == Rarity.LEGENDARY -> LegendaryBorder
        rarity == Rarity.EPIC -> EpicBorder
        rarity == Rarity.RARE -> RareBorder
        else -> listOf(ColorOutlineVariant, ColorOutlineVariant)
    }
    val borderWidth = when {
        isEquipped || rarity == Rarity.ULTIMATE -> 2.dp
        rarity == Rarity.LEGENDARY || rarity == Rarity.EPIC -> 1.5.dp
        else -> 1.dp
    }

    Box(modifier = modifier) {
        GradientBorderCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            borderColors = borderColors,
            backgroundColor = ColorSurface,
            borderWidth = borderWidth,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = minHeight).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                preview()

                if (showName) {
                    Text(
                        name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorOnSurface,
                        maxLines = 1,
                    )
                }

                Spacer(Modifier.weight(1f))

                when {
                    isEquipped -> EquippedLabel()
                    isOwned -> EquipPill(onClick = onEquip)
                    cost > 0 -> BuyPill(
                        cost = cost,
                        affordable = affordable,
                        shortBy = (cost - starBalance).coerceAtLeast(0),
                        onClick = onBuy,
                    )
                }
            }
        }

        // Top-start badge: Ultimate is rarest and always shows its tag; otherwise
        // NEU wins over rarity, and only the top tiers get a rarity tag (Rare/
        // Standard read from the border colour alone).
        when {
            rarity == Rarity.ULTIMATE ->
                RarityCornerBadge(S.current.rarityUltimate, UltimateBadgeColors, Modifier.align(Alignment.TopStart))
            isNew -> NewCornerBadge(modifier = Modifier.align(Alignment.TopStart))
            rarity == Rarity.LEGENDARY ->
                RarityCornerBadge(S.current.rarityLegendary, LegendaryBadgeColors, Modifier.align(Alignment.TopStart))
            rarity == Rarity.EPIC ->
                RarityCornerBadge(S.current.rarityEpic, EpicBadgeColors, Modifier.align(Alignment.TopStart))
            else -> {}
        }
        if (isOwned && !isEquipped) OwnedCornerBadge()
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Category-specific preview slots
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun FramePreview(frame: ProfileFrame) {
    val coloured = frame.borderColors.any { it != Color.Transparent }
    Box(contentAlignment = Alignment.Center) {
        if (coloured) {
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
                .background(
                    Brush.linearGradient(
                        if (coloured) frame.borderColors else listOf(ColorSurfaceVariant, ColorSurfaceVariant)
                    )
                ),
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
}

@Composable
private fun NamePreview(effect: NameEffect, playerName: String) {
    Spacer(Modifier.height(6.dp))
    CosmeticPlayerName(
        name = playerName.take(8),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        nameEffectId = effect.id,
    )
}

@Composable
private fun TitlePreview(title: PlayerTitle) {
    Spacer(Modifier.height(4.dp))
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
            maxLines = 1,
        )
    }
}

@Composable
private fun CardDesignPreview(design: CardDesign) {
    // Mini 3x3 bingo grid — makes it visually distinct from the banner card
    // so users see at a glance this cosmetic applies to in-game cards.
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
}

@Composable
private fun BannerPreview(background: BannerBackground, playerName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (background.gradientColors.size >= 2)
                    Brush.linearGradient(background.gradientColors)
                else
                    Brush.linearGradient(listOf(background.gradientColors.first(), background.gradientColors.first()))
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
}

// ──────────────────────────────────────────────────────────────────────
//  Category filter chips
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryChipRow(selected: CosmeticCategory, onSelect: (CosmeticCategory) -> Unit) {
    val chips = listOf(
        Triple(CosmeticCategory.ALL, S.current.shopAll, Icons.Default.Style),
        Triple(CosmeticCategory.FRAMES, S.current.shopCatFrames, Icons.Default.AccountCircle),
        Triple(CosmeticCategory.NAMES, S.current.shopCatNames, Icons.Default.AutoAwesome),
        Triple(CosmeticCategory.TITLES, S.current.shopCatTitles, Icons.Default.MilitaryTech),
        Triple(CosmeticCategory.BANNERS, S.current.shopCatBanners, Icons.Default.Wallpaper),
        Triple(CosmeticCategory.CARDS, S.current.shopCatCards, Icons.Default.Palette),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { (cat, label, icon) ->
            CategoryChip(
                label = label,
                icon = icon,
                selected = cat == selected,
                onClick = { onSelect(cat) },
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) ColorPrimary.copy(alpha = 0.15f) else ColorSurface,
        animationSpec = tween(200),
        label = "chipBg",
    )
    val outline by animateColorAsState(
        targetValue = if (selected) ColorPrimary else ColorOutlineVariant,
        animationSpec = tween(200),
        label = "chipOutline",
    )
    val content by animateColorAsState(
        targetValue = if (selected) ColorPrimary else ColorOnSurfaceVariant,
        animationSpec = tween(200),
        label = "chipContent",
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, outline, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(15.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Collection progress bar
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun CollectionProgress(owned: Int, total: Int) {
    val fraction = if (total > 0) owned.toFloat() / total else 0f
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Default.Inventory2, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(14.dp))
            Text(
                S.current.shopUnlocked(owned, total),
                style = MaterialTheme.typography.labelMedium,
                color = ColorOnSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
        // Box-based bar (avoids Material3 progress-API version ambiguity).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(ColorOutlineVariant.copy(alpha = 0.4f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(GradientPrimary)),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Shared pills + badges
// ──────────────────────────────────────────────────────────────────────

// Matte gold palette — desaturated so the cost pill reads calm rather than
// glitzy. A single subtle top-bottom gradient gives a soft sheen.
private val MatteGoldTop = Color(0xFFCA8A04)
private val MatteGoldBottom = Color(0xFFA16207)

// Rarity border palettes (derived from price). Rare = cool blue, Epic =
// violet, Legendary = gold, Ultimate = holographic iridescence. Standard items
// keep the neutral outline. The Ultimate palette loops back to its first colour
// so the animated GradientBorderCard sweep stays continuous.
private val RareBorder = listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9))
private val EpicBorder = listOf(Color(0xFFA855F7), Color(0xFF7C3AED), Color(0xFFA855F7))
private val LegendaryBorder = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFEAB308), Color(0xFFFBBF24))
private val UltimateBorder = listOf(
    Color(0xFF22D3EE), Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFFFBBF24), Color(0xFF22D3EE),
)
private val EpicBadgeColors = listOf(Color(0xFFA855F7), Color(0xFF7C3AED))
private val LegendaryBadgeColors = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
private val UltimateBadgeColors = listOf(Color(0xFF22D3EE), Color(0xFFA855F7), Color(0xFFEC4899))
private val NewBadgeColors = listOf(Color(0xFF22D3EE), Color(0xFF6366F1))

@Composable
private fun NewCornerBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(NewBadgeColors))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            S.current.shopNew,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun RarityCornerBadge(label: String, colors: List<Color>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(colors))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(9.dp))
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun BuyPill(cost: Int, affordable: Boolean, shortBy: Int, onClick: () -> Unit) {
    val pillShape = RoundedCornerShape(50)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(pillShape)
                .background(Brush.verticalGradient(listOf(MatteGoldTop, MatteGoldBottom)))
                .alpha(if (affordable) 1f else 0.5f)
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp), tint = Color(0xFFFEF3C7))
                Text(
                    "$cost",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFFEF9C3),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        // Subtle "how many stars short" hint — nudges toward the star shop
        // without blocking the tap (tapping still routes to the MiniShop).
        if (!affordable && shortBy > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(Icons.Default.Star, null, modifier = Modifier.size(9.dp), tint = ColorOnSurfaceVariant)
                Text(
                    "-$shortBy",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
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
    // Compact pinned preview: the banner itself already has its own
    // gradient background and title row, so we drop the outer card chrome
    // to reclaim vertical space on iPhone — the banner stays Hero-sized so
    // the cosmetic details remain readable.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, ColorPrimary.copy(alpha = 0.28f), RoundedCornerShape(18.dp)),
    ) {
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
