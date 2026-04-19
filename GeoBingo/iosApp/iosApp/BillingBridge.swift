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

    /// Best-effort active window scene. On iPad (multi-scene) and on iOS 17+ StoreKit
    /// strongly prefers being given an explicit scene so the purchase sheet can anchor
    /// to the right window — without this, purchase() silently no-ops on iPad.
    @MainActor
    private func activeWindowScene() -> UIWindowScene? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes.first(where: { $0.activationState == .foregroundActive })
            ?? scenes.first(where: { $0.activationState == .foregroundInactive })
            ?? scenes.first
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
                        BillingBridge.shared.onPurchaseError(message: "Produkt nicht gefunden")
                        return
                    }
                    productCache[productId] = fetched
                    product = fetched
                }

                let result: Product.PurchaseResult
                if let scene = activeWindowScene() {
                    // iOS 17+ API — required for iPad to actually present the sheet.
                    if #available(iOS 17.0, *) {
                        result = try await product.purchase(confirmIn: scene)
                    } else {
                        result = try await product.purchase()
                    }
                } else {
                    result = try await product.purchase()
                }

                switch result {
                case .success(let verification):
                    if case .verified(let transaction) = verification {
                        await transaction.finish()
                        BillingBridge.shared.onPurchaseSuccess(productId: productId)
                    } else {
                        BillingBridge.shared.onPurchaseError(message: "Verifizierung fehlgeschlagen")
                    }
                case .userCancelled:
                    BillingBridge.shared.onPurchaseError(message: "Kauf abgebrochen")
                case .pending:
                    BillingBridge.shared.onPurchaseError(message: "Kauf ausstehend")
                @unknown default:
                    BillingBridge.shared.onPurchaseError(message: "Unbekannter Fehler")
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
