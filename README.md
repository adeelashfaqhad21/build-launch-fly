# Build • Launch • Fly

A premium 3D aircraft-building, slingshot-launch and distance-adventure game for Android.
Native Android application, Kotlin application layer, custom real-time 3D engine on
OpenGL ES 3.0, and the portrait UI system modelled on the reference plates.

**Read first:**

- [`docs/MASTER_BRIEF.md`](docs/MASTER_BRIEF.md) — the original creative and technical
  commission (historical reference; its landscape and realism requirements are superseded
  by the plates).
- [`docs/BRIEF.md`](docs/BRIEF.md) — working brief derived from the reference plates.
- [`docs/DESIGN.md`](docs/DESIGN.md) — the design system and **per-component catalogs with
  implementation checklists** (hangar, currencies, upgrade cards, promos, slingshot, launch,
  aim gauge, flight HUD, results, aircraft, environments, camera, settings, economy,
  performance).
- [`docs/reference/`](docs/reference/README.md) — **authoritative** visual references
  (`Ref-1` … `Ref-7`): the primary truth for design, UI and engine look (documentation only,
  never packaged in the APK).

## Current status

Engine, gameplay and the full screen set are implemented.

**3D engine (`gfx`)**

- GLSL ES 3.00 programs, VAO/VBO/IBO meshes, instanced rendering.
- Cook–Torrance style PBR (GGX + Smith + Fresnel), directional sun, hemispheric ambient,
  exponential fog, tone mapping, gamma output.
- Single-cascade directional shadow map with 3×3 PCF.
- Analytic sky with sun disk, glow and cloud banding.
- Normal-mapped water with Fresnel, sun specular and the "Dynamic Water Reflections"
  toggle from Settings.
- Particle renderer (snow, dust).
- Procedurally generated, mip-mapped tiling textures at 256² or 128² (High-Resolution
  Textures toggle). The terrain ground layers have been replaced with imported Poly Haven
  CC0 photos (grass, rock, sand, snow, soil) loaded from `assets/terrain`; the generated
  versions remain as a fallback so a missing or renamed art file can never break a world.
  The slingshot band, the propeller blur disc and the birds read imported rubber and
  painted-metal maps the same way. Only `leaf` (procedural canopies) and `waterNormals`
  (water shader) are still generated.

**Worlds (`world`)** — five ecosystems with distinct terrain shaping, palettes,
atmosphere, water level, vegetation, wildlife and weather: forest, desert, snow/glacier,
ocean/coast, mountain/countryside. Non-uniform terrain grid (dense corridor, cheap
horizon).

Vegetation is scattered per species instead of as one cloned population: three tree shapes,
three shrubs, three rock forms and three tall grass clumps, each with its own mesh and tint,
chosen from noise fields so stands cluster the way real plants do. The species set itself
changes with the realm — broadleaf/conifer/poplar in the forest, saguaro cacti and bare
wind-worn trees in the desert, three conifer forms in the snow and mountains, leaning palms
and reeds on the coast — so each ecosystem reads differently rather than repeating one set.
Grass grows only in noise-driven meadows at roughly three to four feet tall; between them the
terrain shader exposes bare soil patches, so the ground reads as a mix of grass, earth and
rock rather than a uniform green carpet.

The external nature packs that were investigated for this are reviewed in
[`docs/ASSETS.md`](docs/ASSETS.md) with licences, formats, style fit, animation requirements
and the decision. **Quaternius' Ultimate Stylized Nature pack (CC0) is adopted** as a
near-field detail layer, mapped per realm: birch/maple/broadleaf for the forest, three pines
for the alpine worlds, four palms for the coast, dead trees for the desert, and bushes, flower
clumps, grasses, plants and three rock forms everywhere. They are loaded from OBJ at runtime
(`gfx/ObjLoader`, `gfx/AssetTextures`) and instanced in a 18–140 m ring around the launch area,
so they dress the hangar approach and line the first part of every flight. **They now load at
every quality level** — only the number of copies follows the graphics setting — so the
imported packs are never invisible on a mid-range phone.

