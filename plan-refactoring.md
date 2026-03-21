# KatchIt / GeoBingo — Code-Architektur & Performance Refactoring Plan

Dieser Plan beschreibt alle Refactoring-Maßnahmen um den Code effizienter, kompakter und performanter zu machen — **ohne Änderungen an der Spielfunktionalität**.

---

## Übersicht der Probleme

| # | Problem | Schwere | Betroffene Dateien |
|---|---------|---------|-------------------|
| 1 | GameState God Object (23+ mutable Properties) | KRITISCH | GameState.kt, alle Screens |
| 2 | Memory-Leak: Alle Fotos als ByteArray im RAM | KRITISCH | GameState.kt |
| 3 | 4+ `while(true)` Polling-Loops gleichzeitig aktiv | KRITISCH | GameScreen.kt, ReviewScreen.kt, LobbyScreen.kt |
| 4 | Avatar-Download-Code 5x dupliziert | HOCH | 5 Screen-Dateien |
| 5 | Image-Decoding auf Main Thread | HOCH | GameScreen.kt, ReviewScreen.kt, ResultsScreen.kt |
| 6 | Fehlende Compose-Optimierungen | MITTEL | GameState.kt, Models.kt |
| 7 | Screen-Dateien zu groß (bis 851 Zeilen) | MITTEL | GameScreen.kt, ResultsScreen.kt, CreateGameScreen.kt |
| 8 | Stumpfe Fehlerbehandlung (12x silent catch) | MITTEL | GameRepository.kt |
| 9 | Retry-Logik ohne exponentielles Backoff | MITTEL | GameScreen.kt |
| 10 | Photo-Download ohne Caching | MITTEL | GameRepository.kt, ReviewScreen.kt |

---

## Refactoring 1: GameState aufteilen (KRITISCH)

### Problem
`GameState.kt` ist ein monolithisches God Object mit **23+ `mutableStateOf` Properties** aus 8 verschiedenen Domänen. Jede Änderung an irgendeiner Property triggert Recomposition in **allen** Screens die `gameState` beobachten.

**Aktueller Zustand** (`GameState.kt` Zeile 34-122):
```kotlin
class GameState {
    var currentScreen by mutableStateOf(Screen.HOME)
    var soundEnabled by mutableStateOf(true)          // Settings
    var hapticEnabled by mutableStateOf(true)          // Settings
    var gameId by mutableStateOf<String?>(null)        // Multiplayer
    var gameCode by mutableStateOf<String?>(null)      // Multiplayer
    var isHost by mutableStateOf(false)                // Multiplayer
    var myPlayerId by mutableStateOf<String?>(null)    // Multiplayer
    var lobbyPlayers by mutableStateOf(...)            // Multiplayer
    var players by mutableStateOf(...)                 // Game Setup
    var selectedCategories by mutableStateOf(...)      // Game Setup
    var gameDurationMinutes by mutableStateOf(15)      // Game Setup
    var timeRemainingSeconds by mutableStateOf(0)      // Game Runtime
    var isGameRunning by mutableStateOf(false)         // Game Runtime
    var captures by mutableStateOf(...)                // Captures
    var photos by mutableStateOf(...)                  // Photos (HEAVY!)
    var votes by mutableStateOf(...)                   // Votes
    var reviewCategoryIndex by mutableStateOf(0)       // Review
    var allCaptures by mutableStateOf(...)             // Review
    var allVotes by mutableStateOf(...)                // Review
    var jokerMode by mutableStateOf(false)             // Joker
    var playerAvatarBytes by mutableStateOf(...)       // Network/Cache
    // ... und noch mehr
}
```

### Lösung
Aufteilen in **6 fokussierte State-Holder**, jeder nur für seinen Bereich zuständig:

**Neue Dateistruktur:**
```
game/
├── GameState.kt              ← Bleibt als Koordinator (dünn), hält Referenzen auf Sub-States
├── state/
│   ├── SessionState.kt       ← gameId, gameCode, isHost, myPlayerId, currentScreen
│   ├── GamePlayState.kt      ← players, categories, timer, captures, isGameRunning
│   ├── PhotoState.kt         ← photos (ByteArray), uploadingCategories, photo-cache-Logik
│   ├── ReviewState.kt        ← votes, reviewIndex, allCaptures, allVotes, submissions
│   ├── JokerState.kt         ← jokerMode, myJokerUsed, jokerLabels
│   └── UiState.kt            ← pendingToast, consecutiveNetworkErrors, settings
```

