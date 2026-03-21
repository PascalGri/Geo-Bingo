# KatchIt / GeoBingo — Verbesserungsplan

Dieser Plan beschreibt alle geplanten Verbesserungen, gruppiert nach Bereich und priorisiert in Phasen.

---

## Phase 1: UI Polish & Micro-Interactions

Kleine, isolierte Verbesserungen die einzeln umsetzbar sind und sofort spürbaren Impact haben.

### ~~1.1 Glassmorphism-Effekt auf GradientBorderCards~~ ✅
- **Erledigt:** Semi-transparenter Background (0.75 alpha) mit diagonalem Licht-Gradient-Overlay auf `GradientBorderCard` — multiplatform-kompatibel ohne `blur()`

### ~~1.2 Foto-Thumbnails auf Kategorie-Karten im GameScreen~~ ✅
- **Erledigt:** Foto füllt die gesamte Karte mit `ContentScale.Crop`, Gradient-Scrim unten für Kategorie-Name, Checkmark-Badge (CircleShape) oben rechts in Spielerfarbe

### 1.3 Parallax-Scrolling auf ResultsScreen
- **Datei:** `ui/screens/ResultsScreen.kt`
- **Was:** Podium bleibt oben fixiert (sticky header), Galerie und Rankings scrollen darunter
- **Umsetzung:** `LazyColumn` mit `stickyHeader {}` für den Podium-Bereich. Alternativ `nestedScroll` mit `TopAppBar`-Pattern wo das Podium beim Scrollen langsamer mitscrollt (Parallax-Faktor ~0.5)

### ~~1.4 Star-Rating Glow-Effekt~~ ✅
- **Erledigt:** Goldener radialer Glow via `drawBehind` + `drawCircle` mit `Brush.radialGradient` hinter selektierten Sternen, Intensität gekoppelt an Scale-Animation

### ~~1.5 Timer-Puls bei <30 Sekunden~~ ✅
- **Erledigt:** `infiniteRepeatable` Puls-Animation mit Alpha (0→0.4) und Scale (1.0→1.03) hinter dem Timer. Pulsfrequenz steigt: >20s=1.5s, >10s=1.0s, <10s=0.5s Zyklus

### ~~1.6 Animierte Punktezählung auf ResultsScreen~~ ✅
- **Erledigt:** `Animatable<Float>` Counter (0→Endwert) mit `FastOutSlowInEasing` über 1.5s. Staggered: Podium nach Platzierung, Rank-Cards nach Rang verzögert

### ~~1.7 "Best Photo" Highlight~~ ✅
- **Erledigt:** Eigene "Best Photo"-Sektion mit EmojiEvents-Icon über der Galerie. `GradientBorderCard` mit `GradientGold`, zeigt Spielername, Kategorie und Durchschnittsbewertung

---

## Phase 2: Share & Social Features

Features die Viralität und soziale Interaktion fördern.

### 2.1 Share-Cards (Bild-Generierung)
- **Dateien:** Neue Datei `ui/components/ShareCardGenerator.kt`, Anpassung `ui/screens/ResultsScreen.kt`
- **Was:** Generierte Bild-Karte (Spotify-Wrapped-Style) mit Podium, Gewinner-Foto und Stats zum Teilen
- **Umsetzung:**
  - Composable `ShareCard` erstellen das off-screen gerendert wird
  - Card-Layout: App-Logo oben, Podium-Grafik, Gewinner-Name + Foto, Top-Stats (Punkte, beste Kategorie, Spieleranzahl)
  - Hintergrund: Gradient aus Theme-Farben
  - `captureToImage()` via `GraphicsLayer.toImageBitmap()` (Compose 1.7+) oder `drawToBitmap()` auf Android Canvas
  - Plattform-spezifisch: `ImageSaver` erweitern um temporäres Bild zu speichern, dann native Share-Intent mit Bild
- **Achtung:** Multiplatform-Bildgenerierung ist tricky — auf Web ggf. Canvas API nutzen, auf iOS `UIGraphicsImageRenderer`