The **Kenney Nature Kit** (CC0) adds what that pack leaves out: saguaro cacti in the desert,
palms and tall grass on the coast, extra bushes and boulders in the forest and the alpine
realms. Those models carry no texture at all — each one is a handful of flat material colours
in its MTL — so they are split by material at load time and drawn with the shared white map
tinted by each group's own colour, exactly like the animals and ships.

**All five realms are unlocked while the game is in development** (`PlayerProfile.ALL_WORLDS_UNLOCKED`),
so every environment can be tested; the distance-gated progression is retained behind that
switch for the release pass.

**Aircraft (`aircraft`)** — the flying biplane is the imported **samolet** model (CC BY 4.0,
`assets/aircraft/samolet/model.obj` + `materials.mtl`), not generated geometry. It is one welded
mesh with seven flat material colours and no texture, so `aircraft/AircraftModelLoader` splits
it by material and `AircraftMesh` draws each part with the shared white map tinted by its own
colour; the procedural airframe (flying-wing box biplane with canvas, rib, timber, metal and
glazing materials) is still built and drawn only if the OBJ cannot be read, so a missing asset
can never leave the hangar empty. Because the imported model has no separate wing, tail or
propeller objects, build stages change paint, the engine and the tail rather than the whole
wing geometry.

**Slingshot (`slingshot`)** — five upgrade categories and the rig the brief asks for, and
nothing else: **two wide squared timber posts**, one on each side, and **one central elastic
band** strung between their tips. There is no ramp-side lattice, no crossbar, no mast and no
extra structure. The aircraft hangs on the middle of the single band, which is drawn as one
continuous curve from tip to tip through the tow point. The band sags when slack, pulls
straight as the draw deepens, and its thickness and draw length follow the upgrade levels;
the release plays a recoil wobble, a dust puff and a camera impulse.

**Hangar (`world/HangarMesh`)** — the home screen sits inside the reference shed: a sawn-plank
wall down the left on vertical studs, an open right side between heavy posts, a gable roof on
a ridge beam with exposed rafters, tie beams and purlins, a plank floor, a closing back wall,
a heavy timber door frame at the open front, and the clutter of a working airfield (hay bales,
crates, a barrel, a bench and stool, a lamp hanging from the ridge). A **six-metre launch ramp**
rises one metre directly in front of the open end, so the aircraft runs out of the shed and up
the slope; the ramp lip also adds a small upward push to the launch. The furniture standing on
the floor — workbench, stool, bench, boxes and drum — is imported from the **Kenney Furniture
Kit** (CC0), split by its flat material colours the same way as the nature and castle models.

**Scale** — the slingshot stands 4.85 m tall with 3.3 m post spacing, and the aircraft is drawn
at 1.5× so both are the hero elements of the launch shot, as in the plates.

**Fleet (`world/WorldScene`)** — the ocean realm anchors five Quaternius ships at the water
line (Viking boat, sailing boat, lifeboat, tall ship, cruise liner), each rocking slowly on the
swell. Flat MTL colours, instanced, no extra state.

**Landmarks (`world/WorldScene`)** — the Quaternius fantasy RTS set dresses the worlds: houses,
a windmill and a watchtower in the forest; town centre and tower house in the alpine realms;
a dock on the coast; a mine in the desert; crates and a barrel at the launch pad; and large
mountain meshes building the skyline 300–700 m out. Loaded through `gfx/PropLoader` with each
model's own PNG texture.

The **Kenney Castle Kit** (CC0) raises a ruin on every realm's skyline: a three-piece tower
(base, mid, roof) with a wall run beside it. The kit is authored on a single 1-unit grid, so
all of its pieces load at one shared scale (`PropLoader.loadScaled`) chosen to make the tower
15 m tall, rather than each part being normalised to its own height — otherwise the stacked
tower would not line up. The pieces share the kit's `colormap.png` atlas.

**Flight (`aircraft/FlightPhysics`)** — a glider model, because the brief makes the slingshot
the only launch power: after release the aircraft flies on momentum and aerodynamics. Lift
comes from angle of attack (nose angle relative to the airflow), with a real lift curve that
falls away past 16°; pulling the nose up buys lift but adds induced drag, so airspeed bleeds
away and the wing stalls. Banking tilts the lift vector, and that is what curves the flight
path around trees and buildings — there is no separate "steer" force. A gearless, wingless
airframe really cannot fly. The default build is a complete airframe (fuselage, both wings,
tail, propeller) so the first launch is playable; the engine and Mk II surfaces remain
upgrades.

