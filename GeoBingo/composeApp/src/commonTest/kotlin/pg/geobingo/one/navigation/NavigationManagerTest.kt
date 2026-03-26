package pg.geobingo.one.navigation

import pg.geobingo.one.game.Screen
import kotlin.test.*

class NavigationManagerTest {

    @Test
    fun initialScreen() {
        val nav = NavigationManager(Screen.HOME)
        assertEquals(Screen.HOME, nav.currentScreen)
        assertFalse(nav.canGoBack)
        assertEquals(1, nav.backStackSize)
    }

    @Test
    fun navigateTo_pushesScreen() {
        val nav = NavigationManager(Screen.HOME)
        nav.navigateTo(Screen.SELECT_MODE)
        assertEquals(Screen.SELECT_MODE, nav.currentScreen)
        assertTrue(nav.canGoBack)
        assertEquals(2, nav.backStackSize)
    }

    @Test
    fun navigateTo_duplicateIgnored() {
        val nav = NavigationManager(Screen.HOME)
        nav.navigateTo(Screen.SELECT_MODE)
        nav.navigateTo(Screen.SELECT_MODE)
        assertEquals(2, nav.backStackSize)
    }

    @Test
    fun goBack_popsScreen() {
        val nav = NavigationManager(Screen.HOME)
        nav.navigateTo(Screen.SELECT_MODE)
        val result = nav.goBack()
        assertTrue(result)
        assertEquals(Screen.HOME, nav.currentScreen)
        assertFalse(nav.canGoBack)
    }

    @Test
    fun goBack_atRoot_returnsFalse() {
        val nav = NavigationManager(Screen.HOME)
        val result = nav.goBack()
        assertFalse(result)
        assertEquals(Screen.HOME, nav.currentScreen)
    }

    @Test
    fun replaceCurrent_replacesTopScreen() {
        val nav = NavigationManager(Screen.HOME)
        nav.navigateTo(Screen.GAME)
        nav.replaceCurrent(Screen.VOTE_TRANSITION)
        assertEquals(Screen.VOTE_TRANSITION, nav.currentScreen)
        assertEquals(2, nav.backStackSize) // HOME + VOTE_TRANSITION
    }

    @Test
    fun replaceCurrent_onRoot_replacesRoot() {
        val nav = NavigationManager(Screen.ONBOARDING)
        nav.replaceCurrent(Screen.HOME)
        assertEquals(Screen.HOME, nav.currentScreen)
        assertEquals(1, nav.backStackSize)
    }

    @Test
    fun resetTo_clearsStack() {
        val nav = NavigationManager(Screen.HOME)
        nav.navigateTo(Screen.SELECT_MODE)
        nav.navigateTo(Screen.CREATE_GAME)
        nav.navigateTo(Screen.LOBBY)
        nav.resetTo(Screen.HOME)
        assertEquals(Screen.HOME, nav.currentScreen)
        assertEquals(1, nav.backStackSize)
        assertFalse(nav.canGoBack)
    }

    @Test
    fun navigateBackTo_existingScreen() {
        val nav = NavigationManager(Screen.HOME)
        nav.navigateTo(Screen.SELECT_MODE)
        nav.navigateTo(Screen.CREATE_GAME)
        nav.navigateTo(Screen.LOBBY)
        val found = nav.navigateBackTo(Screen.SELECT_MODE)
        assertTrue(found)
        assertEquals(Screen.SELECT_MODE, nav.currentScreen)
        assertEquals(2, nav.backStackSize) // HOME + SELECT_MODE
    }

    @Test
    fun navigateBackTo_notFound_pushes() {
        val nav = NavigationManager(Screen.HOME)
        val found = nav.navigateBackTo(Screen.SETTINGS)
        assertFalse(found)
        assertEquals(Screen.SETTINGS, nav.currentScreen)
        assertEquals(2, nav.backStackSize)
    }

    @Test
    fun backStackSnapshot_returnsCorrectOrder() {
        val nav = NavigationManager(Screen.HOME)
        nav.navigateTo(Screen.SELECT_MODE)
        nav.navigateTo(Screen.CREATE_GAME)
        val snapshot = nav.backStackSnapshot()
        assertEquals(listOf(Screen.HOME, Screen.SELECT_MODE, Screen.CREATE_GAME), snapshot)
    }

    @Test
    fun fullGameFlow_navigation() {
        val nav = NavigationManager(Screen.HOME)
        // Create game flow
        nav.navigateTo(Screen.SELECT_MODE)
        nav.navigateTo(Screen.CREATE_GAME)
        nav.navigateTo(Screen.LOBBY)
        // Start game transition (replace, not push)
        nav.replaceCurrent(Screen.GAME_START_TRANSITION)
        nav.replaceCurrent(Screen.GAME)
        assertEquals(Screen.GAME, nav.currentScreen)
        // Vote transition (replace)
        nav.replaceCurrent(Screen.VOTE_TRANSITION)
        nav.replaceCurrent(Screen.REVIEW)
        // Results transition (replace)
        nav.replaceCurrent(Screen.RESULTS_TRANSITION)
        nav.replaceCurrent(Screen.RESULTS)
        assertEquals(Screen.RESULTS, nav.currentScreen)
        // Back to home
        nav.resetTo(Screen.HOME)
        assertEquals(Screen.HOME, nav.currentScreen)
        assertEquals(1, nav.backStackSize)
    }

    @Test
    fun multipleGoBack_throughStack() {
        val nav = NavigationManager(Screen.HOME)
        nav.navigateTo(Screen.SELECT_MODE)
        nav.navigateTo(Screen.CREATE_GAME)
        nav.goBack() // -> SELECT_MODE
        assertEquals(Screen.SELECT_MODE, nav.currentScreen)
        nav.goBack() // -> HOME
        assertEquals(Screen.HOME, nav.currentScreen)
        assertFalse(nav.goBack()) // at root
    }
}
