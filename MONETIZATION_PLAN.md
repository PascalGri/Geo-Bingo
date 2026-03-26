# Monetarisierungsplan — GeoBingo

## Architektur-Übersicht

Ein neues `StarsManager`-Singleton (analog zu `AdManager`) verwaltet den gesamten Stars-Stand.
Persistenz via `DataStore` (KMP-kompatibel). Alle Interaktionen laufen durch `StarsState`
(neuer Sub-State in `GameState`).

---

## Phase 1 — Stars-Währung (Fundament)

**Neue Dateien:**
- `commonMain/.../game/state/StarsState.kt` — Mutable State: `starCount`, `adsWatchedToday`, `lastAdDate`, `lastLoginDate`, `lastDailyDate`
- `commonMain/.../platform/StarsStorage.kt` — `expect` Interface für persistente Speicherung
- `androidMain/.../platform/StarsStorage.android.kt` + `iosMain/...` — `actual` via DataStore/UserDefaults

**Integration in GameState:**
- `gameState.stars` als neuer Sub-State
- `StarsState` in `GameState.kt` hinzufügen

**UI-Komponente:**
- `StarsChip` — kleine Anzeige (Icon + Zahl) die überall wiederverwendet wird,
  z.B. in der TopBar von HomeScreen, ModeSelectScreen, CreateGameScreen

---

## Phase 2 — Rewarded Ads für Stars (bis zu 5x täglich)

**Trigger-Punkte:**
- Neuer Button "Stars verdienen" auf HomeScreen oder SettingsScreen
- Ein dedizierter `EarnStarsDialog` mit 5 Slots, der zeigt wie viele Ads heute noch verfügbar sind

**Logik in `StarsState`:**
```
adsWatchedToday: Int  (reset wenn lastAdDate != today)
canWatchAd: Boolean = adsWatchedToday < 5
```

**Belohnung:** 10 Stars pro Video

**Flow:**
1. User tippt "Stars verdienen"
2. `EarnStarsDialog` öffnet sich, zeigt verbleibende Slots
3. `AdManager.showRewardedAd(onReward = { stars.add(10) })`
4. State speichern

---

## Phase 3 — "Kein Werbung" In-App-Kauf (1,99 €)

**Produkt-ID:** `pg.geobingo.one.no_ads` (einmaliger Kauf, nicht subscription)

**Neue Dateien:**
- `commonMain/.../platform/BillingManager.kt` — `expect` Interface: `purchaseNoAds()`, `isNoAdsPurchased(): Boolean`, `restorePurchases()`
- `androidMain/.../platform/BillingManager.android.kt` — `actual` via Google Play Billing Library
- `iosMain/.../platform/BillingManager.ios.kt` — `actual` via StoreKit 2

**Persistenz:** `noAdsPurchased: Boolean` in `StarsStorage` (lokal gecacht, beim Start wiederhergestellt)

**Effekt wenn aktiv:**
- `AdManager.showInterstitialAd()` wird übersprungen
- Rewarded Ads bleiben verfügbar (User-initiiert für Stars)
- `EarnStarsDialog`: Hinweis "Werbung nach Runden deaktiviert"

**UI:**
- Kauf-Button in `SettingsScreen.kt`: "Werbung entfernen — 1,99 €"
- "Kauf wiederherstellen" Button darunter (App Store Pflicht)
- Nach Kauf: Button zeigt "Werbung entfernt" (deaktiviert, mit Checkmark-Icon)

**Flow:**
1. User tippt "Werbung entfernen"
2. Native OS-Kaufdialog öffnet sich
3. Bei Erfolg: `noAdsPurchased = true` speichern, `BillingManager.isNoAdsPurchased()` gibt `true` zurück
4. `AdManager.showInterstitialAd()` prüft diesen Flag vor jedem Aufruf

**App Store / Play Store Setup:**
- Google Play Console: In-App-Produkt anlegen, Typ "Einmaliger Kauf", Preis 1,99 €
- App Store Connect: In-App-Purchase anlegen, Typ "Non-Consumable", Preis Tier 2 (1,99 €)

---

## Phase 4 — Automatisches Interstitial nach jeder Runde

**Wo:** `ResultsScreen.kt` — bereits vorhanden, bereits `showInterstitialAd()` Aufruf

**Anpassung:** Sicherstellen dass es nach *jeder* Runde zuverlässig triggert
(aktuell möglicherweise nur einmal). Kein User-Opt-in nötig — passiert automatisch.

---

## Phase 5 — Category Reroll & mehr Vorschläge mit Stars oder Video

**Wo:** `CreateGameScreen.kt` — bei der Kategorien-Auswahl

### Einzelner Reroll (eine Kategorie tauschen)

