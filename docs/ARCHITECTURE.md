# Architecture

## Layers

```
MainActivity (android.app.Activity)
  └── GameSurfaceView (android.opengl.GLSurfaceView, ES 3.0, landscape, immersive)
        └── GameRenderer (GLSurfaceView.Renderer)
              └── GameApp            ← engine owner, built on the GL thread
                    ├── SceneRenderer        (gfx)   passes, programs, uniforms
                    │     ├── ShadowMap
                    │     ├── SkyRenderer
                    │     ├── WaterRenderer
                    │     └── ParticleRenderer
                    ├── WorldScene            (world) terrain + props + wildlife + weather
                    ├── AircraftBuild/Stats/Mesh (aircraft)
                    ├── FlightPhysics         (aircraft)
                    ├── Slingshot + mesh      (slingshot)
                    ├── CameraDirector        (game)
                    ├── PlayerProfile + SaveStore (game)
                    └── AudioEngine           (audio)
```

`GameApp` is created once the EGL context exists (`onSurfaceCreated`) because every
GL object must live on the GL thread. `GameRenderer.onDrawFrame` drives a fixed-step
update followed by a single render pass.

## Kotlin / engine integration

The brief requires documenting how the application layer and the rendering technology
divide the work (brief §9). Here the split is simple and fully Kotlin:

- **Android application layer (Kotlin, framework APIs):** lifecycle, EGL/GL surface
  configuration, touch input, persistence (`SharedPreferences`), audio output
  (`AudioTrack`), logging.
- **Rendering/gameplay layer (Kotlin, `android.opengl.GLES30`):** all scene graph,
  geometry generation, shaders (GLSL ES 3.00 source strings), physics, economy,
  progression.

No Java, C/C++, engine editor, engine runtime or scripting language is used. The only
"engine" language beyond Kotlin is GLSL, which is embedded as string constants in
`gfx/ShaderSources.kt`.

## Renderer

**Passes per frame**

1. **Shadow pass** — depth-only FBO (1536², `DEPTH_COMPONENT24`), orthographic light
   camera fitted around the player focus. Casters: aircraft, slingshot, trees, rocks.
2. **Main opaque pass** — sky (depth-test off), terrain, instanced vegetation, aircraft,
   slingshot, wildlife.
3. **Transparent pass** — water (depth-write off, alpha blend), particles.

**Shading** (`ShaderSources.FRAG_COMMON`)

- GGX distribution, Smith–Schlick geometry, Schlick Fresnel.
- Directional sun with 3×3 PCF shadow lookup and slope-scaled bias.
- Hemispheric ambient from sky/ground colors (single-scattering approximation).
- Exponential-squared distance fog using the world palette fog colour.
- Tone mapping `c/(c+1)` followed by gamma 2.2.

**Terrain**

`TerrainGrid` builds one mesh with a non-uniform axis: 6 m spacing inside the flight
corridor (±400 m × 1900 m) and geometrically growing spacing out to ±3.6 km / 5 km.
Result: ~66k vertices covering the whole visible world in one draw call. Normals are
derived from grid neighbours, UVs are world-scaled, and the fragment shader blends
three surface materials by slope and altitude (rock on steep faces, a detail layer for
sand/snow/ice above a per-world line).

**Textures**

All surface maps are generated at load time from value-noise/FBM into `ByteArray`s and
uploaded as mip-mapped, repeat-wrapped `GL_RGBA` textures. The generator is deterministic
(seeded), so worlds look identical across devices and sessions without shipping large
binaries.

**Instancing**

Trees, rocks and grass are drawn with per-instance 4×4 matrices in a dynamic VBO
(attribute locations 4–7). Wildlife instance matrices are rewritten per frame.

## Simulation

- Fixed physics step of 1/60 s accumulated from the frame delta (max 3 substeps).
- Flight model: thrust along the aircraft forward axis, lift from angle of attack
  (`CL ≈ 2π·α`, clamped, scaled by wing area/efficiency), quadratic drag along the
  relative airflow, gravity, and stability terms that align the aircraft with the flight
  path. Wingless aircraft have negligible lift and therefore fall — which is exactly the
  progression hook.
- The aircraft orientation is a YXZ Euler triple (yaw, pitch, roll) so the mesh, camera
  and physics share a single convention.

## Progression

`PlayerProfile` (coins, best distance, best altitude, per-world bests, part tiers,
slingshot levels, launch angle, unlocked worlds, claimed achievements, settings) is
persisted as JSON in `SharedPreferences`. `AircraftCatalog` prices parts,
`SlingshotState` prices upgrades, `Economy` converts a flight into coins,
`AchievementCatalog` evaluates badge progress and `GameSettings` holds graphics/audio
preferences.

## UI layer

The UI is a single transparent `GameOverlayView` added above the `GLSurfaceView` in the
Activity's `FrameLayout`. The window draws the overlay over the SurfaceView's punched-out
region, so screens composite over the live 3D scene without touching the render loop.

```
MainActivity (FrameLayout)
  ├── GameSurfaceView  → GameRenderer → GameApp  (3D world)
  └── GameOverlayView  → UiFrame → screens/*     (design.md §6)
```

- `gfx/DesignTokens.kt` — the design system (colours, type scale, spacing, radii, safe
  areas) transcribed from `docs/DESIGN.md`; no screen hard-codes a colour.
- `ui/UiPainter.kt` — Paint/typeface set, `UiTextStyle` scale, number formatting.
- `ui/UiFrame.kt` — immediate-mode primitives: glass panels, buttons, pills, badges,
  toggles, sliders, progress bars, vector icons, tracked/wrapped text, hit registry.
- `ui/UiIcons.kt` — resolution-independent vector telemetry icons.
- `ui/GameOverlayView.kt` — `UiNav` implementation: screen routing, touch, slider drags,
  workshop orbit drags, pause modal. One `UiFrame` per frame in a 640×360dp design space
  that adapts to any landscape ratio.
- `ui/screens/` — menu, aircraft workshop, slingshot lab, environment select, flight HUD,
  results, reward, settings, achievements, career profile, pause.

**Threading.** Screens read live data from `GameApp` and never touch GL. Every mutating
action goes through `GameApp.request*`, which queues a `Runnable` drained at the start of
the next GL frame.

**Seams that remain:**

- `game/HudBridge.kt` — optional non-UI observer, still available for headless runs.
- `GameApp` public `request*` methods — the complete action surface the UI uses.
- `game/GraphicsQuality` (in `game/Economy.kt`) — four tiers scaling shadow coverage,
  vegetation density and particle load.
