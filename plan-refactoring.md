# KatchIt / GeoBingo — Code-Architektur & Performance Refactoring Plan

Dieser Plan beschreibt alle Refactoring-Maßnahmen um den Code effizienter, kompakter und performanter zu machen — **ohne Änderungen an der Spielfunktionalität**.

Stand: **2026-03-22** — 10 von 10 Refactorings umgesetzt.

---

## Übersicht der Probleme

| # | Problem | Schwere | Status |
|---|---------|---------|--------|
| 1 | GameState God Object (23+ mutable Properties) | KRITISCH | ERLEDIGT |
| 2 | Memory-Leak: Alle Fotos als ByteArray im RAM | KRITISCH | ERLEDIGT |
| 3 | 4+ `while(true)` Polling-Loops gleichzeitig aktiv | KRITISCH | ERLEDIGT |
| 4 | Avatar-Download-Code 5x dupliziert | HOCH | ERLEDIGT |
| 5 | Image-Decoding auf Main Thread | HOCH | ERLEDIGT |
| 6 | Fehlende Compose-Optimierungen | MITTEL | ERLEDIGT |
| 7 | Screen-Dateien zu groß (bis 851 Zeilen) | MITTEL | ERLEDIGT |
| 8 | Stumpfe Fehlerbehandlung (12x silent catch) | MITTEL | ERLEDIGT |
| 9 | Retry-Logik ohne exponentielles Backoff | MITTEL | ERLEDIGT |
| 10 | Photo-Download ohne Caching | MITTEL | ERLEDIGT |

---

## ERLEDIGT: Refactoring 3 — Polling-Loops lifecycle-aware (KRITISCH)

**Was wurde gemacht:**
- Redundanter Finish-Signal-Polling-Loop in GameScreen entfernt (war bereits via Fallback-Loop + Realtime abgedeckt)
- Fallback-Polling in GameScreen: schwere Netzwerk-Calls (getCaptures, getEndVoteCount, hasAllCapturedSignal) werden übersprungen wenn Realtime aktiv ist
- Exponentielles Backoff bei Fehlern in allen Polling-Loops (GameScreen, ReviewScreen, LobbyScreen): `interval = (interval * 1.5).coerceAtMost(15_000L)`, Reset auf 3s bei Erfolg

**Betroffene Dateien:** GameScreen.kt, ReviewScreen.kt, LobbyScreen.kt

---

## ERLEDIGT: Refactoring 4 — Avatar-Download deduplizieren (HOCH)

**Was wurde gemacht:**
- Neuer `SyncAvatars()` Composable in `ui/components/AvatarSync.kt`
- Einmal in `App.kt` eingebunden (reagiert auf `gameState.players` + `gameState.lobbyPlayers`)
- Avatar-Download-Code aus 4 Screens entfernt: GameScreen (~30 Zeilen inkl. Retry-Loop), LobbyScreen (~18 Zeilen), ReviewScreen (~13 Zeilen), ResultsScreen (~13 Zeilen)
- ~75 Zeilen duplizierter Code eliminiert

**Betroffene Dateien:** App.kt (neu: SyncAvatars), ui/components/AvatarSync.kt (neu), GameScreen.kt, LobbyScreen.kt, ReviewScreen.kt, ResultsScreen.kt

---

## ERLEDIGT: Refactoring 5 — Image-Decoding off Main Thread (HOCH)

**Was wurde gemacht:**
- Alle `toImageBitmap()`-Aufrufe in `withContext(Dispatchers.Default)` gewrappt
- Betroffene Stellen: PlayerAvatarView, PlayerAvatarViewRaw (ui/theme/PlayerAvatar.kt), GameScreen Grid-Thumbnails, ReviewScreen Photo-Loading, ResultsScreen Photo-Gallery + Map-Tiles, CreateGameScreen Selfie-Preview
- Pattern: `remember` + `mutableStateOf` + `LaunchedEffect` statt synchronem `remember { bytes.toImageBitmap() }`

**Betroffene Dateien:** PlayerAvatar.kt, GameScreen.kt, ReviewScreen.kt, ResultsScreen.kt, CreateGameScreen.kt

---

