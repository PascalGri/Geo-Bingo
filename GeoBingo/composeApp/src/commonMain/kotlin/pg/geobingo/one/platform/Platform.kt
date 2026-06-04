package pg.geobingo.one.platform

/**
 * True only on Apple platforms (iOS). Gates features that Apple App Review
 * disallows outside StoreKit — notably promo codes that grant Stars, the paid
 * in-app currency (Guideline 3.1.1). On iOS such giveaways must instead go
 * through StoreKit Offer Codes; on every other platform the in-app redeem
 * flow is permitted.
 */
expect val isApplePlatform: Boolean
