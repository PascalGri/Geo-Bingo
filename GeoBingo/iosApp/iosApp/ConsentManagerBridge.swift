import Foundation
import UserMessagingPlatform
import ComposeApp

/// Bridges the Google UMP consent flow from Swift into Kotlin.
/// Called from iOSApp.swift after ATT authorization.
@objc class ConsentManagerBridge: NSObject {

    @objc static func requestConsent() {
        let parameters = UMPRequestParameters()
        // Set to true only for testing — remove in production
        // parameters.tagForUnderAgeOfConsent = false

        UMPConsentInformation.sharedInstance.requestConsentInfoUpdate(
            with: parameters,
            completionHandler: { error in
                guard error == nil else {
                    // On error: call Kotlin with no personalization
                    ConsentManagerKt.onConsentReady(canPersonalize: false)
                    return
                }

                UMPConsentForm.loadAndPresentIfRequired(from: topViewController()) { formError in
                    let status = UMPConsentInformation.sharedInstance.consentStatus
                    let canPersonalize = (status == .obtained || status == .notRequired)
                    ConsentManagerKt.onConsentReady(canPersonalize: canPersonalize)
                }
            }
        )
    }

    @objc static func showPrivacyOptionsForm() {
        UMPConsentForm.presentPrivacyOptionsForm(from: topViewController()) { _ in
            ConsentManagerBridgeCompanion.shared.privacyOptionsCallback?()
            ConsentManagerBridgeCompanion.shared.privacyOptionsCallback = nil
        }
    }

    private static func topViewController() -> UIViewController {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let root = scene.windows.first?.rootViewController else {
            return UIViewController()
        }
        var top = root
        while let presented = top.presentedViewController {
            top = presented
        }
        return top
    }
}
