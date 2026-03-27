package pg.geobingo.one.platform

actual object BillingManager {
    actual val isBillingSupported: Boolean = true

    actual fun initialize() {
        // TODO: Initialize Google Play Billing Library
        // BillingClient.newBuilder(appContext)
        //   .setListener(purchasesUpdatedListener)
        //   .enablePendingPurchases()
        //   .build()
    }

    actual fun purchaseProduct(
        productId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        // TODO: Implement Google Play Billing purchase flow
        // For now, call onError to indicate not yet implemented
        onError("Billing not yet configured")
    }

    actual fun restorePurchases(
        onRestored: (List<String>) -> Unit,
        onError: (String) -> Unit,
    ) {
        // TODO: Query purchases from Google Play
        // BillingClient.queryPurchasesAsync(...)
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
