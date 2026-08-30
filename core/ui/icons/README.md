# Tile symbols

The drawings behind `TileSymbol`. One SVG per symbol key, converted to a Compose `ImageVector` by
the `generateTileSymbols` Gradle task in `core/ui/build.gradle.kts`.

```
core/ui/icons/
├── lucide/   vendored from Lucide, ISC — see LICENSE in that folder
│   ├── everyday/  food/  care/  home/
│   └── out/  play/  people/  notes/
└── larova/   drawn here, AGPL-3.0 like the rest of the app
    └── people/
```

**The folder is the shelf.** An icon in `food/` appears under "Food and drink" in the picker, so
the grouping lives beside the drawings instead of in a table somebody has to keep in step with
them. 291 drawings across the eight shelves.

## The file name is the key, and the key is frozen

`bed.svg` becomes the symbol key `"bed"`, which is what `Card.icon` stores and what every export
file contains. Renaming a file renames a key, which silently changes what a family's tiles point
at — in their backups as much as on their phone. **Add files; never rename or delete one.** An
unknown key falls back to the default rather than failing, which is what lets a tile written by a
newer Larova still draw in an older one.

The *drawing* carries no such promise. A key can be redrawn a hundred times and the tiles that use
it keep meaning what they meant — that is the whole reason the key is a string and not a picture.
The ten keys that existed before this folder did (`moon`, `sun`, `heart`, `list`, `note`, `phone`,
`clock`, `home`, `meal`, `star`) were drawn by hand and are now Lucide drawings under the same
keys, which is that promise being used rather than merely stated.

## Why vendored rather than a dependency

An icon library added to the build would tie a frozen key to somebody else's naming, and this
folder exists partly because that risk turned out to be real: Lucide had no `smile` or `frown` at
1.37.0, so `smile.svg` and `sad.svg` are drawn here instead. Had those keys been resolved against
the library by name, every tile using them would have lost its symbol on an upgrade.

Vendoring also keeps the app's size honest — 291 icons rather than 1,790 — and keeps the whole
thing offline, which everything else about this app already is.

## Adding one

1. Drop a 24×24 SVG into the right shelf under `lucide/` (from the pinned tag below) or
   `larova/`. The file name becomes the key, so name it for what it is, not for what it draws.
2. To offer it as a suggestion, add the key to `TileSymbol` with its English name and shelf.
   Everything else is reachable through search without touching any Kotlin.
3. Run `./gradlew :core:ui:generateTileSymbols` — or just build; it runs before compilation.
4. Re-record the token goldens: `./gradlew :app:recordRoborazziDebug --tests '*TokenScreenshotTest'`.

Only `path`, `circle`, `rect`, `line`, `ellipse`, `polyline` and `polygon` are understood, which
is everything the set uses. Anything else fails the task loudly rather than producing an icon with
a piece missing. Two files sharing a name fails it too — the name is a key, and two drawings cannot
own one.

## Provenance

Vendored from **Lucide 1.37.0**, https://github.com/lucide-icons/lucide, ISC licence. The files are
unmodified. `lucide/LICENSE` is their copyright notice and must travel with them.

289 of Lucide's 1,790 are kept. The other 1,500 are chevrons, cloud storage, code brackets and
company marks — an icon set built for dashboards, of which only a slice is about a family's day.
Carrying all of them would have cost roughly a megabyte of path data in the APK for drawings a
parent would have to scroll past.