**Feature:** Neben jeder vorgeschlagenen Kategorie ein Reroll-Button (Shuffle-Icon)

**Kosten:** 5 Stars **oder** Rewarded Ad schauen **oder** Skipper-Karte

**Flow:**
1. User tippt Reroll-Icon
2. `RerollDialog` erscheint: "5 Stars" vs. "Video schauen" (vs. "Karte verwenden" wenn vorhanden)
3. Bei Stars: `stars.spend(5)` → neue zufällige Kategorie
4. Bei Video: `AdManager.showRewardedAd(onReward = { reroll() })`
5. Bei Karte: `skipCardsCount--` → neue zufällige Kategorie direkt

### Mehr Vorschläge (ganzen Satz neu laden)

**Feature:** Button "Neue Vorschläge" der alle aktuell angezeigten Kategorien auf einmal austauscht

**Kosten:** 10 Stars **oder** Rewarded Ad schauen **oder** Skipper-Karte

**Flow:**
1. User tippt "Neue Vorschläge"
2. `MoreSuggestionsDialog` erscheint: "10 Stars" vs. "Video schauen" (vs. "Karte verwenden")
3. Bei Reward: alle Kategorie-Slots werden mit neuen Zufallskategorien befüllt

---

## Phase 6 — Exklusiver Modus (Stars oder Videos)

**Wo:** `ModeSelectScreen.kt` — neuer Modus-Slot

**Neuer Modus:** z.B. "Extreme Mode" oder "Night Mode" — visuell als gesperrte Karte
mit Schloss-Icon

**Freischalten pro Spiel (nicht dauerhaft):**
- Option A: 20 Stars zahlen
- Option B: 2 Videos schauen hintereinander

**Flow:**
1. User tippt den gesperrten Modus
2. `UnlockModeDialog` erscheint mit beiden Optionen
3. Nach Unlock: Modus für diese Session freigeschaltet,
   `ModeSelectScreen` navigiert zu `CreateGameScreen` mit diesem Modus

---

## Phase 7 — Daily Login Bonus

**Wo:** `App.kt` oder `HomeScreen.kt` — beim App-Start prüfen

**Logik:**
```
if (today != lastLoginDate) {
    lastLoginDate = today
    stars.add(15)
    show DailyLoginDialog
}
```

**UI:** Kleines Dialog/Snackbar beim ersten Öffnen des Tages: "+15 Stars" mit Star-Animation

---

## Phase 8 — Daily Challenge

**Wo:** Neuer Screen oder Card auf `HomeScreen.kt`

**Konzept:** Täglich wechselnde Challenge, z.B. "Gewinne eine Runde im Classic-Modus"

**Belohnung:** 25–50 Stars bei Abschluss

**Tracking:** `dailyChallengeCompleted: Boolean` + `lastDailyDate` in `StarsState`

**Neue Datei:** `DailyChallengeState.kt` — aktueller Challenge-Typ, Fortschritt,
ob heute schon abgeschlossen

**Challenge-Typen (einfach zu implementieren):**
- Gewinne eine Runde
- Schließe eine Runde mit X Kategorien ab
- Spiele eine Runde im Modus Y

---

## Phase 9 — Shop (Stars kaufen & Ad-Skipper-Karten)

**Wo:** Neuer `ShopScreen.kt` + Einstieg via Button auf HomeScreen (z.B. Shop-Icon in TopBar)

**Neue Datei:** `ui/screens/ShopScreen.kt`

---

### Stars-Pakete (Consumable IAP)

Consumable = können mehrfach gekauft werden.

| Paket | Stars | Preis | Produkt-ID |
|-------|-------|-------|------------|
| Kleines Paket | 50 Stars | 0,99 € | `pg.geobingo.one.stars_50` |
| Mittleres Paket | 150 Stars | 1,99 € | `pg.geobingo.one.stars_150` |
| Grosses Paket | 400 Stars | 3,99 € | `pg.geobingo.one.stars_400` |
| Megapaket | 1000 Stars | 7,99 € | `pg.geobingo.one.stars_1000` |

Nach Kauf: `stars.add(menge)` + Bestätigungs-Animation

---

### Ad-Skipper-Karten (Consumable IAP)

Eine Karte = überspringt ein **freiwilliges Rewarded Ad** (z.B. beim Category Reroll oder
beim "Stars verdienen"). Das automatische Interstitial nach jeder Runde ist davon **nicht**
betroffen — es spielt immer direkt ab (ausser bei aktivem "Kein Werbung"-IAP).

| Paket | Karten | Preis | Produkt-ID |
|-------|--------|-------|------------|
| 3 Karten | 3x Skip | 0,99 € | `pg.geobingo.one.skip_3` |
| 10 Karten | 10x Skip | 2,99 € | `pg.geobingo.one.skip_10` |