### 2.2 Emoji-Reactions auf Fotos in Review-Phase
- **Dateien:** `ui/screens/ReviewScreen.kt`, `network/GameRepository.kt`, neue Supabase-Tabelle `reactions`
- **Was:** Neben Sternen auch Emoji-Reactions (😂🔥👏) während der Bewertung
- **Umsetzung:**
  - Neue DB-Tabelle: `reactions(id, game_id, voter_id, target_player_id, category_id, emoji)`
  - Unter dem Foto eine Emoji-Leiste mit 5-6 Reactions: 😂 🔥 👏 😍 🤮 💀
  - Tap auf Emoji → speichern + kurze fly-up Animation
  - Reactions sind optional (kein Pflichtfeld), werden neben dem Foto in der Galerie aggregiert angezeigt ("🔥 3 😂 2")
  - Reactions werden zusammen mit den Sternen in der Ergebnis-Galerie unter jedem Foto angezeigt

### 2.3 Live-Feed während des Spiels
- **Dateien:** `ui/screens/GameScreen.kt`, bestehender `GameRealtimeManager`
- **Was:** Anonymisierte Benachrichtigungen ("Ein Spieler hat eine Kategorie gefunden!")
- **Umsetzung:**
  - Bestehende Realtime-Subscription für Capture-Inserts nutzen
  - Bei neuem Capture: Snackbar/Toast am oberen Bildschirmrand: "📸 Ein Spieler hat eine Kategorie gefunden!"
  - Bewusst anonymisiert (kein Name, keine Kategorie) um Strategie nicht zu verraten
  - Animation: Slide-in von oben, 2s sichtbar, Fade-out
  - Optional in Settings abschaltbar

### 2.4 Foto-des-Tages
- **Dateien:** Neue Supabase-Tabelle `photo_highlights`, Anpassung `ResultsScreen.kt`
- **Was:** Nach Spielende können Spieler das beste Foto des Spiels highlighten
- **Umsetzung:**
  - Nach dem Results-Screen: optionaler "Foto des Tages wählen"-Button
  - Jeder Spieler wählt 1 Foto (nicht sein eigenes)
  - Foto mit meisten Stimmen wird in der Game-History mit goldenem Badge markiert
  - Später ausbaubar zu einer globalen "Foto des Tages"-Galerie

---

## Phase 3: Themes & Personalisierung

### 3.1 Light Mode Option
- **Dateien:** `ui/theme/Theme.kt`, neue Datei `ui/theme/LightColors.kt`, `ui/screens/SettingsScreen.kt`
- **Was:** Helles Sommer-Theme als Alternative zum Dark Mode
- **Umsetzung:**
  - Neue `lightColorScheme()` definieren: helle Backgrounds (Off-White/Cream), dunkle Texte, angepasste Gradient-Farben die auf hellem Hintergrund funktionieren
  - In `SettingsScreen` Toggle: "Dark Mode" / "Light Mode" / "System"
  - Theme-Preference in `DataStore`/`SharedPreferences` persistieren
  - `KatchItTheme` erweitern: `isSystemInDarkTheme()` + Override aus Settings
  - Alle hardcodierten Farben in Screens durch Theme-Farben ersetzen (Audit nötig)

### 3.2 Saisonale Themes
- **Dateien:** `ui/theme/Theme.kt`, neue Datei `ui/theme/SeasonalThemes.kt`
- **Was:** Automatische Farbpaletten je nach Jahreszeit
- **Umsetzung:**
  - 4 Paletten: Frühling (Rosa/Grün/Hellblau), Sommer (aktuelles Theme), Herbst (Orange/Braun/Dunkelrot), Winter (Eisblau/Weiß/Silber)
  - Automatische Erkennung via `LocalDate.now().monthValue` → Saison-Mapping
  - In Settings: "Saisonales Theme" Toggle (an/aus), wenn aus → manuelles Theme
  - Gradients und Akzentfarben passen sich an
  - Optional: saisonale Confetti-Farben auf ResultsScreen

---

## Phase 4: Gameplay-Erweiterungen

