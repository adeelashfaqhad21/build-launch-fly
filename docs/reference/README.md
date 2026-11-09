# Reference plates — authoritative

`Ref-1` … `Ref-7` are the **primary source of truth for the game's design, UI and engine
look**. Where the references and `docs/DESIGN.md` disagree, the references win; the written
documents fill in what the references do not show.

## Files

| File | Kind | Size | Dimensions |
|---|---|---|---|
| `Ref-1.png` | PNG screenshot | 3.5 MB | 1284 × 2778 |
| `Ref-2.png` | PNG screenshot | 1.4 MB | 1284 × 2778 |
| `Ref-3.png` | PNG screenshot | 390 KB | 1284 × 2778 |
| `Ref-4.png` | PNG screenshot | 708 KB | 1284 × 2778 |
| `Ref-5.png` | PNG screenshot | 1.0 MB | 1284 × 2778 |
| `Ref-6.png` | PNG screenshot | 825 KB | 1284 × 2778 |
| `Ref-7.mp4` | Screen recording | 39.2 MB | not inspectable in-browser |

`Ref.zip` is a local duplicate of the stills and is git-ignored.

## External live reference (not verified)

- **YouTube Short:** <https://youtube.com/shorts/H_qQZOHR04k>
- Page title: *"Epic Plane Evolution - Vintage Plane Level Completed (Minecraft)"* by
  `@harictenname`, 43 s.
- **Status: not inspected.** Our browser could not play it: the Shorts page stayed black
  after tapping unmute, and the no-cookie embed returned YouTube *error 153*. Nothing in
  `BRIEF.md` or `DESIGN.md` is derived from this clip.

To make it usable, supply one of:

1. A clip **under 20 MB** committed into this folder (the browser can open raw repo media up
   to that size — the 39 MB `Ref-7.mp4` is too large).
2. Three to five screenshots of the hangar and the launch-setup frames, saved here like the
   `Ref-*` plates.
3. A short written description of what animates in the hangar and how the pull-back reads on
   screen.

The title does confirm one thing the stills did not: the game has a **level-complete flow**,
which the derived Results component in `docs/DESIGN.md` §3.13 covers.

## Inspected contents (Ref-1 … Ref-6)

| Plate | Screen | Key elements |
|---|---|---|
| Ref-1 | Splash / loading | Toy-room illustration, "Voodoo" mark, `PLANE EVOLUTION` logo, loading ring, `Loading level: 99%` |
| Ref-2 | Hangar home | Coin `28.15K` + gem `0` pills, gear, `VINTAGE PLANE` plaque, promo rail (store, remove-ads, gems `x1 3/3`), chest `OPEN`, `VIP`, rocket `UNLOCK`, `TAP TO FLY`, cards `SLINGSHOT 41 / 69.55K`, `PLANE 58 / 421.04K`, `INCOME 18 x38.4 / 98.52K` |
| Ref-3 | Launch setup | Plane from behind in the slingshot, striped bands from two posts, desert valley, red `BACK` button |
| Ref-4 | Aim / tension | Closer camera, curved segmented gauge (green centre; yellow/orange/red flanks), no text |
| Ref-5 | Flight (good launch) | `94 KM/H` dial top-left, pause, `PERFECT!`, `167M`, right-hand progress track with flag and `4%`, green speed trails |
| Ref-6 | Flight (slow) | `37 KM/H`, red `LOW SPEED`, `370M`, `9%`, plane tilted, same dial/track |

Ref-7: the recording is committed but the browser refused it (20 MB CDN limit), so its
contents are **not** described. Nothing in the design documents is derived from it.

## Precedence

1. `Ref-1` … `Ref-7` (this folder)
2. `docs/DESIGN.md`
3. `docs/BRIEF.md`
4. `docs/MASTER_BRIEF.md`

## Aspect and orientation decision

All six stills are **portrait** (1284 × 2778). The master brief originally asked for
landscape, but the references take precedence: the game runs **portrait**
(`sensorPortrait`, 360 dp design width) and the UI has been re-laid out for it. See
`docs/DESIGN.md` §1.4.

## Packaged?

No. This folder is documentation and art direction only; nothing here is included in the
APK and no game code references these files.