**Beispiel `SessionState.kt`:**
```kotlin
class SessionState {
    var currentScreen by mutableStateOf(Screen.HOME)
    var gameId by mutableStateOf<String?>(null)
    var gameCode by mutableStateOf<String?>(null)
    var isHost by mutableStateOf(false)
    var myPlayerId by mutableStateOf<String?>(null)
    var lobbyPlayers by mutableStateOf(listOf<PlayerDto>())
}
```

**Beispiel `PhotoState.kt`:**
```kotlin
class PhotoState {
    // LRU Cache statt unbegrenzter Map (siehe Refactoring 2)
    private val photoCache = LruCache<String, ByteArray>(maxSize = 20)
    var uploadingCategories by mutableStateOf(setOf<String>())

    fun addPhoto(playerId: String, categoryId: String, bytes: ByteArray) { ... }
    fun getPhoto(playerId: String, categoryId: String): ByteArray? = photoCache.get("$playerId-$categoryId")
}
```

**GameState wird zum dünnen Koordinator:**
```kotlin
class GameState {
    val session = SessionState()
    val gameplay = GamePlayState()
    val photos = PhotoState()
    val review = ReviewState()
    val joker = JokerState()
    val ui = UiState()

    // Delegiert nur noch
    fun resetGame() {
        session.reset()
        gameplay.reset()
        photos.clear()
        review.reset()
        joker.reset()
    }
}
```

### Betroffene Dateien
- `GameState.kt` — komplett umstrukturieren
- Alle 12 Screen-Dateien — `gameState.xxx` → `gameState.session.xxx` bzw. `gameState.gameplay.xxx`
- `App.kt` — Navigation nutzt `gameState.session.currentScreen`

### Impact
- Screens die nur `ReviewState` brauchen (ReviewScreen) recomponieren nicht mehr wenn sich `timeRemainingSeconds` ändert
- Screens die nur `SessionState` brauchen (HomeScreen, SettingsScreen) recomponieren nicht mehr bei Gameplay-Änderungen
- GameScreen recomponiert nicht mehr bei Review-State-Änderungen

---

## Refactoring 2: Photo-Memory-Management (KRITISCH)

### Problem
**`GameState.kt` Zeile 82:**
```kotlin
var photos by mutableStateOf(mapOf<String, Map<String, ByteArray>>())
```

Alle Fotos aller Spieler werden gleichzeitig im RAM gehalten. Bei 5 Spielern × 16 Kategorien × ~500KB pro Foto = **~40 MB RAM** nur für Fotos. Dazu kommt:

- **Zeile 116:** `playerAvatarBytes` — weitere ByteArrays im RAM
- **`clearGameplayState()` (Zeile 279-304):** `photos` wird geleert, aber `playerAvatarBytes` **nicht** beim nächsten Spiel weitergetragen → Memory-Leak über Spiele hinweg

### Lösung

**A) LRU-Cache für Fotos implementieren:**

Neue Datei `data/PhotoCache.kt`:
```kotlin
class PhotoCache(private val maxEntries: Int = 20) {
    private val cache = LinkedHashMap<String, ByteArray>(maxEntries, 0.75f, true)

    fun put(key: String, bytes: ByteArray) {
        if (cache.size >= maxEntries) {
            val oldest = cache.keys.first()
            cache.remove(oldest)
        }
        cache[key] = bytes
    }

    fun get(key: String): ByteArray? = cache[key]

    fun clear() {
        cache.clear()
    }

    val sizeBytes: Long get() = cache.values.sumOf { it.size.toLong() }
}
```

**B) Thumbnails statt Vollbilder im RAM:**

Aktuell wird in `GameScreen.kt` Zeile 640 das volle ByteArray dekodiert:
```kotlin
val thumbnail: ImageBitmap? = remember(photoBytes) { photoBytes?.toImageBitmap() }
```

Besser: Thumbnails in reduzierter Auflösung (200×200px) vorberechnen und nur diese cachen. Vollbilder nur on-demand laden (z.B. für Fullscreen-Ansicht).

**C) `clearGameplayState()` fixen:**

Aktuelle lückenhafte Cleanup-Logik (`GameState.kt` Zeile 279-304) — `playerAvatarBytes` wird zwar geleert (Zeile 301), aber die photos-Map könnte bei einem Crash oder Race-Condition immer noch Daten halten. Zusätzlich `photoCache.clear()` explizit aufrufen.

