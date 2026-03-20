package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.*
import platform.darwin.NSObject
import platform.Foundation.NSError
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
private class LocationDelegate : NSObject(), CLLocationManagerDelegateProtocol {
    var onLocation: ((LatLng?) -> Unit)? = null

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        manager.stopUpdatingLocation()
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        if (location != null) {
            val lat = location.coordinate.useContents { latitude }
            val lng = location.coordinate.useContents { longitude }
            onLocation?.invoke(LatLng(lat, lng))
        } else {
            onLocation?.invoke(null)
        }
        onLocation = null
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        manager.stopUpdatingLocation()
        onLocation?.invoke(null)
        onLocation = null
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val status = manager.authorizationStatus
        if (status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways) {
            manager.startUpdatingLocation()
        } else if (status != 0) {
            onLocation?.invoke(null)
            onLocation = null
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun getCurrentLocation(): LatLng? = suspendCancellableCoroutine { cont ->
    val manager = CLLocationManager()
    manager.desiredAccuracy = kCLLocationAccuracyBest

    val delegate = LocationDelegate()
    delegate.onLocation = { result ->
        cont.resume(result)
    }
    manager.delegate = delegate

    val status = manager.authorizationStatus
    when (status) {
        kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> {
            manager.startUpdatingLocation()
        }
        0 -> { // notDetermined
            manager.requestWhenInUseAuthorization()
        }
        else -> {
            cont.resume(null)
        }
    }

    cont.invokeOnCancellation {
        manager.stopUpdatingLocation()
    }
}

@Composable
actual fun RequestLocationPermission() {
    // iOS handles permission prompts automatically in getCurrentLocation()
}
