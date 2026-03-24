import Foundation
import GoogleMobileAds
import ComposeApp

private let rewardedAdUnitId = "ca-app-pub-4871207394525716/6419331704"
private let interstitialAdUnitId = "ca-app-pub-4871207394525716/3537450244"

/// Implements the AdMob logic for iOS and bridges back to Kotlin via AdManagerBridge.
@objc class AdManagerBridgeImpl: NSObject {

    static let shared = AdManagerBridgeImpl()

    private var rewardedAd: RewardedAd?
    private var interstitialAd: InterstitialAd?

    // Strong references to delegates — fullScreenContentDelegate is weak, so we must retain them
    private var rewardedDelegate: RewardedDelegate?
    private var interstitialDelegate: InterstitialDelegate?

    // Called from ContentView's onAppear after consent
    @objc func preloadAds() {
        loadRewardedAd()
        loadInterstitialAd()
    }

    private func loadRewardedAd() {
        RewardedAd.load(
            with: rewardedAdUnitId,
            request: Request()
        ) { [weak self] ad, error in
            if error == nil { self?.rewardedAd = ad }
        }
    }

    private func loadInterstitialAd() {
        InterstitialAd.load(
            with: interstitialAdUnitId,
            request: Request()
        ) { [weak self] ad, error in
            if error == nil { self?.interstitialAd = ad }
        }
    }

    @objc func showRewardedAd() {
        let bridge = AdManagerBridge.shared
        guard let ad = rewardedAd, let vc = topViewController() else {
            bridge.rewardDismissCallback?()
            bridge.rewardDismissCallback = nil
            bridge.shouldShowRewarded = false
            return
        }

        ad.present(from: vc, userDidEarnRewardHandler: {
            bridge.rewardCallback?()
            bridge.rewardCallback = nil
        })

        let delegate = RewardedDelegate { [weak self] in
            self?.rewardedAd = nil
            self?.rewardedDelegate = nil
            bridge.rewardDismissCallback?()
            bridge.rewardDismissCallback = nil
            bridge.shouldShowRewarded = false
            self?.loadRewardedAd()
        }
        rewardedDelegate = delegate
        ad.fullScreenContentDelegate = delegate
        rewardedAd = nil
    }

    @objc func showInterstitialAd() {
        let bridge = AdManagerBridge.shared
        guard let ad = interstitialAd, let vc = topViewController() else {
            bridge.interstitialDismissCallback?()
            bridge.interstitialDismissCallback = nil
            bridge.shouldShowInterstitial = false
            return
        }

        ad.present(from: vc)
        let delegate = InterstitialDelegate { [weak self] in
            self?.interstitialAd = nil
            self?.interstitialDelegate = nil
            bridge.interstitialDismissCallback?()
            bridge.interstitialDismissCallback = nil
            bridge.shouldShowInterstitial = false
            self?.loadInterstitialAd()
        }
        interstitialDelegate = delegate
        ad.fullScreenContentDelegate = delegate
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

private class RewardedDelegate: NSObject, FullScreenContentDelegate {
    let onDismiss: () -> Void
    init(_ onDismiss: @escaping () -> Void) { self.onDismiss = onDismiss }
    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) { onDismiss() }
    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) { onDismiss() }
}

private class InterstitialDelegate: NSObject, FullScreenContentDelegate {
    let onDismiss: () -> Void
    init(_ onDismiss: @escaping () -> Void) { self.onDismiss = onDismiss }
    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) { onDismiss() }
    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) { onDismiss() }
}