### Betroffene Dateien
- Neue Datei: `data/PhotoCache.kt`
- `GameState.kt` → `PhotoState.kt` — photos-Map durch PhotoCache ersetzen
- `GameScreen.kt` — Thumbnail-Rendering statt Full-Size
- `ReviewScreen.kt` — Photo-Loading mit Cache-First-Strategie
- `ResultsScreen.kt` — Photo-Loading mit Cache-First-Strategie

---

## Refactoring 3: Polling-Loops lifecycle-aware machen (KRITISCH)

### Problem
Aktuell laufen **4+ infinite `while(true)` Loops** gleichzeitig mit fixen Intervallen:

**GameScreen.kt Zeile 252-300 — Fallback-Polling:**
```kotlin
LaunchedEffect(gameId) {
    while (true) {          // ← Läuft EWIG
        delay(3_000)        // ← Fixer Intervall, kein Backoff
        val game = GameRepository.getGameById(gameId)          // Netzwerk-Call 1
        val allCaptures = GameRepository.getCaptures(gameId)   // Netzwerk-Call 2
        val count = GameRepository.getEndVoteCount(gameId)     // Netzwerk-Call 3
        GameRepository.hasAllCapturedSignal(gameId)            // Netzwerk-Call 4
    }
}
```

**GameScreen.kt Zeile 354-364 — Finish-Signal-Polling (REDUNDANT!):**
```kotlin
LaunchedEffect(gameId) {
    while (gameState.isGameRunning && ...) {
        delay(2_000)
        GameRepository.hasAllCapturedSignal(gameId)   // BEREITS im Loop oben abgefragt!
    }
}
```

**ReviewScreen.kt Zeile 155-171 — Game-Status-Polling:**
```kotlin
LaunchedEffect(gameId) {
    while (true) {
        delay(3_000)
        GameRepository.getGameById(gameId)
    }
}
```

**ReviewScreen.kt Zeile 579-591 — Vote-Submission-Polling:**
```kotlin
LaunchedEffect(stepKey) {
    while (true) {
        GameRepository.getVoteSubmissionCount(gameId, stepKey)
        delay(1_500)   // Alle 1.5s!
    }
}
```

**Geschätzter Netzwerk-Traffic pro 15-Minuten-Spiel:**
- Fallback-Loop: 300 Zyklen × 4 Calls = **1.200 Requests**
- Finish-Signal-Loop: 450 Zyklen × 1 Call = **450 Requests** (REDUNDANT)
- Review-Loops: Variabel, aber ~200-400 Requests
- **Total: ~1.800-2.000 unnötige Requests** pro Spiel

### Lösung

**A) Realtime als primärer Kanal, Polling nur als Fallback:**

```kotlin
// Statt:
LaunchedEffect(gameId) {
    while (true) {
        delay(3_000)
        // 4 Netzwerk-Calls ...
    }
}

// Besser:
LaunchedEffect(gameId) {
    if (gameId == null) return@LaunchedEffect
    // Nur wenn Realtime fehlschlägt, starte Fallback-Polling
    val realtimeActive = realtime != null
    if (realtimeActive) return@LaunchedEffect  // Realtime kümmert sich!

    // Fallback mit exponentiellem Backoff
    var interval = 3_000L
    while (true) {
        delay(interval)
        try {
            // Nur EIN kombinierter Netzwerk-Call oder gebatchte Calls
            val game = GameRepository.getGameById(gameId)
            // ... verarbeiten
            interval = 3_000L  // Reset bei Erfolg
        } catch (e: Exception) {
            interval = (interval * 1.5).toLong().coerceAtMost(15_000L)  // Backoff bis 15s
        }
    }
}
```

**B) Redundanten Finish-Signal-Loop entfernen:**

Der Loop in Zeile 354-364 ist komplett redundant — derselbe Check passiert bereits im Fallback-Loop (Zeile 286-292) UND via Realtime (Zeile 234-237). Einfach löschen.

**C) Vote-Submission-Polling optimieren:**

Statt alle 1.5s pollen, Realtime `voteSubmissionInserts` Flow nutzen:
```kotlin
LaunchedEffect(stepKey) {
    // Realtime-basiert
    realtime?.voteSubmissionInserts?.collect { submission ->
        if (submission.category_id == stepKey) {
            submittedCount++
            if (submittedCount >= totalPlayers) onReadyToAdvance()
        }
    }
}
// Einmaliger Fallback-Fetch beim Eintritt
LaunchedEffect(stepKey) {
    submittedCount = GameRepository.getVoteSubmissionCount(gameId, stepKey)
}
```

