# App Privacy Questionnaire — KatchIt! (iOS)

Antworten für App Store Connect → App → App Privacy. Klick dich durch die Apple-UI; die Antworten unten sind 1:1 übertragbar.

Quelle: Audit des Projekts am 2026-04-11 (alle Datenklassen, Supabase-Schemas, Firebase- und AdMob-Integrationen geprüft).

---

## Grundsatzfrage: "Do you or your third-party partners collect data from this app?"

**Antwort: Yes**

---

## Data Types — wähle diese Kategorien aus

### 1. Contact Info → Email Address

- **Collected**: Yes
- **Linked to user**: Yes
- **Used for tracking**: No
- **Purposes**:
  - [x] App Functionality (Login/Account)
- **Begründung (intern)**: Wird bei Email-Signup oder OAuth-Login von Supabase Auth gespeichert. Linked zur user_id.

### 2. User Content → Photos or Videos

- **Collected**: Yes
- **Linked to user**: Yes
- **Used for tracking**: No
- **Purposes**:
  - [x] App Functionality
- **Begründung (intern)**: Spielerfotos werden auf Supabase Storage hochgeladen, damit Mitspieler sie bewerten können. Photos werden 30 Tage nach Spielende automatisch gelöscht (oder wenn der User das Match löscht).

### 3. User Content → Customer Support

- **Collected**: No (Support läuft über support@katchit.app E-Mail, nicht in-app)

### 4. User Content → Other User Content

- **Collected**: Yes
- **Linked to user**: Yes
- **Used for tracking**: No
- **Purposes**:
  - [x] App Functionality
- **Begründung (intern)**: Display Name, Chat-Nachrichten an Freunde, Spiel-Kategorien, Bewertungen (1-5 Sterne).

### 5. Identifiers → User ID

- **Collected**: Yes
- **Linked to user**: Yes
- **Used for tracking**: No
- **Purposes**:
  - [x] App Functionality
  - [x] Analytics
- **Begründung (intern)**: Supabase Auth UUID, verknüpft Spielstände, Käufe, Freunde.

### 6. Identifiers → Device ID

- **Collected**: Yes
- **Linked to user**: No
- **Used for tracking**: Yes
- **Purposes**:
  - [x] Third-Party Advertising
- **Begründung (intern)**: IDFA wird via AdMob/Google Mobile Ads für personalisierte Werbung genutzt — aber NUR wenn User per ATT-Prompt zustimmt. Wenn er ablehnt, werden nur nicht-personalisierte Ads gezeigt.

### 7. Location → Precise Location

- **Collected**: Yes
- **Linked to user**: Yes
- **Used for tracking**: No
- **Purposes**:
  - [x] App Functionality
- **Begründung (intern)**: Latitude/Longitude der Spielerfotos werden gespeichert, damit Mitspieler auf einer Karte sehen wo die Fotos entstanden sind. NUR wenn User per iOS-Location-Prompt zustimmt. Optional, Spiel funktioniert auch ohne.

### 8. Usage Data → Product Interaction

- **Collected**: Yes
- **Linked to user**: Yes
- **Used for tracking**: No
- **Purposes**:
  - [x] Analytics
- **Begründung (intern)**: Events wie game_created, game_joined, photo_captured, vote_cast werden in Supabase `analytics_events` gespeichert, um Produktentscheidungen zu treffen (welche Features genutzt werden).

### 9. Purchases → Purchase History

- **Collected**: Yes
- **Linked to user**: Yes
- **Used for tracking**: No
- **Purposes**:
  - [x] App Functionality
- **Begründung (intern)**: Sterne-Guthaben, Skip-Karten, No-Ads-Purchase werden im Supabase-Profil gespeichert, damit Käufe auf allen Geräten verfügbar sind.

### 10. Diagnostics → Crash Data + Performance Data

- **Collected**: Yes (via Firebase Crashlytics falls integriert, sonst Nein)
- **Linked to user**: No
- **Used for tracking**: No
- **Purposes**:
  - [x] App Functionality
  - [x] Analytics
- **Begründung (intern)**: Falls Firebase Crashlytics aktiv ist. Check: Ist `FirebaseCrashlytics` Pod installiert? Wenn nein → "No" auswählen.

---

## Data Types die NICHT gesammelt werden (zur Klarstellung)

- ❌ Name (nur Display Name, das ist "Other User Content")
- ❌ Phone Number
- ❌ Physical Address
- ❌ Payment Info (StoreKit handhabt das, App sieht keine Kreditkarten-Daten)
- ❌ Credit Info
- ❌ Other Financial Info
- ❌ Precise Location außer Spielerfotos (kein Background-Tracking)
- ❌ Coarse Location
- ❌ Health & Fitness
- ❌ Sensitive Info
- ❌ Contacts
- ❌ Audio Data
- ❌ Gameplay Content (das fällt unter "Other User Content")
- ❌ Browsing History
- ❌ Search History