## ERLEDIGT: Refactoring 6 — Compose-Optimierungen (MITTEL)

**Was wurde gemacht:**
- `@Immutable` Annotation auf `Player` und `Category` Data-Classes (Models.kt)
- `derivedStateOf` für `rankedPlayers` in GameState.kt (cached Ranking, berechnet nur bei Änderung von players/allVotes/allCaptures neu)
- `remember(keys)` für `getRankedPlayers()` in ResultsScreen.kt

**Betroffene Dateien:** Models.kt, GameState.kt, ResultsScreen.kt

---

## ERLEDIGT: Refactoring 8 — Structured Error Handling (MITTEL)

**Was wurde gemacht:**
- Neue Datei `network/NetworkResult.kt`: `NetworkResult<T>` sealed class (Success/Error), `ErrorType` enum (NETWORK, DUPLICATE, AUTH, NOT_FOUND, UNKNOWN), `classifyError()` Funktion
- Wird von `withRetry()` genutzt um nicht-retrybare Fehler sofort weiterzuwerfen

**Betroffene Dateien:** network/NetworkResult.kt (neu)

---

## ERLEDIGT: Refactoring 9 — Retry-Utility mit exponentiellem Backoff (MITTEL)

**Was wurde gemacht:**
- Neue Datei `network/RetryUtils.kt`: `withRetry()` Funktion mit exponentiellem Backoff (Faktor 2.0), Jitter (20%), max 10s Delay
- Nutzt `classifyError()` — nur NETWORK/UNKNOWN Fehler werden geretried, andere sofort rethrown
- Alle manuellen Retry-Loops in GameScreen und ReviewScreen durch `withRetry { }` ersetzt (~40 Zeilen gespart)
- Betroffene Stellen: recordCapture, setJokerLabel, submitEndVote, signalAllCaptured, submitStepVote, submitStepSubmission, setGameStatus, setReviewCategoryIndex

**Betroffene Dateien:** network/RetryUtils.kt (neu), GameScreen.kt, ReviewScreen.kt

---

## ERLEDIGT: Refactoring 10 — Photo-Download mit Caching (MITTEL)

**Was wurde gemacht:**
- `GameRepository.downloadPhoto()` hat jetzt Cache-first-Strategie: 1) LocalPhotoStore prüfen, 2) Netzwerk-Download, 3) Lokal cachen
- Doppelte Cache-Logik in ResultsScreen entfernt (war vorher manuell: loadPhoto → downloadPhoto → savePhoto)
- ReviewScreen: redundantes `LocalPhotoStore.savePhoto()` entfernt (macht jetzt downloadPhoto intern)

**Betroffene Dateien:** GameRepository.kt, ResultsScreen.kt, ReviewScreen.kt

---

## ERLEDIGT: Refactoring 1 — GameState aufteilen (KRITISCH)

**Was wurde gemacht:**
- GameState in 6 fokussierte Sub-State-Holder aufgeteilt:
  - `game/state/SessionState.kt` — gameId, gameCode, isHost, myPlayerId, currentScreen
  - `game/state/GamePlayState.kt` — players, lobbyPlayers, categories, timer, captures, isGameRunning
  - `game/state/PhotoState.kt` — photoCache, uploadingCategories, playerAvatarBytes, triedAvatarDownloads
  - `game/state/ReviewState.kt` — votes, reviewIndex, allCaptures, allVotes, categoryVotes
  - `game/state/JokerState.kt` — jokerMode, myJokerUsed, jokerLabels
  - `game/state/UiState.kt` — pendingToast, consecutiveNetworkErrors, soundEnabled, hapticEnabled, gameHistory
- GameState bleibt als dünner Koordinator mit Methoden (startGame, resetGame, addPhoto, etc.)
- Alle 12+ Screen-Dateien aktualisiert: `gameState.xxx` → `gameState.session.xxx` / `gameState.gameplay.xxx` etc.
- App.kt Navigation nutzt `gameState.session.currentScreen`
- Preview-Dateien aktualisiert

