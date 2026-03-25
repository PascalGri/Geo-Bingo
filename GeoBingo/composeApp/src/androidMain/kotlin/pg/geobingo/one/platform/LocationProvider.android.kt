package pg.geobingo.one.platform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual suspend fun getCurrentLocation(): LatLng? {
    val context = appContext
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) return null

    val client = LocationServices.getFusedLocationProviderClient(context)

    // Try last known location first (fast)
    return try {
        suspendCancellableCoroutine { cont ->
            client.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    cont.resume(LatLng(location.latitude, location.longitude))
                } else {
                    // Request a fresh location
                    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                        .setMaxUpdates(1)
                        .setMaxUpdateDelayMillis(5000L)
                        .build()
                    val callback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            client.removeLocationUpdates(this)
                            val loc = result.lastLocation
                            if (loc != null) {
                                cont.resume(LatLng(loc.latitude, loc.longitude))
                            } else {
                                cont.resume(null)
                            }
                        }
                    }
                    cont.invokeOnCancellation { client.removeLocationUpdates(callback) }
                    client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                }
            }.addOnFailureListener {
                cont.resume(null)
            }
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
actual fun RequestLocationPermission() {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Result doesn't matter — getCurrentLocation checks permission each time */ }

    LaunchedEffect(Unit) {
        val context = appContext
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            launcher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ))
        }
    }
}
