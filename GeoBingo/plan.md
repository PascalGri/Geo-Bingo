# KatchIt! Release & Update Workflow

## Projekt-Infos

| | Android | iOS | Web |
|---|---|---|---|
| Bundle ID | pg.geobingo.one | pg.geobingo.one | - |
| Store | Google Play Console | App Store Connect | katchit.app (Vercel) |
| Signing | release.jks + signing.properties | Automatic (Team ANX2QJ38DC) | - |
| Keystore-Passwort | In signing.properties (git-ignored) | - | - |

---

## Erster Release (einmalig)

### 1. Store-Accounts vorbereiten

**Google Play Console** (https://play.google.com/console)
- App erstellen: Name "KatchIt!", Kategorie "Spiele"
- Store-Listing: Beschreibung, Screenshots (min. 2), Feature-Graphic, App-Icon
- Datenschutz-URL: https://katchit.app/datenschutz.html
- Content Rating Fragebogen ausfuellen
- Preis: Kostenlos, Laender waehlen

**App Store Connect** (https://appstoreconnect.apple.com)
- Neue App: Name "KatchIt!", Bundle ID pg.geobingo.one
- Kategorie "Spiele", Altersfreigabe
- Datenschutz-URL: https://katchit.app/datenschutz.html
- Screenshots: 6.7" und 5.5" Pflicht

### 2. Builds erstellen und hochladen

Siehe "Update Workflow" unten — der Ablauf ist identisch.

### 3. Review einreichen

- Play Store: "Submit for review" (1-7 Tage beim ersten Mal)
- App Store: "Submit for Review" (1-3 Tage beim ersten Mal)

---

## Update Workflow (synchroner Release)

Alle drei Plattformen (Android, iOS, Web) werden gleichzeitig released.

### Schritt 1: Versionsnummern synchron hochzaehlen

**Android** — composeApp/build.gradle.kts:
```kotlin
versionCode = N        // Integer, MUSS bei jedem Upload hoeher sein
versionName = "X.Y"   // String, was der Nutzer sieht
```

**iOS** — iosApp/Configuration/Config.xcconfig:
```
CURRENT_PROJECT_VERSION = N      // Muss bei jedem Upload hoeher sein
MARKETING_VERSION = X.Y          // Was der Nutzer sieht, identisch zu versionName
```

Beide muessen denselben Stand haben:
- versionCode == CURRENT_PROJECT_VERSION
- versionName == MARKETING_VERSION

### Schritt 2: Android Bundle bauen

```bash
cd GeoBingo
./gradlew clean composeApp:bundleRelease
```

Output: `composeApp/build/outputs/bundle/release/composeApp-release.aab`

Pruefen ob signiert:
```bash
jarsigner -verify composeApp/build/outputs/bundle/release/composeApp-release.aab
```

### Schritt 3: iOS Archive bauen

```bash
cd GeoBingo

# KMP Framework bauen
./gradlew composeApp:assembleReleaseXCFramework

# Pods aktualisieren (falls Podfile geaendert)
cd iosApp
pod install
```

Dann in Xcode:
1. `iosApp.xcworkspace` oeffnen (NICHT .xcodeproj)
2. Scheme: iosApp, Destination: "Any iOS Device (arm64)"
3. Product > Archive
4. "Distribute App" > "App Store Connect" > Upload

### Schritt 4: Web deployen

Passiert automatisch beim Push (pre-push Hook baut WASM + deployed auf Vercel).
Falls manuell noetig:
```bash
cd GeoBingo
./gradlew composeApp:buildWeb
vercel --prod
```

### Schritt 5: In Stores hochladen

**Play Console:**
1. Release > Production > "Create new release"
2. .aab hochladen
3. Release Notes eintragen
4. "Review and roll out" > Rollout starten

**App Store Connect:**
1. Neue Version anlegen (selbe Versionsnummer)
2. Build auswaehlen (erscheint nach Xcode-Upload, kann 5-15 Min dauern)
3. Release Notes eintragen ("What's New")
4. "Submit for Review"

### Schritt 6: Git taggen

```bash
git tag vX.Y
git push --tags
```

---

## Checkliste pro Update

```
[ ] Code fertig und getestet
[ ] versionCode/CURRENT_PROJECT_VERSION hochgezaehlt
[ ] versionName/MARKETING_VERSION identisch gesetzt
[ ] Android: ./gradlew clean composeApp:bundleRelease
[ ] iOS: ./gradlew assembleReleaseXCFramework
[ ] iOS: pod install (falls Pods geaendert)
[ ] iOS: Xcode Archive + Upload zu App Store Connect
[ ] Git push (deployed Web automatisch)
[ ] Play Console: .aab hochladen + Rollout
[ ] App Store Connect: Build waehlen + Submit
[ ] Release Notes in beiden Stores identisch
[ ] Git tag setzen: git tag vX.Y && git push --tags
```

---

## Wichtige Dateien (NIE verlieren)

| Datei | Grund |
|---|---|
| release.jks | Ohne Keystore kein Android-Update. Play Store akzeptiert nur denselben Key. |
| signing.properties | Keystore-Passwort |
| Apple Developer Account | Ohne den kein iOS-Upload |
| GoogleService-Info.plist | Firebase Config fuer iOS |
| google-services.json | Firebase Config fuer Android |

---

## Rollback

**Android:** In Play Console unter "Release history" > vorherige Version als neue Release hochladen.

**iOS:** In App Store Connect "Remove from Sale" oder ein Fix-Update einreichen. Apple erlaubt keinen echten Rollback — nur ein neues Update.

**Web:** `vercel rollback` oder den vorherigen Git-Stand pushen.
