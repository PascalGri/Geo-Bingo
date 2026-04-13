# iOS v1.3 Release — Manuelle Schritte Checkliste

Dieses Dokument ist dein Fahrplan. Arbeite es von oben nach unten ab. Alles was **Claude nicht machen konnte** ist hier drin — mit genauen Klick-Pfaden.

Stand: 2026-04-11

---

## Was Claude bereits vorbereitet hat (du musst nichts tun)

- [x] `Config.xcconfig`: `MARKETING_VERSION=1.3`, `CURRENT_PROJECT_VERSION=5`
- [x] `iosApp.entitlements`: `aps-environment` = **production** (war vorher `development`)
- [x] `Info.plist`: `ITSAppUsesNonExemptEncryption = NO` (spart Export-Compliance-Fragen)
- [x] Pods installiert (`pod install`)
- [x] Xcode Workspace geöffnet
- [x] `store-listing/release-notes-v1.3.txt` (Deutsch)
- [x] `store-listing/release-notes-v1.3-en.txt` (Englisch)
- [x] `store-listing/store_texte.md` auf v1.3 aktualisiert
- [x] `store-listing/ios-app-privacy.md` — App-Privacy-Fragebogen-Antworten
- [x] `store-listing/ios-review-notes.md` — Review Notes, Demo-Account-Infos, Age Rating, Content Rights

---

## Was du selbst machen musst

### Block A: Supabase Sign-in-with-Apple Konfigurieren

**Warum**: Der Apple-Login-Button in der App nutzt Supabase OAuth. Ohne Config in Supabase funktioniert er nicht → App Review drückt den Button, nichts passiert → Rejection unter Guideline 2.1.

#### A.1: Services ID in Apple Developer Portal erzeugen

1. Öffne https://developer.apple.com/account/resources/identifiers/list
2. Filter oben: **Services IDs** (nicht App IDs!)
3. Klick **+** → **Services IDs → Continue**
4. Description: `KatchIt Sign In Web`
5. Identifier: `pg.geobingo.one.signin` (muss anders sein als Bundle ID)
6. **Continue → Register**
7. Nach dem Register: Klick auf den neuen Service → **Sign in with Apple** ✅ aktivieren → **Configure**
8. **Primary App ID**: `pg.geobingo.one` aus Dropdown wählen
9. **Domains and Subdomains**: Domain deines Supabase-Projekts (ohne https), z.B. `xxxxxxxxxxxx.supabase.co`
10. **Return URLs**: `https://<dein-supabase-projekt>.supabase.co/auth/v1/callback`
    (ersetze `<dein-supabase-projekt>` mit deiner echten Supabase Projekt-ID)
11. **Next → Done → Save**

#### A.2: Sign-in-with-Apple Private Key erzeugen