### Betroffene Dateien
- `GameScreen.kt` — Fallback-Loop verschlanken, Finish-Signal-Loop entfernen
- `ReviewScreen.kt` — Polling durch Realtime ersetzen, Fallback vereinfachen
- `LobbyScreen.kt` — Polling durch Realtime ersetzen

### Impact
- **~80% weniger Netzwerk-Requests** pro Spiel
- Deutlich weniger Batterieverbrauch
- Weniger State-Thrashing (weniger Recompositions durch identische State-Updates)

---

## Refactoring 4: Avatar-Download deduplizieren (HOCH)

### Problem
Identischer Avatar-Download-Code existiert in **5 verschiedenen Screens**:

**GameScreen.kt Zeile 303-315:**
```kotlin
LaunchedEffect(gameState.players) {
    gameState.players
        .filter { it.id !in gameState.playerAvatarBytes && it.id !in gameState.triedAvatarDownloads }
        .forEach { player ->
            scope.launch {
                gameState.triedAvatarDownloads = gameState.triedAvatarDownloads + player.id
                val bytes = GameRepository.downloadAvatarPhoto(player.id)
                if (bytes != null) {
                    gameState.playerAvatarBytes = gameState.playerAvatarBytes + (player.id to bytes)
                }
            }
        }
}
```

Exakt derselbe Code (Copy-Paste) in:
- `LobbyScreen.kt` Zeile 68-85
- `ReviewScreen.kt` Zeile 65-77
- `ResultsScreen.kt` Zeile 140-152
- Plus ein **zusätzlicher redundanter Retry-Loop** in `GameScreen.kt` Zeile 317-334 der 25 Sekunden lang pollt

### Lösung

**Einen einzigen `@Composable` Utility erstellen:**

Neue Datei `ui/components/AvatarSync.kt`:
```kotlin
@Composable
fun SyncAvatars(gameState: GameState) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(gameState.players, gameState.lobbyPlayers) {
        val allPlayers = (gameState.players + gameState.lobbyPlayers.map { it.toPlayer() })
            .distinctBy { it.id }
        allPlayers
            .filter { it.id !in gameState.playerAvatarBytes && it.id !in gameState.triedAvatarDownloads }
            .forEach { player ->
                scope.launch {
                    gameState.triedAvatarDownloads = gameState.triedAvatarDownloads + player.id
                    val bytes = GameRepository.downloadAvatarPhoto(player.id)
                    if (bytes != null) {
                        gameState.playerAvatarBytes = gameState.playerAvatarBytes + (player.id to bytes)
                    }
                }
            }
    }
}
```

**Einmal in `App.kt` einbinden** statt in jedem Screen:
```kotlin
@Composable
fun App() {
    KatchItTheme {
        val gameState = remember { GameState() }
        SyncAvatars(gameState)  // Einmal für alle Screens
        // ... Screen-Navigation
    }
}
```

**Redundanten Retry-Loop entfernen** (`GameScreen.kt` Zeile 317-334).

### Betroffene Dateien
- Neue Datei: `ui/components/AvatarSync.kt`
- `App.kt` — `SyncAvatars()` einbinden
- `GameScreen.kt` — 30 Zeilen Avatar-Code entfernen (Zeile 303-334)
- `LobbyScreen.kt` — ~18 Zeilen entfernen
- `ReviewScreen.kt` — ~13 Zeilen entfernen
- `ResultsScreen.kt` — ~13 Zeilen entfernen

### Impact
- **~75 Zeilen duplizierter Code** eliminiert
- Ein einziger Ort für Avatar-Logik (leichter wartbar)
- Keine redundanten Retry-Loops mehr

---

## Refactoring 5: Image-Decoding off Main Thread (HOCH)

### Problem
`toImageBitmap()` wird direkt in Composables aufgerufen — das dekodiert JPEG-Bytes auf dem **Main Thread**:

**GameScreen.kt Zeile 640:**
```kotlin
val thumbnail: ImageBitmap? = remember(photoBytes) { photoBytes?.toImageBitmap() }
```
- Wird für **jedes** Grid-Item aufgerufen (16+ Items)
- Full-Resolution-Decoding (kein Downsampling)
- Bei schnellem Scrollen: UI-Freeze möglich

**ReviewScreen.kt Zeile 597-610:**
```kotlin
LaunchedEffect(capture.id) {
    loading = true
    var bytes = LocalPhotoStore.loadPhoto(...)
    if (bytes == null) bytes = GameRepository.downloadPhoto(...)
    photo = bytes?.toImageBitmap()  // Main Thread Decoding
    loading = false
}
```

