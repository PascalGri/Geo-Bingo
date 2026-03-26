package pg.geobingo.one.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pg.geobingo.one.di.ServiceLocator
import pg.geobingo.one.game.GameState
import pg.geobingo.one.game.Screen
import pg.geobingo.one.i18n.S
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.ui.theme.*
import pg.geobingo.one.ui.theme.Spacing
import pg.geobingo.one.ui.theme.rememberStaggeredAnimation

private data class OnboardingSlide(
    val icon: ImageVector,
    val titleKey: () -> String,
    val bodyKey: () -> String,
)

private val slides = listOf(
    OnboardingSlide(
        icon = Icons.Default.Explore,
        titleKey = { S.current.onboardingTitle1 },
        bodyKey = { S.current.onboardingBody1 },
    ),
    OnboardingSlide(
        icon = Icons.Default.GridView,
        titleKey = { S.current.onboardingTitle2 },
        bodyKey = { S.current.onboardingBody2 },
    ),
    OnboardingSlide(
        icon = Icons.Default.CameraAlt,
        titleKey = { S.current.onboardingTitle3 },
        bodyKey = { S.current.onboardingBody3 },
    ),
    OnboardingSlide(
        icon = Icons.Default.Star,
        titleKey = { S.current.onboardingTitle4 },
        bodyKey = { S.current.onboardingBody4 },
    ),
)

@Composable
fun OnboardingScreen(gameState: GameState) {
    val nav = remember { ServiceLocator.navigation }
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val isLastPage by remember { derivedStateOf { pagerState.currentPage == slides.lastIndex } }

    fun completeOnboarding() {
        AppSettings.setBoolean(SettingsKeys.ONBOARDING_COMPLETED, true)
        nav.resetTo(Screen.HOME)
    }

    Scaffold(containerColor = ColorBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ColorBackground),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Top bar: back arrow (left) + skip button (right) ─────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pagerState.currentPage > 0) {
                    IconButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = S.current.back,
                            tint = ColorOnSurfaceVariant,
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                if (!isLastPage) {
                    TextButton(onClick = { completeOnboarding() }) {
                        Text(
                            text = S.current.onboardingSkip,
                            style = MaterialTheme.typography.labelLarge,
                            color = ColorOnSurfaceVariant,
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            // ── Pager + bottom controls ──────────────────────────────────
            HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) { page ->
                    OnboardingPage(slide = slides[page])
                }

            // ── Page indicator dots ──────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp),
            ) {
                repeat(slides.size) { index ->
                    val isActive = pagerState.currentPage == index
                    val color by animateColorAsState(
                        targetValue = if (isActive) ColorPrimary else ColorOutlineVariant,
                        animationSpec = tween(300),
                        label = "dotColor",
                    )
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }

            // ── Action button ────────────────────────────────────────
            GradientButton(
                text = if (isLastPage) S.current.onboardingStart else S.current.onboardingNext,
                onClick = {
                    if (isLastPage) {
                        completeOnboarding()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal),
                gradientColors = GradientPrimary,
                height = 56.dp,
                fontSize = 17.sp,
            )

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun OnboardingPage(slide: OnboardingSlide) {
    val anim = rememberStaggeredAnimation(count = 3, staggerDelay = 100L, animDuration = 500)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // ── Icon in gradient box ─────────────────────────────────────
        Box(
            modifier = Modifier
                .then(anim.modifier(0))
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(GradientPrimary)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = slide.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White,
            )
        }

        Spacer(Modifier.height(40.dp))

        // ── Title ────────────────────────────────────────────────────
        Text(
            text = slide.titleKey(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = ColorOnSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.then(anim.modifier(1)),
        )

        Spacer(Modifier.height(16.dp))

        // ── Body ─────────────────────────────────────────────────────
        Text(
            text = slide.bodyKey(),
            style = MaterialTheme.typography.bodyMedium,
            color = ColorOnSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier
                .then(anim.modifier(2))
                .widthIn(max = 320.dp),
        )
    }
}
