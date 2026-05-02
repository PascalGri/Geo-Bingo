import Foundation
import StoreKit
import UIKit
import ComposeApp

/// Bridges StoreKit 2 purchases from Swift into Kotlin via BillingBridge.
@objc class BillingBridgeImpl: NSObject {

    static let shared = BillingBridgeImpl()

    private var updateListenerTask: Task<Void, Error>? = nil

    // Cache fetched products so we don't re-fetch per tap (cheaper + more reliable).
    private var productCache: [String: Product] = [:]

    private static let consumableIds: Set<String> = [
        "pg.geobingo.one.stars_50",
        "pg.geobingo.one.stars_150",
        "pg.geobingo.one.stars_400",
        "pg.geobingo.one.stars_1000",
        "pg.geobingo.one.skip_3",
        "pg.geobingo.one.skip_10",
    ]

    private static let allProductIds: [String] = consumableIds.sorted() + ["pg.geobingo.one.no_ads"]

    func setup() {
        // Register handlers from Kotlin
        BillingBridgeCompanion.shared.initializeHandler = { [weak self] in
            self?.checkExistingPurchases()
        }
        BillingBridgeCompanion.shared.purchaseHandler = { [weak self] productId in
            self?.purchase(productId: productId as String)
        }
        BillingBridgeCompanion.shared.restoreHandler = { [weak self] in
            self?.restore()
        }

        // Pre-fetch products so the first tap doesn't race an App Store round-trip.
        Task { await self.prefetchProducts() }

        // Listen for transaction updates (renewals, revocations, etc.)
        updateListenerTask = listenForTransactions()
    }

    private func prefetchProducts() async {
        do {
            let products = try await Product.products(for: Self.allProductIds)
            for p in products { productCache[p.id] = p }
        } catch {
            // Non-fatal — purchase() will retry the fetch if cache is empty.
        }
    }

    private func listenForTransactions() -> Task<Void, Error> {
        Task.detached {
            for await result in Transaction.updates {
                if case .verified(let transaction) = result {
                    await transaction.finish()
                    // Only non-consumables (no_ads) need to be re-persisted on updates —
                    // consumables are credited immediately in purchase().
                    if transaction.productID == "pg.geobingo.one.no_ads" {
                        BillingBridge.shared.onPurchaseSuccess(productId: "pg.geobingo.one.no_ads")
                    }
                }
            }
        }
    }

    private func checkExistingPurchases() {
        Task {
            for await result in Transaction.currentEntitlements {
                if case .verified(let transaction) = result {
                    if transaction.productID == "pg.geobingo.one.no_ads" {
                        BillingBridge.shared.onPurchaseSuccess(productId: "pg.geobingo.one.no_ads")
                    }
                }
            }
        }
    }

