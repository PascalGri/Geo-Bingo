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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.BillingManager
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.components.CollectScrollToTop
import pg.geobingo.one.ui.components.ScrollToTopTags
import pg.geobingo.one.ui.components.ShopTabSwitcher
import pg.geobingo.one.ui.theme.*

private data class StarPackage(
    val stars: Int,
    val price: String,
    val productId: String,
    val bonus: Int = 0,
    val badge: StarBadge? = null,
    val tier: StarTier,
)

private enum class StarBadge { POPULAR, BEST_VALUE }

private enum class StarTier { BRONZE, SILVER, GOLD, DIAMOND }

private data class SkipPackage(
    val cards: Int,
    val price: String,
    val productId: String,
)

private val STAR_PACKAGES = listOf(
    StarPackage(50, "0,99 \u20AC", "pg.geobingo.one.stars_50", bonus = 0, tier = StarTier.BRONZE),
    StarPackage(150, "1,99 \u20AC", "pg.geobingo.one.stars_150", bonus = 15, tier = StarTier.SILVER),
    StarPackage(400, "3,99 \u20AC", "pg.geobingo.one.stars_400", bonus = 40, badge = StarBadge.POPULAR, tier = StarTier.GOLD),
    StarPackage(1000, "7,99 \u20AC", "pg.geobingo.one.stars_1000", bonus = 150, badge = StarBadge.BEST_VALUE, tier = StarTier.DIAMOND),
)

