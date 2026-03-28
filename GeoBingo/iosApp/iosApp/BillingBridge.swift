import Foundation
import StoreKit
import ComposeApp

/// Bridges StoreKit 2 purchases from Swift into Kotlin via BillingBridge.
@objc class BillingBridgeImpl: NSObject {

    static let shared = BillingBridgeImpl()

    private var updateListenerTask: Task<Void, Error>? = nil

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

        // Listen for transaction updates (renewals, revocations, etc.)
        updateListenerTask = listenForTransactions()
    }

    private func listenForTransactions() -> Task<Void, Error> {
        Task.detached {
            for await result in Transaction.updates {
                if case .verified(let transaction) = result {
                    await transaction.finish()
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

    private func purchase(productId: String) {
        Task {
            do {
                let products = try await Product.products(for: [productId])
                guard let product = products.first else {
                    BillingBridge.shared.onPurchaseError(message: "Produkt nicht gefunden")
                    return
                }

                let result = try await product.purchase()

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