### 4.1 Kategorie-Packs
- **Dateien:** `ui/screens/CreateGameScreen.kt`, neue Datei `data/CategoryPacks.kt`
- **Was:** Thematische Bundles die als Set geladen werden können
- **Umsetzung:**
  - Datenstruktur: `CategoryPack(name, icon, description, categories: List<Category>)`
  - Vordefinierte Packs: "Stadtbummel" 🏙️, "Natur" 🌿, "Party" 🎉, "Reise" ✈️, "Weihnachtsmarkt" 🎄, "Sport" ⚽, "Essen" 🍕
  - Im CreateGameScreen: Horizontale Pack-Auswahl über dem Kategorie-Grid
  - Tap auf Pack → alle Kategorien des Packs werden selektiert (bestehende Auswahl bleibt oder wird ersetzt — User-Choice)
  - Packs zunächst hardcoded, später von Supabase ladbar

### 4.2 Challenge-Kategorien
- **Dateien:** `data/CategoryPacks.kt`, `ui/screens/GameScreen.kt`
- **Was:** Spezielle Kategorien mit Zusatzbedingungen
- **Umsetzung:**
  - Neue Property auf Category: `challenge: String?` (z.B. "Muss in Bewegung sein", "Selfie erforderlich", "Mindestens 2 Personen im Bild")
  - Challenge-Text wird auf der Kategorie-Karte und im Info-Dialog angezeigt
  - Visuell abgehoben: anderer Border-Style (z.B. gestrichelt) oder spezielles Badge "⚡ Challenge"
  - Bewertung der Challenge-Erfüllung durch die Mitspieler in der Review-Phase (Zusatzfrage: "Challenge erfüllt? Ja/Nein" → Bonus-Punkt)

### 4.3 Tägliche Challenge
- **Dateien:** Neue Supabase-Tabelle `daily_challenges`, neue Datei `ui/screens/DailyChallengeScreen.kt`, Anpassung `HomeScreen.kt`
- **Was:** Globale Tages-Kategorien, alle Spieler weltweit spielen dasselbe Set
- **Umsetzung:**
  - Backend: Cron-Job oder Supabase Edge Function die täglich um 00:00 UTC 5 Kategorien auswählt
  - HomeScreen: "Tägliche Challenge"-Banner mit Countdown bis Mitternacht
  - Eigener Spielmodus: Solo oder mit Freunden, aber immer dieselben Kategorien
  - Globale Bestenliste des Tages (opt-in)
  - Streak-Counter: Wie viele Tage in Folge teilgenommen

### 4.4 Team-Modus (2v2 / 3v3)
- **Dateien:** `game/GameState.kt`, `network/GameRepository.kt`, `ui/screens/LobbyScreen.kt`, `ui/screens/GameScreen.kt`, `ui/screens/ResultsScreen.kt`
- **Was:** Teams teilen sich die Kategorien auf
- **Umsetzung:**
  - Neue DB-Felder: `team_id` auf Player, `team_mode` auf Game
  - Lobby: Host teilt Spieler in Teams ein (Drag & Drop oder Auto-Assign)
  - Im Spiel: Team-Score = Summe aller Team-Mitglieder. Jedes Team-Mitglied sieht die Captures der Teammitglieder
  - Strategie: Kategorien können aufgeteilt werden ("Du machst die oberen 3, ich die unteren 3")
  - Results: Team-Podium + individuelle Beiträge pro Team
  - **Komplexität:** Hoch — Voting-Logik muss angepasst werden (Team-Mitglieder bewerten sich nicht gegenseitig?)

### 4.5 Geofencing-Modus
- **Dateien:** `platform/LocationProvider.kt` (alle Plattformen), `ui/screens/CreateGameScreen.kt`, `ui/screens/GameScreen.kt`
- **Was:** Spielbereich auf einen Radius begrenzen (z.B. 500m)
- **Umsetzung:**
  - CreateGameScreen: Optionaler "Spielbereich begrenzen"-Toggle mit Radius-Slider (100m – 2km)
  - Beim Spielstart: aktuelle GPS-Position als Mittelpunkt speichern
  - Im GameScreen: Mini-Map-Overlay das den erlaubten Bereich zeigt
  - Bei Foto-Aufnahme: GPS-Check ob innerhalb des Radius. Wenn außerhalb → Warnung, Foto wird trotzdem akzeptiert aber markiert
  - LocationProvider auf allen Plattformen muss kontinuierliches GPS-Tracking unterstützen
  - **Abhängigkeit:** Funktionierender `LocationProvider` auf allen Zielplattformen