---

## Tracking Declaration (WICHTIG)

Apple definiert "Tracking" als: *Linking user/device data from this app to data from other companies' apps, websites, or offline properties, OR sharing data with data brokers.*

**Antwort auf "Do you or your third-party partners use data for tracking purposes?"**: **Yes**

Begründung: AdMob/Google Mobile Ads nutzt den IDFA (wenn per ATT erlaubt) um Werbung über mehrere Apps hinweg zu personalisieren. Das ist per Apple-Definition Tracking.

---

## Checkliste beim Ausfüllen

- [ ] Alle 10 Kategorien oben in ASC ankreuzen
- [ ] Bei jeder Kategorie "Linked to User" korrekt setzen
- [ ] Nur **Device ID** hat "Used for Tracking = Yes"
- [ ] Unter "Tracking" **Yes** auswählen und AdMob-Begründung eintragen
- [ ] Privacy Policy URL eingeben: `https://katchit.app/datenschutz.html`
- [ ] **Support URL** eingeben: `https://katchit.app/support.html` (NICHT `https://katchit.app` — das wurde von Apple in v1.0 abgelehnt, weil die Root-URL keine Support-Informationen anzeigt)
- [ ] Save + Publish

---

## Re-Submission nach v1.0-Ablehnung (April 2026)

Apple lehnte v1.0 am 13.04.2026 mit zwei Begründungen ab:

### 1.5.0 — Safety: Developer Information (Support URL)
**Apple-Beschwerde**: Die Support URL `https://katchit.app` zeigt nur die Marketing-Landingpage an, keine Support-Informationen.

**Fix**: Eigene Support-Seite unter `https://katchit.app/support.html` (Inhalte: Kontakt-Email, FAQ, Links zu Datenschutz/Impressum). In App Store Connect die Support URL auf diese neue Seite ändern.

### 5.1.1 — Legal: Privacy / Data Collection (AI / Cloudflare Workers AI)
**Apple-Beschwerde**: Die App teilt Foto-Daten mit einem Third-Party-AI-Dienst, ohne in der App offen zu legen, was gesendet wird, an wen, und ohne explizite Einwilligung des Users einzuholen.

**Fix in der App**:
1. Vor dem ersten KI-Foto-Validierungs-Call (Solo-Modus + Multiplayer-AI-Judge-Modus) wird ein Consent-Dialog gezeigt: erklärt dass das Foto an Cloudflare Workers AI gesendet wird, was übermittelt wird (Foto + Kategorie, keine Namen/IDs), und bittet um „Einverstanden" / „Ablehnen".
2. Consent wird in `AppSettings` (`AI_CONSENT_ACCEPTED`) persistiert — pro Gerät einmal abfragen.
3. Bei „Ablehnen" wird kein Foto an Cloudflare gesendet — es gibt einen lokalen Default-Score (3 Sterne) im Solo-Modus, im Multiplayer-AI-Judge wird die KI-Bewertung übersprungen.
4. Privacy Policy unter `https://katchit.app/datenschutz.html` Abschnitt 3d / 5c nennt den Anbieter (Cloudflare Inc., USA), Zweck, übermittelte Daten und Rechtsgrundlage explizit.

**Fix in App Store Connect**:
- Privacy-Sektion → "Other Data" hinzufügen mit Kategorie **Other User Content** (Photos), Linked to User = **No**, Used for Tracking = **No**, Purpose = **App Functionality** (KI-Bewertung von Spielfotos via Cloudflare Workers AI).
- Hinweis im "App Privacy Details" Resolution-Reply: „We added an in-app consent dialog before any photo data is sent to our third-party AI provider (Cloudflare Workers AI). The privacy policy at https://katchit.app/datenschutz.html section 3d/5c explicitly names the provider and the data shared. Photos are not stored — only processed in real time and discarded."

---

## Häufige Fehler, die zu Rejections führen

1. **"No data collected"** angegeben, obwohl Firebase/AdMob/Supabase läuft → Instant-Rejection
2. **"Tracking = No"** angegeben, obwohl IDFA via ATT genutzt wird → Rejection
3. **Privacy Policy URL** zeigt auf leere oder generische Seite → Rejection (die Policy muss die oben gelisteten Datenkategorien explizit nennen)
4. **ATT-Prompt wird in der App nicht getriggert** obwohl "Tracking = Yes" angegeben → Rejection

Alle 4 sind bei KatchIt! gecheckt: Daten werden ehrlich angegeben, ATT wird korrekt in `iOSApp.swift` aufgerufen, Privacy Policy liegt unter `https://katchit.app/datenschutz.html`.