    /// Active foreground window scene. Returns nil if no scene is foregroundActive
    /// or foregroundInactive — passing a backgrounded / unattached scene to
    /// `Product.purchase(confirmIn:)` on iOS 17+ makes StoreKit silently no-op
    /// on iPad (no sheet, no callback, no error). That was the App Review 2.1(b)
    /// rejection on builds 13–15: the previous fallback to `scenes.first` quietly
    /// handed StoreKit a bad scene and the purchase tap appeared to do nothing.
    @MainActor
    private func activeWindowScene() -> UIWindowScene? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        if let active = scenes.first(where: { $0.activationState == .foregroundActive }) {
            return active
        }
        if let inactive = scenes.first(where: { $0.activationState == .foregroundInactive }) {
            NSLog("KatchIt[Billing]: no foregroundActive scene, using foregroundInactive")
            return inactive
        }
        NSLog("KatchIt[Billing]: no foreground scene available — falling back to scene-less purchase()")
        return nil
    }

    /// User-facing message strings for IAP failure paths. Hardcoded (rather
    /// than routed through Kotlin `S.current`) because the bridged Compose
    /// `mutableStateOf` accessor is fragile and these paths run on the
    /// StoreKit error edge. Locale-aware so an English-locale Apple Reviewer
    /// doesn't see German-only error toasts (the previous "Produkt nicht
    /// gefunden" / "Verifizierung fehlgeschlagen" prose was a likely
    /// contributor to the 5.1.1(i) reviewer-confusion).
    @MainActor
    private func msg(_ en: String, _ de: String) -> String {
        let isGerman = Locale.current.language.languageCode?.identifier == "de"
        return isGerman ? de : en
    }
    @MainActor private func noForegroundSceneMessage() -> String {
        msg(
            "Couldn't open the App Store dialog. Please bring KatchIt to the foreground and try again.",
            "App-Store-Dialog konnte nicht geöffnet werden. Bitte hole KatchIt in den Vordergrund und versuche es erneut."
        )
    }
    @MainActor private func productNotFoundMessage() -> String {
        msg("Product not found in the App Store.", "Produkt nicht gefunden.")
    }
    @MainActor private func verificationFailedMessage() -> String {
        msg("Purchase verification failed.", "Verifizierung fehlgeschlagen.")
    }
    @MainActor private func purchasePendingMessage() -> String {
        msg(
            "Purchase pending — waiting for approval (e.g. Ask to Buy).",
            "Kauf ausstehend — wartet auf Genehmigung (z. B. Ask to Buy)."
        )
    }
    @MainActor private func unknownErrorMessage() -> String {
        msg("Unknown error.", "Unbekannter Fehler.")
    }

    private func purchase(productId: String) {
        Task { @MainActor in
            do {
                let product: Product
                if let cached = productCache[productId] {
                    product = cached
                } else {
                    let products = try await Product.products(for: [productId])
                    guard let fetched = products.first else {
                        BillingBridge.shared.onPurchaseError(message: productNotFoundMessage())
                        return
                    }
                    productCache[productId] = fetched
                    product = fetched
                }

                let result: Product.PurchaseResult
                if #available(iOS 17.0, *) {
                    // iOS 17+ requires an explicit scene. Without one, the
                    // purchase sheet silently never presents on iPad — fail
                    // visibly instead of hanging.
                    guard let scene = activeWindowScene() else {
                        BillingBridge.shared.onPurchaseError(message: noForegroundSceneMessage())
                        return
                    }
                    result = try await product.purchase(confirmIn: scene)
                } else {
                    result = try await product.purchase()
                }

                switch result {
                case .success(let verification):
                    if case .verified(let transaction) = verification {
                        await transaction.finish()
                        BillingBridge.shared.onPurchaseSuccess(productId: productId)
                    } else {
                        BillingBridge.shared.onPurchaseError(message: verificationFailedMessage())
                    }
                case .userCancelled:
                    // Sentinel — Kotlin side recognises this and silently
                    // resets the loading spinner without showing an error
                    // snackbar (user explicitly cancelled, not a failure).
                    BillingBridge.shared.onPurchaseError(message: "USER_CANCELLED")
                case .pending:
                    BillingBridge.shared.onPurchaseError(message: purchasePendingMessage())
                @unknown default:
                    BillingBridge.shared.onPurchaseError(message: unknownErrorMessage())
                }
            } catch {
                BillingBridge.shared.onPurchaseError(message: error.localizedDescription)
            }
        }
    }

    private func restore() {
        Task {
            // Force StoreKit to refresh entitlements from Apple's servers — without
            // this, currentEntitlements only returns what's already cached locally,
            // so a freshly-reinstalled app would never see prior purchases.
            do {
                try await AppStore.sync()
            } catch {
                BillingBridge.shared.onRestoreError(message: error.localizedDescription)
                return
            }
            var restoredIds: [String] = []
            for await result in Transaction.currentEntitlements {
                if case .verified(let transaction) = result {
                    restoredIds.append(transaction.productID)
                }
            }
            BillingBridge.shared.onRestoreComplete(productIds: restoredIds)
        }
    }
}
