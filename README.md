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
  Textures toggle) — the build ships no binary assets, so CI cannot miss files.

**Worlds (`world`)** — five ecosystems with distinct terrain shaping, palettes,
atmosphere, water level, vegetation, wildlife and weather: forest, desert, snow/glacier,
ocean/coast, mountain/countryside. Non-uniform terrain grid (dense corridor, cheap
horizon).

Vegetation is scattered per species instead of as one cloned population: three tree shapes
(broadleaf, conifer, poplar), three shrubs, three rock forms and three tall grass clumps, each
with its own mesh and tint, chosen from noise fields so stands cluster the way real plants do.
Grass grows only in noise-driven meadows at roughly three to four feet tall; between them the
terrain shader exposes bare soil patches, so the ground reads as a mix of grass, earth and
rock rather than a uniform green carpet.

**Aircraft (`aircraft`)** — procedural vintage biplane per
[`docs/DESIGN.md`](docs/DESIGN.md) §3.14: tan doped canvas upper/lower wings with
orange-brown rib decals, a narrow timber lattice fuselage, cabane and interplane struts,
X-braced wires, a dark cockpit opening, two wheels and a green two-blade propeller with a
spin-blur disc. Geometry is split by material (canvas, rib, wood, metal, glazing,
propeller), so every stage changes visible structure rather than colour: wing halves,
longer Mk II spans, tail end-plates and the engine block each arrive as separate pieces.

**Slingshot (`slingshot`)** — five upgrade categories and exactly the reference rig: two plain
squared wooden posts with a slight outward splay, standing about 1.2× the wingspan apart, and
**one** striped elastic sling tied to both tips and meeting at a single tow point on the
fuselage at the wing root — no ramp, no crossbar, no braces, no metalwork. The sling sags when
slack and pulls straight as the draw deepens; sling thickness and draw length follow the
upgrade levels, every segment casts a shadow, and the release plays a recoil wobble, a dust
puff and a camera impulse. Launch elevation (20°–55°) is stored in the profile.

**Hangar (`world/HangarMesh`)** — the home screen sits inside the reference barn: a dark
weathered plank wall down the left, a heavy beamed roof over the whole rig, a warm tan plank
floor, corner posts down the open right side and crates and bales outside in the daylight. The
hangar view is a fixed close rear-side oblique like Ref-2; the launch view stays centred
behind the aircraft under the roof eave, as in Ref-3.

**Flight (`aircraft/FlightPhysics`)** — angle-of-attack lift, quadratic drag, thrust,
gravity, stability terms, stall behaviour, ground contact with landing/crash resolution.

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
SharedPreferences JSON, world unlocks, achievements, four quality tiers, and procedural
audio (wind, propeller, creak, release, per-world ambience, UI cues) with three mixer
faders.

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
- Wildlife uses simple procedural meshes with scripted motion.
- Ecosystem layouts are still procedural: reference plates in `docs/reference/` guide art
  direction but are not baked into authored terrain or building assets.
- Play Protect warnings on self-signed sideloads are outside the app's control; publishing to
  Play would require a Play-signed build and a developer account.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the engine and UI architecture.
