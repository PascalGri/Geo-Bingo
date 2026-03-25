package pg.geobingo.one.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Platform-specific connectivity check.
 * Returns a Compose State<Boolean> that is true when the device is online.
 */
@Composable
expect fun rememberConnectivityState(): State<Boolean>
