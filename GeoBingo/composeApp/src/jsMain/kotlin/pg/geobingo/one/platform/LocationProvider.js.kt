package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.js.json

actual suspend fun getCurrentLocation(): LatLng? = try {
    suspendCancellableCoroutine { cont ->
        val geolocation = js("navigator.geolocation")
        if (geolocation == null || geolocation == undefined) {
            cont.resume(null)
        } else {
            geolocation.getCurrentPosition(
                { pos: dynamic ->
                    val lat = pos.coords.latitude as Double
                    val lng = pos.coords.longitude as Double
                    cont.resume(LatLng(lat, lng))
                },
                { _: dynamic ->
                    cont.resume(null)
                },
                json("enableHighAccuracy" to true, "timeout" to 10000, "maximumAge" to 60000)
            )
        }
    }
} catch (_: Exception) {
    null
}

@Composable
actual fun RequestLocationPermission() {}
