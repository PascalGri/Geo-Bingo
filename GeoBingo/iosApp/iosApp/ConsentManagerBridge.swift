import Foundation
import UserMessagingPlatform
import ComposeApp

/// Bridges the Google UMP consent flow from Swift into Kotlin.
/// Called from iOSApp.swift after ATT authorization.
@objc class ConsentManagerBridge: NSObject {

    @objc static func requestConsent() {
        let parameters = RequestParameters()
        // Set to true only for testing — remove in production
        // parameters.tagForUnderAgeOfConsent = false

        ConsentInformation.shared.requestConsentInfoUpdate(
            with: parameters,
            completionHandler: { error in
                guard error == nil else {
                    // On error: call Kotlin with no personalization
                    ConsentManagerBridgeCompanion.shared.onConsentReady(canPersonalize: false)
                    return
                }

                guard let presenter = topViewController() else {
                    NSLog("KatchIt: Skipping consent form — no foreground scene available")
                    ConsentManagerBridgeCompanion.shared.onConsentReady(canPersonalize: false)
                    return
                }
                ConsentForm.loadAndPresentIfRequired(from: presenter) { formError in
                    let status = ConsentInformation.shared.consentStatus
                    let canPersonalize = (status == .obtained || status == .notRequired)
                    ConsentManagerBridgeCompanion.shared.onConsentReady(canPersonalize: canPersonalize)
                }
            }
        )
    }

    @objc static func showPrivacyOptionsForm() {
        guard let presenter = topViewController() else {
            NSLog("KatchIt: Skipping privacy options form — no foreground scene available")
            ConsentManagerBridgeCompanion.shared.privacyOptionsCallback?()
            ConsentManagerBridgeCompanion.shared.privacyOptionsCallback = nil
            return
        }
        ConsentForm.presentPrivacyOptionsForm(from: presenter) { _ in
            ConsentManagerBridgeCompanion.shared.privacyOptionsCallback?()
            ConsentManagerBridgeCompanion.shared.privacyOptionsCallback = nil
        }
    }

    /// Resolves the topmost view controller suitable for presenting a sheet.
    /// Returns nil instead of a detached UIViewController() when no
    /// foreground-attached window exists — UMP's loadAndPresentIfRequired
    /// crashes when handed a viewcontroller that isn't in any scene.
    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let scene = scenes.first(where: { $0.activationState == .foregroundActive })
            ?? scenes.first(where: { $0.activationState == .foregroundInactive })
        guard let scene = scene else { return nil }
        let window = scene.windows.first(where: { $0.isKeyWindow }) ?? scene.windows.first
        guard let root = window?.rootViewController else { return nil }
        var top = root
        while let presented = top.presentedViewController {
            top = presented
        }
        return top
    }
}
