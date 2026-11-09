# ROADMAP — Build • Launch • Fly

The single master plan. It holds the vision, the audited state of the build, the issue
register, the asset catalog and the phases, in that order. Everything else in `docs/` is
either a source of truth (`reference/`, `BRIEF.md`, `DESIGN.md`) or a record
(`ASSETS.md`, `ARCHITECTURE.md`, `licenses/`); when a fact here disagrees with them, the
reference plates win and this file is corrected.

It absorbs the three former asset notes (`ASSET_AUDIT.md`, `ASSET_GAPS.md`, `DOWNLOADS.md`),
which no longer exist.

**How this file stays true:** after every phase or commit that changes state, the affected
row is edited in place and the log at the end gains one line. A plan that is not updated is
worse than none.

---

## 1. The vision, reconstructed

A **portrait** mobile game about a vintage biplane. The player owns it, upgrades it, hooks it
to a slingshot and throws it across a valley for distance, then spends the coins on making the
next throw longer. It is a distance game, not a flying game: the slingshot is the only launch
power and everything after release is a glider riding momentum and aerodynamics.

The loop, exactly as `Ref-1` … `Ref-6` show it:

```
Splash (brand + real load %)  →  Hangar home (currencies, plane, 3 upgrade cards, TAP TO FLY)
        ↓
Launch setup (plane in the rig, red BACK)  →  Aim / tension (segmented arc, green centre)
        ↓
Release  →  Flight HUD (speed dial, pause, distance, progress track, PERFECT / LOW SPEED)
        ↓
Results (derived; the recording's "level completed" confirms one exists)  →  coins
        ↓
Hangar  →  SLINGSHOT / PLANE / INCOME upgrades  →  fly again
```

What the plates fix about the look:

- **3D:** low-poly, flat-shaded, pastel and desert; simple terrain with cacti, bare trees and
  ridges; big cyan sky, light haze, soft shadows, no PBR gloss.
- **UI:** white rounded cards, thick light borders, drop shadows; saturated headers (orange,
  cyan, purple); chunky uppercase type with dark outlines; glossy circular instruments; gold
  coin and blue gem pills with green `+`; 360 dp design width, portrait.
- **Palette anchors:** sky `#40B9E8`, sand `#C5A078`, card `#FFF9EF`, blue `#1975D1`, outline
  navy `#185AB7`, green `#35D52D`, yellow `#FFD52B`, gold `#FFB51F`, red `#F33135`, purple
  `#9A4BEA`, lavender `#E7B5F3`, cyan card `#19A9ED`, orange card `#FF9D12`, gem `#22B7F0`,
  plaque tan `#A58B70`, plaque text `#583B1E`.
- **Numbers the plates actually show:** `94 KM/H` at `167 M` (good launch, `PERFECT!`) and
  `37 KM/H` at `370 M` with `LOW SPEED`. A flight is a slow decay, and the player is meant to
  feel it.

Our own identity is not copied: name, logo, art, copy and numbers stay original.

## 2. Where the build stands

Nothing here has been seen running on a device since build `1870682`; every statement about
appearance below is a reading of the code and the assets, not an observation.

