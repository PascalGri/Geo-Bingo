# KatchIt! Post-Launch Plan

## Bereits erledigt
- [x] Google OAuth in Produktion gesetzt
- [x] Custom SMTP (Resend) eingerichtet
- [x] E-Mail Templates gebrandet
- [x] Deep Linking (Android + iOS)
- [x] Edge Function fuer Account-Loeschung
- [x] RLS + Storage Policies gehaertet
- [x] Rate Limits konfiguriert
- [x] Google Play Billing Library integriert (Code fertig)
- [x] StoreKit 2 integriert (Code fertig)

## Vor Release: Produkte in den Stores anlegen

### Google Play Console - In-App-Produkt anlegen
1. Google Play Console > App auswaehlen > Monetarisierung > Produkte > In-App-Produkte
2. "Produkt erstellen" klicken
3. Produkt-ID: `pg.geobingo.one.no_ads` (muss exakt so heissen!)
4. Name: "Werbung entfernen"
5. Beschreibung: "Entfernt alle Werbung dauerhaft aus KatchIt!"
6. Preis festlegen (z.B. 2,99 EUR)
7. Status: Aktiv
8. Speichern

### App Store Connect - In-App-Kauf anlegen
1. App Store Connect > App > In-App-Kaeufe > "+" klicken
2. Typ: "Nicht-verbrauchbar" (Non-Consumable)
3. Referenzname: "Werbung entfernen"
4. Produkt-ID: `pg.geobingo.one.no_ads` (muss exakt so heissen!)
5. Preis festlegen (z.B. Stufe 3 = 2,99 EUR)
6. Lokalisierung hinzufuegen (DE + EN):
   - Anzeigename: "Werbung entfernen" / "Remove Ads"
   - Beschreibung: "Entfernt alle Werbung dauerhaft" / "Permanently removes all ads"
7. Screenshot fuer Review hochladen (Screenshot vom Kaufdialog)
8. Status: "Bereit zur Uebermittlung"

### Apple Sign-In einrichten (sobald Developer Account aktiv)
- Apple Developer > Certificates, Identifiers & Profiles
- App ID + Service ID + Key erstellen
- Supabase Dashboard: Authentication > Providers > Apple konfigurieren
- iOS plist hat URL-Schema bereits konfiguriert

## Erste Woche

### Monitoring aufsetzen
- Supabase Dashboard: Logs regelmaessig pruefen (Auth errors, DB errors)
- Crash-Reporting pruefen (Firebase Crashlytics / Sentry)
- Storage-Verbrauch im Auge behalten (Fotos wachsen schnell)
- AdMob-Einnahmen im AdMob Dashboard verfolgen
- In-App-Kauf-Transaktionen pruefen (Google Play Console + App Store Connect)

### Performance pruefen
- Supabase Advisors laufen lassen (Index-Empfehlungen)
- Slow Queries identifizieren
- Realtime-Connections monitoren

## Erster Monat

### Sicherheit nachruestzen
- Game-Tabellen RLS-Policies verschaerfen (z.B. nur Spieler eines Games sehen dessen Daten)
- Storage: File-Size-Limit setzen (z.B. 5 MB pro Bild)
- Rate Limiting fuer Edge Functions konfigurieren

### Datenschutz / DSGVO
- Daten-Export-Funktion implementieren (User kann eigene Daten herunterladen)
- Automatische Game-Daten-Bereinigung (alte Spiele nach 30-90 Tagen loeschen)
- Cookie-Banner / Tracking-Consent fuer Web-Version pruefen

### Skalierung
- Supabase Plan evaluieren (Free -> Pro wenn noetig)
- CDN fuer Storage-Assets aktivieren
- Database Pooling konfigurieren bei steigenden Connections

### Web-App Werbung (optional)
- Google AdSense erneut beantragen (vorherige Ablehnung pruefen)
- Alternative: Web-App werbefrei lassen, nur Mobile monetarisieren

## Laufend

### Regelmaessige Wartung
- Supabase SDK Updates (aktuell 3.0.3)
- Kotlin/Compose Multiplatform Updates
- Abgelaufene OAuth-Tokens / Zertifikate erneuern
- Apple Developer Account jaehrlich verlaengern
- Google Play / App Store Reviews beantworten

### Metriken tracken
- Taegliche aktive Nutzer (DAU)
- Sign-up Conversion Rate (wie viele erstellen einen Account)
- Spiele pro Tag
- Crash-Free Rate
- App Store Bewertungen
- AdMob Revenue (eCPM, Fill Rate)
- In-App-Kauf Conversion Rate
