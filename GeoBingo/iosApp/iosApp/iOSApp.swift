import SwiftUI
import AppTrackingTransparency
import GoogleMobileAds

@main
struct iOSApp: App {
    init() {
        // ATT muss VOR UMP/AdMob-Consent aufgerufen werden (Apple-Anforderung seit iOS 14.5)
        requestTrackingAuthorization()
        // StoreKit 2 Bridge initialisieren
        BillingBridgeImpl.shared.setup()
        // Analytics platform
        Analytics.shared.platform = "ios"
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

    private func requestTrackingAuthorization() {
        // Kurze Verzögerung damit der App-Launch-Bildschirm fertig ist
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            ATTrackingManager.requestTrackingAuthorization { _ in
                // Nach ATT: UMP Consent-Dialog zeigen (DSGVO-Pflicht)
                ConsentManagerBridge.requestConsent()
                // MobileAds initialisieren (parallel, unabhängig vom Consent-Status)
                MobileAds.shared.start(completionHandler: nil)
            }
        }
    }
}