private val SKIP_PACKAGES = listOf(
    SkipPackage(3, "0,99 \u20AC", "pg.geobingo.one.skip_3"),
    SkipPackage(10, "2,99 \u20AC", "pg.geobingo.one.skip_10"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val snackbarHostState = remember { SnackbarHostState() }
    var purchaseLoading by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    CollectScrollToTop(ScrollToTopTags.SHOP_STARS, scrollState)

    LaunchedEffect(Unit) { gameState.stars.resetDailyChallengeIfNewDay() }

    SystemBackHandler { nav.goBack() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedGradientText(
                        text = S.current.shop,
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ShopTabSwitcher(activeScreen = pg.geobingo.one.game.Screen.SHOP)

            // Balance hero — big, warm, lets the user see what they've got.
            BalanceHeroCard(stars = gameState.stars.starCount)

            // Earn-stars rewarded-ad card
            if (pg.geobingo.one.platform.AdManager.isAdSupported) {
                ShopSection(title = S.current.earnStars, icon = Icons.Default.PlayCircle) {
                    EarnStarsCard(gameState = gameState)
                }
            }

            // Redeem-code section — giveaways, influencer drops, apology gifts.
            ShopSection(title = S.current.redeemCode, icon = Icons.Default.CardGiftcard) {
                RedeemCodeCard(gameState = gameState)
            }

            if (!BillingManager.isBillingSupported) {
                BillingUnavailableNote()
            }

            // ── Stars Packages ─────────────────────────────────────────
            if (BillingManager.isBillingSupported) ShopSection(title = S.current.buyStars, icon = Icons.Default.Star) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    STAR_PACKAGES.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { pkg ->
                                StarShopCard(
                                    modifier = Modifier.weight(1f),
                                    pkg = pkg,
                                    loading = purchaseLoading == pkg.productId,
                                    onClick = {
                                        purchaseLoading = pkg.productId
                                        BillingManager.purchaseProduct(
                                            productId = pkg.productId,
                                            onSuccess = {
                                                val total = pkg.stars + pkg.bonus
                                                gameState.stars.add(total)
                                                gameState.ui.pendingReward = pg.geobingo.one.game.state.RewardEvent(
                                                    label = S.current.starsEarned,
                                                    stars = total,
                                                )
                                                purchaseLoading = null
                                            },
                                            onError = { purchaseLoading = null },
                                        )
                                    },
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Ad-Skipper Cards ────────────────────────────────────────
            if (BillingManager.isBillingSupported) ShopSection(title = S.current.adSkipper, icon = Icons.Default.Style) {
                Text(
                    S.current.skipCardsRemaining(gameState.stars.skipCardsCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SKIP_PACKAGES.forEach { pkg ->
                        SkipShopCard(
                            modifier = Modifier.weight(1f),
                            pkg = pkg,
                            loading = purchaseLoading == pkg.productId,
                            onClick = {
                                purchaseLoading = pkg.productId
                                BillingManager.purchaseProduct(
                                    productId = pkg.productId,
                                    onSuccess = {
                                        gameState.stars.addSkipCards(pkg.cards)
                                        gameState.ui.pendingReward = pg.geobingo.one.game.state.RewardEvent(
                                            label = S.current.skipCardsEarned(pkg.cards),
                                            stars = 0,
                                            emoji = "\uD83C\uDCCF",
                                        )
                                        purchaseLoading = null
                                    },
                                    onError = { purchaseLoading = null },
                                )
                            },
                        )
                    }
                }
            }

            // ── Remove Ads ──────────────────────────────────────────────
            if (BillingManager.isBillingSupported && !gameState.stars.noAdsPurchased) {
                ShopSection(title = S.current.removeAds, icon = Icons.Default.Block) {
                    RemoveAdsCard(
                        loading = purchaseLoading == "no_ads",
                        onClick = {
                            purchaseLoading = "no_ads"
                            BillingManager.purchaseProduct(
                                productId = "pg.geobingo.one.no_ads",
                                onSuccess = {
                                    gameState.stars.updateNoAdsPurchased(true)
                                    gameState.ui.pendingReward = pg.geobingo.one.game.state.RewardEvent(
                                        label = S.current.rewardAdsRemoved,
                                        stars = 0,
                                        emoji = "\uD83D\uDEAB",
                                    )
                                    purchaseLoading = null
                                },
                                onError = { purchaseLoading = null },
                            )
                        },
                    )
                }
            } else if (BillingManager.isBillingSupported && gameState.stars.noAdsPurchased) {
                ShopSection(title = S.current.removeAds, icon = Icons.Default.Block) {
                    AdsAlreadyRemovedCard()
                }
            }

            // ── Restore Purchases ───────────────────────────────────────
            if (BillingManager.isBillingSupported) TextButton(
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
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Default.Refresh, null, tint = ColorOnSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(S.current.restorePurchases, style = MaterialTheme.typography.labelMedium, color = ColorOnSurfaceVariant)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Balance hero
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun BalanceHeroCard(stars: Int) {
    val reduceMotion = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "heroPulse")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroShimmer",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF3D2500),
                        Color(0xFF5A3200),
                        Color(0xFF3D1A2D),
                    ),
                    start = Offset(shimmer, 0f),
                    end = Offset(shimmer + 500f, 500f),
                )
            )
            .border(1.dp, ColorWarning.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(GradientGold)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Star,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    S.current.stars,
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorWarning.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "$stars",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp,
                    ),
                    color = Color.White,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Section header with icon
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun ShopSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
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
        content()
    }
}

@Composable
private fun BillingUnavailableNote() {
    GradientBorderCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        borderColors = GradientPrimary,
        backgroundColor = ColorSurface,
        borderWidth = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.Info, null, tint = ColorPrimary, modifier = Modifier.size(20.dp))
            Text(
                S.current.billingNotAvailableWeb,
                style = MaterialTheme.typography.bodySmall,
                color = ColorOnSurface,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Earn-Stars rewarded-ad card
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun EarnStarsCard(gameState: GameState) {
    val reduceMotion = LocalReduceMotion.current
    val canWatch = gameState.stars.canWatchAd
    val remaining = gameState.stars.adsRemainingToday

    val transition = rememberInfiniteTransition(label = "earnPulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (reduceMotion || !canWatch) 1f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "earnScale",
    )

    GradientBorderCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canWatch) {
                if (gameState.stars.canWatchAd) {
                    pg.geobingo.one.platform.AdManager.showRewardedAd(
                        onReward = {
                            gameState.stars.add(10)
                            gameState.stars.recordAdWatched()
                            gameState.ui.pendingReward = pg.geobingo.one.game.state.RewardEvent(
                                label = S.current.rewardVideoWatched,
                                stars = 10,
                            )
                        },
                        onDismiss = {},
                    )
                }
            },
        cornerRadius = 18.dp,
        borderColors = GradientQuickStart,
        backgroundColor = ColorSurface,
        borderWidth = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .clip(CircleShape)
                    .background(Brush.linearGradient(GradientQuickStart)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    S.current.earnStars,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Star, null, tint = ColorWarning, modifier = Modifier.size(14.dp))
                    Text(
                        "+10 ${S.current.stars}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorWarning,
                    )
                }
                Text(
                    S.current.adsRemaining(remaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                )
            }

            if (canWatch) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(GradientQuickStart))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Text(
                        S.current.watchVideo,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            } else {
                Text(
                    S.current.adsExhaustedToday,
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.widthIn(max = 110.dp),
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Star-Pack card
// ──────────────────────────────────────────────────────────────────────

private fun tierGradient(tier: StarTier): List<Color> = when (tier) {
    StarTier.BRONZE -> listOf(Color(0xFFCD7F32), Color(0xFFE39456), Color(0xFFA0522D))
    StarTier.SILVER -> listOf(Color(0xFFB8B8CF), Color(0xFFE5E5F0), Color(0xFF8B8BA8))
    StarTier.GOLD -> listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFF97316))
    StarTier.DIAMOND -> listOf(Color(0xFF60A5FA), Color(0xFFC4B5FD), Color(0xFFF472B6))
}

private fun tierGlowColor(tier: StarTier): Color = when (tier) {
    StarTier.BRONZE -> Color(0xFFCD7F32)
    StarTier.SILVER -> Color(0xFFE5E5F0)
    StarTier.GOLD -> Color(0xFFFBBF24)
    StarTier.DIAMOND -> Color(0xFFC4B5FD)
}

@Composable
private fun RedeemCodeCard(gameState: GameState) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var codeInput by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun attemptRedeem() {
        if (codeInput.isBlank()) {
            errorMessage = S.current.redeemCodeErrorInvalid
            return
        }
        if (!pg.geobingo.one.network.AccountManager.isLoggedIn) {
            errorMessage = S.current.redeemCodeErrorNotLoggedIn
            return
        }
        loading = true
        errorMessage = null
        scope.launch {
            when (val res = pg.geobingo.one.network.RedeemCodeManager.redeem(codeInput)) {
                is pg.geobingo.one.network.RedeemCodeManager.Result.Success -> {
                    gameState.stars.setBalance(res.newBalance)
                    gameState.ui.pendingReward = pg.geobingo.one.game.state.RewardEvent(
                        label = S.current.redeemCodeSuccess,
                        stars = res.starsGranted,
                    )
                    codeInput = ""
                }
                pg.geobingo.one.network.RedeemCodeManager.Result.NotAuthenticated ->
                    errorMessage = S.current.redeemCodeErrorNotLoggedIn
                pg.geobingo.one.network.RedeemCodeManager.Result.InvalidCode ->
                    errorMessage = S.current.redeemCodeErrorInvalid
                pg.geobingo.one.network.RedeemCodeManager.Result.UnknownCode ->
                    errorMessage = S.current.redeemCodeErrorUnknown
                pg.geobingo.one.network.RedeemCodeManager.Result.Expired ->
                    errorMessage = S.current.redeemCodeErrorExpired
                pg.geobingo.one.network.RedeemCodeManager.Result.Depleted ->
                    errorMessage = S.current.redeemCodeErrorDepleted
                pg.geobingo.one.network.RedeemCodeManager.Result.AlreadyRedeemed ->
                    errorMessage = S.current.redeemCodeErrorAlreadyUsed
                is pg.geobingo.one.network.RedeemCodeManager.Result.Error ->
                    errorMessage = S.current.purchaseFailed
            }
            loading = false
        }
    }

    GradientBorderCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColors = GradientGold,
        backgroundColor = ColorSurface,
        borderWidth = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.uppercase().take(24) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(S.current.redeemCodePlaceholder, color = ColorOnSurfaceVariant) },
                    enabled = !loading,
                    textStyle = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { attemptRedeem() },
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GradientGold.first(),
                        unfocusedBorderColor = ColorOutlineVariant,
                    ),
                )
                FilledTonalButton(
                    onClick = { attemptRedeem() },
                    enabled = !loading && codeInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(S.current.redeemCodeRedeem, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (errorMessage != null) {
                Text(
                    errorMessage!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorError,
                )
            }
        }
    }
}