1. https://developer.apple.com/account/resources/authkeys/list
2. Klick **+** (neues Key)
3. Key Name: `KatchIt Apple Sign In Key`
4. Aktiviere **Sign In with Apple** ✅ → **Configure**
5. Primary App ID: `pg.geobingo.one` → **Save**
6. **Continue → Register**
7. **Download** die `.p8`-Datei — **das geht NUR EINMAL**. Speichere sie sofort sicher ab (z.B. `~/.config/katchit/SIWA_AuthKey_XXXXXX.p8`)
8. Notiere dir **Key ID** (10 Zeichen, auf der Key-Detail-Seite)
9. Notiere dir deine **Team ID** (https://developer.apple.com/account → Membership → Team ID, 10 Zeichen)

#### A.3: Supabase konfigurieren

1. https://supabase.com/dashboard → dein Projekt → **Authentication → Providers**
2. Scroll zu **Apple** → aktivieren
3. **Client IDs** (kommagetrennt, BEIDE angeben):
   ```
   pg.geobingo.one,pg.geobingo.one.signin
   ```
   (Bundle ID für native Flows + Services ID für Web-Flow)
4. **Secret Key (for OAuth)**: Hier musst du einen **JWT generieren** aus deinen .p8, Key ID, Team ID und Services ID
   - Einfachster Weg: Supabase CLI oder Online-Tool
   - **Online-Tool (empfohlen weil schnell)**: https://github.com/supabase/auth-helpers/tree/main/packages/shared/apple-client-secret (siehe README)
   - **Alternative (via Node)**: `npx @supabase/gen-apple-secret` (falls existiert) oder manuell via dem Snippet in der Supabase-Docs: https://supabase.com/docs/guides/auth/social-login/auth-apple
   - Das generierte Secret ist ein JWT-String (`eyJ...`) mit 6 Monaten Laufzeit. Muss vor Ablauf erneuert werden.
5. **Save**

#### A.4: Smoke-Test

1. App neu starten auf dem Simulator oder Device
2. Einstellungen → Anmelden → "Mit Apple fortfahren" tappen
3. Der Apple-OAuth-Browser sollte erscheinen, du loggst dich mit Apple ID ein, wirst zurück in die App geleitet, bist eingeloggt
4. Wenn das nicht funktioniert → Apple-Sign-in ist broken, **nicht submiten**, erst fixen oder Apple-Button temporär aus dem UI entfernen

---

### Block B: Demo-Account für Apple-Reviewer anlegen

1. Öffne die App (oder Supabase Dashboard → Authentication → Users → Add User)
2. Account erstellen:
   - Email: `apple-review@katchit.app`
   - Password: `AppleReview2026!`
3. Falls via Supabase Dashboard: Häkchen bei "Auto Confirm User"
4. Optional: Log dich einmal mit dem Account in der App ein und spiel ein Solo-Spiel, damit der Account schon Daten hat und der Reviewer nicht eine leere App sieht

---

### Block C: Xcode — Archive bauen und hochladen

1. Xcode ist geöffnet. Wenn nicht: `open GeoBingo/iosApp/iosApp.xcworkspace`
2. Target auf **iosApp** (nicht "iosAppTests")
3. Destination oben: **Any iOS Device (arm64)** (nicht Simulator)
4. **Product → Clean Build Folder** (Cmd+Shift+K) — sicher ist sicher
5. **Product → Archive** (dauert 5-15 Min beim ersten Mal)
   - Falls Build-Fehler: Screenshot nehmen und zurück zu mir
   - Häufigster Fehler: "No Profile Found" → Signing & Capabilities → "Automatically manage signing" aus und wieder an
6. Wenn Archive erfolgreich: Organizer öffnet sich automatisch
7. Neuestes Archive auswählen → **Distribute App**
8. Method: **App Store Connect** → Next
9. Destination: **Upload** → Next
10. App Store distribution options: alle Defaults lassen → Next
11. Distribution certificate + profile: **Automatically manage signing** → Next
12. Review-Screen zeigt die Summary → **Upload**
13. Warten (3-5 Min). Bei Erfolg: "Upload Successful" Dialog
14. Check: https://appstoreconnect.apple.com → KatchIt → TestFlight Tab → Build 1.3 (5) sollte mit Status **"Processing"** erscheinen
15. Warten bis Processing fertig (15-60 Min). Status wird dann zu "Ready to Submit" oder "Missing Compliance" (→ Encryption Frage beantworten, siehe unten)

---

### Block D: App Store Connect — App-Seite ausfüllen

Sobald der Build hochgeladen ist und Processing durchgelaufen ist, gehst du zu App Store Connect → KatchIt → App Store (nicht TestFlight!) → **+ Version or Platform → iOS** → **1.3**.

#### D.1: App Information (einmalig, nicht versionsgebunden)

Links im Menü **App Information**:

- **Name**: `KatchIt!` (oder `KatchIt! - Das Foto-Spiel`)
- **Subtitle**: `Foto-Spiel mit Freunden` (max 30 Zeichen)
- **Privacy Policy URL**: `https://katchit.app/datenschutz.html`
- **Category**:
  - Primary: **Games**
  - Subcategory: **Casual**
  - Secondary (optional): **Entertainment**
- **Content Rights**: "Does your app contain, show, or access third-party content?" → **No** (siehe `ios-review-notes.md`)
- **Age Rating**: Klick **Edit**, fülle den Fragebogen gemäß Tabelle in `ios-review-notes.md`. User Generated Content = **Yes** → Moderation in Notes erklären.

#### D.2: Pricing and Availability

- **Price**: Free
- **Availability**: Alle Länder (oder wähle gezielt aus — Deutschland/Österreich/Schweiz/EU mindestens)

#### D.3: App Privacy

Links **App Privacy** → **Get Started** bzw. **Edit**.

Arbeite dich durch den Fragebogen mit `store-listing/ios-app-privacy.md` als Vorlage. Jede der 10 Datenkategorien ankreuzen, je nach Anleitung dort.

Danach **Publish**.

#### D.4: Version 1.3 — Store-Präsenz

Links **1.3 Prepare for Submission**.

##### Version-Info:
- **Promotional Text** (170 Zeichen, kann später OHNE Review geändert werden):
  ```
  Neu: Rocket-League-Spielerbanner, animierte Hintergrunde und Cloud-Sync fur deine Cosmetics. Ruste dein Profil aus und zeige es in jeder Lobby!
  ```
- **Description**: Vollständiger Text aus `store-listing/store_texte.md` (ab "KatchIt! ist das ultimative...")
- **Keywords** (max 100 Zeichen, kommagetrennt, keine Leerzeichen nach Komma):
  ```
  foto,partyspiel,multiplayer,schnitzeljagd,gruppenspiel,outdoor,kameraspiel,fotobattle,freunde
  ```
- **Support URL**: `https://katchit.app`
- **Marketing URL** (optional): `https://katchit.app`

##### "What's New in This Version":
Inhalt von `store-listing/release-notes-v1.3.txt` einfügen.

##### App Previews and Screenshots:
Lade deine iOS-Screenshots hoch. Pflicht sind:
- **iPhone 6.9" Display** (iPhone 16 Pro Max: 1290×2796) — min. 3, max. 10
  - ODER **iPhone 6.7" Display** (1284×2778) — alternativ
- **iPad 13" Display** (2064×2752) — nur erforderlich wenn du iPad unterstützt

Falls deine Screenshots im Play-Store-Format sind: die iOS-Mindestgröße ist anders! Notfalls mit dem iOS-Simulator neue Screenshots machen:
```bash
# Simulator starten, dann in Xcode → Simulator → File → New Screen Shot (Cmd+S)
```

##### App Icon
Wird automatisch aus deinem Asset Catalog (`AppIcon.appiconset/app-icon-1024.png`) gezogen. Nichts zu tun.

##### Build
- Klick **Build** Sektion → **+ Add Build**
- Dein hochgeladener Build **1.3 (5)** sollte auswählbar sein
- Select → Done

##### App Review Information (ganz unten auf der Version-Seite):
- Contact Information, Demo Account, Notes aus `store-listing/ios-review-notes.md` 1:1 einfügen

##### Version Release
- Wähle **"Manually release this version"** (sicherer)

##### Export Compliance
- Sollte durch `ITSAppUsesNonExemptEncryption = NO` in Info.plist auto-gelöst sein
- Falls Apple trotzdem fragt: Antworten aus `ios-review-notes.md` Abschnitt "Export Compliance"

---

### Block E: Submit for Review

Wenn ALLES auf der Version-1.3-Seite grün/ausgefüllt ist:

1. **Save** oben rechts (falls nicht automatisch gespeichert)
2. **Add for Review** → **Submit to App Review**
3. Apple stellt noch 3-5 Fragen (IDFA, Encryption) → wähle die für KatchIt! zutreffenden Optionen:
   - **Does this app use the Advertising Identifier (IDFA)?** → **Yes**
     - Reasons: ✅ Serve advertisements within the app
     - ✅ Confirm iOS App Tracking Transparency string is displayed (ATT-Prompt existiert in `iOSApp.swift`)
4. **Submit**
5. Status ändert sich zu **"Waiting for Review"**, später **"In Review"**, dann **"Pending Developer Release"** oder **"Rejected"**

---

## Zeitplan ab jetzt

| Zeit | Schritt |
|---|---|
| 15 Min | Block A (Services ID + Key + Supabase Config) |
| 5 Min | Block B (Demo-Account anlegen) |
| 20 Min | Block C (Archive + Upload) + Warten auf Processing |
| 30 Min | Block D (App Store Connect Seite ausfüllen, Screenshots hochladen) |
| 2 Min | Block E (Submit) |
| 1-3 Tage | **Apple Review Wartezeit** |
| Approval | Manuell "Release" klicken in App Store Connect |

---

## Wenn Apple Rejection schickt

Rejections kommen in ASC + per E-Mail. Die häufigsten Gründe bei KatchIt!:

| Rejection-Grund | Fix |
|---|---|
| **Guideline 2.1 — App Completeness** (Login geht nicht) | Supabase Apple Sign In Config kaputt → Block A erneut prüfen |
| **Guideline 4.8 — Sign in with Apple** | Native SIWA implementieren (separates Thema, siehe Notiz unten) |
| **Guideline 5.1.1 — Data Collection and Storage** | App Privacy Fragebogen falsch ausgefüllt → `ios-app-privacy.md` nochmal checken |
| **Guideline 5.1.1(v) — Account Deletion** | Delete-Account-Button funktioniert nicht → testen |
| **Performance 2.1 — crashes on launch** | Archive neu bauen, Crash-Log aus ASC runterladen |
| **Metadata rejection** (Screenshots/Beschreibung) | Kostenlos fixbar, kein neuer Build nötig |

### Guideline-4.8-Fallback (falls Apple SIWA-Nachrüstung verlangt)

Falls Apple Rejection schickt mit "you must offer Sign in with Apple": Die saubere Lösung ist native SIWA via `AuthenticationServices` framework. Das ist ein Kotlin-iOS-Bridge-Projekt (analog zu `BillingBridge.swift`), ca. 150 Zeilen Code. Sag mir in dem Fall Bescheid und ich implementiere das in v1.4.

Schneller Workaround: Apple-Button aus der App entfernen (nur für iOS-Build), Email + Google behalten. Weil Email primär ist, greift 4.8 dann gar nicht mehr.

---

## Wenn du fertig bist

Melde dich bei mir sobald:
- [ ] Der Build "Ready to Submit" ist in TestFlight-Tab
- [ ] Du die App Store Seite ausgefüllt hast
- [ ] Du submitted hast

Dann prüfe ich mit dir zusammen dass nichts fehlt bevor Apple den Build in Review nimmt.

Und gib mir Bescheid, sobald die Review durch ist — dann setzen wir Fastlane auf für Block C, damit du beim nächsten Release alles mit einem Befehl erledigen kannst.