**ResultsScreen.kt** — Ähnliches Pattern für Galerie-Fotos.

### Lösung

**A) Decoding in Background-Coroutine:**

```kotlin
// Statt:
val thumbnail: ImageBitmap? = remember(photoBytes) { photoBytes?.toImageBitmap() }

// Besser:
var thumbnail by remember(photoBytes) { mutableStateOf<ImageBitmap?>(null) }
LaunchedEffect(photoBytes) {
    thumbnail = withContext(Dispatchers.Default) {
        photoBytes?.toImageBitmap()
    }
}
```

**B) Thumbnail-Utility mit Größenlimit:**

Neue Funktion in `platform/ImageUtils.kt`:
```kotlin
expect fun ByteArray.toThumbnailBitmap(maxSize: Int = 200): ImageBitmap?
```

Plattform-spezifische Implementierung die beim Dekodieren direkt downsamplet:
- **Android:** `BitmapFactory.Options.inSampleSize`
- **iOS:** `CGImageSourceCreateThumbnailAtIndex` mit `kCGImageSourceThumbnailMaxPixelSize`
- **Web:** Canvas-basiertes Resize

**C) ImageBitmap-Cache:**

Nicht jedes Mal neu dekodieren, sondern decodierte Bitmaps cachen:
```kotlin
object ImageBitmapCache {
    private val cache = LinkedHashMap<String, ImageBitmap>(30, 0.75f, true)

    fun getOrDecode(key: String, bytes: ByteArray): ImageBitmap {
        return cache.getOrPut(key) { bytes.toImageBitmap() }
    }

    fun clear() = cache.clear()
}
```

### Betroffene Dateien
- Neue Datei: `platform/ImageUtils.kt` (+ plattformspezifische Implementierungen)
- `GameScreen.kt` — Thumbnail-Rendering
- `ReviewScreen.kt` — Background-Decoding
- `ResultsScreen.kt` — Background-Decoding für Galerie

### Impact
- Kein UI-Freeze mehr beim Photo-Loading
- ~75% weniger RAM-Verbrauch durch Thumbnails in Grid-Views
- Schnelleres Scrollen in ResultsScreen-Galerie

---

## Refactoring 6: Compose-Optimierungen (MITTEL)

### Problem A: Fehlende `@Stable`/`@Immutable` Annotations

**`Models.kt`:**
```kotlin
data class Player(
    val id: String,
    val name: String,
    val color: Color,    // Color ist NICHT stable in Compose!
    val avatar: String = ""
)

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String = ""
)
```

Ohne Annotations behandelt Compose diese als **unstable** → jeder Composable der `Player` oder `Category` als Parameter bekommt, wird bei **jeder** Parent-Recomposition neu gerendert, auch wenn sich nichts geändert hat.

### Lösung A:
```kotlin
@Immutable
data class Player(
    val id: String,
    val name: String,
    val color: Color,
    val avatar: String = ""
)

@Immutable
data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String = ""
)
```

### Problem B: Fehlende `derivedStateOf`

**`GameState.kt` Zeile 123-124:**
```kotlin
val currentPlayer: Player? get() = players.getOrNull(currentPlayerIndex)
val reviewPlayer: Player? get() = players.getOrNull(reviewPlayerIndex)
```

**`GameState.kt` Zeile 256-263 — `getRankedPlayers()`:**
```kotlin
fun getRankedPlayers(): List<Pair<Player, Int>> =
    players.map { it to getPlayerScore(it.id) }  // Berechnet ALLE Scores
        .sortedWith(
            compareByDescending<Pair<Player, Int>> { it.second }
                .thenByDescending { getSpeedBonusCount(it.first.id) }  // Nochmal berechnen
                .thenBy { getLastCaptureTime(it.first.id) }  // Nochmal berechnen
        )
```

Wird bei **jeder Recomposition** komplett neu berechnet — inklusive aller Score-, SpeedBonus- und LastCaptureTime-Berechnungen.

### Lösung B:
```kotlin
// In GameState (oder besser: GamePlayState nach Refactoring 1)
val rankedPlayers by derivedStateOf {
    players.map { it to getPlayerScore(it.id) }
        .sortedWith(
            compareByDescending<Pair<Player, Int>> { it.second }
                .thenByDescending { getSpeedBonusCount(it.first.id) }
                .thenBy { getLastCaptureTime(it.first.id) }
                .thenBy { it.first.name }
        )
}
```

`derivedStateOf` cached das Ergebnis und berechnet nur neu wenn sich `players`, `allVotes` oder `allCaptures` tatsächlich ändern.

