package pg.geobingo.one.platform

actual object BillingManager {
    actual val isBillingSupported: Boolean = false
    actual fun initialize() {}
    actual fun purchaseProduct(productId: String, onSuccess: () -> Unit, onError: (String) -> Unit) = onError("Not supported")
    actual fun restorePurchases(onRestored: (List<String>) -> Unit, onError: (String) -> Unit) = onRestored(emptyList())
    actual fun isNoAdsPurchased(): Boolean = false
}
