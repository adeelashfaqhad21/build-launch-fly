# Reference plates — authoritative

`Ref-1` … `Ref-7` are the **primary source of truth for the game's design, UI and engine
look**. Where the references and `docs/DESIGN.md` disagree, the references win. The written
documents stay authoritative only for what the references do not show: tokens, exact copy,
build and engineering constraints.

## Files

| File | Kind | Size | Measured dimensions |
|---|---|---|---|
| `Ref-1.png` | PNG screenshot | 3.5 MB | 1284 × 2778 |
| `Ref-2.png` | PNG screenshot | 1.4 MB | 1284 × 2778 |
| `Ref-3.png` | PNG screenshot | 390 KB | 1284 × 2778 |
| `Ref-4.png` | PNG screenshot | 708 KB | 1284 × 2778 |
| `Ref-5.png` | PNG screenshot | 1.0 MB | 1284 × 2778 |
| `Ref-6.png` | PNG screenshot | 825 KB | 1284 × 2778 |
| `Ref-7.mp4` | Screen recording | 39.2 MB | — |

`Ref.zip` is a local duplicate of the stills and is git-ignored.

## Precedence

1. `Ref-1` … `Ref-7` (this folder)
2. `docs/DESIGN.md`
3. `docs/MASTER_BRIEF.md`

## Aspect note

All six stills are **portrait** (1284 × 2778), while `docs/MASTER_BRIEF.md` §9 and
`docs/DESIGN.md` §2.3 mandate **landscape, 16:9-first**. Those two positions cannot both be
literal. Until the references are re-captured in landscape (or the rule is changed), the
working interpretation is: the references define the visual language — palette, materials,
silhouettes, HUD content, camera framing, aircraft and slingshot forms — and it is adapted
to the landscape canvas the brief requires.

## What each plate is used for

To be completed from the plates themselves (screen type, biome, HUD elements, aircraft
stage, slingshot state). Keep this table in sync whenever a plate is added or replaced:

| File | Screen / state | Biome | Notes |
|---|---|---|---|
| `Ref-1.png` | _to fill_ | _to fill_ | |
| `Ref-2.png` | _to fill_ | _to fill_ | |
| `Ref-3.png` | _to fill_ | _to fill_ | |
| `Ref-4.png` | _to fill_ | _to fill_ | |
| `Ref-5.png` | _to fill_ | _to fill_ | |
| `Ref-6.png` | _to fill_ | _to fill_ | |
| `Ref-7.mp4` | _to fill_ | _to fill_ | moving reference for flight/launch feel |

## Packaged?

No. This folder is documentation and art direction only; nothing here is included in the
APK and no game code references these files.
