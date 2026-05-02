# App Review Reply — Submission ae4b6aae-be22-4168-bc56-8f9bbb3938f5 (Build 15 → 16)

Use this as the reply to App Review in App Store Connect for **Build 16**.

Attach (1) a screen recording of the upfront AI consent dialog at mode select
showing the new "Continue with AI" button + clickable "View Privacy Policy"
link, and (2) a screen recording of an actual sandbox StoreKit purchase
completing on iPad Air (iPadOS 26.4+).

---

## Sign-in Credentials (paste into App Store Connect → App Information → "Sign-in required" → demo account)

**Username:** `apple-review@katchit.app`
**Password:** `KatchIt-Apple-2026!`

**Notes for the sign-in credentials field:**

> Demo account for App Review (created 2 May 2026, replaces the previously
> issued credentials). The account uses email + password sign-in; no SMS or
> magic-link confirmation is required — the email is pre-confirmed
> server-side. The account starts with no friends, no game history, and no
> purchased entitlements, so the reviewer can exercise the full sign-up /
> first-use flow as if it were a fresh user.
>
> Note that **sign-in is NOT required to test the IAP flow or the AI
> consent flow**: both Solo mode and the in-app shop are fully accessible
> without an account. The demo account is only needed to test
> friends, multiplayer game creation, the multiplayer "AI Judge" mode
> (which requires a host account), and the in-app account-deletion flow
> under guideline 5.1.1(v).
>
> If the credentials stop working for any reason during the review,
> please contact support@katchit.app — I am the sole developer and will
> respond same-day.

---

## EN — English Reply

Hello App Review Team,

Thank you for the detailed feedback on the previous submission. Build 16 directly
addresses both 5.1.1(i)/5.1.2(i) and 2.1(b) findings. Both fixes are real
behavioural changes — I have attached screen recordings as evidence and listed
the exact root causes below.

### 1. Guidelines 5.1.1(i) and 5.1.2(i) — Third-party AI service disclosure

The app uses third-party AI processors for two distinct purposes. **All
providers below act solely as data processors under their commercial /
enterprise agreements and do not use submitted photos to train their AI
models.** Full breakdown:

| Service | Used for | Data sent | Consent model |
|---|---|---|---|
| **Google Gemini API** (Google LLC, USA) — model `gemini-2.5-flash` | **Primary** AI rater for BOTH Solo mode and the Multiplayer "AI Judge" mode | Photo bytes + category name. No PII. Discarded immediately. | Explicit per-round in-app consent ("Continue with AI" / "Decline"). Decline blocks the round. |
| **Cloudflare Workers AI** (Cloudflare Inc., USA) — model `@cf/meta/llama-4-scout-17b-16e-instruct` | (a) Automatic **fallback** AI rater for both modes when Gemini is unavailable; (b) image safety moderation before storing every multiplayer game photo and avatar (App Store guideline 1.2 compliance) | Photo bytes (+ category name for use (a)). No PII. Discarded immediately. | Use (a) is covered by the same per-round AI consent. Use (b) is a technical safety check (Art. 6(1)(f) GDPR — legitimate interest in user safety) and is documented in the privacy policy rather than gated behind individual consent, in line with how Apple's own NeuralHash and similar safety-screening systems are treated. |

**What the user sees in build 16 (attached recording):**

1. The user opens the app and selects either the Solo mode or the
   Multiplayer "AI Judge" mode.
2. **Before any photo is captured and before any data leaves the device**,
   a modal AlertDialog appears with the title "AI Photo Rating".
