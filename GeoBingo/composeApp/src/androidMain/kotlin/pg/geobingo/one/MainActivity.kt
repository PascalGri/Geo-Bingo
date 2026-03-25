package pg.geobingo.one

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.ads.MobileAds
import pg.geobingo.one.platform.appContext
import pg.geobingo.one.platform.currentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        currentActivity = this

        MobileAds.initialize(this)

        setContent {
            App()
        }
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