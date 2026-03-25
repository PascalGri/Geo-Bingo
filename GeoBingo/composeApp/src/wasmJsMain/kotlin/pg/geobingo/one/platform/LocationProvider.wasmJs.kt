package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@JsFun("""(onSuccess, onError) => {
    if (!navigator.geolocation) { onError(); return; }
    navigator.geolocation.getCurrentPosition(
        function(pos) { onSuccess(pos.coords.latitude, pos.coords.longitude); },
        function() { onError(); },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    );
}""")
private external fun jsGetLocation(onSuccess: (Double, Double) -> Unit, onError: () -> Unit)

actual suspend fun getCurrentLocation(): LatLng? = try {
    suspendCancellableCoroutine { cont ->
        jsGetLocation(
            onSuccess = { lat, lng -> cont.resume(LatLng(lat, lng)) },
            onError = { cont.resume(null) }
        )
    }
} catch (_: Exception) {
    null
}

@Composable
actual fun RequestLocationPermission() {
    // Web browsers handle permission prompts automatically via navigator.geolocation
}
