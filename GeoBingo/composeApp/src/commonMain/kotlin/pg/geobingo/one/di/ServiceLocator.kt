package pg.geobingo.one.di

import io.ktor.client.HttpClient
import pg.geobingo.one.game.GameState
import pg.geobingo.one.navigation.NavigationManager
import pg.geobingo.one.game.Screen
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys
import pg.geobingo.one.viewmodel.GameViewModel
import pg.geobingo.one.viewmodel.ReviewViewModel

/**
 * Lightweight service locator for dependency injection.
 * Central composition root for shared resources and factories.
 */
object ServiceLocator {
    // ── HttpClient (shared, properly managed) ────────────────────────────
    @kotlin.concurrent.Volatile
    private var _httpClient: HttpClient? = null

    val httpClient: HttpClient
        get() = _httpClient ?: HttpClient().also { _httpClient = it }

    // ── Navigation ───────────────────────────────────────────────────────
    @kotlin.concurrent.Volatile
    private var _navigation: NavigationManager? = null

    val navigation: NavigationManager
        get() = _navigation ?: createDefaultNavigation().also { _navigation = it }

    private fun createDefaultNavigation(): NavigationManager {
        val initial = if (AppSettings.getBoolean(SettingsKeys.ONBOARDING_COMPLETED))
            Screen.HOME else Screen.ONBOARDING
        return NavigationManager(initial)
    }

    // ── GameState (shared singleton) ─────────────────────────────────────
    @kotlin.concurrent.Volatile
    private var _gameState: GameState? = null

    val gameState: GameState
        get() = _gameState ?: GameState().also { _gameState = it }

    // ── ViewModel factories ──────────────────────────────────────────────

    fun createGameViewModel(): GameViewModel =
        GameViewModel(gameState, navigation)

    fun createReviewViewModel(): ReviewViewModel =
        ReviewViewModel(gameState, navigation)

    // ── Lifecycle ────────────────────────────────────────────────────────

    fun shutdown() {
        _httpClient?.close()
        _httpClient = null
    }

    fun reset() {
        _httpClient?.close()
        _httpClient = null
        _navigation = null
        _gameState = null
    }

    // ── Test overrides ───────────────────────────────────────────────────

    fun overrideHttpClient(client: HttpClient) {
        _httpClient?.close()
        _httpClient = client
    }

    fun overrideNavigation(nav: NavigationManager) {
        _navigation = nav
    }

    fun overrideGameState(state: GameState) {
        _gameState = state
    }
}
