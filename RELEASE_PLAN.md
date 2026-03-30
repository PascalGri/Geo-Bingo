# KatchIt! Release Plan

## VOR dem Release

### 1. Apple Developer Account einrichten
- [ ] Apple Developer Program beitreten (99$/Jahr) unter https://developer.apple.com/programs/
- [ ] Warten auf Freischaltung (kann 24-48h dauern)

### 2. App IDs & Zertifikate erstellen
- [ ] Im Apple Developer Portal: Certificates, Identifiers & Profiles
- [ ] App ID erstellen: Bundle ID = `pg.geobingo.one`
- [ ] Push Notifications Capability aktivieren fuer diese App ID
- [ ] APNs Key erstellen: Keys -> "+" -> "Apple Push Notifications service (APNs)" -> Download (.p8 Datei)
- [ ] APNs Key in Firebase hochladen: Firebase Console -> Projekteinstellungen -> Cloud Messaging -> Apple-App-Konfiguration -> APNs-Authentifizierungsschluessel hochladen (.p8 + Key ID + Team ID)

### 3. Zertifikate fuer App-Signierung
- [ ] iOS Distribution Certificate erstellen (oder Xcode automatisch verwalten lassen)
- [ ] Provisioning Profile erstellen (App Store Distribution)
- [ ] In Xcode: Signing & Capabilities -> Team auswaehlen -> Automatic Signing aktivieren

### 4. Push Notifications in Xcode aktivieren
- [ ] Xcode Projekt oeffnen: `GeoBingo/iosApp/iosApp.xcworkspace`
- [ ] Target "iosApp" -> Signing & Capabilities
- [ ] "+ Capability" -> "Push Notifications" hinzufuegen
- [ ] "+ Capability" -> "Background Modes" -> "Remote notifications" aktivieren

### 5. Associated Domains fuer Deep Links (optional aber empfohlen)
- [ ] In Xcode: "+ Capability" -> "Associated Domains"
- [ ] Domain hinzufuegen: `applinks:katchit.app`
- [ ] Auf dem Webserver (Vercel) die Datei `/.well-known/apple-app-site-association` ablegen:
```json
{
  "applinks": {
    "apps": [],
    "details": [
      {
        "appID": "TEAMID.pg.geobingo.one",
        "paths": ["/join/*"]
      }
    ]
  }
}
```
- [ ] TEAMID durch deine echte Team ID ersetzen (findest du im Developer Portal unter Membership)

### 6. App Store Connect vorbereiten
- [ ] App in App Store Connect erstellen: https://appstoreconnect.apple.com
- [ ] Bundle ID: `pg.geobingo.one`
- [ ] App-Name: "KatchIt!"
- [ ] Primaere Sprache: Deutsch
- [ ] Kategorie: Spiele -> Brettspiele oder Trivia
- [ ] Preis: Kostenlos

### 7. App Store Listing vorbereiten
- [ ] Screenshots erstellen (min. 3 pro Geraetegroesse):
  - iPhone 6.7" (iPhone 15 Pro Max)
  - iPhone 6.1" (iPhone 15 Pro)
  - iPad Pro 12.9" (falls iPad unterstuetzt)
- [ ] App-Vorschauvideos (optional, aber stark empfohlen - 15-30 Sek.)
- [ ] App-Beschreibung schreiben (DE + EN)
- [ ] Keywords definieren (max. 100 Zeichen, kommagetrennt)
- [ ] Support-URL: https://katchit.app
- [ ] Datenschutz-URL: https://katchit.app/datenschutz.html
- [ ] App-Icon: 1024x1024px (ohne Transparenz, ohne abgerundete Ecken)

### 8. App-Review Vorbereitung
- [ ] Demo-Account fuer Apple Review erstellen (falls Login noetig fuer Features)
- [ ] Testnotizen schreiben: erklaeren wie man das Spiel testet (Runde erstellen, Code teilen, etc.)
- [ ] Datenschutz-Fragebogen in App Store Connect ausfuellen:
  - Kamera: Zum Fotografieren von Kategorien
  - Standort: Fotostandort speichern
  - Kontaktdaten (E-Mail): Account-Erstellung
  - Tracking: AdMob personalisierte Werbung

### 9. iOS Build & Upload
- [ ] In Xcode: Product -> Archive
- [ ] Archive validieren
- [ ] An App Store Connect senden (Upload)
- [ ] In App Store Connect: Build auswaehlen und der Version zuweisen
- [ ] TestFlight: Internen Test starten (automatisch nach Upload verfuegbar)