### Problem C: Fehlende `remember` für teure Berechnungen in Composables

**ResultsScreen.kt Zeile 69:**
```kotlin
val ranked = gameState.getRankedPlayers()  // Jede Recomposition neu!
```

### Lösung C:
```kotlin
val ranked = remember(gameState.players, gameState.allVotes, gameState.allCaptures) {
    gameState.getRankedPlayers()
}
```

### Betroffene Dateien
- `data/Models.kt` — `@Immutable` hinzufügen
- `GameState.kt` — `derivedStateOf` für computed Properties
- `ResultsScreen.kt` — `remember` für `getRankedPlayers()`
- `GameScreen.kt` — `remember` für Capture-Berechnungen

---

## Refactoring 7: Screen-Dateien aufteilen (MITTEL)

### Problem
Die größten Screen-Dateien sind schwer wartbar:

| Datei | Zeilen | LaunchedEffects | Problem |
|-------|--------|-----------------|---------|
| `ResultsScreen.kt` | 851 | 4 | Podium, Rankings, Galerie, Fullscreen-View, Map, Share — alles in einer Datei |
| `CreateGameScreen.kt` | 784 | 3 | Settings, Kategorie-Grid, Custom-Input, Duration-Slider — alles verschachtelt |
| `GameScreen.kt` | 783 | 9+ | Timer, Player-Tabs, Kategorie-Grid, Dialoge, Realtime, Polling — alles vermischt |
| `ReviewScreen.kt` | 613 | 5 | Voting-UI, Star-Rating, Waiting-Screen, Polling — vermischt |

### Lösung
Jede Screen-Datei in **logische Sub-Composables** aufteilen:

**GameScreen.kt → 4 Dateien:**
```
ui/screens/game/
├── GameScreen.kt              ← Koordinator (LaunchedEffects + State-Wiring, ~150 Zeilen)
├── GameScreenContent.kt       ← Layout (Timer, Tabs, Grid, ~200 Zeilen) — existiert teilweise schon
├── CategoryCard.kt            ← Einzelne Kategorie-Karte (~100 Zeilen)
└── GameDialogs.kt             ← Joker-Dialog, Info-Dialog, End-Vote-Dialog (~80 Zeilen)
```

**ResultsScreen.kt → 4 Dateien:**
```
ui/screens/results/
├── ResultsScreen.kt           ← Koordinator + LazyColumn-Struktur (~150 Zeilen)
├── PodiumSection.kt           ← Podium mit Animationen (~150 Zeilen)
├── RankingList.kt             ← Spieler-Rankings mit Stats (~150 Zeilen)
├── PhotoGallery.kt            ← Galerie + Fullscreen-View + Map (~250 Zeilen)
```

**ReviewScreen.kt → 3 Dateien:**
```
ui/screens/review/
├── ReviewScreen.kt            ← Koordinator + Polling/Realtime (~100 Zeilen)
├── StarRatingView.kt          ← Star-Rating-Input mit Animationen (~150 Zeilen)
├── ReviewWaitingView.kt       ← Warte-Screen zwischen Votes (~80 Zeilen)
```

**CreateGameScreen.kt → 3 Dateien:**
```
ui/screens/create/
├── CreateGameScreen.kt        ← Koordinator (~100 Zeilen)
├── CategorySelector.kt        ← Kategorie-Grid + Custom-Input (~250 Zeilen)
├── GameSettings.kt            ← Duration, Joker-Mode, Speed-Bonus Info (~150 Zeilen)
```

### Betroffene Dateien
- `GameScreen.kt` — aufteilen in 4 Dateien
- `ResultsScreen.kt` — aufteilen in 4 Dateien
- `ReviewScreen.kt` — aufteilen in 3 Dateien
- `CreateGameScreen.kt` — aufteilen in 3 Dateien

### Impact
- Jede Datei unter 250 Zeilen → leichter lesbar, testbar, reviewbar
- Compose kann Sub-Composables granularer skippen (weniger Recomposition)
- Parallelisiertes Arbeiten an verschiedenen UI-Teilen möglich

---

## Refactoring 8: Structured Error Handling (MITTEL)

### Problem
**12 silent `catch (e: Exception)` Blocks** in `GameRepository.kt`. Errors werden entweder komplett verschluckt oder per fragiler String-Prüfung gefiltert:

