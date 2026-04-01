import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    // Polling-Timer: prüft ob Kotlin Ad-Requests gestellt hat
    let adPollTimer = Timer.publish(every: 2.0, on: .main, in: .common).autoconnect()

    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .onAppear {
                // Ads nach Consent vorladen
                AdManagerBridgeImpl.shared.preloadAds()
            }
            .onReceive(adPollTimer) { _ in
                handleKotlinAdRequests()
            }
    }

    private func handleKotlinAdRequests() {
        let bridge = AdManagerBridge.shared

        if bridge.shouldShowRewarded {
            bridge.shouldShowRewarded = false
            AdManagerBridgeImpl.shared.showRewardedAd()
        }
        if bridge.shouldShowInterstitial {
            bridge.shouldShowInterstitial = false
            AdManagerBridgeImpl.shared.showInterstitialAd()
        }
        if bridge.shouldPreload {
            bridge.shouldPreload = false
            AdManagerBridgeImpl.shared.preloadAds()
        }

        // Privacy options form
        let consentBridge = ConsentManagerBridgeCompanion.shared
        if consentBridge.shouldShowPrivacyForm {
            consentBridge.shouldShowPrivacyForm = false
            ConsentManagerBridge.showPrivacyOptionsForm()
        }
    }
}
