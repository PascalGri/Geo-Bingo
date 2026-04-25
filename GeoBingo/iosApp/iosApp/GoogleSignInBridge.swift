import GoogleSignIn
import CryptoKit
import Security
import UIKit
import ComposeApp

/// Native "Continue with Google" via the GoogleSignIn SDK.
///
/// Why native: the old Supabase flow opened Safari and relied on URL-scheme
/// callbacks. On iPad multi-scene that's fragile — the same class of crash we
/// fixed for Apple Sign-In. GIDSignIn uses ASWebAuthenticationSession with a
/// proper presentation anchor, so iPad / Stage Manager work cleanly.
///
/// Nonce handling: Supabase requires that any `nonce` claim in the id_token
/// matches a nonce passed to the token-exchange call. GoogleSignIn 9.x ALWAYS
/// embeds the nonce we give it as a plaintext `nonce` claim, so we generate
/// one here and hand it back to Kotlin → AccountManager forwards it as
/// `nonce = rawNonce` on the Supabase signInWith(IDToken) call. If we skipped
/// the nonce entirely, GIDSignIn might still add one internally → Supabase 400
/// "Passed nonce and nonce in id_token should either both exist or not."
///
/// Prerequisites (user-side, one-time):
///  1. Firebase Console → Authentication → Sign-in method → enable Google.
///     Download GoogleService-Info.plist (contains CLIENT_ID + REVERSED_CLIENT_ID).
///  2. REVERSED_CLIENT_ID as a CFBundleURLSchemes entry in Info.plist.
///  3. Supabase Dashboard → Auth → Providers → Google → add the iOS CLIENT_ID
///     to "Authorized Client IDs" (comma-separated with the web client ID).
///  4. `pod install` in iosApp/.
@objc class GoogleSignInBridgeImpl: NSObject {

    static let shared = GoogleSignInBridgeImpl()

    private var isConfigured = false

    func setup() {
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
        GoogleSignInBridgeCompanion.shared.setConfigured(configured: true)
    }

    @MainActor
    private func startSignIn() {
        guard isConfigured else {
            GoogleSignInBridge.shared.onError(message: "not_configured")
            return
        }
        guard let presenter = presentingViewController() else {
            GoogleSignInBridge.shared.onError(message: "no_presenter")
            return
        }

        // Supabase's OIDC flow for Google matches Apple's: send the SHA256
        // hash to the provider (ends up as the plaintext `nonce` claim in the
        // id_token), send the raw nonce to Supabase (Supabase hashes and
        // compares). Passing rawNonce to GIDSignIn produced
        // `invalid nonce: Nonces mismatch` in auth logs.
        let rawNonce = Self.makeNonce()
        let hashedNonce = Self.sha256(rawNonce)

        GIDSignIn.sharedInstance.signIn(
            withPresenting: presenter,
            hint: nil,
            additionalScopes: nil,
            nonce: hashedNonce
        ) { result, error in
            if let err = error as NSError? {
                // -5 = GIDSignInErrorCode.canceled
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
            GoogleSignInBridge.shared.onSuccess(idToken: idToken, rawNonce: rawNonce)
        }
    }

    private static func makeNonce() -> String {
        var bytes = [UInt8](repeating: 0, count: 32)
        let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        if status != errSecSuccess {
            // Extremely rare (Secure Enclave unavailable / OOM). Don't trap
            // the process on the auth path — fall back to UUID-derived bytes,
            // which is still ~122 bits of entropy and good enough for a nonce.
            NSLog("KatchIt: SecRandomCopyBytes failed (\(status)), falling back to UUID nonce")
            let fallback = (UUID().uuidString + UUID().uuidString).utf8
            bytes = Array(fallback.prefix(32))
        }
        return bytes.map { String(format: "%02x", $0) }.joined()
    }

    private static func sha256(_ input: String) -> String {
        let digest = SHA256.hash(data: Data(input.utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
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
