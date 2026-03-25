package pg.geobingo.one.platform

import androidx.compose.runtime.Composable

data class LatLng(val latitude: Double, val longitude: Double)

/** Returns the device's current GPS coordinates, or null if unavailable/denied. */
expect suspend fun getCurrentLocation(): LatLng?

/**
 * Composable that requests location permission on platforms that need it (Android).
 * Call this once when entering the game screen. On iOS/Web, this is a no-op
 * because those platforms handle permission prompts automatically.
 */
@Composable
expect fun RequestLocationPermission()