**Tracking:** `skipCardsCount: Int` in `StarsStorage`

**Flow bei freiwilligem Rewarded Ad (Reroll, Stars verdienen):**
```
if (skipCardsCount > 0) → Option "Karte verwenden" anbieten → skipCardsCount-- , Reward direkt gutschreiben
else → showRewardedAd(onReward = { ... })
```

**Flow beim automatischen Interstitial nach Runde:**
```
if (noAdsPurchased) → kein Ad
else → showInterstitialAd()  // immer, keine Karten möglich
```

**UI im Shop:**
- Karten als eigene Sektion mit Karten-Icon
- Aktueller Kartenstand sichtbar ("Du hast noch X Karten")

**UI bei Rewarded Ad Prompt:**
- Wenn Karten vorhanden: zusätzlicher Button "Karte verwenden (X übrig)" neben "Video schauen"

---

### Shop-UI Aufbau

```
ShopScreen
├── TopBar: "Shop" + aktueller Sternstand (StarsChip)
├── Sektion "Stars kaufen"
│   └── 4 Kauf-Karten nebeneinander (2x2 Grid)
├── Sektion "Ad-Skipper"
│   └── 2 Kauf-Karten
├── Sektion "Werbung entfernen" (falls noch nicht gekauft)
│   └── Einzelne prominente Karte: 1,99 €
└── "Käufe wiederherstellen" TextButton unten
```

**Navigation:** HomeScreen TopBar → ShopScreen, zurück via Back

---

### Technische Ergänzung zu BillingManager

`BillingManager` (aus Phase 3) wird erweitert um:
- `getAvailableProducts(): List<Product>` — lädt Preise live von Store
- `purchaseProduct(productId)` — generischer Kauf-Flow
- Preise immer live vom Store laden, nie hardcoden (App Store Pflicht)

---

## Umsetzungsreihenfolge

| # | Feature | Aufwand | Priorität |
|---|---------|---------|-----------|
| 1 | StarsState + Persistenz | M | Pflicht (Fundament) |
| 2 | StarsChip UI-Komponente | S | Pflicht |
| 3 | "Kein Werbung" IAP 1,99 € | M | Sofort |
| 4 | Interstitial nach Runde fixieren | XS | Sofort |
| 5 | 5x tägliche Rewarded Ads | M | Hoch |
| 6 | Daily Login Bonus | S | Hoch |
| 7 | Category Reroll | M | Mittel |
| 8 | Exklusiver Modus | M | Mittel |
| 9 | Shop Screen + Stars-IAPs | M | Hoch |
| 10 | Ad-Skipper-Karten IAP | S | Hoch |
| 11 | Daily Challenge | L | Niedrig |

---

## Stars-Wirtschaft (Balance)

| Aktion | Stars |
|--------|-------|
| Video schauen (max. 5x/Tag) | +10 |
| Daily Login Bonus | +15 |
| Daily Challenge abschliessen | +25–50 |
| Category Reroll | -5 |
| Exklusiver Modus freischalten | -20 |

---

## Betroffene Dateien (Zusammenfassung)

| Datei | Änderung |
|-------|---------|
| `game/GameState.kt` | `StarsState` hinzufügen |
| `game/state/StarsState.kt` | Neu erstellen |
| `platform/StarsStorage.kt` | Neu erstellen (expect) |
| `androidMain/.../StarsStorage.android.kt` | Neu erstellen (actual) |
| `iosMain/.../StarsStorage.ios.kt` | Neu erstellen (actual) |
| `ui/screens/HomeScreen.kt` | StarsChip + "Stars verdienen" Button |
| `ui/screens/ModeSelectScreen.kt` | Gesperrter Modus + StarsChip |
| `ui/screens/create/CreateGameScreen.kt` | Reroll-Button pro Kategorie |
| `ui/screens/results/ResultsScreen.kt` | Interstitial-Trigger sicherstellen (No-Ads-Check) |
| `ui/screens/SettingsScreen.kt` | "Werbung entfernen" Kauf-Button |
| `App.kt` | Daily Login Check beim Start, Purchases wiederherstellen |
| `platform/BillingManager.kt` | Neu erstellen (expect), inkl. getAvailableProducts() |
| `androidMain/.../BillingManager.android.kt` | Neu erstellen (actual, Google Play Billing) |
| `iosMain/.../BillingManager.ios.kt` | Neu erstellen (actual, StoreKit 2) |
| `ui/screens/ShopScreen.kt` | Neu erstellen |
| `ui/screens/HomeScreen.kt` | Shop-Icon in TopBar |
