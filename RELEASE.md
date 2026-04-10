# KatchIt Release Runbook

Wie du **Android + iOS** baust und released — erst manuell zum Testen, dann via **Fastlane** automatisiert mit einem einzigen Befehl.

Stand: April 2026 (v1.3)

---

## TL;DR (wenn du es eilig hast)

```bash
# Einmalige Setups (siehe unten):
# 1. Google Play Service Account JSON erzeugen
# 2. App in App Store Connect anlegen + Cert + Provisioning Profile
# 3. App Store Connect API Key erzeugen
# 4. Fastlane installieren

# Dann für jeden Release:
cd /Users/pascalgrimm/Desktop/Geo-Bingo/GeoBingo
fastlane beta               # beides parallel: Play Closed Test + TestFlight
# oder
fastlane android_beta       # nur Android
fastlane ios_beta           # nur iOS
```

---

## Teil 1: Der erste iOS-Release (manuell via Xcode)

**Warum manuell?** Dein Apple Developer Account ist frisch. Beim allerersten Release müssen Dinge wie Zertifikate, Provisioning Profiles, App-Eintrag im App Store Connect und Bundle ID durchlaufen werden. Das ist viel neues auf einmal. Mach den ersten Upload **manuell via Xcode Organizer** — dann siehst du sofort ob alles stimmt, und **erst wenn TestFlight läuft**, automatisierst du mit Fastlane.

### Schritt 1: Bundle ID registrieren
1. https://developer.apple.com/account → **Certificates, Identifiers & Profiles**
2. **Identifiers → + → App IDs → App**
3. Description: `KatchIt`
4. Bundle ID: **Explicit** → `pg.geobingo.one` (muss exakt zu `Info.plist` passen)
5. **Capabilities aktivieren** (gleiche die du im Xcode-Projekt brauchst):
   - Push Notifications (für Game-Invite-Notifications)
   - Sign in with Apple (OAuth)
   - Associated Domains (für deep links falls genutzt)
   - In-App Purchase (für Stars / No-Ads / Skip Cards)
6. **Continue → Register**

### Schritt 2: App im App Store Connect anlegen
1. https://appstoreconnect.apple.com → **My Apps → +**
2. **Platform**: iOS
3. **Name**: KatchIt (oder wie du es benennen willst)
4. **Primary Language**: Deutsch (oder English, je nach deiner Haupt-Zielgruppe)
5. **Bundle ID**: `pg.geobingo.one` (aus Dropdown — erscheint nach Schritt 1)
6. **SKU**: `katchit-ios` (intern, egal welcher String)
7. **User Access**: Full Access
8. **Create**

### Schritt 3: In-App Purchases anlegen
Du brauchst die gleichen Products wie auf Android (siehe [store-listing/](store-listing/) und [ShopScreen.kt](GeoBingo/composeApp/src/commonMain/kotlin/pg/geobingo/one/ui/screens/ShopScreen.kt)):

| Product ID | Typ | Preis-Tier |
|---|---|---|
| `pg.geobingo.one.stars_50` | Consumable | 0.99 € |
| `pg.geobingo.one.stars_150` | Consumable | 1.99 € |
| `pg.geobingo.one.stars_400` | Consumable | 3.99 € |
| `pg.geobingo.one.stars_1000` | Consumable | 7.99 € |
| `pg.geobingo.one.skip_3` | Consumable | 0.99 € |
| `pg.geobingo.one.skip_10` | Consumable | 2.99 € |
| `pg.geobingo.one.no_ads` | Non-Consumable | 2.99 € |

Für jedes: **Features → In-App Purchases → + → Reference Name, Product ID, Price, Display Name, Description** → Save.

