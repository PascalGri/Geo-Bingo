package pg.geobingo.one.platform

import androidx.compose.runtime.Composable

actual suspend fun getCurrentLocation(): LatLng? = null

@Composable
actual fun RequestLocationPermission() {}
