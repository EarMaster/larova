# Design system

Colour, icon and the rules that keep them working across three appearance modes.

---

## 1. Eight plus two

**Eight tile colours** that users choose from. **Two reserved system colours** that they cannot.

This split is the central decision. The app speaks in amber and red — progress, active step, get help. Everything a user colours themselves speaks in the eight tile tones. That is why the palette stops at ten values instead of eighteen.

### Tile colours

| Token | Light | Dark | Night | Accent light | Accent dark |
| --- | --- | --- | --- | --- | --- |
| `sand` | `#F7E4C2` | `#4C3B1E` | `#32281E` | `#8A5B12` | `#F0C88A` |
| `clay` | `#F4DBCB` | `#4E332A` | `#332325` | `#8E4A2E` | `#EFB496` |
| `rose` | `#F6DBE1` | `#4A2C38` | `#311F2D` | `#8C3D57` | `#F0AEC0` |
| `lilac` | `#E4DCF6` | `#37305A` | `#26223F` | `#4B3E86` | `#C6B8F0` |
| `sky` | `#D8E5F5` | `#243B55` | `#1C283D` | `#2F4E7C` | `#A8C8EC` |
| `sage` | `#D5E9E1` | `#22423A` | `#1B2C2E` | `#1F5F4E` | `#8FD0BC` |
| `moss` | `#DFEBD4` | `#2F4229` | `#222C24` | `#41652C` | `#B4D69A` |
| `stone` | `#E7E2DA` | `#3A3733` | `#28252A` | `#5A5348` | `#CFC7BA` |

Token keys are English and match the picker's English labels one for one. They were German until this rename, which happened while the app had no users — the only time it could. A key is not just an identifier: it is a value written into `Card.colorToken` and into every export file, so renaming one after content exists means migrating other people's data, including data sitting in backup files nobody can reach. The keys shown in the picker are still localized strings (`colour_<key>` in `strings.xml`); it is only the internal key that changed.

Night accents are a sixth value per token, listed in `AppColors.kt` rather than here: they are the dark accents pulled 35 % toward the warm night ink, so nothing glows cool in a dark room.

Title contrast against tile surface exceeds 9:1 in all three modes; subtitle contrast exceeds 4.5:1.

### Surfaces and text

| | Background | Raised | Ink | Muted ink |
| --- | --- | --- | --- | --- |
| Light | `#FBF7F1` | `#FFFFFF` | `#241F35` | `#5A5470` |
| Dark | `#1B1830` | `#241F3E` | `#F1ECF8` | `#B9B2CC` |
| Night | `#12101F` | `#1A1729` | `#E8D9BE` | `#A99781` |

Night uses warm ink rather than cool, so nothing glows blue in a dark bedroom.

### Reserved

**Amber** — the lamp in the icon. Marks the active step and progress. `#FFC46B` on dark surfaces, `#9E5E0C` for text and symbols on light ones; the bright tone carries no text on light backgrounds at 2.4:1.

**Alarm** — the help bar and nothing else. `#C0392B` with white text in light mode (5.4:1), `#FF9B8F` with `#2A1512` text in dark mode (8.5:1).

Because alarm red appears exactly once in the product, it is never misread. This is also why **no tile preset is a saturated red**: `clay` and `rose` sit either side of it and are deliberately muted.

## 2. Rules

**Store the key, never the value.** The database holds `colorToken = "sage"`. The theme resolves it per mode. Storing the hex value instead makes every user tile unreadable in dark mode, retroactively and unrepairably. Same for icons: symbol keys, not bitmaps.

**Colour is never the only differentiator.** Every tile also carries a symbol and a label. Under red-green colour blindness `moss` and `clay` converge visibly; the tile stays unambiguous because text and symbol do the work.

**Night is not a third palette.** It is the dark palette pulled 55 % toward `#12101F` with warm ink. Express it as a function, not another table — eight fewer values that can drift.

**Amber is not available as a tile colour.** It marks what is happening right now. Once every third tile is amber, that signal stops working, permanently, because users assign the colours. `sand` is the deliberate compromise: a desaturated relative of the brand amber, close enough for family resemblance, far enough not to be mistaken for a signal. It is the default for new tiles.

## 3. Icon

The **lararium** — the small arched wall niche in a Roman home where a lamp burned while the house was lived in. It reads three ways at once, none needing explanation:

- a shrine, so the name's meaning sits inside the image rather than beside it
- a doorway with the light left on, which is the entire product in one mark
- a nightlight, tying to the leading use case and the night mode

Files in [`../../brand/`](../../brand/):

| File | Purpose |
| --- | --- |
| `icon-foreground.svg` | Adaptive icon foreground layer, 108dp |
| `icon-background.svg` | Adaptive icon background layer, full bleed |
| `icon-monochrome.svg` | `<monochrome>` layer for themed icons, Android 13+ |
| `icon-store-512.svg` | Play Store listing, export at 512 × 512 PNG, no transparency |

Import through **New → Image Asset** in Android Studio. The monochrome layer must go into the same `ic_launcher.xml`; without it, themed icons fall back to a grey placeholder.

The outermost point of the drawing sits 32dp from centre against a 33dp safe radius — it survives every launcher mask.

**Deliberately not used:** a child, a hand or a heart. Those three motifs occupy nearly every family app and are indistinguishable in a home screen grid. The arch is a rare shape in the store and therefore findable.

## 4. Typography

Material 3 type scale with two adjustments:

- Guide step text at 22sp. It is read aloud, in dim light, often by someone over 65.
- Body minimum 16sp, honouring system font scale to 200 % without clipping.

Use the system font. A bundled display face would need glyph coverage for Latin, Cyrillic, Arabic, Devanagari, Han and Japanese, which no single reasonable file provides.

## 5. Layout

- Touch targets at least 56dp; tiles considerably larger
- Tile corner radius 24dp, chip radius 14dp
- Grid: two columns, 12dp gutter, 20dp screen margin
- Every tile is the same height — 72dp of chrome plus 84dp of text, the text half multiplied by
  the font scale. Content-sized tiles turn a grid into a skyline; the symbol sits at the top of
  the tile and the words at the bottom, so a one-line title still looks composed
- Help bar pinned 16dp from the bottom, above all content, on every screen a caregiver reads.
  **Not** on settings or the activity log: those are opened on purpose by a parent, and an
  emergency bar under a list of preferences spends the one colour that means "now"

### Width

One axis, three sizes, and no notion of "tablet" anywhere in the code — a phone in landscape, a
small tablet and a split-screen window are all just widths.

| Width | Tile columns | Everything else |
|---|---|---|
| under 600dp | 2 | full width, as drawn above |
| 600–839dp | 3 | capped at 640dp and centred |
| 840dp and over | 4 | capped at 640dp and centred |

The grid gets a wider cap than that — 1120dp — because a grid is scanned rather than read. The
640dp cap is a measure, not a margin: a guide step at 22sp run across 1280dp is a line the eye
loses on the way back, which is the failure the one-step-per-screen guide exists to prevent. The
help bar is capped and centred with the content so the two stay one column.

## 6. Prototypes

[`prototypes/`](prototypes/) holds three clickable HTML references: the screen flow, the icon sheet and the colour grid.

They predate the switch to English as the project language and still carry German annotations and sample content. The sample content is fine — it reflects the launch market. The annotations should be translated when the screens are next revised.
