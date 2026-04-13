# App Review Information — KatchIt! (iOS)

Wird in App Store Connect → App → iOS App → Version 1.3 → Scroll ganz nach unten zu "App Review Information" eingetragen.

---

## Demo Account (Sign-In required = Yes)

Apple-Reviewer brauchen einen Test-Account, weil KatchIt! Login verlangt für Multiplayer, Shop und Freunde-Features.

**Username**: `apple-review@katchit.app`
**Password**: `AppleReview2026!`

> ⚠️ **MUSST DU VORHER ANLEGEN**: Erstelle diesen Account manuell in deiner Produktions-App (oder Supabase Dashboard → Authentication → Users → Add User). Account muss aktiv und bestätigt sein, damit der Reviewer sich einloggen kann.

---

## Contact Information

| Feld | Wert |
|---|---|
| First Name | Pascal |
| Last Name | Grimm |
| Phone Number | [deine Nummer mit Ländercode, z.B. +49 170 1234567] |
| Email | support@katchit.app |

---

## Notes for the Reviewer (copy-paste in das Notes-Feld)

```
Thank you for reviewing KatchIt!

ABOUT THE APP
KatchIt! is a photo scavenger-hunt party game. Players join a game via code, receive photo categories (e.g. "something red", "an animal", "street art"), go out and take photos, then rate each other's submissions 1-5 stars.

HOW TO TEST

1. LOGIN
   - Tap "Einstellungen" (gear icon, bottom-right tab)
   - Tap "Anmelden"
   - Use the demo account above (email + password)
   - Alternative: tap "Ohne Konto spielen" to explore in guest mode (some features require account)

2. SOLO MODE (fastest way to see core gameplay)
   - From Home tab, tap "Solo spielen"
   - Pick a category pack
   - AI rates your photos in real-time
   - Stars are awarded based on AI rating

3. MULTIPLAYER (requires network)
   - From Home tab, tap "Spiel erstellen" or "Beitreten"
   - Use game code to invite others — OR test solo by creating a game and submitting photos yourself, then completing the game

4. SHOP / IAP
   - Go to "Shop" tab
   - View star packages and skip-card packages
   - Tap any item to trigger StoreKit purchase sheet (sandbox)
   - "Restore Purchases" button is available in Einstellungen

5. ACCOUNT DELETION (Guideline 5.1.1(v))
   - Einstellungen → Konto → Scroll down → "Konto löschen"
   - Confirm in the dialog
   - Account, profile, and all user-uploaded photos are deleted server-side via a Supabase Edge Function

PERMISSIONS
- Camera: required for taking photos during gameplay
- Photo Library: optional, for selecting existing photos
- Location: optional, to tag where photos were taken (shown on post-game map)
- Notifications: optional, for game invites from friends
- Tracking (ATT): optional, for personalized ads. App works fully without it.

THIRD-PARTY SERVICES
- Supabase (auth, database, photo storage, realtime multiplayer)
- Firebase Cloud Messaging (push notifications)
- Google Mobile Ads / AdMob (with UMP consent dialog for EU users)

CONTENT MODERATION
User-generated content (photos + category ideas) is moderated through:
- Auto-flagging for reported content via in-app report button
- Manual review by us for any reports received at support@katchit.app
- Block user functionality in friends list

SIGN IN WITH APPLE NOTE
KatchIt! offers Email signup as the primary authentication method, with Google and Apple as optional alternatives via OAuth. This complies with Guideline 4.8 because email is available as a non-third-party option.

Thanks for your time!
Pascal Grimm
```

---

## Attachment / Screenshots / Video

Optional, aber hilfreich. Apple akzeptiert ein kurzes Screencast-Video das zeigt wie man sich einloggt und ein Solo-Spiel startet. Bei einem normalen Foto-Party-Spiel nicht zwingend erforderlich.

---

## Version Release Option

Unter "Version Release" wählen:
- [ ] **Automatically release this version** — App geht LIVE sobald Approved (riskant beim ersten Release — wenn was schief geht, ist es sofort im Store)
- [x] **Manually release this version** — **EMPFOHLEN**: Du kriegst eine Approval-Mail und klickst dann selbst auf "Release" wann du willst

---

## Export Compliance

Beim Submit fragt Apple nach Export Compliance.

**Antworten**:
1. *"Does your app use encryption?"* → **Yes** (wegen HTTPS-Traffic, das zählt technisch als Encryption)
2. *"Does your app qualify for any of the exemptions provided in Category 5, Part 2 of the U.S. Export Administration Regulations?"* → **Yes**
3. *"Does your app implement any proprietary encryption algorithms instead of, or in addition to, using or accessing the standard encryption algorithms available on iOS?"* → **No**
4. *"Is your app going to be available on the French App Store?"* → **Yes** (falls geplant) oder **No**

Ergebnis: Du qualifizierst dich für die standard HTTPS-Exemption. Du brauchst KEIN separates Compliance-Dokument.

Alternative, einfacher: Trage in `Info.plist` einmalig den Key `ITSAppUsesNonExemptEncryption` = `NO` ein, dann fragt Apple beim Submit NICHT mehr nach. Aber: nur wenn du wirklich NUR Standard-HTTPS nutzt, keine Custom-Crypto. Bei KatchIt! ist das der Fall.

---

## Age Rating

Wird via Fragebogen in ASC unter "App Information → Age Rating" beantwortet. Die Antworten sind für KatchIt!:

| Frage | Antwort |
|---|---|
| Cartoon or Fantasy Violence | None |
| Realistic Violence | None |
| Prolonged Graphic or Sadistic Realistic Violence | None |
| Profanity or Crude Humor | None |
| Mature/Suggestive Themes | None |
| Horror/Fear Themes | None |
| Medical/Treatment Information | None |
| Alcohol, Tobacco, or Drug Use or References | None |
| Simulated Gambling | None |
| Sexual Content or Nudity | None |
| Graphic Sexual Content and Nudity | None |
| Contests | None |
| Unrestricted Web Access | **No** |
| Gambling | **No** |
| User Generated Content | **Yes** (Photos + Chat) — erfordert Content Moderation Erklärung |

Ergebnis: Rating **4+** (bei "User Generated Content = Yes" eventuell 9+ oder 12+, je nach wie Apple es einstuft).

⚠️ Wenn Apple wegen User-Generated Content höher einstuft: Reagiere in den Review Notes darauf mit Verweis auf die Moderation (siehe Notes oben).

---

## Content Rights

Unter **App Information → Content Rights Information**:

*"Does your app contain, show, or access third-party content?"*

**Antwort: No**

Begründung: KatchIt! zeigt ausschließlich von Usern hochgeladene Fotos. Keine eingebetteten YouTube-Videos, keine Musik-Libraries, keine Drittanbieter-Texte.