| System | State | Evidence / file |
|---|---|---|
| Engine | Done. GLSL ES 3.00, instancing, shadow map + PCF, analytic sky, water, particles, tone map | `gfx/` |
| Records below | Destroyed  | `f107559` |
| Worlds | Done — five realms, shaped terrain, palettes, weather, water level | `world/WorldDefinition.kt` |
| Ground | Imported. 15 diffuse/colour maps; three-layer blend on a photographic scale | `gfx/TextureFactory.kt`, `SceneRenderer` |
| Vegetation | Near field (18–140 m) imported, per-realm; 140–400 m scatter still procedural | `Vegetation.kt`, `WorldScene.buildHeroProps` |
| Wildlife | Ground: 11 Quaternius models, 2–3 species per realm on their own rings. Birds: imported `parrot.glb` via `gfx/GlbLoader` | `WorldScene.buildAnimalParts` |
| Aircraft | Imported `samolet` biplane split by material; procedural airframe as fallback | `aircraft/AircraftModelLoader` |
| Flight | Glider physics: lift curve to 16°, induced drag, stall, banked lift; the stick asks for an angle of attack inside the working part of the curve | `aircraft/FlightPhysics.kt` |
| Obstacles | Every tree, building and kit prop is a solid cylinder in one grid the flight model queries each frame | `world/ObstacleField.kt`, `WorldScene` |
| Slingshot | Two squared posts + one band; posts generated | `slingshot/SlingshotMesh.kt` |
| Hangar | Shell and 6 m ramp generated; furniture imported (Kenney) | `world/HangarMesh.kt` |
| Landmarks | Quaternius RTS buildings + mountains; Kenney Castle ruin; industrial and town dressing | `WorldScene.buildStructures`, `buildKitDressing` |
| Fleet | Five Quaternius ships on the coast, rocking | `WorldScene.buildShipParts` |
| UI | All ten screens, vector/Canvas only, on the reference palette | `ui/` |
| Audio | One-shots imported (Kenney, SoundPool); loops synthesised | `audio/SoundBank.kt`, `AudioEngine.kt` |
| Build | CI only. Fixed after a break at `0f9aefd`; green run required before any tag | `.github/workflows/android.yml` |

## 3. Issue register

Priority is about what the player meets, not about effort.

**P0 — blocking, fix before anything else**

| # | Issue | Decision |
|---|---|---|
| 1 | The build broke at `0f9aefd` (five Kotlin errors) and stayed broken for one commit. | Fixed in `8d1a306`. Keep the rule: a push that leaves CI red is reverted or fixed in the next commit, immediately. |
| 2 | Nothing was ever verified running while six commits' worth of visuals were written. | Accept that CI proves compilation only. Any visual claim is written as "by inspection" until a device or screenshot pass exists. |
| 3 | `Ref-7.mp4` (39.2 MB) has never been opened. | Treat as unavailable. Do not infer its contents. |
| 3b | **Installed build dies on the splash screen after a few seconds** (first on-device report, `9d811c8`). Everything built since `1870682` was never run, so the fault is somewhere in the new startup path: the 12.2 MB `parrot.glb` parse, the herd / kit / fence loaders, or an out-of-memory kill. | Fixed defensively first so the app cannot die silently: engine build is wrapped in `Throwable` (the splash shows the failure), each asset group in `WorldScene` is guarded, and `GlbLoader` refuses blobs over 4 MB so the parrot no longer holds ~12 MB plus boxed lists on the heap. The exact cause is confirmed by the message the splash now prints. |

**P1 — next, high value**

| # | Issue | Decision |
|---|---|---|
| 4 | `birds/parrot.glb` has no documented licence; 12.2 MB for a distant flock. | Keep it (a licence note exists saying *unconfirmed*), but it is the first thing replaced if a small documented bird is found. Do not let it grow the release APK further. |
| 5 | `birds/bird.json` is a supported path with no file behind it. | **Done** — `WorldScene` now picks the reader from what `assets/birds/` actually contains, so neither path logs a failure for a file that was never shipped. |
| 6 | `leaf` and `waterNormals` are still generated. | `leaf` **stays generated**: the imported leaf maps in `quaternius/` are alpha cut-out cards for cross-plane canopies, so as an opaque albedo on closed geometry they would show the card, not foliage. It is the one tile that reads correctly as a tiling colour. `waterNormals` needs a real normal map or a shader without one — no download in use has one. |
| 7 | The 140–400 m vegetation ring is procedural. | Keep until billboard cards exist; the imported trees are 5 200–35 000-line OBJs and cannot be instanced at that distance. |
| 8 | The forest calls a Shiba Inu a "dog" in the code. | Rename the species label; the model is a Shiba and the realm should say so. |
| 9 | `assets-source/kenney_rpg-audio/` and `assets-source/parrot-light.glb` are untracked and not ignored. | Add to `.gitignore`; the packs are staging, the game files are in `assets/`. |

**P2 — polish, when the P1 list is clear**

