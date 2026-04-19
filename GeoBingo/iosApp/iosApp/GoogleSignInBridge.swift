import GoogleSignIn
import UIKit
import ComposeApp

/// Native "Continue with Google" via the GoogleSignIn SDK.
///
/// Why native: the old Supabase flow opened Safari and relied on URL-scheme
/// callbacks. On iPad multi-scene that's fragile — the same class of crash we
/// fixed for Apple Sign-In. GIDSignIn uses ASWebAuthenticationSession with a
/// proper presentation anchor, so iPad / Stage Manager work cleanly.
///
/// Prerequisites (user-side, one-time):
///  1. Firebase Console → Authentication → Sign-in method → enable Google.
///     Download the updated GoogleService-Info.plist (now contains CLIENT_ID +
///     REVERSED_CLIENT_ID) and replace the existing one.
///  2. Add the REVERSED_CLIENT_ID (e.g. com.googleusercontent.apps.XXX) as a
///     CFBundleURLSchemes entry in Info.plist so Google can call back.
///  3. In Supabase Dashboard → Auth → Providers → Google, add the iOS OAuth
///     Client ID from GoogleService-Info.plist to "Authorized Client IDs"
///     (comma-separated with the existing Web client ID).
///  4. `pod install` in iosApp/.
///
/// If CLIENT_ID is missing from GoogleService-Info.plist the bridge reports
/// itself as unavailable and the Kotlin side falls back to Supabase's web
/// OAuth flow — so the app keeps working even when the user hasn't finished
/// the config steps above.
@objc class GoogleSignInBridgeImpl: NSObject {

    static let shared = GoogleSignInBridgeImpl()

    private var isConfigured = false

    func setup() {
        // Read the iOS OAuth client ID from GoogleService-Info.plist. If it's
        // missing we leave isConfigured=false and the Kotlin side will fall
        // back to the web flow.
        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
              let plist = NSDictionary(contentsOfFile: path),
              let clientId = plist["CLIENT_ID"] as? String,
              !clientId.isEmpty else {
            return
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientId)
        isConfigured = true

        GoogleSignInBridgeCompanion.shared.startHandler = { [weak self] in
            Task { @MainActor [weak self] in
                self?.startSignIn()
            }
        }
        // Flip the Kotlin-side flag so NativeGoogleSignIn.isSupported returns true.
        GoogleSignInBridgeCompanion.shared.setConfigured(configured: true)
    }

    @MainActor
    private func startSignIn() {
        guard isConfigured else {
            GoogleSignInBridge.shared.onError(message: "not_configured")
            return
        }
        // Try restoring a cached session first — silent if valid, falls through
        // to the interactive flow if not.
        if GIDSignIn.sharedInstance.hasPreviousSignIn() {
            GIDSignIn.sharedInstance.restorePreviousSignIn { [weak self] user, error in
                if let idToken = user?.idToken?.tokenString {
                    GoogleSignInBridge.shared.onSuccess(idToken: idToken)
                } else {
                    self?.interactiveSignIn()
                }
            }
        } else {
            interactiveSignIn()
        }
    }

    @MainActor
    private func interactiveSignIn() {
        guard let presenter = presentingViewController() else {
            GoogleSignInBridge.shared.onError(message: "no_presenter")
            return
        }
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let err = error as NSError? {
                // -5 = canceled (GIDSignInErrorCode.canceled)
                if err.code == -5 {
                    GoogleSignInBridge.shared.onError(message: "cancelled")
                } else {
                    GoogleSignInBridge.shared.onError(message: err.localizedDescription)
                }
                return
            }
            guard let idToken = result?.user.idToken?.tokenString else {
                GoogleSignInBridge.shared.onError(message: "no_id_token")
                return
            }
            GoogleSignInBridge.shared.onSuccess(idToken: idToken)
        }
    }

    /// Foreground-active view controller — iPad-safe scene pick (same pattern
    /// as the other bridges).
    @MainActor
    private func presentingViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let scene = scenes.first(where: { $0.activationState == .foregroundActive })
            ?? scenes.first(where: { $0.activationState == .foregroundInactive })
            ?? scenes.first
        let window = scene?.windows.first(where: { $0.isKeyWindow }) ?? scene?.windows.first
        var top = window?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}

// Called from iOSApp.onOpenURL so Google can complete the OAuth callback.
extension GoogleSignInBridgeImpl {
    @objc func handleOpenUrl(_ url: URL) -> Bool {
        return GIDSignIn.sharedInstance.handle(url)
    }
}