@Composable
private fun StarShopCard(
    modifier: Modifier = Modifier,
    pkg: StarPackage,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val gradient = remember(pkg.tier) { tierGradient(pkg.tier) }
    val glow = remember(pkg.tier) { tierGlowColor(pkg.tier) }

    Box(modifier = modifier) {
        GradientBorderCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !loading, onClick = onClick),
            cornerRadius = 18.dp,
            borderColors = gradient,
            backgroundColor = ColorSurface,
            borderWidth = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 168.dp)
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (loading) {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp, color = glow)
                    Spacer(Modifier.weight(1f))
                } else {
                    // Glowing star medallion — tier-tinted.
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(glow.copy(alpha = 0.35f), Color.Transparent),
                                    )
                                ),
                        )
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(gradient)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Star,
                                null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.White,
                            )
                        }
                    }

                    // Big stars count
                    Text(
                        "${pkg.stars}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                        ),
                        color = ColorOnSurface,
                    )

                    // Bonus pill (or spacer to keep card heights aligned)
                    if (pkg.bonus > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ColorSuccessContainer)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "+${pkg.bonus} ${S.current.stars}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = ColorSuccess,
                            )
                        }
                    } else {
                        Spacer(Modifier.height(18.dp))
                    }

                    Spacer(Modifier.weight(1f))

                    // Price pill — same gradient as the tier.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(gradient))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            pkg.price,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        if (pkg.badge != null && !loading) {
            val (label, colors) = when (pkg.badge) {
                StarBadge.POPULAR -> S.current.badgePopular to GradientPrimary
                StarBadge.BEST_VALUE -> S.current.badgeBestValue to GradientHot
            }
            ShineBadge(
                label = label.uppercase(),
                colors = colors,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Skip-Pack card
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun SkipShopCard(
    modifier: Modifier = Modifier,
    pkg: SkipPackage,
    loading: Boolean,
    onClick: () -> Unit,
) {
    GradientBorderCard(
        modifier = modifier.clickable(enabled = !loading, onClick = onClick),
        cornerRadius = 18.dp,
        borderColors = GradientPrimary,
        backgroundColor = ColorSurface,
        borderWidth = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (loading) {
                Spacer(Modifier.height(10.dp))
                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp, color = ColorPrimary)
                Spacer(Modifier.weight(1f))
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(ColorPrimary.copy(alpha = 0.35f), Color.Transparent),
                                )
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(GradientPrimary)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Style, null, modifier = Modifier.size(22.dp), tint = Color.White)
                    }
                }
                Text(
                    "${pkg.cards}×",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp,
                    ),
                    color = ColorOnSurface,
                )
                Text(
                    S.current.useSkipCard,
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(GradientPrimary))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        pkg.price,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Remove-Ads card
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun RemoveAdsCard(loading: Boolean, onClick: () -> Unit) {
    GradientBorderCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        borderColors = GradientHot,
        backgroundColor = ColorSurface,
        borderWidth = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !loading, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(GradientHot)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Block, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    S.current.removeAds,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnSurface,
                )
                Text(
                    S.current.removeAdsDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurfaceVariant,
                )
            }
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = ColorPrimary)
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(GradientHot))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Text(
                        S.current.removeAdsPrice,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdsAlreadyRemovedCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ColorSuccessContainer.copy(alpha = 0.5f))
            .border(1.dp, ColorSuccess.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ColorSuccess.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = ColorSuccess, modifier = Modifier.size(22.dp))
        }
        Text(
            S.current.adsRemoved,
            style = MaterialTheme.typography.bodyMedium,
            color = ColorSuccess,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Soft-glow badge (POPULAR / BEST VALUE)
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun ShineBadge(
    label: String,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    // A gentle breathe of overall alpha instead of a sliding rectangular sheen —
    // the old sheen visibly "clipped" past the rounded corners and looked cheap.
    // This just fades the whole badge between full opacity and slightly softer,
    // which reads as a subtle glow regardless of badge shape.
    val transition = rememberInfiniteTransition(label = "badgeGlow")
    val alpha by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (reduceMotion) 0.85f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "badgeAlpha",
    )
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.linearGradient(colors))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
    }
}
