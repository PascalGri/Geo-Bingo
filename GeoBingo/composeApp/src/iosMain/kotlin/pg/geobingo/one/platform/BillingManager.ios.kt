package pg.geobingo.one.platform

actual object BillingManager {
    actual val isBillingSupported: Boolean = true

    actual fun initialize() {
        // TODO: Initialize StoreKit 2
    }

    actual fun purchaseProduct(
        productId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        // TODO: Implement StoreKit 2 purchase flow
        onError("Billing not yet configured")
    }

    actual fun restorePurchases(
        onRestored: (List<String>) -> Unit,
        onError: (String) -> Unit,
    ) {
        // TODO: Restore via StoreKit 2
        val noAdsPurchased = AppSettings.getBoolean(SettingsKeys.NO_ADS_PURCHASED, false)
        if (noAdsPurchased) {
            onRestored(listOf("pg.geobingo.one.no_ads"))
        } else {
            onRestored(emptyList())
        }
    }

    actual fun isNoAdsPurchased(): Boolean =
        AppSettings.getBoolean(SettingsKeys.NO_ADS_PURCHASED, false)
}