---

## Phase 5: Progression & Langzeitmotivation

### 5.1 Achievements/Badges
- **Dateien:** Neue Supabase-Tabelle `achievements`, neue Datei `data/AchievementDefinitions.kt`, neue Datei `ui/screens/AchievementsScreen.kt`, neue Datei `ui/components/AchievementPopup.kt`
- **Was:** Freischaltbare Erfolge basierend auf Spielleistung
- **Achievement-Liste:**
  - 📸 "Erster Fang" — Erste Kategorie in einem Spiel fotografiert
  - ⚡ "Speedrunner" — Alle Kategorien in unter 50% der Spielzeit geschafft
  - ⭐ "Fotograf" — Durchschnittliche Bewertung >4.0 in einem Spiel
  - 🏆 "Seriensieger" — 3 Spiele hintereinander gewonnen
  - 🎯 "Perfektionist" — Alle Kategorien als Erster gefunden (alle Speed-Boni)
  - 🌍 "Weltenbummler" — 50 verschiedene Kategorien insgesamt fotografiert
  - 👥 "Gesellig" — 10 Spiele mit 4+ Spielern gespielt
  - 🃏 "Joker-König" — 5x den Joker mit >4.0 Bewertung eingesetzt
- **Umsetzung:**
  - Nach jedem Spiel: Achievement-Check gegen Spieler-Historie
  - Unlock-Popup: Slide-in von oben mit Badge-Icon, Konfetti, Sound-Effekt
  - Achievement-Screen: Grid aller Achievements (freigeschaltet = farbig, gesperrt = grau mit Hinweis)
  - Persistierung: Supabase mit User-Account oder lokaler Storage

### 5.2 Spieler-Profil mit Stats
- **Dateien:** Neue Datei `ui/screens/ProfileScreen.kt`, neue Supabase-Tabellen `player_profiles`, `game_stats`
- **Was:** Persönliches Profil mit aggregierten Statistiken
- **Stats:**
  - Gesamtsiege / Spiele gespielt / Win-Rate
  - Durchschnittliche Bewertung (erhalten)
  - Schnellster Fang (Sekunden nach Spielstart)
  - Meistgespielte Kategorie
  - Längste Siegesserie
  - Anzahl vergebener 5-Sterne-Bewertungen
- **Umsetzung:**
  - Profil-Icon in der Navigation (HomeScreen oder Settings)
  - Stats werden nach jedem Spiel aktualisiert
  - Visuell: Stat-Cards mit Icons und animierten Zahlen
  - Avatar aus dem letzten Spiel als Profilbild
  - **Voraussetzung:** Persistente Spieler-Identität (Account-System oder Device-ID)

### 5.3 XP/Level-System
- **Dateien:** Neue Datei `data/XpSystem.kt`, Anpassung `ui/screens/ProfileScreen.kt`, `ui/screens/ResultsScreen.kt`
- **Was:** Erfahrungspunkte über Spiele hinweg, Levels mit Belohnungen
- **Umsetzung:**
  - XP-Quellen: Spiel gespielt (+50), Spiel gewonnen (+100), Achievement freigeschaltet (+75), Speed-Bonus erhalten (+25), Hohe Bewertung erhalten (+10 pro Stern)
  - Level-Kurve: Level 1 = 0 XP, Level 2 = 200 XP, Level 3 = 500 XP, ... (exponentiell)
  - Belohnungen: Kosmetisch — neue Profilrahmen-Farben, spezielle Titel ("Meisterfotograf", "Entdecker")
  - Im ResultsScreen: XP-Gain Animation nach der Punktezählung
  - Profil: XP-Fortschrittsbalken zum nächsten Level
  - **Abhängigkeit:** Account-System + Spieler-Profil (5.2)

---

## Phase 6: Saisonale Features

