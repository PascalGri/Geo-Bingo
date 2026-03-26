package pg.geobingo.one.di

import io.ktor.client.HttpClient
import pg.geobingo.one.navigation.NavigationManager
import pg.geobingo.one.game.Screen
import pg.geobingo.one.platform.AppSettings
import pg.geobingo.one.platform.SettingsKeys

/**
 * Lightweight service locator for dependency injection.
 * Manages shared resource lifecycles and enables test overrides.
 */
object ServiceLocator {
    // ── HttpClient (shared, properly managed) ────────────────────────────
    private var _httpClient: HttpClient? = null

    val httpClient: HttpClient
        get() = _httpClient ?: HttpClient().also { _httpClient = it }

    // ── Navigation ───────────────────────────────────────────────────────
    private var _navigation: NavigationManager? = null

    val navigation: NavigationManager
        get() = _navigation ?: createDefaultNavigation().also { _navigation = it }

    private fun createDefaultNavigation(): NavigationManager {
        val initial = if (AppSettings.getBoolean(SettingsKeys.ONBOARDING_COMPLETED))
            Screen.HOME else Screen.ONBOARDING
        return NavigationManager(initial)
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    /**
     * Clean up resources. Call on app termination.
     */
    fun shutdown() {
        _httpClient?.close()
        _httpClient = null
    }

    /**
     * Reset all instances. Used in tests.
     */
    fun reset() {
        shutdown()
        _navigation = null
    }

    // ── Test overrides ───────────────────────────────────────────────────

    fun overrideHttpClient(client: HttpClient) {
        _httpClient?.close()
        _httpClient = client
    }

    fun overrideNavigation(nav: NavigationManager) {
        _navigation = nav
    }
}
