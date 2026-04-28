import SwiftUI
import AppTrackingTransparency
import GoogleMobileAds
import FirebaseCore
import FirebaseMessaging
import UserNotifications
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Only call FirebaseApp.configure if the plist is present + parseable.
        // Apple's automated iPad review runs in a sandbox where an invalid/missing
        // GoogleService-Info.plist (or one whose GOOGLE_APP_ID doesn't match the
        // signed bundle id) makes configure() NSAssert-abort → hard launch crash.
        if
            let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
            let plist = NSDictionary(contentsOfFile: path),
            plist["GOOGLE_APP_ID"] as? String != nil,
            plist["BUNDLE_ID"] as? String == Bundle.main.bundleIdentifier
        {
            FirebaseApp.configure()
            Messaging.messaging().delegate = self
        } else {
            NSLog("KatchIt: Skipping FirebaseApp.configure — plist missing or bundle id mismatch")
        }
        UNUserNotificationCenter.current().delegate = self
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
        // Reject iPad: app is iPhone-only.
        // Some Apple reviewers test on iPad despite TARGETED_DEVICE_FAMILY=1,
        // so we fail explicitly rather than silently allowing iPad compatibility.
        if UIDevice.current.userInterfaceIdiom == .pad {
            let alert = UIAlertController(
                title: "KatchIt! ist nur für iPhone verfügbar",
                message: "Diese App ist für iPhone optimiert. Bitte installieren Sie KatchIt! auf einem iPhone.",
                preferredStyle: .alert
            )
            alert.addAction(UIAlertAction(title: "OK", style: .cancel) { _ in
                exit(0)
            })
            // Present on the app window; defer presentation until the window is available
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                      let window = windowScene.windows.first else { return }
                window.rootViewController?.present(alert, animated: true)
            }
        }

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
