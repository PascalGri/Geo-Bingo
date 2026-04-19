import AuthenticationServices
import CryptoKit
import UIKit
import ComposeApp

/// Native Sign in with Apple via ASAuthorizationController.
///
/// Apple App Review 4.1.2 requires native Sign in with Apple whenever an app offers
/// third-party login — and the web-based flow (opening Safari via UIApplication.openURL)
/// crashes on iPad in multi-scene scenarios. This bridge does it the native way:
/// ASAuthorizationController anchors to the active UIWindow, returns an identity token
/// + nonce that we feed straight to Supabase via signInWith(IDToken).
@objc class AppleSignInBridgeImpl: NSObject {

    static let shared = AppleSignInBridgeImpl()

    // Must be retained for the callback lifetime — ASAuthorizationController holds
    // only a weak reference to its delegate / presentation context provider.
    private var activeDelegate: AppleSignInDelegate?

    func setup() {
        AppleSignInBridgeCompanion.shared.startHandler = { [weak self] nonce in
            // Hop to main actor — ASAuthorizationController must be presented on main.
            let raw = nonce as String
            Task { @MainActor [weak self] in
                self?.startSignIn(rawNonce: raw)
            }
        }
    }

    @MainActor
    private func startSignIn(rawNonce: String) {
        let hashed = Self.sha256(rawNonce)

        let provider = ASAuthorizationAppleIDProvider()
        let request = provider.createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = hashed

        let delegate = AppleSignInDelegate(
            rawNonce: rawNonce,
            onComplete: { [weak self] in
                self?.activeDelegate = nil
            }
        )
        self.activeDelegate = delegate

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = delegate
        controller.presentationContextProvider = delegate
        controller.performRequests()
    }

    private static func sha256(_ input: String) -> String {
        let data = Data(input.utf8)
        let digest = SHA256.hash(data: data)
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}

private final class AppleSignInDelegate: NSObject, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {

    private let rawNonce: String
    private let onComplete: () -> Void

    init(rawNonce: String, onComplete: @escaping () -> Void) {
        self.rawNonce = rawNonce
        self.onComplete = onComplete
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        // Prefer the foreground-active window; fall back to any attached window so we
        // never hand back a detached UIWindow() (which crashes on iPad).
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let scene = scenes.first(where: { $0.activationState == .foregroundActive })
            ?? scenes.first(where: { $0.activationState == .foregroundInactive })
            ?? scenes.first
        if let window = scene?.windows.first(where: { $0.isKeyWindow }) ?? scene?.windows.first {
            return window
        }
        return ASPresentationAnchor()
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        defer { onComplete() }
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let tokenData = credential.identityToken,
              let idToken = String(data: tokenData, encoding: .utf8) else {
            AppleSignInBridge.shared.onError(message: "Kein Identity-Token von Apple erhalten")
            return
        }
        AppleSignInBridge.shared.onSuccess(idToken: idToken, rawNonce: rawNonce)
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        defer { onComplete() }
        let err = error as NSError
        // ASAuthorizationError.canceled = 1001 — treat as user cancel (no error toast).
        if err.domain == ASAuthorizationError.errorDomain && err.code == ASAuthorizationError.canceled.rawValue {
            AppleSignInBridge.shared.onError(message: "cancelled")
        } else {
            AppleSignInBridge.shared.onError(message: err.localizedDescription)
        }
    }
}
