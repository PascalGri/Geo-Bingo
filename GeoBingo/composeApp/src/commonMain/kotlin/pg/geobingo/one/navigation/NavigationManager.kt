package pg.geobingo.one.navigation

import androidx.compose.runtime.mutableStateListOf
import pg.geobingo.one.game.Screen

/**
 * Stack-based navigation manager with proper backstack support.
 * Replaces the flat enum-based navigation via SessionState.currentScreen.
 */
class NavigationManager(initialScreen: Screen) {
    private val _backStack = mutableStateListOf(initialScreen)

    val currentScreen: Screen
        get() = _backStack.last()

    val canGoBack: Boolean
        get() = _backStack.size > 1

    val backStackSize: Int
        get() = _backStack.size

    /**
     * Push a new screen onto the backstack.
     */
    fun navigateTo(screen: Screen) {
        if (_backStack.lastOrNull() == screen) return
        _backStack.add(screen)
    }

    /**
     * Pop the current screen and return to the previous one.
     * Returns true if navigation happened, false if already at root.
     */
    fun goBack(): Boolean {
        if (_backStack.size <= 1) return false
        _backStack.removeLast()
        return true
    }

    /**
     * Replace the current screen without adding to backstack.
     * Useful for transitions like GAME -> VOTE_TRANSITION -> REVIEW.
     */
    fun replaceCurrent(screen: Screen) {
        if (_backStack.isNotEmpty()) _backStack.removeLast()
        _backStack.add(screen)
    }

    /**
     * Clear backstack and set a single root screen.
     * Useful for resetGame() -> HOME.
     */
    fun resetTo(screen: Screen) {
        _backStack.clear()
        _backStack.add(screen)
    }

    /**
     * Navigate back to a screen already in the stack, popping everything above it.
     * If not found, pushes to the stack.
     */
    fun navigateBackTo(screen: Screen): Boolean {
        val index = _backStack.lastIndexOf(screen)
        if (index >= 0) {
            while (_backStack.size > index + 1) {
                _backStack.removeLast()
            }
            return true
        }
        _backStack.add(screen)
        return false
    }

    /**
     * Returns the full backstack as an immutable list (for debugging/testing).
     */
    fun backStackSnapshot(): List<Screen> = _backStack.toList()
}
