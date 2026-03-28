# KatchIt! Post-Launch Plan

## Sofort nach Veroeffentlichung

### Google OAuth in Produktion setzen
- Google Cloud Console > OAuth-Zustimmungsbildschirm
- Status von "Testing" auf "In Produktion" umstellen
- Google-Pruefung abwarten (dauert wenige Tage)
- Ohne diesen Schritt koennen sich nur Testnutzer anmelden

### Apple Sign-In einrichten
- Apple Developer Account registrieren (99 USD/Jahr)
- App ID + Service ID + Key erstellen
- Supabase Dashboard: Authentication > Providers > Apple konfigurieren
- iOS plist hat URL-Schema bereits konfiguriert

### Custom SMTP einrichten
- Resend.com Account erstellen (kostenlos, 100 Emails/Tag)
- Domain katchit.app verifizieren (DNS-Eintraege)
- Supabase Dashboard: Project Settings > SMTP Settings konfigurieren
- Dann Rate Limit fuer Emails von 2/h auf 30/h erhoehen

## Erste Woche

### Monitoring aufsetzen
- Supabase Dashboard: Logs regelmaessig pruefen (Auth errors, DB errors)
- Crash-Reporting pruefen (Firebase Crashlytics / Sentry)
- Storage-Verbrauch im Auge behalten (Fotos wachsen schnell)

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
