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
///
/// iPad crash note: ASAuthorizationController calls `presentationAnchor(for:)`
/// synchronously from `performRequests()`. If we returned a freshly constructed
/// `ASPresentationAnchor()` (i.e. a detached UIWindow with no scene) on iPad in
/// iPhone-compat mode under iPadOS 26.x multi-scene, the framework crashes
/// while trying to attach its presentation context to the window's scene. We
/// resolve a real, scene-attached UIWindow up-front and fail the auth request
/// cleanly via `onError` if none can be found, instead of presenting on a
/// detached anchor.
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
        NSLog("KatchIt: Apple Sign-In start (idiom=%d, scenes=%d)",
              UIDevice.current.userInterfaceIdiom.rawValue,
              UIApplication.shared.connectedScenes.count)

        guard let anchor = Self.resolveAnchor() else {
            NSLog("KatchIt: Apple Sign-In aborted — no presentation anchor resolvable")
            AppleSignInBridge.shared.onError(message: "no_presentation_anchor")
            return
        }
        NSLog("KatchIt: Apple Sign-In anchor resolved (scene=%@)",
              anchor.windowScene?.session.persistentIdentifier ?? "nil")

        let hashed = Self.sha256(rawNonce)

        let provider = ASAuthorizationAppleIDProvider()
        let request = provider.createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = hashed

        let delegate = AppleSignInDelegate(
            rawNonce: rawNonce,
            anchor: anchor,
            onComplete: { [weak self] in
                self?.activeDelegate = nil
            }
        )
        self.activeDelegate = delegate

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = delegate
        controller.presentationContextProvider = delegate
        controller.performRequests()
        NSLog("KatchIt: Apple Sign-In performRequests dispatched")
    }

    /// Find a window suitable for hosting an ASAuthorizationController.
    /// Tries foregroundActive → foregroundInactive → any window scene as a
    /// last resort. iPad iPhone-compat mode under iPadOS 26.x can leave the
    /// scene in an unexpected state, so we don't want to silently bail when
    /// there's still a window we could anchor to.
    @MainActor
    private static func resolveAnchor() -> ASPresentationAnchor? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let preferred = scenes.first(where: { $0.activationState == .foregroundActive })
            ?? scenes.first(where: { $0.activationState == .foregroundInactive })
            ?? scenes.first
        guard let scene = preferred else {
            NSLog("KatchIt: resolveAnchor — no UIWindowScene in connectedScenes")
            return nil
        }
        if let key = scene.windows.first(where: { $0.isKeyWindow }) {
            return key
        }
        if let any = scene.windows.first {
            NSLog("KatchIt: resolveAnchor — falling back to non-key window")
            return any
        }
        NSLog("KatchIt: resolveAnchor — scene has no windows")
        return nil
    }

    private static func sha256(_ input: String) -> String {
        let data = Data(input.utf8)
        let digest = SHA256.hash(data: data)
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}

private final class AppleSignInDelegate: NSObject, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {

    private let rawNonce: String
    private let anchor: ASPresentationAnchor
    private let onComplete: () -> Void

    init(rawNonce: String, anchor: ASPresentationAnchor, onComplete: @escaping () -> Void) {
        self.rawNonce = rawNonce
        self.anchor = anchor
        self.onComplete = onComplete
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        return anchor
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
