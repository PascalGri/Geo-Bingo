package pg.geobingo.one.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.sp
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.BillingManager
import pg.geobingo.one.platform.SystemBackHandler
import pg.geobingo.one.ui.components.StarsChip
import pg.geobingo.one.ui.theme.*

private data class StarPackage(
    val stars: Int,
    val price: String,
    val productId: String,
)

private data class SkipPackage(
    val cards: Int,
    val price: String,
    val productId: String,
)

private val STAR_PACKAGES = listOf(
    StarPackage(50, "0,99 \u20AC", "pg.geobingo.one.stars_50"),
    StarPackage(150, "1,99 \u20AC", "pg.geobingo.one.stars_150"),
    StarPackage(400, "3,99 \u20AC", "pg.geobingo.one.stars_400"),
    StarPackage(1000, "7,99 \u20AC", "pg.geobingo.one.stars_1000"),
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
    val scope = rememberCoroutineScope()
    var purchaseLoading by remember { mutableStateOf<String?>(null) }

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
            // ── Stars Packages ──────────────────────────────────────────
            ShopSection(title = S.current.buyStars) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rows = STAR_PACKAGES.chunked(2)
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { pkg ->
                                ShopCard(
                                    modifier = Modifier.weight(1f),
                                    topText = "${pkg.stars}",
                                    topIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp), tint = ColorWarning) },
                                    bottomText = pkg.price,
                                    gradientColors = GradientGold,
                                    loading = purchaseLoading == pkg.productId,
                                    onClick = {
                                        purchaseLoading = pkg.productId
                                        BillingManager.purchaseProduct(
                                            productId = pkg.productId,
                                            onSuccess = {
                                                gameState.stars.add(pkg.stars)
                                                purchaseLoading = null
                                            },
                                            onError = {
                                                purchaseLoading = null
                                            },
                                        )
                                    },
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Ad-Skipper Cards ─────────────────────────────────────────
            ShopSection(title = S.current.adSkipper) {
                Text(
                    S.current.skipCardsRemaining(gameState.stars.skipCardsCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SKIP_PACKAGES.forEach { pkg ->
                        ShopCard(
                            modifier = Modifier.weight(1f),
                            topText = "${pkg.cards}x",
                            topIcon = { Icon(Icons.Default.Style, null, modifier = Modifier.size(16.dp), tint = ColorPrimary) },
                            bottomText = pkg.price,
                            gradientColors = GradientPrimary,
                            loading = purchaseLoading == pkg.productId,
                            onClick = {
                                purchaseLoading = pkg.productId
                                BillingManager.purchaseProduct(
                                    productId = pkg.productId,
                                    onSuccess = {
                                        gameState.stars.addSkipCards(pkg.cards)
                                        purchaseLoading = null
                                    },
                                    onError = {
                                        purchaseLoading = null
                                    },
                                )
                            },
                        )
                    }
                }
            }

            // ── Remove Ads ───────────────────────────────────────────────
            if (!gameState.stars.noAdsPurchased) {
                ShopSection(title = S.current.removeAds) {
                    GradientBorderCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        borderColors = GradientHot,
                        backgroundColor = ColorSurface,
                        borderWidth = 1.5.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    purchaseLoading = "no_ads"
                                    BillingManager.purchaseProduct(
                                        productId = "pg.geobingo.one.no_ads",
                                        onSuccess = {
                                            gameState.stars.updateNoAdsPurchased(true)
                                            purchaseLoading = null
                                        },
                                        onError = {
                                            purchaseLoading = null
                                        },
                                    )
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
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
                            Spacer(Modifier.width(12.dp))
                            if (purchaseLoading == "no_ads") {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ColorPrimary)
                            } else {
                                Text(
                                    S.current.removeAdsPrice,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorPrimary,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }
            } else {
                ShopSection(title = S.current.removeAds) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ColorSurface)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = ColorSuccess, modifier = Modifier.size(20.dp))
                        Text(
                            S.current.adsRemoved,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorSuccess,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // ── Cosmetics ────────────────────────────────────────────────
            ShopSection(title = S.current.cosmeticShop) {
                GradientBorderCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigateTo(pg.geobingo.one.game.Screen.COSMETIC_SHOP) },
                    cornerRadius = 14.dp,
                    borderColors = GradientPrimary,
                    backgroundColor = ColorSurface,
                    borderWidth = 1.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                S.current.cosmeticShop,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = ColorOnSurface,
                            )
                            Text(
                                S.current.cosmeticsDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorOnSurfaceVariant,
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = ColorPrimary)
                    }
                }
            }

            // ── Restore Purchases ────────────────────────────────────────
            TextButton(
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
                Text(S.current.restorePurchases, style = MaterialTheme.typography.labelMedium, color = ColorOnSurfaceVariant)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ShopSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = ColorOnSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
        )
        content()
    }
}

@Composable
private fun ShopCard(
    modifier: Modifier = Modifier,
    topText: String,
    topIcon: @Composable () -> Unit,
    bottomText: String,
    gradientColors: List<Color>,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    GradientBorderCard(
        modifier = modifier.clickable(enabled = !loading, onClick = onClick),
        cornerRadius = 14.dp,
        borderColors = gradientColors,
        backgroundColor = ColorSurface,
        borderWidth = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = gradientColors.first())
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    topIcon()
                    Text(
                        topText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorOnSurface,
                    )
                }
            }
            Text(
                bottomText,
                style = MaterialTheme.typography.labelSmall,
                color = ColorOnSurfaceVariant,
            )
        }
    }
}
