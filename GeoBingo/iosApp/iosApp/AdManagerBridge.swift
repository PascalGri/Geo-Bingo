import Foundation
import GoogleMobileAds
import ComposeApp

private let rewardedAdUnitId = "ca-app-pub-4871207394525716/6419331704"
private let interstitialAdUnitId = "ca-app-pub-4871207394525716/3537450244"

/// Implements the AdMob logic for iOS and bridges back to Kotlin via AdManagerBridgeCompanion.
@objc class AdManagerBridgeImpl: NSObject {

    static let shared = AdManagerBridgeImpl()

    private var rewardedAd: GADRewardedAd?
    private var interstitialAd: GADInterstitialAd?

    // Called from ContentView's onAppear after consent
    @objc func preloadAds() {
        loadRewardedAd()
        loadInterstitialAd()
    }

    private func loadRewardedAd() {
        GADRewardedAd.load(
            withAdUnitID: rewardedAdUnitId,
            request: GADRequest()
        ) { [weak self] ad, error in
            if error == nil { self?.rewardedAd = ad }
        }
    }

    private func loadInterstitialAd() {
        GADInterstitialAd.load(
            withAdUnitID: interstitialAdUnitId,
            request: GADRequest()
        ) { [weak self] ad, error in
            if error == nil { self?.interstitialAd = ad }
        }
    }

    @objc func showRewardedAd() {
        guard let bridge = AdManagerBridge.companion as? AdManagerBridgeCompanion else { return }
        guard let ad = rewardedAd, let vc = topViewController() else {
            bridge.rewardDismissCallback?()
            bridge.rewardDismissCallback = nil
            bridge.shouldShowRewarded = false
            return
        }

        ad.present(fromRootViewController: vc, userDidEarnRewardHandler: {
            bridge.rewardCallback?()
            bridge.rewardCallback = nil
        })

        ad.fullScreenContentDelegate = RewardedDelegate {
            self.rewardedAd = nil
            bridge.rewardDismissCallback?()
            bridge.rewardDismissCallback = nil
            bridge.shouldShowRewarded = false
            self.loadRewardedAd()
        }
        rewardedAd = nil
    }

    @objc func showInterstitialAd() {
        guard let bridge = AdManagerBridge.companion as? AdManagerBridgeCompanion else { return }
        guard let ad = interstitialAd, let vc = topViewController() else {
            bridge.interstitialDismissCallback?()
            bridge.interstitialDismissCallback = nil
            bridge.shouldShowInterstitial = false
            return
        }

        ad.present(fromRootViewController: vc)
        ad.fullScreenContentDelegate = InterstitialDelegate {
            self.interstitialAd = nil
            bridge.interstitialDismissCallback?()
            bridge.interstitialDismissCallback = nil
            bridge.shouldShowInterstitial = false
            self.loadInterstitialAd()
        }
        interstitialAd = nil
    }

    private func topViewController() -> UIViewController? {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let root = scene.windows.first?.rootViewController else { return nil }
        var top = root
        while let presented = top.presentedViewController { top = presented }
        return top
    }
}

// MARK: - Delegates

private class RewardedDelegate: NSObject, GADFullScreenContentDelegate {
    let onDismiss: () -> Void
    init(_ onDismiss: @escaping () -> Void) { self.onDismiss = onDismiss }
    func adDidDismissFullScreenContent(_ ad: GADFullScreenPresentingAd) { onDismiss() }
    func ad(_ ad: GADFullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) { onDismiss() }
}

private class InterstitialDelegate: NSObject, GADFullScreenContentDelegate {
    let onDismiss: () -> Void
    init(_ onDismiss: @escaping () -> Void) { self.onDismiss = onDismiss }
    func adDidDismissFullScreenContent(_ ad: GADFullScreenPresentingAd) { onDismiss() }
    func ad(_ ad: GADFullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) { onDismiss() }
}
