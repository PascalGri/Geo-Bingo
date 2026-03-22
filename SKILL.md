# KatchIt / GeoBingo -- Architecture Guide for AI Prompts

This document describes the established architecture patterns in this codebase.
Follow these conventions when adding new features or modifying existing code.

---

## Project Structure

```
composeApp/src/commonMain/kotlin/pg/geobingo/one/
├── App.kt                      # Root composable, screen routing, SyncAvatars
├── data/
│   ├── Models.kt               # @Immutable data classes: Player, Category
│   ├── Categories.kt           # Category definitions
│   └── CategoryIcons.kt        # Icon mappings
├── game/
│   └── GameState.kt            # Central state holder (mutableStateOf properties)
├── network/
│   ├── GameRepository.kt       # Supabase data layer (suspend functions)
│   ├── GameRealtime.kt         # Realtime subscriptions (Flows)
│   ├── SupabaseClient.kt       # Supabase client singleton
│   ├── NetworkResult.kt        # Sealed error types + classifyError()
│   └── RetryUtils.kt           # withRetry() exponential backoff utility
├── platform/                   # expect/actual platform abstractions
│   ├── PhotoCapture.kt         # Camera + toImageBitmap()
│   ├── LocalPhotoStore.kt      # Local photo/avatar caching
│   ├── LocationProvider.kt     # GPS
│   ├── SoundPlayer.kt          # Audio feedback
│   └── ...
└── ui/
    ├── components/
    │   └── AvatarSync.kt       # Centralized avatar download composable
    ├── screens/                # One file per screen
    │   ├── GameScreen.kt
    │   ├── ReviewScreen.kt
    │   ├── ResultsScreen.kt
    │   └── ...
    └── theme/                  # Colors, gradients, reusable UI components
        ├── Theme.kt
        ├── PlayerAvatar.kt     # PlayerAvatarView / PlayerAvatarViewRaw
        └── ...
```

---

## Architecture Patterns

### 1. State Management

- **Single `GameState` instance** created in `App.kt` via `remember { GameState() }`.
- All state properties use `mutableStateOf` for Compose reactivity.
- Computed/derived values use `derivedStateOf` to avoid redundant recalculation:
  ```kotlin
  val rankedPlayers by derivedStateOf { /* expensive sort */ }
  ```
- Screens receive `gameState: GameState` as parameter and read/write directly.

### 2. Network Layer

- **`GameRepository`** is a singleton `object` with `suspend` functions for all Supabase operations.
- **`GameRealtime`** provides Kotlin `Flow`s for real-time subscriptions (captures, votes, game updates).
- **Error handling**: Use `NetworkResult<T>` sealed class and `classifyError()` from `NetworkResult.kt`.
- **Retries**: Always use `withRetry { }` from `RetryUtils.kt` instead of manual retry loops:
  ```kotlin
  // Good
  try { withRetry { GameRepository.someCall() } } catch (_: Exception) {}

  // Bad - do NOT write manual retry loops
  var attempt = 0
  while (attempt < 3) { ... }
  ```
- `withRetry` uses exponential backoff with jitter and only retries network/unknown errors.

### 3. Polling Strategy

- **Realtime is the primary channel** via `GameRealtimeManager` Flows.
- **Fallback polling** runs alongside but skips heavy work when realtime is active:
  ```kotlin
  // Only do expensive polling when realtime is not active
  if (gameState.isGameRunning && realtime == null) {
      // heavy polling calls...
  }
  ```
- All polling loops must use **exponential backoff on errors**:
  ```kotlin
  var interval = 3_000L
  while (true) {
      delay(interval)
      try {
          // ... network calls
          interval = 3_000L  // reset on success
      } catch (e: Exception) {
          interval = (interval * 1.5).toLong().coerceAtMost(15_000L)
      }
  }
  ```
- **Never add redundant polling loops** that duplicate checks already handled by realtime or other loops.

### 4. Photo & Image Handling

- **Photo downloads use cache-first strategy** (built into `GameRepository.downloadPhoto()`):
  1. Check `LocalPhotoStore` cache
  2. Download from Supabase Storage if not cached
  3. Save to local cache after download
- **Image decoding MUST happen off the main thread**:
  ```kotlin
  // Good - off main thread
  var bitmap by remember(bytes) { mutableStateOf<ImageBitmap?>(null) }
  LaunchedEffect(bytes) {
      bitmap = withContext(Dispatchers.Default) { bytes?.toImageBitmap() }
  }

  // Bad - blocks main thread
  val bitmap = remember(bytes) { bytes?.toImageBitmap() }
  ```
- `PlayerAvatarView` and `PlayerAvatarViewRaw` already handle off-thread decoding internally.

### 5. Avatar Downloads

- **Centralized in `SyncAvatars()` composable** called once in `App.kt`.
- **Do NOT add avatar download `LaunchedEffect`s in individual screens.**
- `SyncAvatars` reacts to changes in `gameState.players` and `gameState.lobbyPlayers`.

### 6. Data Classes

- All domain data classes (`Player`, `Category`) must be annotated with `@Immutable`:
  ```kotlin
  @Immutable
  data class Player(val id: String, val name: String, val color: Color, val avatar: String = "")
  ```
- This tells Compose the class is deeply immutable, enabling skip optimizations.

### 7. Compose Best Practices

- Use `remember(keys) { }` for expensive calculations in composables.
- Use `derivedStateOf` for computed properties in state holders.
- Prefer `LaunchedEffect` + `mutableStateOf` over synchronous `remember` for I/O or CPU-heavy work.
- Do NOT use emojis in UI text -- use Material 3 icons instead.

---

## Adding a New Screen

1. Create `ui/screens/NewScreen.kt` with `@Composable fun NewScreen(gameState: GameState)`.
2. Add screen enum value to `Screen` in `GameState.kt`.
3. Add routing case in `App.kt`.
4. **Do NOT** add avatar download code -- `SyncAvatars` handles it globally.
5. Use `withRetry { }` for any network calls with retry logic.
6. If polling is needed, add backoff and check if realtime already covers the use case.

## Adding a New Network Call

1. Add the `suspend fun` to `GameRepository`.
2. Use `NetworkResult<T>` for calls where the caller needs to distinguish error types.
3. Wrap in `withRetry { }` at the call site if the operation should be retried.
4. For photo downloads, use the cache-first pattern already in `downloadPhoto()`.

---

## Performance Rules

| Rule | Why |
|------|-----|
| No `toImageBitmap()` on main thread | Causes UI freezes during JPEG decoding |
| No duplicate polling loops | Wastes battery and network bandwidth |
| Skip polling when realtime is active | Avoid ~80% unnecessary network requests |
| Use `withRetry` with backoff + jitter | Prevents thundering herd on server errors |
| Cache photos locally via `LocalPhotoStore` | Avoids re-downloading same photos across screens |
| `@Immutable` on data classes | Enables Compose recomposition skipping |
| `derivedStateOf` for expensive computations | Only recalculates when inputs actually change |
