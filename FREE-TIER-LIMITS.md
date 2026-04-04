# Free-Tier Engpass-Ranking

Ubersicht aller genutzten Services, deren Gratis-Limits und wann ein Upgrade notwendig wird.

## Services im Einsatz

| Service | Verwendung |
|---|---|
| **Supabase** (Free) | Datenbank, Auth, Realtime, Storage, Edge Functions |
| **Cloudflare Workers AI** (Free) | Solo-Modus Foto-Auswertung (Llama 4 Scout Vision) |
| **Vercel** (Hobby) | Web-App Hosting, statische Assets (WASM/JS) |
| **Firebase** (Spark) | Push Notifications (FCM), Crashlytics |
| **OpenStreetMap** | Karten-Tiles in Ergebnis-Ansicht |
| **Google AdMob** | Werbung (generiert Einnahmen) |
| **Google Play Billing** | In-App-Kaufe (generiert Einnahmen) |

---

## Engpass-Ranking

### #1 — Supabase Realtime (gleichzeitige Spieler)

| Limit | Kapazitat |
|---|---|
| 200 gleichzeitige WebSocket-Connections | ~150-200 gleichzeitige Multiplayer-Spieler |

- Jeder Spieler in einem aktiven Multiplayer-Spiel halt 1 Connection offen
- Spieler in Menus/Lobbys/Ergebnissen verbrauchen keine Connection
- **Upgrade-Zeitpunkt:** Sobald regelmaßig >100 Spieler gleichzeitig in aktiven Spielen sind

> **Upgrade:** Supabase Pro ($25/Monat) → 500 Connections, danach skalierbar

---

### #2 — Supabase Storage Bandwidth (monatlich)

| Limit | Kapazitat |
|---|---|
| 5 GB/Monat | ~1.500-2.000 Spiele/Monat |

- Jedes Multiplayer-Spiel: ~3 MB (Fotos hoch- und runterladen)
- Bei ~4 Spielern/Spiel und ~3-5 Spielen/Monat pro Nutzer: ~1.500-2.000 MAUs
- **Upgrade-Zeitpunkt:** Ab ~1.000 MAUs oder wenn Bandwidth-Warnungen kommen

> **Upgrade:** Supabase Pro ($25/Monat) → 250 GB Bandwidth

---

### #3 — Cloudflare Workers AI (taglich)

| Limit | Kapazitat |
|---|---|
| 10.000 Neurons/Tag | ~12-30 Solo-Spiele/Tag (je nach Bildgroße) |

- Llama 4 Scout: ~65-165 Neurons pro Foto-Auswertung
- 5 Fotos pro Solo-Spiel = ~325-825 Neurons pro Spiel
- **Upgrade-Zeitpunkt:** Ab ~10+ Solo-Spiele/Tag regelmaßig

> **Upgrade:** Cloudflare Workers Paid ($5/Monat) → $0.011/1.000 Neurons danach, praktisch unbegrenzt

---

### #4 — Supabase Edge Function Invocations

| Limit | Kapazitat |
|---|---|
| 500.000 Invocations/Monat | Ausreichend fur ~100.000 Solo-Auswertungen + Push-Notifications |

- Relevant fur: `validate-photo`, `send-push`, `send-daily-reminders`
- **Upgrade-Zeitpunkt:** Erst bei sehr hoher Nutzung relevant

> **Upgrade:** Supabase Pro ($25/Monat) → 2 Mio. Invocations

---

### #5 — Supabase Auth

| Limit | Kapazitat |
|---|---|
| 50.000 MAUs | 50.000 monatlich aktive Nutzer |

- **Upgrade-Zeitpunkt:** Ab ~40.000 MAUs

> **Upgrade:** Supabase Pro ($25/Monat) → 100.000 MAUs

---

### #6 — Supabase Datenbank

| Limit | Kapazitat |
|---|---|
| 500 MB Speicher | ~500.000+ Spiele (nur Metadaten) |

- Fotos liegen im Storage, nicht in der DB
- **Upgrade-Zeitpunkt:** Erst sehr spat relevant

> **Upgrade:** Supabase Pro ($25/Monat) → 8 GB

---

### #7 — Vercel (Hobby)

| Limit | Kapazitat |
|---|---|
| 100 GB Bandwidth/Monat | Ausreichend fur ~100.000+ Web-Besucher |

- Liefert nur statische WASM/JS-Assets
- **Upgrade-Zeitpunkt:** Erst bei extremem Web-Traffic

> **Upgrade:** Vercel Pro ($20/Monat) → 1 TB Bandwidth

---

### #8 — Firebase (Spark)

| Limit | Kapazitat |
|---|---|
| FCM Push Notifications | Unbegrenzt |
| Crashlytics | Unbegrenzt |

- Kein Upgrade notwendig solange nur FCM und Crashlytics genutzt werden

---

### #9 — OpenStreetMap

| Limit | Kapazitat |
|---|---|
| Fair-Use-Policy | ~1 Request/Sekunde empfohlen |

- Bei moderater Nutzung kein Problem
- **Upgrade-Zeitpunkt:** Bei sehr vielen gleichzeitigen Kartenansichten eigenen Tile-Server oder Mapbox erwagen

---

## Upgrade-Fahrplan

| Phase | Trigger | Aktion | Kosten |
|---|---|---|---|
| **Jetzt** | — | Alles gratis | $0/Monat |
| **Erste Traktion** | >10 Solo-Spiele/Tag | Cloudflare Workers Paid | +$5/Monat |
| **Wachstum** | >1.000 MAUs oder >100 gleichzeitige Spieler | Supabase Pro | +$25/Monat |
| **Skalierung** | >100.000 Web-Besucher/Monat | Vercel Pro | +$20/Monat |

### Gesamtkosten-Prognose

| MAUs | Geschatzte Kosten |
|---|---|
| 0-1.000 | $0/Monat (alles gratis) |
| 1.000-5.000 | ~$30/Monat (Supabase Pro + CF Paid) |
| 5.000-20.000 | ~$50/Monat (+ Vercel Pro) |
| 20.000+ | Individuell, je nach Nutzungsmuster |
