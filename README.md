# Build • Launch • Fly

A premium 3D aircraft-building, slingshot-launch and distance-adventure game for Android.
Native Android application, Kotlin application layer, custom real-time 3D engine on
OpenGL ES 3.0, and the complete landscape UI system specified in `design.md`.

**Read first:**

- [`docs/MASTER_BRIEF.md`](docs/MASTER_BRIEF.md) — creative and technical directive.
- [`docs/DESIGN.md`](docs/DESIGN.md) — normative visual/UI specification (tokens, five
  ecosystems, aircraft stages, all ten screens).

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

**Aircraft (`aircraft`)** — six-stage construction exactly as specified: bare fuselage with
tinted canopy → left wing → right wing → tail → propeller → stage-6 winglets, tail
fairings and engine cowling. Each purchase rebuilds the visible mesh. Fuselage uses the
specified composite values (roughness 0.35, metallic 0.15); the canopy is its own
low-roughness glass material.

**Slingshot (`slingshot`)** — five upgrade categories, launch-speed model, visible bands
stretched from both prongs to the cradle, tinted amber → neon cyan with draw tension,
launch elevation arc (20°–55°) stored in the profile, and a camera impulse shake on
release.

**Flight (`aircraft/FlightPhysics`)** — angle-of-attack lift, quadratic drag, thrust,
gravity, stability terms, stall behaviour, ground contact with landing/crash resolution.

**UI (`ui`)** — every screen from `docs/DESIGN.md` §6, driven by live data:

1. Main Menu (profile pill, level/rank, coins, hotspots, nav deck, primary CTA)
2. Aircraft Workshop (five tabs, airframe stat bars, part drawer with deltas and price)
3. Slingshot Lab (telemetry, five upgrade rows with level pips, elevation-arc slider)
4. Environment Selection (five cards, weather/wind, personal best, lock gates)
5. Gameplay HUD (distance/altitude/airspeed, attitude gimbal, stall and thermal alerts,
   launch tension meter)
6. Flight Results (record banner, breakdown ledger, retry/workshop/claim)
7. Upgrade & Reward (installed part, resulting stats, equip action)
8. Settings (four quality tiers, three fidelity toggles, three audio sliders)
9. Progress & Achievements (six claimable badges with progress)
10. Pilot Career Profile (career totals, favourite realm, configuration)

Plus a pause modal. Design tokens live in `gfx/DesignTokens.kt`; the UI is drawn at a
640×360 dp design scale that stretches to any landscape ratio without distortion.

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

## CI and Android packages

`.github/workflows/android.yml` runs on pushes to every branch and on pull requests. It builds two distinct APKs:

- **`Build-Launch-Fly-TEST-SIGNED.apk`** — installable QA release variant, signed with Android's standard debug certificate so testers can install it without an unsigned-package error. This is a test signature, not a production identity; Android or Play Protect may still show a sideloading warning. It must not be used for public distribution.
- **`Build-Launch-Fly-DEBUG.apk`** — debug variant for diagnostics; it is not the recommended tester package.

CI verifies the QA APK signature, zip alignment and package ID (`com.buildlaunchfly.game`) before uploading it. For production distribution, configure a private upload/release key in protected CI secrets and sign the release with that key. Never commit the private keystore or passwords.

## Controls

- **Menu:** tap the nav deck (Aircraft / Slingshot / Realms), gear, trophy or pilot icon.
- **Realms:** tap a card to select it, then *Launch in this realm*.
- **Launch:** hold anywhere to draw the bands, release to fire.
- **Flight:** swipe down for nose up, swipe up for nose down, swipe right to yaw right, and swipe left to yaw left; expand the pause button to stop the flight. Pitch and yaw are independent controls.
- **Workshops / menu:** drag on empty space to orbit the 3D aircraft or rig.

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

## Known gaps (honest list)

- The propeller has a high-RPM translucent blur-disc pass and individually animated blades; appearance and performance still require target-device validation.
- Rajdhani / Plus Jakarta Sans are substituted with system families of the same sizes,
  weights and tracking (see `docs/DESIGN.md` §2.2 implementation note).
- UI ships as procedural vector drawing instead of 9-patch/SVG slices; the layout hooks
  are in `ui/UiFrame.kt` and `ui/UiIcons.kt`.
- Workshop orbit is drag-only (no pinch zoom).
- Wildlife uses simple procedural meshes with scripted motion, not final authored/rigged animal assets.
- Five ecosystems are temporarily unlocked for QA; restore progression gates only after ecosystem testing is accepted.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the engine and UI architecture.
