package pg.geobingo.one.platform

import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

actual object ConsentManager {
    private var consentInfo: ConsentInformation? = null

    actual fun requestConsent(onReady: () -> Unit) {
        val activity = currentActivity ?: run { onReady(); return }
        val params = ConsentRequestParameters.Builder().build()
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo = info

        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Update succeeded — show form if required
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    // Form dismissed or not required
                    onReady()
                }
            },
            {
                // Error — proceed without consent (non-personalized ads)
                onReady()
            }
        )
    }

    actual fun showPrivacyOptionsForm(onDismiss: () -> Unit) {
        val activity = currentActivity ?: run { onDismiss(); return }
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            onDismiss()
        }
    }

    actual val canShowPersonalizedAds: Boolean
        get() {
            val info = consentInfo ?: return false
            return info.consentStatus == ConsentInformation.ConsentStatus.OBTAINED ||
                info.consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED
        }
}
