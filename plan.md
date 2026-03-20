# KatchIt / GeoBingo — Verbesserungsplan

Dieser Plan beschreibt alle geplanten Verbesserungen, gruppiert nach Bereich und priorisiert in Phasen.

---

## Phase 1: UI Polish & Micro-Interactions

Kleine, isolierte Verbesserungen die einzeln umsetzbar sind und sofort spürbaren Impact haben.

### 1.1 Glassmorphism-Effekt auf GradientBorderCards
- **Datei:** `ui/theme/` (neue Modifier-Extension) + alle Screens die `GradientBorderCard` nutzen
- **Was:** Leichter Blur-Hintergrund (frosted glass) hinter den Karten für mehr visuelle Tiefe
- **Umsetzung:** `Modifier.blur()` mit semi-transparentem Surface-Background kombinieren. Fallback für Plattformen ohne Blur-Support (einfach halbtransparenter Background)
- **Achtung:** `blur()` ist ab Compose 1.6+ verfügbar. Auf älteren Android-Versionen (<12) ggf. kein nativer Blur — Fallback einbauen

### 1.2 Foto-Thumbnails auf Kategorie-Karten im GameScreen
- **Datei:** `ui/screens/GameScreen.kt`
- **Was:** Statt nur Checkmark nach Foto-Aufnahme das eigene Foto als Thumbnail auf der Kategorie-Karte anzeigen
- **Umsetzung:** Aus `gameState.photos[myPlayerId][categoryId]` das ByteArray laden und als `Image` in der Karte rendern. Thumbnail skaliert auf Kartengröße mit `ContentScale.Crop` und leichtem Overlay für den Kategorie-Text
- **Layout:** Foto füllt die Karte, Kategorie-Name als Overlay unten mit Gradient-Scrim. Kleines Checkmark-Icon oben rechts als Badge

### 1.3 Parallax-Scrolling auf ResultsScreen
- **Datei:** `ui/screens/ResultsScreen.kt`
- **Was:** Podium bleibt oben fixiert (sticky header), Galerie und Rankings scrollen darunter
- **Umsetzung:** `LazyColumn` mit `stickyHeader {}` für den Podium-Bereich. Alternativ `nestedScroll` mit `TopAppBar`-Pattern wo das Podium beim Scrollen langsamer mitscrollt (Parallax-Faktor ~0.5)

### 1.4 Star-Rating Glow-Effekt
- **Datei:** `ui/screens/ReviewScreen.kt`
- **Was:** Sterne glühen beim Hover/Drag kurz auf mit Partikel-Effekt
- **Umsetzung:** Beim Auswählen eines Sterns: kurzer Scale-Up (1.0 → 1.3 → 1.0), goldener Glow via `drawBehind` mit `drawCircle` und radialem Gradient (Amber → Transparent). Optional: 3-5 kleine Partikel die nach oben/außen fliegen (ähnlich dem bestehenden Confetti-System, aber kleiner und kürzer)

### 1.5 Timer-Puls bei <30 Sekunden
- **Datei:** `ui/screens/GameScreen.kt`
- **Was:** Timer-Hintergrund pulsiert subtil wenn weniger als 30 Sekunden verbleiben
- **Umsetzung:** `animateFloatAsState` für Alpha-Wert des Timer-Backgrounds (0.3 → 0.7 → 0.3) mit `infiniteRepeatable` und `LinearEasing`. Pulsfrequenz steigt: >20s = 1.5s Zyklus, >10s = 1.0s, <10s = 0.5s. Dazu leichter Scale-Puls (1.0 → 1.02 → 1.0)

### 1.6 Animierte Punktezählung auf ResultsScreen
- **Datei:** `ui/screens/ResultsScreen.kt`
- **Was:** Punkte zählen von 0 hoch (Counter-Animation) statt sofort den Endwert zu zeigen
- **Umsetzung:** `Animatable<Float>` von 0f zum Endwert mit `tween(durationMillis = 1500, easing = FastOutSlowInEasing)`. Anzeige als `animatedValue.toInt()`. Staggered pro Spieler (Platz 3 zuerst, dann 2, dann 1). Sternbewertung und Speed-Bonus separat hochzählen lassen

### 1.7 "Best Photo" Highlight
- **Datei:** `ui/screens/ResultsScreen.kt`
- **Was:** Das Foto mit der höchsten Durchschnittsbewertung bekommt goldenen Rahmen + Krone
- **Umsetzung:** Aus `allVotes` die durchschnittliche Bewertung pro Foto berechnen, höchstes identifizieren. In der Galerie: goldener `GradientBorder` (GradientGold) + Kronen-Emoji (👑) als Badge oben. "Best Photo"-Label unter dem Foto. Eigene Sektion über der normalen Galerie

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

## Empfohlene Reihenfolge

| Priorität | Feature | Aufwand | Impact |
|-----------|---------|---------|--------|
| 1 | 1.2 Foto-Thumbnails auf Karten | Klein | Hoch |
| 2 | 1.6 Animierte Punktezählung | Klein | Hoch |
| 3 | 1.7 Best Photo Highlight | Klein | Mittel |
| 4 | 1.5 Timer-Puls | Klein | Mittel |
| 5 | 1.4 Star-Rating Glow | Klein | Mittel |
| 6 | 1.1 Glassmorphism | Mittel | Mittel |
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
