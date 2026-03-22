package pg.geobingo.one.platform

import platform.UIKit.UIApplication
import kotlinx.cinterop.ExperimentalForeignApi

// UMP for iOS is integrated via Swift/ObjC interop.
// The Swift layer (iOSApp.swift) initializes MobileAds after ATT.
// For the consent dialog itself we call UMP via the Objective-C bridge.
// Since direct Swift import is limited in KMP, we use a simple callback bridge.

// This object is called from Kotlin — the actual UMP consent on iOS
// is triggered by the Swift AppDelegate/App init via ConsentManagerBridge.
// See iosApp/iosApp/ConsentManagerBridge.swift for the Swift side.

private var consentReady = false
private var personalizedAdsAllowed = true  // UMP sets this via bridge

// Called from Swift ConsentManagerBridge after consent is determined
fun onConsentReady(canPersonalize: Boolean) {
    personalizedAdsAllowed = canPersonalize
    consentReady = true
    pendingOnReady?.invoke()
    pendingOnReady = null
}

private var pendingOnReady: (() -> Unit)? = null

actual object ConsentManager {
    actual fun requestConsent(onReady: () -> Unit) {
        if (consentReady) {
            onReady()
        } else {
            pendingOnReady = onReady
            // Swift-side bridge will call onConsentReady() when done
        }
    }

    actual fun showPrivacyOptionsForm(onDismiss: () -> Unit) {
        // Forwarded to Swift bridge — see ConsentManagerBridge.swift
        ConsentManagerBridgeCompanion.showPrivacyOptions(onDismiss)
    }

    actual val canShowPersonalizedAds: Boolean
        get() = personalizedAdsAllowed
}

// Companion object so Swift can call back into Kotlin
object ConsentManagerBridgeCompanion {
    var privacyOptionsCallback: (() -> Unit)? = null

    fun showPrivacyOptions(onDismiss: () -> Unit) {
        privacyOptionsCallback = onDismiss
        // Signal Swift to show the form — polling via a shared flag
        shouldShowPrivacyForm = true
    }

    var shouldShowPrivacyForm = false
}
