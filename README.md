# Build • Launch • Fly

A premium 3D aircraft-building, slingshot-launch and distance-adventure game for Android.
Native Android application, Kotlin application layer, custom real-time 3D engine on
OpenGL ES 3.0.

**Read first:** [`docs/MASTER_BRIEF.md`](docs/MASTER_BRIEF.md) — the central creative and
technical directive for this project.

## Current status

This repository currently contains the **engine and gameplay foundation** (Phases 2–5 of the
brief). The UI/UX layer is deliberately **not** implemented yet: the design system is
pending `design.md`, which will drive all screens, typography, HUD and visual identity
(Phase 1).

What is implemented right now:

- Native Android shell: landscape, fullscreen, immersive, `GLSurfaceView` with an
  ES 3.0 context, lifecycle-safe, keeps the screen on.
- Custom 3D engine in Kotlin (`gfx` package):
  - GLSL ES 3.00 program management, VAO/VBO/IBO meshes, instanced rendering.
  - Cook–Torrance style PBR shading (GGX + Smith + Fresnel), directional sun,
    hemispheric ambient, exponential fog, ACES-like tone mapping, gamma output.
  - Single-cascade directional shadow map with 3×3 PCF.
  - Analytic sky dome with sun disk and cloud banding.
  - Normal-mapped water with Fresnel, sun specular and shore blending.
  - Particle renderer (snow, dust, spray).
  - Procedurally generated, mip-mapped, tiling surface textures (grass, rock, sand,
    snow, ice, bark, foliage, metal, rubber, water normals) — no binary assets required,
    so the build has no asset pipeline and no missing-file risk.
- World system (`world` package): five worlds (forest, desert, snow/glacier, ocean/coast,
  mountain/countryside) with per-world terrain shaping, palettes, atmosphere, water level,
  vegetation density, weather and wildlife counts. Non-uniform terrain grid gives dense
  geometry in the flight corridor and cheap geometry toward the horizon.
- Aircraft system (`aircraft` package): per-slot part tiers, physically visible parts
  (body, left wing, right wing, tail, propeller, engine), derived flight statistics,
  procedural mesh rebuilt when parts change, spinner animation.
- Slingshot system (`slingshot` package): five upgrade categories, launch-speed model,
  procedural 3D frame and elastic bands that stretch with the pull.
- Flight physics (`aircraft/FlightPhysics.kt`): lift from angle of attack, drag, thrust,
  gravity, stability terms, stall behaviour, ground contact detection, landing vs crash.
- Progression (`game` package): earnings model, save file (SharedPreferences + JSON),
  part prices, slingshot upgrade prices, world unlock gating.
- Procedural audio (`audio` package): synthesized wind, propeller, slingshot creak and
  release, per-world ambience and UI cues via `AudioTrack`. No audio assets needed.
- Headless game flow: pull to stretch, release to launch, fly, earn, return to the pad.
  The full loop is playable without any UI.

Not implemented yet (blocked on `design.md` / later phases):

- Every screen (main menu, workshops, world select, HUD, results, settings).
- HUD/state wiring (`HudBridge` exists as the seam it will plug into).
- Authored high-poly art, animations, authored audio mix, tutorial, achievements UI.

## Build

The build requires JDK 17 and the Android SDK (API 34).

```bash
gradle :app:assembleDebug
```

The produced APK is installable directly:

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
4. `assembleDebug` → installable APK artifact
5. `assembleRelease` (unsigned) + artifact upload

## Controls (temporary, pre-UI)

- **Drag** on the launch pad: stretch the slingshot bands.
- **Release**: launch.
- During flight, **drag** to pitch/roll the aircraft.
- After a flight the session automatically banks the earnings and returns to the pad.

## Repository layout

```
app/                      Android application module
  src/main/java/com/buildlaunchfly/game/
    platform/             Activity, GLSurfaceView, renderer, input
    gfx/                  renderer core: shaders, meshes, shadows, sky, water, particles
    world/               five worlds, terrain, vegetation, wildlife, weather
    aircraft/            parts catalog, build state, mesh, flight physics
    slingshot/           upgrades, launch model, mesh
    game/                session flow, economy, progression, save, camera director
    audio/               procedural audio engine
    math/                Vec3, Mat4, utilities
docs/                     master brief, architecture, design rules
.github/workflows/        automated APK builds
```

## Engine choice

The brief leaves the 3D technology open (Phases 2 and 8). This project uses a
purpose-written renderer on **OpenGL ES 3.0** driven from Kotlin, which:

- keeps the entire game logic and rendering in Kotlin as the brief prefers,
- removes third-party native build steps from CI (no NDK, no engine downloads),
- gives direct control over shadows, PBR, fog and terrain LOD, which is what the visual
  bar depends on.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full decision record and
[`docs/DESIGN.md`](docs/DESIGN.md) once the design system is supplied.
