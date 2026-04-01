package pg.geobingo.one

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.ads.MobileAds
import com.google.firebase.crashlytics.FirebaseCrashlytics
import pg.geobingo.one.platform.DeepLinkHandler
import pg.geobingo.one.platform.appContext
import pg.geobingo.one.platform.currentActivity
import pg.geobingo.one.util.Analytics
import pg.geobingo.one.util.AppLogger
import pg.geobingo.one.util.LogLevel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        currentActivity = this
        Analytics.platform = "android"

        // Only log errors in release builds
        if (!pg.geobingo.one.BuildConfig.DEBUG) {
            AppLogger.minLevel = LogLevel.ERROR
        }

        // Forward AppLogger errors to Firebase Crashlytics
        AppLogger.crashReporter = { message, throwable ->
            FirebaseCrashlytics.getInstance().log(message)
            throwable?.let { FirebaseCrashlytics.getInstance().recordException(it) }
        }

        MobileAds.initialize(this)

        // Handle deep link from cold start
        intent?.data?.toString()?.let { DeepLinkHandler.handleUrl(it) }

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep link when app is already running
        intent.data?.toString()?.let { DeepLinkHandler.handleUrl(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentActivity === this) currentActivity = null
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}