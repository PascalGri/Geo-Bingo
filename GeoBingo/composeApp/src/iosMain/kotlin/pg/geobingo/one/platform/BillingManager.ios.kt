package pg.geobingo.one.platform

actual object BillingManager {
    actual val isBillingSupported: Boolean = true

    actual fun initialize() {
        BillingBridge.initialize()
    }

    actual fun purchaseProduct(
        productId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        BillingBridge.purchaseProduct(productId, onSuccess, onError)
    }

    actual fun restorePurchases(
        onRestored: (List<String>) -> Unit,
        onError: (String) -> Unit,
    ) {
        BillingBridge.restorePurchases(onRestored, onError)
    }

    actual fun isNoAdsPurchased(): Boolean =
        AppSettings.getBoolean(SettingsKeys.NO_ADS_PURCHASED, false)
}

// Bridge object - Swift calls back into these
object BillingBridge {
    private var purchaseSuccessCallback: (() -> Unit)? = null
    private var purchaseErrorCallback: ((String) -> Unit)? = null
    private var restoreCallback: ((List<String>) -> Unit)? = null
    private var restoreErrorCallback: ((String) -> Unit)? = null

    fun initialize() {
        // Swift side checks existing purchases on init
        BillingBridgeCompanion.initialize()
    }

    fun purchaseProduct(productId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        purchaseSuccessCallback = onSuccess
        purchaseErrorCallback = onError
        BillingBridgeCompanion.purchase(productId = productId)
    }

    fun restorePurchases(onRestored: (List<String>) -> Unit, onError: (String) -> Unit) {
        restoreCallback = onRestored
        restoreErrorCallback = onError
        BillingBridgeCompanion.restore()
    }

    // Called from Swift:
    fun onPurchaseSuccess(productId: String) {
        if (productId == "pg.geobingo.one.no_ads") {
            AppSettings.setBoolean(SettingsKeys.NO_ADS_PURCHASED, true)
        }
        purchaseSuccessCallback?.invoke()
        purchaseSuccessCallback = null
        purchaseErrorCallback = null
    }

    fun onPurchaseError(message: String) {
        purchaseErrorCallback?.invoke(message)
        purchaseSuccessCallback = null
        purchaseErrorCallback = null
    }

    fun onRestoreComplete(productIds: List<String>) {
        for (id in productIds) {
            if (id == "pg.geobingo.one.no_ads") {
                AppSettings.setBoolean(SettingsKeys.NO_ADS_PURCHASED, true)
            }
        }
        restoreCallback?.invoke(productIds)
        restoreCallback = null
        restoreErrorCallback = null
    }

    fun onRestoreError(message: String) {
        restoreErrorCallback?.invoke(message)
        restoreCallback = null
        restoreErrorCallback = null
    }
}

// Swift-side companion for bridge calls
object BillingBridgeCompanion {
    var initializeHandler: (() -> Unit)? = null
    var purchaseHandler: ((String) -> Unit)? = null
    var restoreHandler: (() -> Unit)? = null

    fun initialize() {
        initializeHandler?.invoke()
    }

    fun purchase(productId: String) {
        purchaseHandler?.invoke(productId)
    }

    fun restore() {
        restoreHandler?.invoke()
    }
}