### 6.1 Saisonale Events
- **Dateien:** Neue Supabase-Tabelle `seasonal_events`, Anpassung `HomeScreen.kt`, `ui/theme/SeasonalThemes.kt`
- **Was:** Zeitlich begrenzte spezielle Kategorien und Themes
- **Event-Ideen:**
  - 🎃 Halloween (Oktober): Gruselige Kategorien, dunkles Lila/Orange Theme
  - 🎄 Weihnachten (Dezember): Festliche Kategorien, Rot/Grün/Gold Theme, Schneeflocken-Confetti
  - 🎆 Silvester (31.12.): Party-Kategorien, Feuerwerk-Animation statt Confetti
  - ☀️ Sommer-Special (Juli/August): Strand/Outdoor-Kategorien, helles Theme
  - 🌸 Frühlings-Event (April): Natur-Kategorien, Pastellfarben
- **Umsetzung:**
  - Backend liefert aktives Event (Start/End-Datum, spezielle Kategorien, Theme-Override)
  - HomeScreen: Event-Banner mit Countdown
  - Spezielle Event-Achievements (nur während Event freischaltbar)
  - Event-Kategorie-Pack automatisch verfügbar im CreateGameScreen

---

## Phase 7: UX & Accessibility

### 7.1 Quick-Rejoin
- **Dateien:** `App.kt`, `game/GameState.kt`, neue Datei `data/ActiveGameStore.kt`
- **Was:** Wenn die App geschlossen wird während ein Spiel läuft → beim Öffnen direkt "Zurück zum Spiel?"-Dialog statt HomeScreen
- **Umsetzung:**
  - Beim Spielstart `gameId` + `playerId` + `gameCode` in lokalen Storage (`DataStore`/`SharedPreferences`) persistieren
  - Bei App-Start prüfen: gibt es eine gespeicherte Session? → Supabase-Query ob Spiel noch `status = "running"` oder `"voting"` hat
  - Wenn ja: AlertDialog "Du hast ein laufendes Spiel. Zurück zum Spiel?" mit "Ja" → direkt zum GameScreen/ReviewScreen navigieren, "Nein" → Session löschen, HomeScreen zeigen
  - Bei Spielende Session aus Storage löschen
  - **Edge Case:** Spiel wurde beendet während App zu war → "Das Spiel ist bereits vorbei" → direkt zu Results navigieren

### 7.2 Haptic Patterns
- **Dateien:** `platform/HapticProvider.kt` (alle Plattformen), alle Screens mit Interaktionen
- **Was:** Unterschiedliche Vibrationsmuster für verschiedene Events
- **Muster:**
  - 📸 Foto aufgenommen: kurzer einzelner Tick (10ms)
  - ⭐ Stern vergeben: sanfter doppelter Tick (10ms-Pause-10ms)
  - 🏆 Spiel gewonnen: langer Erfolgs-Pattern (Buzz-Pause-Buzz-Pause-Buzz, aufsteigend)
  - 🎖️ Achievement freigeschaltet: spezieller Pattern (kurz-kurz-lang)
  - ⏰ Timer <10s: rhythmischer Herzschlag-Pattern pro Sekunde
  - ❌ Fehler: einzelner harter Buzz (50ms)
- **Umsetzung:**
  - `HapticProvider` um Pattern-basierte Methoden erweitern (aktuell vermutlich nur einfaches `vibrate()`)
  - Android: `VibrationEffect.createWaveform()` mit Timing-Arrays
  - iOS: `UIImpactFeedbackGenerator` mit verschiedenen Styles (.light, .medium, .heavy) + `UINotificationFeedbackGenerator`
  - Weiterhin über Settings-Toggle abschaltbar

---

## Phase 8: Neue Spielmodi

