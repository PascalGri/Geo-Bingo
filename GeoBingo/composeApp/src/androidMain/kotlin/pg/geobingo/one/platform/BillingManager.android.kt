package pg.geobingo.one.platform

import com.android.billingclient.api.*
import pg.geobingo.one.util.AppLogger

actual object BillingManager {
    private const val TAG = "BillingManager"
    actual val isBillingSupported: Boolean = true

    private var billingClient: BillingClient? = null
    private var pendingOnSuccess: (() -> Unit)? = null
    private var pendingOnError: ((String) -> Unit)? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgePurchase(purchase)
                    handlePurchaseSuccess(purchase)
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            pendingOnError?.invoke("Kauf abgebrochen")
            clearCallbacks()
        } else {
            pendingOnError?.invoke(billingResult.debugMessage)
            clearCallbacks()
        }
    }

    actual fun initialize() {
        if (billingClient != null) return
        billingClient = BillingClient.newBuilder(appContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    AppLogger.d(TAG, "Billing client connected")
                    checkExistingPurchases()
                } else {
                    AppLogger.w(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                AppLogger.d(TAG, "Billing service disconnected")
            }
        })
    }

    actual fun purchaseProduct(
        productId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val client = billingClient
        if (client == null || !client.isReady) {
            initialize()
            onError("Billing wird initialisiert, bitte erneut versuchen")
            return
        }

        pendingOnSuccess = onSuccess
        pendingOnError = onError

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || productDetailsList.isEmpty()) {
                onError("Produkt nicht gefunden")
                clearCallbacks()
                return@queryProductDetailsAsync
            }

            val productDetails = productDetailsList.first()
            val activity = currentActivity
            if (activity == null) {
                onError("Kauf nicht moeglich")
                clearCallbacks()
                return@queryProductDetailsAsync
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                )
                .build()

            client.launchBillingFlow(activity, flowParams)
        }
    }

    actual fun restorePurchases(
        onRestored: (List<String>) -> Unit,
        onError: (String) -> Unit,
    ) {
        val client = billingClient
        if (client == null || !client.isReady) {
            // Fallback to local
            val noAdsPurchased = AppSettings.getBoolean(SettingsKeys.NO_ADS_PURCHASED, false)
            if (noAdsPurchased) {
                onRestored(listOf("pg.geobingo.one.no_ads"))
            } else {
                onRestored(emptyList())
            }
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        client.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val restoredIds = mutableListOf<String>()
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        for (product in purchase.products) {
                            restoredIds.add(product)
                            if (product == "pg.geobingo.one.no_ads") {
                                AppSettings.setBoolean(SettingsKeys.NO_ADS_PURCHASED, true)
                            }
                        }
                    }
                }
                onRestored(restoredIds)
            } else {
                onError(billingResult.debugMessage)
            }
        }
    }

    actual fun isNoAdsPurchased(): Boolean =
        AppSettings.getBoolean(SettingsKeys.NO_ADS_PURCHASED, false)

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient?.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                AppLogger.d(TAG, "Purchase acknowledged")
            }
        }
    }

    private fun handlePurchaseSuccess(purchase: Purchase) {
        for (product in purchase.products) {
            if (product == "pg.geobingo.one.no_ads") {
                AppSettings.setBoolean(SettingsKeys.NO_ADS_PURCHASED, true)
            }
        }
        pendingOnSuccess?.invoke()
        clearCallbacks()
    }

    private fun checkExistingPurchases() {
        val client = billingClient ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { _, purchases ->
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    for (product in purchase.products) {
                        if (product == "pg.geobingo.one.no_ads") {
                            AppSettings.setBoolean(SettingsKeys.NO_ADS_PURCHASED, true)
                        }
                    }
                }
            }
        }
    }

    private fun clearCallbacks() {
        pendingOnSuccess = null
        pendingOnError = null
    }
}