| # | Issue | Decision |
|---|---|---|
| 10 | Hangar shell and ramp are generated. | **Keep generated.** The plates show an open-sided timber shed with a 6 m ramp; the City Kit is closed industrial boxes and the Fantasy Town kit is stone/wood village modules. Imported buildings dress the airfield around the shed instead. |
| 11 | Slingshot posts are generated. | **Keep generated.** The plates specify wide squared timber; every kit offers round logs. |
| 12 | UI is vector, not sliced PNG. | **Keep vector.** The plates are flat white rounded cards with navy outlines; Kenney's Fantasy UI Borders are pixel-art frames and would move away from them. |
| 13 | Wind, propeller, creak and five ambience beds are synthesised. | Acceptable until a looping-sample path exists; the one-shots already carry the interactions. |
| 14 | No skeletal animation: animals and the parrot are static-pose. | Needs a skinning pass in the renderer; independent of any pack. Out of scope until P1 closes. |
| 15 | Unused on-disk material: Poly-Google airframe, `Horse.obj`, castle bridges/siege engines, nature cliffs/crops/fences/logs, ambientCG zips. | Keep on disk, adopt only where the plates ask for it. Delete the redundant ambientCG zips. |

**Device findings, first look (`5deecbd`)**

| # | Report | Change |
|---|---|---|
| 16 | The ground reads as a white/bluish floor with no grass or soil. | The layered terrain program is the only shader whose absence leaves the ground drawn by whatever program was last bound (the sky). `drawTerrain` now falls back to the plain PBR program and the base layer, and a failed terrain shader is survivable while a failed lit or depth shader is reported on the splash. |
| 17 | The plane is never on the ground, even in the hangar. | It was always drawn at the band's tow point (4.55 m up), including the hangar view. In `ORBIT_AIRCRAFT` it now stands on the plank floor at `HANGAR_PARK_HEIGHT`; the launch setup still hangs it in the band. |
| 18 | The hangar and slingshot look disoriented. | In the hangar view the slingshot is no longer drawn, and the camera focus moved from the band height down to the parked aircraft, with the orbit clamped to 4.5–7 m so it stays inside the shed instead of swinging through the plank wall. |
| 19 | The aircraft launches sideways, off its wing, and the hangar wall blocks the camera from getting behind it. | `AircraftModelLoader` now reads the airframe's own extents: the long horizontal axis is the fuselage, and it is turned so that axis lies on Z with the propeller end on -Z (the rig's forward axis). The hangar orbit was cut to 4 m so the camera stays inside the shed, and the launch camera sits under the eave (3.6 m) with the roof no longer drawn. |
| 20 | Grass reads as a repeating pattern; soil and bare ground never break it up. | The terrain's bare-earth mask now mixes three noise scales (450 m, 125 m, 32 m) so grass grows in large irregular regions with ragged edges and dry stretches between, and `scatterGrass` varies blade height 0.6–1.6 m, thins its fringes with noise and adds a fine clump mask. |
| 21 | Large trees and buildings are missing; objects look far below and too small. | Trees were scattered across ~5 km² (~1 per 1 500 m²) at 2.6–7 m tall, so the flight path had almost none. They are now 10–20 m tall and scattered over the whole playable world (±1150 m, −1900…+700 m) at 2.6× the count. |
| 22 | Layout rebuild: the aircraft hung in the band 4.55 m up, inside the roof and over the ramp. | `SlingshotLayout.HOLD_POINT` is now **(0, 1.02, +3.0)**: fuselage centre one metre above the plank floor with the wheels on it, 3 m behind the posts and 13 m ahead of the ramp, which still starts at the door. The launch camera dropped to 2.6 m high, 5.5 m behind, so the aim view is inside the shed at aircraft height instead of above the roof. |
| 23 | The aircraft flies far above everything, so no tree or building is ever in the way. | The stick asked for up to **20° of angle of attack** against a wing that stalls at 16°, so a pull sat past the lift peak: the aircraft climbed until it stalled, then fell. The request is now clamped inside the working curve (`ALPHA_LIMIT = 0.96 × ALPHA_STALL`), trim is the efficient glide attitude, and pitch authority grows with airspeed (`speed / 18`, capped at 1.5×). |
| 24 | Once it is nose-down it will not climb again, however fast it is going. | Same cause: at full pull the deep stall produced drag instead of lift. With the request inside the curve, a dive at speed builds the most lift the wing has (`q·S·CL/max`) and the flight path arcs back up; the nose follows because `MAX_PITCH` was raised to 1.50 rad so the attitude is never clipped below what the pull asks for. |
| 25 | Trees and buildings are decoration: nothing tests them. | `world/ObstacleField` files every tree instance (crown height and width read from its own matrix), every building and every kit prop as an upright cylinder in a 32 m grid. `GameApp.updateFlight` asks it once a frame with a 1.5 m aircraft disc and calls `FlightPhysics.impact()` on a hit. Trees are kept 55 m clear of the launch pad so a take-off cannot end in a trunk. |