### 8.1 Bingo-Grid Modus (3x3 / 4x4 / 5x5)
- **Dateien:** `game/GameState.kt`, `ui/screens/CreateGameScreen.kt`, `ui/screens/GameScreen.kt`, `ui/screens/ResultsScreen.kt`, `network/GameRepository.kt`
- **Was:** Klassisches Bingo — Kategorien in einem Grid angeordnet, wer zuerst eine Reihe/Spalte/Diagonale fotografiert hat, ruft "Bingo!"
- **Umsetzung:**
  - Neues Feld auf Game: `mode: "classic" | "bingo"`, `bingo_size: 3 | 4 | 5`
  - CreateGameScreen: Mode-Auswahl (Classic / Bingo) mit Grid-Size Picker
  - Kategorien werden zufällig in ein NxN Grid verteilt (jeder Spieler bekommt dasselbe Grid)
  - GameScreen: Grid-Darstellung mit Zeilen/Spalten-Linien statt freiem Kategorie-Grid
  - Win-Condition: Erste vollständige Reihe, Spalte oder Diagonale → "BINGO!"-Animation + 30s Countdown für andere
  - Scoring: Bingo-Bonus (+50 Punkte) für den ersten Bingo, danach weiter spielen für Zusatzpunkte
  - Mehrere Bingos möglich (jeder weitere +25 Punkte)
  - **Komplexität:** Mittel — hauptsächlich UI-Änderung und neue Win-Condition-Logik

### 8.2 Elimination-Modus
- **Dateien:** `game/GameState.kt`, `network/GameRepository.kt`, `ui/screens/GameScreen.kt`, neue Datei `ui/screens/EliminationRoundScreen.kt`
- **Was:** Jede Runde fliegt der langsamste Spieler raus. Letzte 2 Spieler im Finale
- **Umsetzung:**
  - Neues Feld auf Game: `mode: "elimination"`, `current_round: Int`, `eliminated_players: List<String>`
  - Rundenbasiert: Jede Runde 2-3 Kategorien, kürzerer Timer (2-3 Minuten)
  - Nach jeder Runde: Zwischenscreen mit Ranking → letzter Platz wird eliminiert (dramatische Animation: Spieler-Avatar fällt runter / wird ausgegraut)
  - Eliminierte Spieler werden zu Zuschauern (können Fotos der verbleibenden Spieler sehen)
  - Finale: 2 Spieler, 1 Kategorie, 1 Minute — dramatisches 1v1
  - **Mindestens 4 Spieler** erforderlich
  - **Komplexität:** Hoch — neue Runden-Logik, Zuschauer-Modus, Zwischen-Screens

### 8.3 Blind-Modus
- **Dateien:** `game/GameState.kt`, `ui/screens/GameScreen.kt`, `network/GameRepository.kt`
- **Was:** Kategorien werden erst nach und nach enthüllt (alle 2 Minuten eine neue)
- **Umsetzung:**
  - Neues Feld auf Game: `mode: "blind"`, `reveal_interval_s: Int` (Standard: 120s)
  - Alle Kategorien werden bei Spielstart erstellt aber mit `revealed_at` Timestamp versehen
  - GameScreen zeigt nur bereits enthüllte Kategorien. Verdeckte als "?" mit Countdown bis Enthüllung
  - Enthüllungs-Animation: Karte dreht sich um (3D-Flip) mit Sound-Effekt
  - Strategie-Element: Schnell die aktuellen Kategorien abarbeiten oder auf neue warten?
  - **Komplexität:** Mittel — Timer-basierte Reveal-Logik + Animation

### 8.4 Sabotage-Modus
- **Dateien:** `game/GameState.kt`, `ui/screens/GameScreen.kt`, `network/GameRepository.kt`, neue Supabase-Tabelle `sabotages`
- **Was:** Jeder Spieler darf 1x eine Kategorie eines Gegners "sperren" — der muss dann eine Ersatzkategorie finden
- **Umsetzung:**
  - Jeder Spieler hat 1 Sabotage-Token pro Spiel
  - Button auf Gegner-Tab: "Sabotage!" → Auswahl welche Kategorie des Gegners gesperrt wird
  - Gesperrte Kategorie wird beim Ziel-Spieler rot markiert mit 🔒, kann nicht mehr fotografiert werden
  - Stattdessen erscheint eine zufällige Ersatzkategorie (aus einem Pool)
  - Notification an den sabotierten Spieler: "Deine Kategorie X wurde gesperrt! Neue Kategorie: Y"
  - Sabotage erst nach 25% der Spielzeit möglich (Schutzphase)
  - **Komplexität:** Mittel — neue Interaktion + DB-Tabelle + Ersatzkategorie-Logik

