package pg.geobingo.one.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Global signal triggered when the user re-taps the currently active bottom-nav tab.
 * Top-level screens observe this with [collectScrollToTop] and scroll their list/column to the top.
 */
object ScrollToTopBus {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    fun fire(tag: String) {
        _events.tryEmit(tag)
    }
}

/** Tag constants — one per top-level navbar destination. */
object ScrollToTopTags {
    const val HOME = "home"
    const val FRIENDS = "friends"
    const val SHOP_STARS = "shop_stars"
    const val SHOP_COSMETICS = "shop_cosmetics"
    const val SETTINGS = "settings"
}

@Composable
fun CollectScrollToTop(tag: String, lazyState: LazyListState) {
    LaunchedEffect(tag) {
        ScrollToTopBus.events.collect { evt ->
            if (evt == tag) {
                runCatching { lazyState.animateScrollToItem(0) }
            }
        }
    }
}

@Composable
fun CollectScrollToTop(tag: String, scrollState: ScrollState) {
    LaunchedEffect(tag) {
        ScrollToTopBus.events.collect { evt ->
            if (evt == tag) {
                runCatching { scrollState.animateScrollTo(0) }
            }
        }
    }
}