**UI (`ui`)** — portrait screens modelled on the reference plates, with the full component
catalog and checklists in [`docs/DESIGN.md`](docs/DESIGN.md) §3:

1. Splash / loading (brand + real load progress)
2. Hangar home (3D hangar set, currency pills, plaque, promo rail, chest/VIP/rocket,
   `TAP TO FLY`)
3. Upgrade cards: SLINGSHOT / PLANE / INCOME with level pips and prices
4. Launch setup (plane in the rig, bottom BACK)
5. Aim gauge (segmented arc with a green perfect zone)
6. Flight HUD (speed dial, pause, distance, progress track, PERFECT / LOW SPEED / STALL)
7. Results with a reward breakdown and count-up
8. Settings, achievements/career, and the settings/career surfaces

Design tokens live in `gfx/DesignTokens.kt` and now carry the reference palette (white cards,
gold/gem/blue/green/purple accents, navy outlines); the UI is drawn at a 360×640 dp portrait
design scale. Typefaces bundled under SIL OFL (`app/src/main/res/font`, licences in
`docs/licenses/`): **Fredoka** for headings/buttons/HUD numerals, Plus Jakarta Sans for body,
Rajdhani retained for instrumentation.

**Orientation:** portrait. The master brief asked for landscape, but the reference plates
(`Ref-1` … `Ref-7`) are portrait captures and take precedence — see
[`docs/reference/README.md`](docs/reference/README.md).

**Progression & audio** — coins from distance/performance, persistence in
SharedPreferences JSON, world unlocks, achievements, four quality tiers, and audio with three
mixer faders: wind, propeller, creak and the per-realm ambience are synthesised, while UI
clicks, confirmations, unlocks, results and the slingshot release play the imported Kenney
samples (`audio/SoundBank`) with the synth as fallback.

## Build

Requires JDK 17 and the Android SDK (API 34).

```bash
gradle :app:assembleDebug
```

Installable output:

```
app/build/outputs/apk/debug/app-debug.apk
```

There is intentionally **no Gradle wrapper JAR** in the repository (binary files are not
checked in); CI installs Gradle 8.7 explicitly. Locally, use any Gradle 8.7+ install.

## CI

`.github/workflows/android.yml` runs on every push and pull request:

1. JDK 17 (Temurin)
2. Android SDK setup (licenses accepted)
3. Gradle 8.7
4. `assembleDebug` → debug APK artifact
5. Release keystore (from `BLF_*` repository secrets when present, otherwise a cached or
   generated one) → **signed** `assembleRelease` APK artifact
6. `apksigner verify --print-certs` is printed in the log to confirm the signature

## Controls

- **Menu:** tap the nav deck (Aircraft / Slingshot / Realms), gear, trophy or pilot icon.
- **Realms:** tap a card to select it, then *Launch in this realm*.
- **Launch:** hold anywhere to draw the bands, release to fire.
- **Flight:** drag to pitch and roll; the pause button stops the flight.
- **Workshops / menu:** drag on empty space to orbit the 3D aircraft or rig, pinch with two
  fingers to zoom.

## Repository layout

```
app/                      Android application module
  src/main/java/com/buildlaunchfly/game/
    platform/             Activity, GLSurfaceView, renderer, input
    gfx/                  renderer core, design tokens, materials, shadows, sky, water
    world/                five worlds, terrain, vegetation, wildlife, weather
    aircraft/             parts catalog, build state, mesh, flight physics
    slingshot/            upgrades, launch model, mesh
    game/                 session flow, economy, progression, achievements, settings, save
    ui/                   overlay view, frame primitives, icons, ten screens
    audio/                procedural audio engine
    math/                 Vec3, Mat4, utilities
docs/                     master brief, design system, architecture
.github/workflows/        automated APK builds
```

## Installing the CI builds