### 8.5 Zeitdruck-Runden
- **Dateien:** `game/GameState.kt`, `ui/screens/GameScreen.kt`, `network/GameRepository.kt`
- **Was:** Statt ein langer Timer → 5 kurze Runden à 2 Minuten, jede Runde 1-2 neue Kategorien
- **Umsetzung:**
  - Neues Feld: `mode: "blitz"`, `total_rounds: Int`, `current_round: Int`
  - Pro Runde: 1-2 Kategorien werden enthüllt, 2 Minuten Timer
  - Zwischen den Runden: 15s Pause mit Zwischenstand
  - Nicht geschaffte Kategorien verfallen (0 Punkte)
  - Am Ende: Gesamtbewertung aller Runden
  - Erzeugt mehr Druck und kürzere, intensivere Spielsessions
  - **Komplexität:** Mittel — Runden-Management + Zwischen-Screens

---

## Phase 9: Gameplay-Tweaks

### 9.1 Kategorie-Tausch
- **Dateien:** `ui/screens/GameScreen.kt`, `game/GameState.kt`, `network/GameRepository.kt`
- **Was:** Einmal pro Spiel eine Kategorie gegen eine zufällige neue tauschen
- **Umsetzung:**
  - "Tauschen"-Button (🔄) auf jeder noch nicht fotografierten Kategorie-Karte
  - Tap → Bestätigungs-Dialog: "Kategorie X tauschen? Du hast nur 1 Tausch!"
  - Backend: Zufällige Kategorie aus Pool wählen (nicht bereits im Spiel)
  - Animation: Alte Karte fliegt raus, neue dreht sich rein
  - Counter im UI: "1 Tausch übrig" → nach Nutzung ausgegraut
  - Getauschte Kategorie gilt nur für diesen Spieler (andere behalten ihre)

### 9.2 Goldene Kategorie
- **Dateien:** `game/GameState.kt`, `ui/screens/GameScreen.kt`, `network/GameRepository.kt`
- **Was:** Eine zufällige Kategorie gibt doppelte Punkte — wird erst nach 50% der Zeit enthüllt
- **Umsetzung:**
  - Bei Spielerstellung: eine Kategorie zufällig als `golden = true` markieren (in DB gespeichert, aber nicht an Clients gesendet)
  - Nach 50% der Spielzeit: Realtime-Event enthüllt die goldene Kategorie
  - Enthüllungs-Animation: Goldener Glitter-Effekt, Karte bekommt goldenen Rahmen (GradientGold)
  - Scoring: Sternbewertungen für diese Kategorie zählen doppelt
  - Strategisches Element: Lohnt es sich, die goldene Kategorie nochmal zu fotografieren?
  - UI: Goldenes "2x"-Badge auf der Karte

---

## Phase 10: Export & Integration

### 10.1 Clip-Export (Slideshow-Video)
- **Dateien:** Neue Datei `platform/VideoExporter.kt`, Anpassung `ui/screens/ResultsScreen.kt`
- **Was:** Automatisch aus allen Fotos eines Spiels ein kurzes Slideshow-Video generieren
- **Umsetzung:**
  - Aus allen Fotos des Spiels (sortiert nach Aufnahmezeit) eine Slideshow bauen
  - Pro Foto: 2s Anzeige mit Ken-Burns-Effekt (leichtes Zoom/Pan)
  - Übergänge: Cross-Fade zwischen Fotos
  - Overlay: Kategorie-Name + Spieler-Name + Sternbewertung pro Foto
  - Outro: Endstand mit Podium (statisches Bild, 3s)
  - Hintergrundmusik: Lizenzfreier Track (lokal gebundled, 2-3 Optionen)
  - **Plattform-spezifisch:**
    - Android: `MediaCodec` + `MediaMuxer` für MP4-Export
    - iOS: `AVFoundation` (`AVAssetWriter`)
    - Web: Canvas API + `MediaRecorder`
  - Export als MP4, Share via native Share-Sheet
  - **Komplexität:** Sehr hoch — plattformspezifische Video-Encoding-APIs