⚠️ **Wichtig**: Product IDs müssen *exakt* mit denen in [ShopScreen.kt:43-51](GeoBingo/composeApp/src/commonMain/kotlin/pg/geobingo/one/ui/screens/ShopScreen.kt#L43-L51) übereinstimmen.

### Schritt 4: Code-Signing in Xcode
1. Öffne Xcode: `open /Users/pascalgrimm/Desktop/Geo-Bingo/GeoBingo/iosApp/iosApp.xcworkspace`
2. Klick auf **iosApp** Projekt (links) → **Targets → iosApp → Signing & Capabilities**
3. **Team**: Dein Apple Developer Team auswählen
4. **Bundle Identifier**: muss `pg.geobingo.one` sein
5. **Automatically manage signing** ankreuzen — Xcode erzeugt automatisch Development + Distribution Certs + Provisioning Profile
6. Wenn Fehler kommen (z.B. "no matching provisioning profile"): Apple Developer Account in Xcode Einstellungen hinterlegen via **Xcode → Settings → Accounts → +**

### Schritt 5: Version + Build Number setzen
1. Im Xcode-Projekt: **iosApp target → General**
2. **Version**: `1.3` (matcht Android `versionName`)
3. **Build**: `5` (matcht Android `versionCode`) — muss bei jedem Upload inkrementiert werden
4. Stelle sicher dass die Capabilities gesetzt sind: **Signing & Capabilities → + Capability**
   - Push Notifications
   - In-App Purchase
   - Sign in with Apple

### Schritt 6: Archive bauen
1. In Xcode oben den Build-Target auf **Any iOS Device (arm64)** stellen (nicht Simulator!)
2. **Product → Archive** (klassischer Weg) — dauert ein paar Minuten
3. Wenn fertig öffnet sich der **Organizer** mit deinem Archive

### Schritt 7: Hochladen via Xcode Organizer
1. Im Organizer: Archive auswählen → **Distribute App**
2. **Method**: App Store Connect
3. **Destination**: Upload
4. **App Store Distribution options**: Häkchen bei "Upload your app's symbols..." und "Manage Version and Build Number" lassen
5. **Distribution certificate**: automatisch erzeugen lassen
6. **App Store Connect distribution profile**: automatisch erzeugen lassen
7. **Review → Upload** — dauert 3-5 Minuten

### Schritt 8: Warten + TestFlight Build freigeben
1. Nach dem Upload: App Store Connect → **TestFlight** Tab
2. Dein Build erscheint dort mit Status "Processing" (dauert 15-60 Minuten)
3. Apple macht automatische Checks. Wenn grün: Build ist in **Internal Testing** verfügbar.
4. **Compliance-Frage**: Beim ersten Build fragt Apple ob deine App Verschlüsselung nutzt → "None of the algorithms mentioned above" wenn du nur HTTPS nutzt
5. **Test Information**: Füge Test-Notes hinzu (sieht TestFlight-Tester) und wen du einladen willst

### Schritt 9: TestFlight Tester einladen
1. **TestFlight → Internal Testing** → Gruppe erstellen → dich selbst per E-Mail einladen
2. **Oder External Testing** für externe Beta-Tester (bis zu 10.000 User, braucht Apple Review beim ersten Build — dauert 1-2 Tage)

---

## Teil 2: Fastlane Setup (Automatisierung beider Plattformen)

Sobald TestFlight mit dem manuellen Upload einmal funktioniert hat, automatisierst du alle zukünftigen Releases mit einem einzigen Befehl.

### Schritt 1: Fastlane installieren

```bash
# macOS (empfohlen)
brew install fastlane

# Verifizieren
fastlane --version
```

### Schritt 2: Google Play Service Account erzeugen (Android Credentials)

1. https://play.google.com/console/ → **Einstellungen → Entwicklerkontozugang → API-Zugriff**
2. **Neues Service-Konto erstellen** → öffnet Google Cloud Console in neuem Tab
3. Im GCP: **IAM & Admin → Service Accounts → + Create Service Account**
   - Name: `fastlane-katchit`
   - Rolle: *keine* (das wird in der Play Console gesetzt)
   - **Create + Continue → Done**
4. Zurück bei der Liste: Service Account anklicken → **Keys → Add Key → Create new key → JSON**
5. Datei wird heruntergeladen — **umbenennen** zu `play-service-account.json`
6. **Sicher ablegen**, NICHT im Repo:
   ```bash
   mkdir -p ~/.config/katchit
   mv ~/Downloads/*.json ~/.config/katchit/play-service-account.json
   chmod 600 ~/.config/katchit/play-service-account.json
   ```
7. **Zurück in Play Console**: API-Zugriff Tab → Service Account sollte jetzt in der Liste stehen → **Zugriff gewähren** → Rolle **"Release-Manager"** (oder "Admin") → "Erstellen der App" + "Release zur Production + Closed Testing" Häkchen → **Einladen**
8. Warte 1-2 Minuten bis die Rechte greifen

### Schritt 3: App Store Connect API Key erzeugen (iOS Credentials)

1. https://appstoreconnect.apple.com → **Users and Access → Integrations → App Store Connect API**
2. **+ Generate API Key** (oder **+** oben rechts)
3. Name: `fastlane-katchit`
4. Access: **App Manager** (reicht für TestFlight + Release)
5. **Generate** — der Key-File (`.p8`) kann **NUR EINMAL** heruntergeladen werden, sofort sichern!
6. Notiere dir außerdem:
   - **Key ID** (10 Zeichen, z.B. `ABC1234567`)
   - **Issuer ID** (UUID-Format, ganz oben auf der Seite)
7. **Sicher ablegen**:
   ```bash
   mv ~/Downloads/AuthKey_*.p8 ~/.config/katchit/AppStoreConnectAPI.p8
   chmod 600 ~/.config/katchit/AppStoreConnectAPI.p8
   ```

### Schritt 4: Fastlane im Projekt initialisieren

```bash
cd /Users/pascalgrimm/Desktop/Geo-Bingo/GeoBingo
fastlane init
```

Bei den Fragen:
- *"What would you like to use fastlane for?"* → `4. Manual setup`
- Erzeugt einen `fastlane/` Ordner mit `Fastfile` und `Appfile`

### Schritt 5: `fastlane/Appfile` konfigurieren

Datei: `GeoBingo/fastlane/Appfile`

```ruby
# Android
json_key_file("/Users/pascalgrimm/.config/katchit/play-service-account.json")
package_name("pg.geobingo.one")

# iOS
app_identifier("pg.geobingo.one")
apple_id("DEIN_APPLE_ID_EMAIL@example.com")
itc_team_id("DEINE_APP_STORE_CONNECT_TEAM_ID")
team_id("DEIN_APPLE_DEV_TEAM_ID")  # 10-Zeichen Team-ID aus developer.apple.com
```

Die Team-IDs findest du:
- **Developer Team ID**: https://developer.apple.com/account → Membership → Team ID
- **App Store Connect Team ID**: https://appstoreconnect.apple.com → oben rechts auf deinen Namen klicken → die numerische ID hinter deinem Team

### Schritt 6: `fastlane/Fastfile` erstellen

Datei: `GeoBingo/fastlane/Fastfile`

```ruby
default_platform(:android)

# ─────────────────────────────────────────────────────────────
# ANDROID
# ─────────────────────────────────────────────────────────────
platform :android do
  desc "Build + upload Android bundle to Play Closed Test"
  lane :android_beta do
    gradle(
      task: "composeApp:bundleRelease",
      project_dir: "."
    )
    upload_to_play_store(
      track: "internal",  # oder "alpha" für Closed Test
      aab: "composeApp/build/outputs/bundle/release/composeApp-release.aab",
      skip_upload_metadata: true,
      skip_upload_images: true,
      skip_upload_screenshots: true,
      release_status: "draft",  # Manuell in Console finalisieren
    )
  end

  desc "Upload to Play Production"
  lane :android_release do
    gradle(
      task: "composeApp:bundleRelease",
      project_dir: "."
    )
    upload_to_play_store(
      track: "production",
      aab: "composeApp/build/outputs/bundle/release/composeApp-release.aab",
      skip_upload_metadata: true,
      skip_upload_images: true,
      skip_upload_screenshots: true,
    )
  end
end

# ─────────────────────────────────────────────────────────────
# iOS
# ─────────────────────────────────────────────────────────────
platform :ios do
  desc "Build + upload iOS to TestFlight"
  lane :ios_beta do
    # API Key laden (siehe Schritt 3)
    api_key = app_store_connect_api_key(
      key_id: "DEINE_KEY_ID",
      issuer_id: "DEINE_ISSUER_ID",
      key_filepath: "/Users/pascalgrimm/.config/katchit/AppStoreConnectAPI.p8",
      in_house: false,
    )

    # Build-Nummer automatisch hochzählen (basierend auf TestFlight latest)
    increment_build_number(
      xcodeproj: "iosApp/iosApp.xcodeproj",
      build_number: latest_testflight_build_number(api_key: api_key) + 1,
    )

    # Pods installieren (falls nötig)
    cocoapods(
      podfile: "iosApp/Podfile",
    )

    # Archive bauen + signieren
    build_app(
      workspace: "iosApp/iosApp.xcworkspace",
      scheme: "iosApp",
      export_method: "app-store",
      configuration: "Release",
      clean: true,
      output_directory: "build/ios",
    )

    # Upload zu TestFlight
    upload_to_testflight(
      api_key: api_key,
      skip_waiting_for_build_processing: true,
      changelog: "Siehe store-listing/release-notes-v1.X.txt",
    )
  end
end

# ─────────────────────────────────────────────────────────────
# Cross-Platform
# ─────────────────────────────────────────────────────────────
lane :beta do
  android_beta
  ios_beta
end
```

### Schritt 7: `.gitignore` erweitern

Füge in `GeoBingo/.gitignore` hinzu:

```gitignore
# Fastlane
fastlane/report.xml
fastlane/Preview.html
fastlane/screenshots
fastlane/test_output
fastlane/*.json
fastlane/*.p8
```

Auch sicherstellen dass **weder** `play-service-account.json` **noch** `AppStoreConnectAPI.p8` jemals ins Repo kommen — die liegen in `~/.config/katchit/`, nicht im Projekt-Ordner.

### Schritt 8: Testen

```bash
cd /Users/pascalgrimm/Desktop/Geo-Bingo/GeoBingo

# Nur Android
fastlane android_beta

# Nur iOS (das erste Mal dauert 10-15 Min wegen Code-Signing)
fastlane ios_beta

# Beides parallel
fastlane beta
```

---

## Teil 3: Day-to-Day Release Workflow

Sobald alles einmal läuft, sieht ein neuer Release so aus:

```bash
# 1. Version bumpen in composeApp/build.gradle.kts:
#    versionCode = X+1
#    versionName = "1.X"

# 2. In Xcode: iosApp target → General → Version "1.X" (Build bumped Fastlane automatisch)

# 3. Release-Notes schreiben:
#    store-listing/release-notes-v1.X.txt        (Deutsch)
#    store-listing/release-notes-v1.X-en.txt     (English)

# 4. Commit + Tag (optional)
git add composeApp/build.gradle.kts iosApp/ store-listing/
git commit -m "chore: v1.X release"
git tag v1.X

# 5. Hochladen zu Play + TestFlight
fastlane beta

# 6. In Play Console + App Store Connect:
#    - Play: Release-Draft finalisieren + Rollout starten
#    - App Store Connect: TestFlight-Build nach Review für External Testers freigeben
```

---

## Troubleshooting

### Android: "Package name not found"
Das Google Play Service Account hat keinen Zugriff auf die App. Zurück zu Teil 2 → Schritt 2.7 und sicherstellen dass die Rolle zugewiesen und die App ausgewählt ist.

### Android: "Version code X has already been used"
`versionCode` in `composeApp/build.gradle.kts` erhöhen — Play akzeptiert keine doppelten Versions Codes.

### iOS: "No profiles for 'pg.geobingo.one' were found"
Xcode kann kein passendes Provisioning Profile finden. Lösung:
1. Xcode → **Settings → Accounts** → dein Apple ID → **Manage Certificates** → **+** → "Apple Development" UND "Apple Distribution"
2. Dann: Target → Signing & Capabilities → **Automatically manage signing** aus und wieder an → Xcode regeneriert das Profile

### iOS: "Build X has already been used for version Y"
In Xcode die **Build Number** (nicht Version!) hochzählen. Fastlane macht das automatisch via `increment_build_number`, manuell einfach Build auf z.B. `6` setzen.

### iOS: Pods error
```bash
cd GeoBingo/iosApp && pod install --repo-update
```

### Fastlane: API Key 401
Prüfe dass der `.p8` File-Path korrekt ist und die Key ID / Issuer ID exakt aus App Store Connect kopiert sind.

### Fastlane hängt beim "Waiting for processing"
`skip_waiting_for_build_processing: true` ist gesetzt — sollte sofort durchlaufen. Wenn nicht: Apple-API ist gerade langsam, einfach manuell in App Store Connect checken.

---

## Sicherheits-Checkliste

- [ ] `play-service-account.json` liegt in `~/.config/katchit/`, NICHT im Projekt
- [ ] `AppStoreConnectAPI.p8` liegt in `~/.config/katchit/`, NICHT im Projekt
- [ ] `.gitignore` enthält `fastlane/*.json` und `fastlane/*.p8`
- [ ] `chmod 600` auf beiden Credential-Dateien
- [ ] `git status` zeigt KEINE Credentials vor dem ersten Commit
- [ ] App Store Connect API Key hat nur "App Manager"-Rechte, nicht "Admin"
- [ ] Google Play Service Account hat nur "Release-Manager"-Rechte

---

## Referenzen

- Fastlane Android: https://docs.fastlane.tools/getting-started/android/setup/
- Fastlane iOS: https://docs.fastlane.tools/getting-started/ios/setup/
- App Store Connect API: https://developer.apple.com/documentation/appstoreconnectapi
- Play Developer API: https://developers.google.com/android-publisher
- Fastlane `supply` (Play): https://docs.fastlane.tools/actions/supply/
- Fastlane `pilot` (TestFlight): https://docs.fastlane.tools/actions/pilot/

---

## Nächste Schritte nach dem ersten Release

Wenn du später z.B. **Screenshots automatisch hochladen** oder **`fastlane match`** für Cert-Management via privatem Git-Repo nutzen willst — das sind separate Schritte die du hinzufügen kannst wenn die Basis steht. Für einen Solo-Dev mit Closed Test ist die hier beschriebene Basis meistens mehr als genug.
