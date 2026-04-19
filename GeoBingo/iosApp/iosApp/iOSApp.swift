import SwiftUI
import AppTrackingTransparency
import GoogleMobileAds
import FirebaseCore
import FirebaseMessaging
import UserNotifications
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        // Request notification permission
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
            if granted {
                DispatchQueue.main.async {
                    application.registerForRemoteNotifications()
                }
            }
        }
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        // Pass FCM token to Kotlin side
        PushManager.shared.apnsToken = token
    }

    // Show notifications even when app is in foreground
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .badge, .sound])
    }

    // Handle notification tap
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo
        if let gameCode = userInfo["game_code"] as? String {
            DeepLinkHandler.shared.handleUrl(url: "https://katchit.app/join/\(gameCode)")
        }
        completionHandler()
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        // Disable verbose logging in release builds
        #if !DEBUG
        AppLogger.shared.minLevel = LogLevel.error
        #endif
        // ATT muss VOR UMP/AdMob-Consent aufgerufen werden (Apple-Anforderung seit iOS 14.5)
        requestTrackingAuthorization()
        // StoreKit 2 Bridge initialisieren
        BillingBridgeImpl.shared.setup()
        // Sign in with Apple (nativ via ASAuthorizationController)
        AppleSignInBridgeImpl.shared.setup()
        // Sign in with Google (nativ via GIDSignIn)
        GoogleSignInBridgeImpl.shared.setup()
        // Analytics platform
        Analytics.shared.platform = "ios"
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Google's native sign-in completes via a reverse-client-ID URL —
                    // let GIDSignIn claim the callback first, fall through otherwise.
                    if GoogleSignInBridgeImpl.shared.handleOpenUrl(url) {
                        return
                    }
                    // Legacy web OAuth callback (Apple or fallback Google via Supabase).
                    if url.absoluteString.contains("login-callback") {
                        IosAuthCallback.shared.handle(url: url)
                    } else {
                        DeepLinkHandler.shared.handleUrl(url: url.absoluteString)
                    }
                }
        }
    }

    private func requestTrackingAuthorization() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            ATTrackingManager.requestTrackingAuthorization { _ in
                ConsentManagerBridge.requestConsent()
                MobileAds.shared.start(completionHandler: nil)
            }
        }
    }
}