**GameRepository.kt Zeile 262-281:**
```kotlin
try {
    supabase.from("votes").insert(...)
} catch (e: Exception) {
    e.printStackTrace()  // Geloggt aber nicht gehandelt
}
try {
    supabase.from("vote_submissions").insert(...)
} catch (e: Exception) {
    val msg = e.message ?: ""
    // FRAGIL: String-basierte Fehler-Erkennung
    if (!msg.contains("duplicate", ignoreCase = true)
        && !msg.contains("unique", ignoreCase = true)
        && !msg.contains("23505", ignoreCase = true)) {
        throw e
    }
}
```

Probleme:
- Echte Fehler (Netzwerk down, Auth abgelaufen) werden verschluckt
- String-basierte Duplicate-Erkennung ist fragil (Supabase könnte Fehlermeldungen ändern)
- Kein einheitliches Error-Reporting

### Lösung

**A) Sealed Error-Types:**

Neue Datei `network/NetworkResult.kt`:
```kotlin
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val type: ErrorType, val cause: Exception? = null) : NetworkResult<Nothing>()
}

enum class ErrorType {
    NETWORK,        // Kein Internet / Timeout
    DUPLICATE,      // Duplicate-Key (erwartbar bei Votes)
    AUTH,           // Session abgelaufen
    NOT_FOUND,      // Resource nicht gefunden
    UNKNOWN         // Unbekannter Fehler
}
```

**B) Zentraler Error-Classifier:**

```kotlin
fun classifyError(e: Exception): ErrorType {
    val msg = e.message ?: ""
    return when {
        msg.contains("duplicate", true) || msg.contains("23505") -> ErrorType.DUPLICATE
        msg.contains("network", true) || msg.contains("timeout", true) -> ErrorType.NETWORK
        msg.contains("401") || msg.contains("auth", true) -> ErrorType.AUTH
        msg.contains("404") || msg.contains("not found", true) -> ErrorType.NOT_FOUND
        else -> ErrorType.UNKNOWN
    }
}
```

**C) Repository-Methoden mit Result-Type:**

```kotlin
// Statt:
suspend fun submitEndVote(gameId: String, voterId: String) {
    try { ... } catch (e: Exception) { /* fragile string check */ }
}

// Besser:
suspend fun submitEndVote(gameId: String, voterId: String): NetworkResult<Unit> {
    return try {
        supabase.from("vote_submissions").insert(...)
        NetworkResult.Success(Unit)
    } catch (e: Exception) {
        val type = classifyError(e)
        if (type == ErrorType.DUPLICATE) NetworkResult.Success(Unit)  // Duplicate ist OK
        else NetworkResult.Error(type, e)
    }
}
```

### Betroffene Dateien
- Neue Datei: `network/NetworkResult.kt`
- `GameRepository.kt` — Alle Methoden mit Result-Type wrappen
- Alle Screens — Error-Handling anpassen (statt `try/catch` → `when (result)`)

---

## Refactoring 9: Retry-Logik mit exponentiellem Backoff (MITTEL)

### Problem
Retry-Logik in `GameScreen.kt` nutzt **lineares Backoff ohne Jitter**:

**GameScreen.kt Zeile 95-106:**
```kotlin
var attempt = 0
while (attempt < 3 && !captureSuccess) {
    try {
        if (attempt > 0) delay(2_000L * attempt)  // 0, 2s, 4s — LINEAR
        GameRepository.recordCapture(...)
        captureSuccess = true
    } catch (e: Exception) {
        e.printStackTrace()
        attempt++
    }
}
```

Gleiches Pattern in:
- Joker-Label-Upload (Zeile 110-119): 0, 1s, 2s
- Vote-to-End (Zeile 410-419): 0, 1s, 2s
- Signal-All-Captured (Zeile 375-381): 0, 1s, 2s

**Probleme:**
- Lineares Backoff ist bei Netzwerkproblemen ineffektiv
- Kein Jitter → alle Clients retrien exakt gleichzeitig (Thundering Herd)
- Keine Unterscheidung zwischen recoverable und non-recoverable Errors

### Lösung

**Zentrale Retry-Utility:**

Neue Datei `network/RetryUtils.kt`:
```kotlin
suspend fun <T> withRetry(
    maxAttempts: Int = 3,
    initialDelay: Long = 1_000L,
    maxDelay: Long = 10_000L,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            val type = classifyError(e)
            if (type != ErrorType.NETWORK && type != ErrorType.UNKNOWN) throw e  // Nicht retrybar
        }
        // Exponentielles Backoff mit Jitter
        val jitter = (currentDelay * 0.2 * Random.nextDouble()).toLong()
        delay(currentDelay + jitter)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block()  // Letzter Versuch
}
```