## 4. Asset catalog

**Shipped in `app/src/main/assets/` (~110 models + 15 maps + 5 cues)**

| Pack | Files | Licence |
|---|---|---|
| Quaternius Ultimate Stylized Nature | 22 trees/plants/rocks + 14 maps | CC0 |
| Quaternius Ultimate Animated Animals | 11 animals (`.obj` + `.mtl`) | CC0 |
| Quaternius Ships | 5 (`.obj` + `.mtl`) | CC0 |
| Quaternius Fantasy RTS | 10 buildings/mountains (`.obj` + `.png`) | CC0 |
| Poly Pizza `samolet` | biplane (`.obj` + `.mtl`) | CC BY 4.0 |
| Kenney Nature Kit | 9 models (`models/nature`) | CC0 |
| Kenney Castle Kit | 11 models + `colormap.png` (`models/castle`) | CC0 |
| Kenney Furniture Kit | 5 props (`models/furniture`) | CC0 |
| Kenney City Kit Industrial | 6 models + `colormap.png` (`models/industrial`) | CC0 |
| Kenney Fantasy Town Kit | 8 props + `colormap.png` (`models/town`) | CC0 |
| heyheythere Aircraft Kit | 1 archive, read at runtime (`kits/aircraft-kit.zip`) | CC BY 4.0 |
| Poly Haven | 13 diffuse maps (`terrain`) | CC0 |
| ambientCG | `rubber`, `hazard` colour maps (`terrain`) | CC0 |
| Kenney Interface + Impact Sounds | 5 OGG cues (`audio`) | CC0 |
| Bird | `birds/parrot.glb` | **unconfirmed** |

Three loaders cover all of it: `gfx/ObjLoader` (model with a PNG), `gfx/AnimalLoader`
(flat `Kd` materials, split per group, drawn with `textures.white` tinted), `gfx/PropLoader`
(modular kits on one 1-unit grid, loaded at a shared scale). `gfx/GlbLoader` and
`gfx/RtsModelLoader` read the bird formats.

**Missing, and what would fill it**

| Gap | Would fill | Note |
|---|---|---|
| A small documented bird | `poly.pizza` bird or an rts-models JSON | Removes issue 4 |
| Water normal map | Poly Haven / ambientCG water surface | Issue 6 |
| Far-field tree cards | Any billboard pack | Issue 7 |
| Timber shed shell | none in use — plates define it | Issue 10 stays generated |

## 5. Phases

Each phase ends with a commit, a push and a CI check; a phase is not done until CI is green
on its final commit.

**Phase 0 — Green and honest** *(in progress)*
- Fix the build. **Done** (`8d1a306`).
- Replace the three asset notes with this file; point the README at it; strip claims the
  build cannot support.
- Tidy `.gitignore` for the staging packs.
- **Exit:** CI green on `main`; no document describes an observation nobody made.

**Phase 1 — Finish the texture set**
- `leaf`: decided against — see issue 6. Recorded so nobody reopens it without a
  colour (not alpha) foliage tile.
- `waterNormals`: keep the generated one and say so, or adopt a real normal map if one can
  be fetched directly.
- **Exit:** every remaining generated texture has a recorded decision, and
  `TextureFactory` generates nothing except the 1×1 white and the two decided tiles.

**Phase 2 — Realm dressing, by what the plates ask for** *(in progress)*
- Realm-appropriate buildings where the fantasy houses read wrong (desert, coast, alpine).
  **Desert done** — the half-timbered tower house and cottage are gone; the realm is a mining
  outpost with the rock-cut mine, three mines on the horizon and its own mountains, dressed
  by the industrial kit already standing around the airfield.