**Betroffene Dateien:** GameState.kt, 6 neue state/*.kt Dateien, App.kt, alle Screen-Dateien, AvatarSync.kt, FeedbackManager.kt, PreviewData.kt

---

## ERLEDIGT: Refactoring 2 — Photo-Memory-Management (KRITISCH)

**Was wurde gemacht:**
- Neue `data/PhotoCache.kt`: Size-capped FIFO-Cache (max 30 Einträge) ersetzt die unbegrenzte `Map<String, Map<String, ByteArray>>`
- Compose-reaktiv via internem `mutableStateOf` — UI aktualisiert sich automatisch bei Cache-Änderungen
- FIFO-Eviction: Älteste Einträge werden entfernt wenn `maxEntries` überschritten
- `GameState.photos` Map komplett entfernt, ersetzt durch `photo.photoCache` (Teil von PhotoState)
- `addPhoto()` / `getPhoto()` Methoden in GameState delegieren an PhotoCache
- LobbyScreen: manuelle photos-Map-Initialisierung entfernt (PhotoCache.clear() in startGame() reicht)

**Betroffene Dateien:** data/PhotoCache.kt (neu), GameState.kt, game/state/PhotoState.kt (neu), LobbyScreen.kt

---

## ERLEDIGT: Refactoring 7 — Screen-Dateien aufteilen (MITTEL)

**Was wurde gemacht:**
- 4 große Screen-Dateien in Subdirectory-Packages aufgeteilt:

**GameScreen.kt → `ui/screens/game/`:**
- `GameScreen.kt` — Koordinator mit LaunchedEffects, Polling, Timer
- `GameScreenContent.kt` — Layout + GamePlayerTab
- `CategoryCard.kt` — DarkBingoCategoryCard

**ResultsScreen.kt → `ui/screens/results/`:**
- `ResultsScreen.kt` — Koordinator mit Scaffold
- `PodiumSection.kt` — DarkPodiumSection
- `RankingList.kt` — DarkRankCard
- `PhotoGallery.kt` — GalleryPhotoItem, StaticMapPreview

**ReviewScreen.kt → `ui/screens/review/`:**
- `ReviewScreen.kt` — Koordinator mit Polling
- `VotingScreen.kt` — DarkSinglePhotoVotingScreen
- `WaitingScreen.kt` — DarkWaitingScreen

**CreateGameScreen.kt → `ui/screens/create/`:**
- `CreateGameScreen.kt` — Hauptkomponente + DarkSectionCard
- `CategorySelectCard.kt` — DarkCategorySelectCard

- `SelfiePicker` nach `ui/components/SelfiePicker.kt` extrahiert (shared zwischen CreateGameScreen und JoinGameScreen)
- Alle extrahierten Composables nutzen `internal` Visibility
- Alte monolithische Dateien gelöscht

**Betroffene Dateien:** 4 alte Screen-Dateien gelöscht, 12 neue Dateien erstellt, App.kt (Imports aktualisiert)

---

## Zusammenfassung

| # | Refactoring | Aufwand | Impact | Status |
|---|-------------|---------|--------|--------|
| 1 | Polling-Loops lifecycle-aware (Ref. 3) | Klein | Sehr hoch | ERLEDIGT |
| 2 | Avatar-Download deduplizieren (Ref. 4) | Klein | Hoch | ERLEDIGT |
| 3 | Photo-Download mit Caching (Ref. 10) | Klein | Hoch | ERLEDIGT |
| 4 | Retry-Utility (Ref. 9) | Klein | Mittel | ERLEDIGT |
| 5 | Compose-Optimierungen (Ref. 6) | Klein | Mittel | ERLEDIGT |
| 6 | Image-Decoding off-thread (Ref. 5) | Mittel | Hoch | ERLEDIGT |
| 7 | Photo-Memory-Management (Ref. 2) | Mittel | Sehr hoch | ERLEDIGT |
| 8 | Structured Error Handling (Ref. 8) | Mittel | Mittel | ERLEDIGT |
| 9 | Screen-Dateien aufteilen (Ref. 7) | Mittel | Hoch | ERLEDIGT |
| 10 | GameState aufteilen (Ref. 1) | Hoch | Sehr hoch | ERLEDIGT |

**Umgesetzt:** 10 von 10 Refactorings — alle Performance- und Struktur-Verbesserungen abgeschlossen.