**Nutzung:**
```kotlin
// Statt 10 Zeilen manuelles Retry:
withRetry { GameRepository.recordCapture(gameId, pid, cid, bytes, latitude, longitude) }
```

### Betroffene Dateien
- Neue Datei: `network/RetryUtils.kt`
- `GameScreen.kt` — 4 manuelle Retry-Loops durch `withRetry` ersetzen (~40 Zeilen gespart)
- Potenziell auch `ReviewScreen.kt`, `LobbyScreen.kt`

---

## Refactoring 10: Photo-Download mit Caching (MITTEL)

### Problem

**`GameRepository.kt` Zeile 244-248:**
```kotlin
suspend fun downloadPhoto(gameId: String, playerId: String, categoryId: String): ByteArray? = try {
    val path = "$gameId/$playerId/$categoryId.jpg"
    val url = supabase.storage.from("photos").createSignedUrl(path, 3600.seconds)
    httpClient.get(url).readRawBytes()   // IMMER Netzwerk-Call, KEIN Cache!
} catch (_: Exception) { null }
```

Jeder Aufruf macht:
1. Signed-URL erstellen (Netzwerk-Call an Supabase)
2. Foto downloaden (Netzwerk-Call an Storage)

In ReviewScreen und ResultsScreen werden dieselben Fotos **mehrfach heruntergeladen** — einmal für Review, nochmal für Results-Galerie.

### Lösung

**Cache-First-Strategie in `downloadPhoto()`:**

```kotlin
suspend fun downloadPhoto(gameId: String, playerId: String, categoryId: String): ByteArray? {
    // 1. Lokaler Cache
    try {
        val cached = LocalPhotoStore.loadPhoto(gameId, playerId, categoryId)
        if (cached != null) return cached
    } catch (_: Exception) {}

    // 2. Netzwerk-Download
    return try {
        val path = "$gameId/$playerId/$categoryId.jpg"
        val url = supabase.storage.from("photos").createSignedUrl(path, 3600.seconds)
        val bytes = httpClient.get(url).readRawBytes()
        // 3. Lokal cachen
        try { LocalPhotoStore.savePhoto(gameId, playerId, categoryId, bytes) } catch (_: Exception) {}
        bytes
    } catch (_: Exception) { null }
}
```

### Betroffene Dateien
- `GameRepository.kt` — `downloadPhoto()` mit Cache erweitern
- `ReviewScreen.kt` — Doppelte Cache-Logik entfernen (ist dann in Repository)
- `ResultsScreen.kt` — Doppelte Cache-Logik entfernen

---

## Zusammenfassung & Empfohlene Reihenfolge

| # | Refactoring | Aufwand | Impact | Zeilen gespart |
|---|-------------|---------|--------|----------------|
| 1 | Polling-Loops lifecycle-aware (Ref. 3) | Klein | Sehr hoch | ~50 Zeilen, -80% Requests |
| 2 | Avatar-Download deduplizieren (Ref. 4) | Klein | Hoch | ~75 Zeilen |
| 3 | Photo-Download mit Caching (Ref. 10) | Klein | Hoch | ~20 Zeilen |
| 4 | Retry-Utility (Ref. 9) | Klein | Mittel | ~40 Zeilen |
| 5 | Compose-Optimierungen (Ref. 6) | Klein | Mittel | 0 (Annotations) |
| 6 | Image-Decoding off-thread (Ref. 5) | Mittel | Hoch | ~10 Zeilen |
| 7 | Photo-Memory-Management (Ref. 2) | Mittel | Sehr hoch | ~20 Zeilen |
| 8 | Structured Error Handling (Ref. 8) | Mittel | Mittel | ~30 Zeilen |
| 9 | Screen-Dateien aufteilen (Ref. 7) | Mittel | Hoch | 0 (Reorganisation) |
| 10 | GameState aufteilen (Ref. 1) | Hoch | Sehr hoch | 0 (Reorganisation) |

**Empfehlung:** Starte mit 1-5 (kleine Aufwände, großer Impact), dann 6-8 (mittlerer Aufwand), dann 9-10 (größere Umstrukturierungen die auf den vorherigen aufbauen).

**Geschätzter Gesamteffekt:**
- ~215 Zeilen Code eingespart
- ~80% weniger Netzwerk-Requests während eines Spiels
- ~60% weniger RAM-Verbrauch durch Photo-Caching
- Deutlich weniger unnötige Recompositions
- Keine UI-Freezes mehr durch Main-Thread-Decoding
