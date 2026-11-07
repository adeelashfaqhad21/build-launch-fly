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

`PlayerProfile` (coins, best distance, part tiers, slingshot levels, selected/unlocked
worlds) is serialised to JSON with `org.json` and stored in `SharedPreferences`.
`AircraftCatalog` prices parts, `SlingshotCatalog` prices upgrades, `Economy` converts a
flight result into coins.

## Seams reserved for the UI

- `game/HudBridge.kt` — callbacks the HUD will consume (distance, speed, coins, state,
  results). `GameApp.hud` accepts an implementation once screens exist.
- `GameApp` public actions (`launch`, `installPart`, `upgradeSlingshot`, `selectWorld`,
  `setGraphicsQuality`) are the exact entry points menus will call.
- `game/GraphicsQuality` (in `game/Economy.kt`) — quality tiers that scale draw distance,
  shadow size, vegetation counts and particle budgets.

- `gfx/DesignTokens.kt` — the single place where `design.md` values (colours, spacing,
  type scale) will be mapped into code.