Use the **`build-launch-fly-release-signed`** artifact. The release build is signed (from
`BLF_*` repository secrets when set, otherwise a cached CI keystore) and not debuggable, so
Android installs it over the previous signed build.

Why the two APKs behaved differently before:

- **Release APK — "Can't install app":** it was built unsigned, and Android refuses an APK
  with no signature. The release build type now always has a signing config (a real keystore
  when one is provided, the debug key otherwise), so it can never ship unsigned again.
- **Debug APK — Play Protect "Harmful app blocked":** this is Play Protect's standard warning
  for a debug-signed app installed from outside the store, not an indication of anything in
  the code. It cannot be removed by the app itself; installing the signed release APK above
  avoids the debug certificate. If Play Protect still offers "Install anyway", that is the
  expected path for a self-signed sideload.

## Known gaps (honest list)

- UI ships as procedural vector drawing instead of 9-patch/SVG slices; the layout hooks are
  in `ui/UiFrame.kt` and `ui/UiIcons.kt`.
- Wildlife: ground animals are Quaternius models (static pose, flat MTL colours) driven by the
  existing scripted grazing paths, two or three species per realm — deer, fox and dog in the
  forest, donkey and white horse in the desert, husky and wolf in the snow, cattle and alpaca
  on the coast, stag and bull in the mountains. Birds use the imported `parrot.glb` through
  `gfx/GlbLoader` (a BufferGeometry JSON and the old triangles remain as fallbacks); its
  licence is not yet documented. Skeletal animation would need a skinning pass.
- Ecosystem layouts are still procedural: reference plates in `docs/reference/` guide art
  direction but are not baked into authored terrain or building assets.
- Play Protect warnings on self-signed sideloads are outside the app's control; publishing to
  Play would require a Play-signed build and a developer account.

## Credits

- **Quaternius** (CC0): Ultimate Stylized Nature, Ultimate Animated Animals, Ships and
  Ultimate Fantasy RTS packs — `docs/licenses/Quaternius-*.txt`.
- **Kenney** (CC0): [Nature Kit](https://kenney.nl/assets/nature-kit),
  [Castle Kit](https://kenney.nl/assets/castle-kit),
  [Furniture Kit](https://kenney.nl/assets/furniture-kit),
  [City Kit (Industrial)](https://kenney.nl/assets/city-kit-industrial),
  [Fantasy Town Kit](https://kenney.nl/assets/fantasy-town-kit),
  [Interface Sounds](https://kenney.nl/assets/interface-sounds) and
  [Impact Sounds](https://kenney.nl/assets/impact-sounds) —
  `docs/licenses/Kenney-NatureKit-CC0.txt`, `docs/licenses/Kenney-CastleKit-CC0.txt`,
  `docs/licenses/Kenney-FurnitureKit-CC0.txt`, `docs/licenses/Kenney-Audio-CC0.txt`.
- **Poly Haven** (CC0): the thirteen ground/terrain and wood/bark diffuse maps in
  `assets/terrain` — `docs/licenses/PolyHaven-CC0.txt`.
- **ambientCG** (CC0): the rubber and painted-metal colour maps in `assets/terrain`
  ([Rubber 004](https://ambientcg.com/a/Rubber004),
  [Painted Metal 016](https://ambientcg.com/a/PaintedMetal016)) —
  `docs/licenses/AmbientCG-CC0.txt`.
- **"Low-Poly Aircraft Kit Free" by heyheythere** —
  <https://heyheythere.itch.io/low-poly-aircraft-kit-free>, licensed **CC BY 4.0**
  (<https://creativecommons.org/licenses/by/4.0/>). Used: the windsock and traffic cone,
  loaded from the shipped kit archive. Licence record in
  `docs/licenses/LowPolyAircraftKit-CC-BY-4.0.txt`.
- **"samolet" by vrlab development** — <https://poly.pizza/m/6OgsLgd7vKM>, licensed
  **CC BY 4.0**. The vintage biplane the game flies, shipped as
  `assets/aircraft/samolet/`. Licence record in
  `docs/licenses/PolyPizza-Aircraft-CC-BY-4.0.txt`.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the engine and UI architecture.