- Field boundaries in the mountain realm from the Kenney Nature `fence_*` set already on
  disk. **Done** — forest, desert and highland runs in `WorldScene.buildFieldBoundaries`,
  sections spaced by the model's own width-to-height ratio.
- **Exit:** every realm reads as its own place at launch distance, with no asset that
  contradicts it.

**Phase 3 — Flight feel against the plates' numbers**
- Reconcile `FlightPhysics` and the HUD with `94 km/h @ 167 m` and `37 km/h @ 370 m`.
- Only tune constants; do not restructure the glider model.
- **Exit:** a competent launch produces a curve in that shape.

**Phase 4 — UI fidelity pass**
- Compare each screen's layout to its plate, one screen per commit: hangar, launch setup,
  aim gauge, flight HUD, results.
- **Exit:** every screen's element list matches its plate, checked screen by screen.

**Phase 5 — Release readiness**
- Signed release APK, Play Protect note for sideloads, performance budget on a mid-range
  phone, licence page complete.
- **Exit:** an installable signed APK plus an honest known-issues list.

## 6. Standing rules

1. **References outrank documents.** `Ref-1` … `Ref-7` > `DESIGN.md` > `BRIEF.md` >
   `MASTER_BRIEF.md`.
2. **Never big-bang delete.** Import → loader → switch behind a fallback → delete the old
   builder in its own commit. The APK stays playable at every commit.
3. **CI is the only build.** One push, one check; never loop on a running job. A red build is
   the next commit's first job.
4. **Only what ships is what is placed.** Whole packs stay in `assets-source/` (git-ignored);
   the models actually used are copied into `assets/`.
5. **Say what was verified.** "By inspection" is the honest label for anything not seen on a
   device.
6. **Do not import for its own sake.** Where the plates describe something no pack provides,
   the generated version is the correct answer and is recorded as a decision.

## 7. Log

- **8d1a306** — Phase 0: build fixed (SoundBank context capture, expression-body returns in
  the two bird loaders, a `/*` inside a KDoc that opened a nested comment).
- **915367a** — Phase 0: this roadmap written; the three asset notes folded in; README and
  `.gitignore` corrected.
- **3283780** — kit pieces sized by their own height; `structures` (airfield buildings, town
  props, castle obstacles) added to the shadow pass.
- **645d7b9** — Phase 2: field boundaries for forest, desert and highland from the Kenney
  Nature `fence_simple` sections, spaced by the model's own width-to-height ratio.
- **c94649d** — Phase 1 closed with a decision on `leaf`; unused fence corner removed.
- **88424c3** — Phase 2: the desert becomes a mining outpost and gains a skyline.
- **845d44c** — the bird reader is chosen from the files actually in the APK, so the missing
  `bird.json` no longer logs a load failure.
- **9d811c8** — bird lookup written null-safe so it cannot fail to compile.
- **f0f0222** — startup cannot die silently: `GameApp` catches the engine build and the
  splash prints the failure; `WorldScene` guards each asset group; `GlbLoader` refuses any
  blob over 4 MB (the parrot is 12.2 MB), so the flock falls back to the procedural birds.
- **85d1dc6**, **efc9e74** — a world or a frame that throws is reported on the splash screen
  instead of ending the process; the splash stays up while an error is set.
- **5deecbd** — the crash report and its handling written down.
- **91a2f33** — first device-look fixes: terrain falls back to plain PBR when its layered
  program is missing, the aircraft stands on the hangar floor in the hangar view, the
  slingshot is hidden there, and the hangar camera focuses the parked plane from inside.
- **6df1cb0** — the airframe is turned onto the flight axis from its own extents; the hangar
  orbit is kept inside the shed.
- **859004e**, **ad18de7**, **59183b3** — the meadow pattern broken up, mature timber across
  the whole flyable world, farmsteads and towers scattered in every realm.
- **e532d57**, **383cf59** — the launch stands inside the shed, the release ribbon is gone,
  a retry resets to the aim camera, and the aircraft waits on the hangar floor ahead of the
  ramp.
- **this commit** — flight feel and obstacles: the stick's angle-of-attack request is clamped
  inside the lift curve so a dive can be pulled out of at speed, pitch authority follows
  airspeed, and every tree, building and prop is now a solid cylinder the flight model tests
  each frame.
