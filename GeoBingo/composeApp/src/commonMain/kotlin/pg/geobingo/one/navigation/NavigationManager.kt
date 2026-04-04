package pg.geobingo.one.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import pg.geobingo.one.game.Screen

/**
 * Stack-based navigation manager with proper backstack support and typed arguments.
 */
class NavigationManager(initialScreen: Screen) {
    private val _backStack = mutableStateListOf(initialScreen)
    private val _args = mutableStateMapOf<Screen, NavArgs>()

    val currentScreen: Screen
        get() = _backStack.last()

    val canGoBack: Boolean
        get() = _backStack.size > 1

    val backStackSize: Int
        get() = _backStack.size

    /**
     * Push a new screen onto the backstack.
     */
    fun navigateTo(screen: Screen, args: NavArgs? = null) {
        if (_backStack.lastOrNull() == screen) return
        if (args != null) _args[screen] = args else _args.remove(screen)
        _backStack.add(screen)
    }

    /**
     * Pop the current screen and return to the previous one.
     */
    fun goBack(): Boolean {
        if (_backStack.size <= 1) return false
        val removed = _backStack.removeAt(_backStack.lastIndex)
        _args.remove(removed)
        return true
    }

    /**
     * Replace the current screen without adding to backstack.
     */
    fun replaceCurrent(screen: Screen, args: NavArgs? = null) {
        if (_backStack.isNotEmpty()) {
            val removed = _backStack.removeAt(_backStack.lastIndex)
            _args.remove(removed)
        }
        if (args != null) _args[screen] = args else _args.remove(screen)
        _backStack.add(screen)
    }

    /**
     * Clear backstack and set a single root screen.
     */
    fun resetTo(screen: Screen) {
        _backStack.clear()
        _args.clear()
        _backStack.add(screen)
    }

    /**
     * Navigate back to a screen already in the stack, popping everything above it.
     */
    fun navigateBackTo(screen: Screen): Boolean {
        val index = _backStack.lastIndexOf(screen)
        if (index >= 0) {
            while (_backStack.size > index + 1) {
                val removed = _backStack.removeAt(_backStack.lastIndex)
                _args.remove(removed)
            }
            return true
        }
        _backStack.add(screen)
        return false
    }

    /**
     * Get typed arguments for the current screen.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : NavArgs> getArgs(): T? = _args[currentScreen] as? T

    /**
     * Get typed arguments for a specific screen.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : NavArgs> getArgs(screen: Screen): T? = _args[screen] as? T

    /**
     * Returns the full backstack as an immutable list (for debugging/testing).
     */
    fun backStackSnapshot(): List<Screen> = _backStack.toList()
}
