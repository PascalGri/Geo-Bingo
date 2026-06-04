# App-Screenshots hier ablegen (1320 × 2868)

Lege deine **5 sauberen Roh-Screenshots** aus dem iPhone-Simulator hier ab.
Sie werden von `frame.html` in den schräg gestellten iPhone-Rahmen eingesetzt.

| Datei          | Was aufnehmen                                  | Marketing-Headline                |
|----------------|------------------------------------------------|-----------------------------------|
| `praemien.png` | Prämien-/Belohnungsscreen (Sterne sichtbar)    | Gewinne echte **Prämien**         |
| `home.png`     | Startseite / Home                              | Dein Alltag **wird zum Spiel**    |
| `game.png`     | Laufendes Spiel, 5×5-Raster                    | 5×5 Aufgaben. **1 Gewinner.**     |
| `solo.png`     | Solo-Modus (KI-Bewertung sichtbar)             | Solo gegen **die KI**             |
| `shop.png`     | Shop / Cosmetics                               | Style deinen **Spieler**          |

## So nimmst du saubere 6,9"-Screenshots auf

1. Simulator: **iPhone 16 Pro Max** (oder 15 Pro Max) → liefert exakt 1320 × 2868.
2. App im Simulator starten, zum jeweiligen Screen navigieren.
3. **⌘ + S** (Simulator → *Save Screen*) speichert auf den Schreibtisch.
4. Datei hierher kopieren und exakt wie oben benennen.

> Tipp: keine Preis-/„kostenlos"-Wörter im Bild (Apple-Richtlinie 2.3.7).
> Die Headlines kommen ohnehin aus `frames.js`, nicht aus dem Screenshot.

## Rendern

Wenn die 5 Dateien hier liegen:

```bash
./render.sh            # alle Frames → out/screenshot-01..05.png
./render.sh 0          # nur Frame 0 (praemien)
```

Fehlt ein Screenshot, rendert das Frame trotzdem — mit Platzhalter-Hinweis
statt Foto, damit du den Aufbau prüfen kannst.
