package pg.geobingo.one.platform

data class StoreProduct(
    val id: String,
    val title: String,
    val price: String,
)

/**
 * Platform-agnostic billing interface for In-App Purchases.
 * On Desktop and Web all methods are No-Ops.
 */
expect object BillingManager {
    val isBillingSupported: Boolean

    fun initialize()

    fun purchaseProduct(
        productId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    fun restorePurchases(
        onRestored: (List<String>) -> Unit,
        onError: (String) -> Unit,
    )

    fun isNoAdsPurchased(): Boolean
}
