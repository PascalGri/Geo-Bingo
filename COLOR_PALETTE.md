# GeoBingo — Neon Night Color Palette

> Theme: **Rose → Fuchsia → Purple** on neutral pure dark
> Target audience: TikTok / Gen Z
> Strategy: Deep neutral background lets neon accents pop; pink-purple spectrum is dominant on TikTok

---

## Base Colors

| Role              | Name             | Hex       | Preview |
|-------------------|------------------|-----------|---------|
| Background        | Neutral Pure Dark | `#050508` | ██ |
| Surface           | Dark Purple-Black | `#0C0B15` | ██ |
| Surface Variant   | Dark Purple       | `#15132A` | ██ |
| Outline           | Purple Border     | `#3A2A5C` | ██ |
| Outline Variant   | Deep Purple       | `#1E1535` | ██ |

---

## Brand Colors

| Role      | Name        | Hex       | Usage |
|-----------|-------------|-----------|-------|
| Primary   | Fuchsia 500 | `#D946EF` | CTA buttons, key highlights, speed bonus |
| Secondary | Purple 500  | `#A855F7` | Secondary accents, gradient stops |
| Tertiary  | Rose 500    | `#F43F5E` | Energy accents, gradient starts, error-adjacent |
| Error     | Hot Coral   | `#FF4D6D` | Errors, warnings |

---

## On-Color (Text / Icon tints on brand colors)

| Background      | Text Color  | Hex       |
|-----------------|-------------|-----------|
| On Background   | Ghost White | `#F5F0FF` |
| On Surface      | Pale Lavender | `#EDE9F7` |
| On Surface Var  | Muted Purple | `#9D7FD4` |
| On Primary      | Deep Fuchsia | `#1F0026` |
| On Secondary    | Deep Purple  | `#1A0033` |
| On Tertiary     | Deep Rose    | `#2D0010` |

---

## Container Colors

| Role                  | Hex       |
|-----------------------|-----------|
| Primary Container     | `#330040` |
| On Primary Container  | `#F5D0FE` |
| Secondary Container   | `#280047` |
| On Secondary Container | `#E9D5FF` |
| Tertiary Container    | `#420018` |
| On Tertiary Container | `#FFD9E2` |
| Error Container       | `#4A0017` |
| On Error Container    | `#FFD9E2` |

---

## Gradients

| Name             | Colors (left → right)                          | Use case |
|------------------|------------------------------------------------|----------|
| **GradientPrimary** | `#F43F5E` → `#D946EF` → `#A855F7`           | Hero elements, main CTA, borders |
| **GradientHot**  | `#FF6B6B` → `#F43F5E` → `#D946EF`             | Energy moments, confetti, celebrations |
| **GradientWarm** | `#FB7185` → `#E879F9` → `#C026D3`             | Soft pink-to-fuchsia for cards |
| **GradientCool** | `#A855F7` → `#7C3AED` → `#6366F1`             | Purple-violet-indigo for contrast |
| **GradientGold** | `#F59E0B` → `#FBBF24` → `#F97316`             | Medals, rankings, trophies only |

---

## Player Avatar Colors

These 8 colors are assigned to players in lobby order:

| # | Name       | Hex       |
|---|------------|-----------|
| 1 | Hot Pink   | `#EC4899` |
| 2 | Fuchsia    | `#D946EF` |
| 3 | Purple     | `#A855F7` |
| 4 | Violet     | `#7C3AED` |
| 5 | Rose       | `#F43F5E` |
| 6 | Cyan       | `#22D3EE` |
| 7 | Orange     | `#FB923C` |
| 8 | Coral      | `#FF6B6B` |

---

## Medal / Ranking Colors (semantic, not theme)

These are intentionally kept as gold/silver/bronze regardless of theme:

| Rank   | Color   | Hex       |
|--------|---------|-----------|
| Gold   | Amber   | `#FBBF24` |
| Silver | Slate   | `#94A3B8` |
| Bronze | Bronze  | `#CD7F32` |

---

## Confetti Colors

Used in the celebration burst animation after game end:

`#F43F5E` (Rose) · `#D946EF` (Fuchsia) · `#A855F7` (Purple) · `#7C3AED` (Violet) · `#E879F9` (Fuchsia Light) · `#22D3EE` (Cyan) · `#FF6B6B` (Coral)

---

## Design Rationale

- **#050508 background**: Neutral (not green, not blue-black) — the app reads as "tech/social", not "nature/eco"
- **Fuchsia as primary**: Dominant TikTok-Gen-Z color; high contrast on dark, stands out in screen recordings
- **Purple as secondary**: Pairs naturally with fuchsia; adds depth without clashing
- **Rose as tertiary**: The "red" energy without being aggressive; used for speed/urgency moments
- **Gold kept for medals**: Semantic meaning of gold is universal — changing it would confuse users