### 10.2 Deep Links
- **Dateien:** Plattform-spezifische Konfiguration (AndroidManifest.xml, Info.plist, Web-Routing), `App.kt`
- **Was:** `katchit://join/ABC123` — Link öffnet App direkt im Join-Screen mit vorausgefülltem Code
- **Umsetzung:**
  - **Android:** Intent-Filter in `AndroidManifest.xml` für Schema `katchit://` und HTTPS `katchit.app/join/*`
  - **iOS:** URL-Schema in `Info.plist` + Associated Domains für Universal Links
  - **Web:** Route `/join/:code` im Web-Router
  - App.kt: Bei Start Intent/URL parsen → wenn `/join/{code}` → direkt zu JoinGameScreen navigieren mit vorausgefülltem Code
  - Share-Funktionalität anpassen: Statt nur Text-Code auch Deep Link generieren
  - QR-Code generieren: `katchit://join/ABC123` als QR-Code auf dem Lobby-Screen anzeigen
  - **Komplexität:** Mittel — hauptsächlich Plattform-Konfiguration

---

## Empfohlene Reihenfolge

| Priorität | Feature | Aufwand | Impact |
|-----------|---------|---------|--------|
| ~~1~~ | ~~1.2 Foto-Thumbnails auf Karten~~ | ~~Klein~~ | ~~Hoch~~ | ✅ |
| ~~2~~ | ~~1.6 Animierte Punktezählung~~ | ~~Klein~~ | ~~Hoch~~ | ✅ |
| ~~3~~ | ~~1.7 Best Photo Highlight~~ | ~~Klein~~ | ~~Mittel~~ | ✅ |
| ~~4~~ | ~~1.5 Timer-Puls~~ | ~~Klein~~ | ~~Mittel~~ | ✅ |
| ~~5~~ | ~~1.4 Star-Rating Glow~~ | ~~Klein~~ | ~~Mittel~~ | ✅ |
| ~~6~~ | ~~1.1 Glassmorphism~~ | ~~Mittel~~ | ~~Mittel~~ | ✅ |
| 7 | 1.3 Parallax Results | Mittel | Mittel |
| 8 | 4.1 Kategorie-Packs | Mittel | Hoch |
| 9 | 2.3 Live-Feed | Mittel | Hoch |
| 10 | 2.2 Emoji-Reactions | Mittel | Mittel |
| 11 | 2.1 Share-Cards | Hoch | Sehr hoch |
| 12 | 4.2 Challenge-Kategorien | Mittel | Mittel |
| 13 | 3.1 Light Mode | Hoch | Mittel |
| 14 | 5.1 Achievements | Hoch | Hoch |
| 15 | 2.4 Foto-des-Tages | Mittel | Mittel |
| 16 | 5.2 Spieler-Profil | Hoch | Hoch |
| 17 | 4.3 Tägliche Challenge | Hoch | Sehr hoch |
| 18 | 3.2 Saisonale Themes | Mittel | Mittel |
| 19 | 4.5 Geofencing | Hoch | Mittel |
| 20 | 4.4 Team-Modus | Sehr hoch | Hoch |
| 21 | 5.3 XP/Level-System | Hoch | Hoch |
| 22 | 6.1 Saisonale Events | Hoch | Mittel |
| 23 | 7.1 Quick-Rejoin | Mittel | Sehr hoch |
| 24 | 7.2 Haptic Patterns | Klein | Mittel |
| 25 | 8.1 Bingo-Grid Modus | Mittel | Sehr hoch |
| 26 | 8.2 Elimination-Modus | Hoch | Hoch |
| 27 | 8.3 Blind-Modus | Mittel | Hoch |
| 28 | 8.4 Sabotage-Modus | Mittel | Hoch |
| 29 | 8.5 Zeitdruck-Runden | Mittel | Hoch |
| 30 | 9.1 Kategorie-Tausch | Klein | Mittel |
| 31 | 9.2 Goldene Kategorie | Klein | Hoch |
| 32 | 10.1 Clip-Export | Sehr hoch | Hoch |
| 33 | 10.2 Deep Links | Mittel | Hoch |
