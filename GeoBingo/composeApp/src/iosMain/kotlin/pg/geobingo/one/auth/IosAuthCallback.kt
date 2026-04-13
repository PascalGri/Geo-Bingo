package pg.geobingo.one.auth

import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.Foundation.NSURL
import pg.geobingo.one.network.AccountManager
import pg.geobingo.one.network.supabase
import pg.geobingo.one.util.AppLogger

/**
 * Called from Swift (iOSApp.swift onOpenURL) when an OAuth callback URL arrives.
 *
 * Supabase Kotlin's handleDeeplinks extension on iOS expects an NSURL, parses the
 * access/refresh tokens from the fragment (or exchanges the code in PKCE flow),
 * and imports the session into auth state. After a successful import we re-run
 * the startup sync so the new user's profile + cosmetics land in local settings.
 */
object IosAuthCallback {
    private const val TAG = "IosAuthCallback"
    private val scope = CoroutineScope(Dispatchers.Main)

    fun handle(url: NSURL) {
        try {
            supabase.handleDeeplinks(url) { session ->
                AppLogger.i(TAG, "OAuth session imported for user=${session.user?.id}")
                scope.launch {
                    // Re-run the full startup sync: profile creation + cloud->local
                    AccountManager.handleAppStartup()
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "OAuth deeplink handling failed", e)
        }
    }
}