3. The dialog body contains the full disclosure (English text below for the
   reviewer's reference).
4. Below the disclosure, a clickable **"View Privacy Policy"** button opens
   the locale-appropriate page (https://katchit.app/privacy.html for English
   locale, https://katchit.app/datenschutz.html for German locale) in the
   system browser.
5. The user must tap **"Continue with AI"** for the round to start. Tapping
   **"Decline"** returns them to mode select where they can pick a non-AI
   mode (Classic / Blind Bingo / Weird Core / Quick Start).
6. The consent flag is account-bound (cleared on sign-out and on user-switch),
   so a different user signing in on the same device must give their own
   consent before any AI mode can start.

**Exact dialog text shown to English-locale users (build 16):**

> **AI Photo Rating**
>
> This mode uses AI for automatic photo rating. Your photos will be sent to
> one of our AI processors:
>
> • Solo mode: Cloudflare Workers AI (Cloudflare Inc., USA)
> • Multiplayer "AI Judge" mode: Google Gemini API (Google LLC, USA)
>
> Both providers act solely as data processors under their enterprise
> agreements and do not use your photos to train their AI models. Only the
> image and the category name are transmitted — no personal data such as
> your name, email address, or user ID. Photos are not stored and are
> discarded immediately after rating.
>
> Legal basis: Art. 6(1)(a) GDPR (consent). Data is transferred to the USA
> under the EU-U.S. Data Privacy Framework and Standard Contractual Clauses.
>
> If you decline, the round will not start — you can choose a different
> (non-AI) game mode instead.
>
> [View Privacy Policy ↗]
>
> [Decline]   [Continue with AI]

**Privacy policy:** Available in **English** at
https://katchit.app/privacy.html (newly published with build 16) and in
**German** at https://katchit.app/datenschutz.html. Both versions list both
AI providers (Sections 5(c) and 5(d)) with: provider identity and address,
purpose, data categories, server location, retention (none), legal basis
(Art. 6(1)(a) GDPR — consent), processor status (data processor only, no
training), and international transfer safeguards (EU-U.S. DPF + SCCs). The
English version is reachable from the in-app consent dialog with a single
tap on "View Privacy Policy".

### 2. Guideline 2.1(b) — In-App Purchases not completing

I identified the root cause of the "unable to make a purchase" symptom and
fixed it in build 16.

**Root cause:** On iPadOS 26.x in iPhone-compat mode (the reviewer's
environment: iPad Air 11-inch with iPadOS 26.4.2), `UIApplication.connectedScenes`
can include `UIWindowScene` instances whose `activationState` is
`.background` or `.unattached` — for example after Stage Manager / Split View
focus changes. Our previous StoreKit bridge (BillingBridge.swift) tried to
resolve a presentation scene with the chain
`foregroundActive → foregroundInactive → scenes.first`, and the trailing
`scenes.first` could quietly hand StoreKit a backgrounded scene. On iOS 17+
`Product.purchase(confirmIn:)` then **silently no-ops** — the purchase sheet
never presents, the awaited `try await product.purchase()` call effectively
hangs, and the UI shows only the loading spinner with no error feedback.
This is the exact symptom the reviewer reported.

**Fixes shipped in build 16:**

- BillingBridge.swift `activeWindowScene()` now returns `nil` (instead of
  `scenes.first`) when no `foregroundActive` or `foregroundInactive` scene
  exists. Each fallback step also `NSLog`s for diagnostics, matching the
  pattern of our auth bridges that were hardened in build 14.
- When `activeWindowScene()` returns `nil` on iOS 17+, the bridge now
  immediately surfaces a localised user-facing error instead of attempting
  a scene-less `Product.purchase()` (which on iPad reproduced the silent
  no-op): "Couldn't open the App Store dialog. Please bring KatchIt to the
  foreground and try again."
- All previously German-only error strings in the StoreKit bridge
  ("Produkt nicht gefunden" / "Verifizierung fehlgeschlagen" / "Kauf
  ausstehend" / "Unbekannter Fehler") are now locale-aware (EN/DE), so an
  English-locale reviewer no longer sees German prose on a sandbox edge case.
- ShopScreen.kt — every IAP failure path (Buy Stars, Buy Skip Cards, Remove
  Ads, Restore Purchases) now surfaces the underlying error to the user via
  a Material3 Snackbar with the actual message. The previous behaviour was
  to silently set `purchaseLoading = null` (i.e., the spinner just
  disappeared with no feedback). User-cancellation is correctly distinguished
  from a real failure via a `USER_CANCELLED` sentinel and is suppressed from
  the snackbar (since the user explicitly chose to cancel).
- Restore Purchases now also confirms success with a snackbar
  ("Purchases restored successfully" / "No previous purchases found"),
  whereas previously it was silent on both success and failure.

**Tested on physical iPad Pro 11" (iPadOS 26.3.1, iPhone-compat mode) in
the StoreKit Sandbox** — every product (consumable star packs, consumable
skip-card packs, non-consumable "Remove Ads") successfully presents the
StoreKit sheet on the first tap, completes the purchase with a sandbox
account, and credits the entitlement immediately. Restore Purchases
correctly recovers the non-consumable "Remove Ads" entitlement.

**Paid Apps Agreement:** Confirmed in effect in App Store Connect → Business.

### Demo account for review

A fresh demo account has been provisioned for this review:

- **Username:** apple-review@katchit.app
- **Password:** KatchIt-Apple-2026!

The previously issued demo credentials were rotated and the corresponding
account purged from our backend on 2 May 2026. The new account is
pre-confirmed (no email verification needed) and starts empty so the
reviewer can exercise the full first-use flow including the new AI
awareness onboarding slide and the per-round AI consent dialog. Sign-in
is **not required** to test either the IAP flow or the Solo-mode AI
consent — both are reachable from the home screen of a guest session.

### Other context

KatchIt! is configured iPhone-only (`TARGETED_DEVICE_FAMILY = 1`,
`UISupportedInterfaceOrientations~iPhone` only declared). The App Store
listing is iPhone-only and the app is not designed or QA'd for iPad form
factors. We understand iPad reviewers can install iPhone-only apps in
iPhone-compat mode for review purposes, and both fixes above (the auth
fix in build 14 and the IAP fix in this build 16) are specifically aimed
at the multi-scene environment that iPhone-compat creates on iPad. Both
core flows now work in that mode.

If anything is unclear, please let us know — happy to provide additional
screen recordings (Google OAuth, email/password, both AI modes triggering
the consent dialog, each individual IAP product completing in sandbox) or
any other clarification.

Best regards,
Pascal Grimm
support@katchit.app

---

## DE — Deutsch (zur Sicherheit, falls deutsches Review-Team)

Hallo App Review Team,

vielen Dank für das detaillierte Feedback zur vorherigen Einreichung. Build 16
adressiert beide Punkte (5.1.1(i)/5.1.2(i) und 2.1(b)) mit echten
Verhaltensänderungen — Bildschirmaufzeichnungen und genaue Ursachen finden Sie
unten.

### 1. Richtlinien 5.1.1(i) und 5.1.2(i) — KI-Drittanbieter

Die App nutzt KI-Drittanbieter für zwei verschiedene Zwecke. **Alle Anbieter
handeln ausschließlich als Auftragsverarbeiter auf Basis ihrer
Enterprise-Verträge und verwenden übermittelte Fotos NICHT zum Training
ihrer KI-Modelle.** Im Detail:

| Dienst | Verwendung | Daten | Einwilligung |
|---|---|---|---|
| **Google Gemini API** (Google LLC, USA) — Modell `gemini-2.5-flash` | **Primärer** KI-Bewerter für SOWOHL Solo-Modus ALS AUCH Multiplayer-„KI-Bewerter" | Foto + Kategoriename, keine PII, sofort verworfen | Explizite Per-Runden-Einwilligung („Mit KI fortfahren" / „Ablehnen"). Bei Ablehnung startet die Runde nicht. |
| **Cloudflare Workers AI** (Cloudflare Inc., USA) — Modell `@cf/meta/llama-4-scout-17b-16e-instruct` | (a) Automatischer **Fallback**-Bewerter wenn Gemini nicht erreichbar; (b) Bild-Sicherheits-Moderation vor Speicherung jedes Multiplayer-Fotos und Avatars (Compliance mit App-Store-Richtlinie 1.2) | Nur Bild-Bytes (+ Kategoriename für (a)), sofort verworfen | (a) durch dieselbe Per-Runden-KI-Einwilligung abgedeckt. (b) ist eine technische Sicherheitsprüfung (Art. 6 Abs. 1 lit. f DSGVO — berechtigtes Interesse an Nutzersicherheit), in der Datenschutzerklärung dokumentiert statt durch individuelle Einwilligung gegated. |

Der Consent-Dialog erscheint **vor jeglicher Datenübertragung**, vor dem
ersten Foto, beim Klick auf einen KI-Modus. Der Button heißt jetzt explizit
**„Mit KI fortfahren"** (statt zuvor „Einverstanden") und ein klickbarer
Link **„Datenschutzerklärung öffnen"** führt direkt zur passenden
Sprachversion (DE: /datenschutz.html, EN: /privacy.html — neu mit Build 16
veröffentlicht).

### 2. Richtlinie 2.1(b) — In-App-Käufe

**Ursache identifiziert und in Build 16 gefixt:** Auf iPad in
iPhone-Compat-Mode unter iPadOS 26.x kann `UIApplication.connectedScenes`
eine UIWindowScene mit `.background`/`.unattached` ActivationState
zurückgeben. Unser bisheriger StoreKit-Bridge fiel in seiner Anchor-Resolution
auf `scenes.first` zurück und übergab StoreKit damit eine ungültige Scene.
`Product.purchase(confirmIn:)` macht in dem Fall unter iOS 17+ ein
**stilles No-Op** — kein Sheet, kein Callback, kein Fehler. Genau das, was
der Reviewer beobachtet hat.

**Fixes:**
- `activeWindowScene()` gibt jetzt `nil` zurück, wenn keine
  foreground-Scene verfügbar ist; iOS 17+ Path bricht dann mit einer
  lokalisierten Fehlermeldung ab statt zu hängen.
- Alle StoreKit-Fehler werden in ShopScreen jetzt als Material3-Snackbar
  angezeigt (vorher: stille Spinner-Auflösung).
- User-Cancel wird sauber von echten Fehlern unterschieden (Sentinel-Token).
- Restore Purchases zeigt Erfolg + Fehler.
- Alle Bridge-Fehlertexte sind jetzt locale-aware (EN/DE).

Getestet auf physischem iPad Pro 11" (iPadOS 26.3.1, iPhone-Compat-Mode)
im StoreKit-Sandbox — alle Produkte (Star-Packs, Skip-Cards, Remove Ads)
präsentieren beim ersten Tap das Sheet, schließen den Kauf erfolgreich ab
und schalten die Berechtigung sofort frei. Restore Purchases stellt die
Remove-Ads-Berechtigung korrekt wieder her.

**Paid Apps Agreement:** Bestätigt aktiv in App Store Connect → Business.

### Demo-Account für die Review

Frischer Demo-Account für diese Einreichung:

- **Username:** apple-review@katchit.app
- **Passwort:** KatchIt-Apple-2026!

Die alten Demo-Zugangsdaten wurden am 2. Mai 2026 rotiert und der zugehörige
Account aus unserem Backend gelöscht. Der neue Account ist
serverseitig vorbestätigt (keine E-Mail-Verifizierung nötig) und startet
leer, sodass der gesamte Erstnutzer-Flow inklusive der neuen
KI-Awareness-Onboarding-Folie und des Per-Runden-KI-Consent-Dialogs getestet
werden kann. Login ist **nicht erforderlich**, um den IAP-Flow oder den
Solo-Modus-KI-Consent zu testen — beides ist im Gast-Modus erreichbar.

Bei Rückfragen gerne melden — wir können auf Wunsch weitere Aufzeichnungen
(jeder einzelne IAP, jeder KI-Modus mit Consent-Dialog, OAuth-Pfade) liefern.

Beste Grüße,
Pascal Grimm
support@katchit.app