### 10. Android (Google Play) vorbereiten
- [ ] Google Play Developer Account (25$ einmalig): https://play.google.com/console
- [ ] App in Google Play Console erstellen
- [ ] App-Listing ausfuellen (Titel, Beschreibung, Screenshots, Icon)
- [ ] Datenschutzerklaerung-URL hinterlegen
- [ ] Content Rating Fragebogen ausfuellen
- [ ] App-Signierung: Google Play App Signing verwenden (empfohlen)
- [ ] Release-APK/AAB erstellen: `./gradlew bundleRelease`
- [ ] AAB hochladen -> Interner Test Track

### 11. Testen vor Release
- [ ] TestFlight (iOS): Mit 3-5 Testgeraeten testen
- [ ] Google Play Internal Testing (Android): Mit Testgeraeten testen
- [ ] Checkliste:
  - [ ] Account erstellen / einloggen / ausloggen
  - [ ] Runde erstellen (alle 4 Modi)
  - [ ] Runde beitreten per Code
  - [ ] Runde beitreten per Deep Link (katchit.app/join/CODE)
  - [ ] Foto aufnehmen und hochladen
  - [ ] Voting / Review Phase
  - [ ] Ergebnisse + Rematch
  - [ ] Solo-Modus komplett durchspielen
  - [ ] Freund hinzufuegen per Code
  - [ ] Freund zum Spiel einladen
  - [ ] Push Notification empfangen (Freundschaftsanfrage + Spieleinladung)
  - [ ] Taeglich Challenge erledigen -> gruener Haken
  - [ ] Woechentliche Challenge Fortschritt pruefen
  - [ ] In-Game Chat waehrend Runde testen
  - [ ] Profil-Seite / Bestenliste pruefen
  - [ ] Werbung wird angezeigt (Interstitial + Rewarded)
  - [ ] In-App-Kauf testen (No Ads)
  - [ ] Offline-Banner erscheint bei fehlender Verbindung

---

## RELEASE

### iOS App Store
- [ ] In App Store Connect: "Zur Ueberpruefung einreichen"
- [ ] Review dauert typisch 24-48h (kann bis zu 7 Tage dauern)
- [ ] Bei Ablehnung: Grund lesen, fixen, erneut einreichen
- [ ] Nach Genehmigung: Release-Datum waehlen (sofort oder geplant)

### Google Play Store
- [ ] Vom internen Test zum "Production" Track wechseln
- [ ] Review dauert typisch einige Stunden bis 3 Tage
- [ ] Nach Genehmigung: Rollout starten (stufenweise empfohlen: 20% -> 50% -> 100%)

---

## NACH dem Release

### Erste Woche
- [ ] Crash Reports ueberwachen:
  - Firebase Crashlytics (wenn eingerichtet)
  - Xcode Organizer -> Crashes
  - Google Play Console -> Android Vitals
- [ ] App Store Reviews lesen und beantworten
- [ ] Server-Last ueberwachen (Supabase Dashboard)
- [ ] Push Notifications verifizieren (werden sie zugestellt?)

### Marketing & Wachstum
- [ ] App Store Optimization (ASO):
  - Keywords optimieren basierend auf Suchtrends
  - A/B Tests fuer Screenshots/Icon (Google Play Experiments)
- [ ] Social Media Accounts erstellen (Instagram, TikTok)
- [ ] Launch-Post auf Social Media
- [ ] QR-Code mit Deep Link drucken fuer Flyer/Sticker
- [ ] Freunde + Familie bitten, Bewertungen zu schreiben (min. 10-20 Reviews)

### Laufende Wartung
- [ ] Woechentlich: Crash Reports + Reviews checken
- [ ] Monatlich: Supabase Storage aufraumen (alte Spielfotos loeschen)
- [ ] Alle 2-4 Wochen: Update mit Bugfixes + kleinen Features
- [ ] Supabase-Kosten im Auge behalten (Free Tier Limits: 500MB DB, 1GB Storage, 2GB Bandwidth)

### Spaetere Updates (Roadmap)
- [ ] Spectator Mode (laufende Spiele beobachten)
- [ ] Achievements / Badge-System
- [ ] Season Pass / Battle Pass
- [ ] Weitere Spielmodi
- [ ] iPad-optimiertes Layout
- [ ] Apple Watch Companion (Timer + Benachrichtigungen)
- [ ] Android Widgets (naechste Challenge, Freunde online)
