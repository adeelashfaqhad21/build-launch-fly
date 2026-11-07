# BUILD • LAUNCH • FLY — MASTER UI/UX & GRAPHICAL DESIGN SPECIFICATION

**Document:** `design.md`
**Role:** Graphical Designer / UI/UX Designer / 3D Environment Visual Designer
**Target Platform:** High-End Android (Landscape 16:9 Primary Composition: 1920×1080 px, Adaptive to 18:9, 19.5:9, 20:9)
**Quality Benchmark:** AAA Mobile Visual Fidelity (Benchmark: PUBG Mobile / Call of Duty Mobile engine material shaders, atmospheric depth, and terrain fidelity) applied strictly to aviation, exploration, and realistic natural ecosystems without military or weapon motifs.

This document is normative for visuals. Where it and an earlier document disagree about
visuals, tokens, layout or screen content, this document wins. `docs/MASTER_BRIEF.md` stays
normative for scope, technology and quality bar.

---

## 1. PROJECT SCOPE & DESIGNER BOUNDARIES (Brief Sections 1 & 11)

### 1.1 Graphical & UI/UX Designer Deliverables (Editable Visual Scope)
- **Visual Identity & Atmosphere:** Natural world aesthetics, aviation themes, physically based lighting (PBR), atmospheric depth grading, realistic sky and weather conditions.
- **5 Complete Living Natural Ecosystems:** Forest/Green Landscape, Desert, Snow/Glacier, Ocean/Coastal, Mountain/Countryside. Every ecosystem designed as a complete piece of living land with geological consistency, multi-layered depth, authentic flora/fauna, and dynamic skies.
- **Strict Aircraft Modular Visual Progression (Exact 6 Stages):**
  1. Initial wingless aircraft body (fuselage chassis).
  2. First wing installation (port-side primary wing).
  3. Second wing installation (starboard wing completing dual dihedral airfoil).
  4. Tail assembly (vertical fin and dual horizontal rudders).
  5. Functional rotating propeller (hub assembly with motion-blur shader support).
  6. Further visual aircraft upgrades (streamlined canopy, dihedral winglets, aerodynamic fairings).
- **Kinetic Slingshot Visual System:** Heavy-duty launch frame, vulcanized tension elastics, cradle carriage, hydraulic recoil pylons, and trajectory angle guide.
- **Complete Landscape UI/UX System (10 Dedicated Screens):** Main Menu, Aircraft Workshop, Slingshot Workshop, Environment Selection, Gameplay HUD, Flight Results, Upgrade & Reward Screens, Settings, Progress & Achievement Screens, Pilot Career Profile.
- **Design Tokens & Asset Architecture:** Reusable typography scale, color tokens, glassmorphism panel metrics, vector telemetry icons, 9-patch slice specs, and 3D bone rigging hierarchy.

### 1.2 Developer Boundary (Hand-Off Systems Handled by Android / Game Dev)
- Aerodynamic flight equations (lift, drag, thrust, gravity curves).
- Slingshot release vector physics & Hooke's law elastic calculations.
- Currency economy formulas and database persistence.
- Android application architecture, Kotlin lifecycle, game engine scripts/shaders, and APK compilation.
- *Note on Gameplay Proposals:* Features like gems, energy cooldowns, nitro boosts, loot crates, and daily quest systems are treated strictly as design proposals for UI layout demonstration and are unconfirmed until approved by the dev/product team.

---

## 2. DESIGN TOKENS & VISUAL FOUNDATIONS (Brief Sections 2 & 9)

### 2.1 Color System & Aesthetic Palette

#### Primary Aviation Palette

| Token Name | Hex Code | RGBA | Usage |
|---|---|---|---|
| `color-primary-aero` | `#0284C7` | `rgba(2, 132, 199, 1.0)` | Core aviation buttons, active tab indicators, branding |
| `color-primary-glow` | `#38BDF8` | `rgba(56, 189, 248, 1.0)` | Active neon accents, altimeter markers, highlight strokes |
| `color-primary-deep` | `#03254C` | `rgba(3, 37, 76, 1.0)` | Submerged navigation chrome, secondary card containers |

#### Currency & Status Accent Tokens (Proposals / Base Visuals)

| Token Name | Hex Code | RGBA | Usage |
|---|---|---|---|
| `color-coin-gold` | `#F59E0B` | `rgba(245, 158, 11, 1.0)` | Aero Coins, primary Launch CTA, reward highlights |
| `color-coin-gold-glow` | `#FBBF24` | `rgba(251, 191, 36, 0.4)` | Outer glow for launch button and upgrade cards |
| `color-status-success` | `#10B981` | `rgba(16, 185, 129, 1.0)` | Stat improvement bars, positive climb indicator |
| `color-status-hazard` | `#EF4444` | `rgba(239, 68, 68, 1.0)` | Stall warnings, critical pitch indicators, speed limits |

#### Glassmorphism Surface & Background Tokens

| Token Name | Hex Code | RGBA | Usage |
|---|---|---|---|
| `surface-hud-bg` | `#0A0F1D` | `rgba(10, 15, 29, 0.72)` | HUD panels, floating workshop cards (`backdrop-filter: blur(16px)`) |
| `surface-modal-bg` | `#050814` | `rgba(5, 8, 20, 0.88)` | Fullscreen overlay backdrops (`backdrop-filter: blur(24px)`) |
| `surface-glass-border` | `#38BDF8` | `rgba(56, 189, 248, 0.18)` | Fine 1px stroke around all interactive panels |
| `surface-glass-highlight` | `#FFFFFF` | `rgba(255, 255, 255, 0.08)` | Top inner edge highlight line for tactile physical depth |

### 2.2 Typography Scale (Aviation Instrumentation & Clean Ergonomics)
- **Display & Headings:** `Rajdhani`, Bold / SemiBold (Geometric, aviation cockpit instrumentation feel).
- **Body & Data:** `Plus Jakarta Sans` or `Inter`, Medium / Regular (Clean, highly legible on high-DPI Android displays).

| Style | Font Family | Weight | Size (dp) | Line Height | Tracking | Usage |
|---|---|---|---|---|---|---|
| `Display Hero` | Rajdhani | 700 Bold | 36dp | 40dp | +0.02em | Game title, victory banners |
| `Header 1` | Rajdhani | 700 Bold | 24dp | 28dp | +0.03em | Screen titles, distance meters |
| `Header 2` | Rajdhani | 600 SemiBold | 18dp | 22dp | +0.02em | Section headers, card titles |
| `Subhead HUD` | Rajdhani | 600 SemiBold | 14dp | 18dp | +0.05em | Telemetry labels (ALT, SPD, DIST) |
| `Body Large` | Plus Jakarta Sans | 500 Medium | 14dp | 20dp | 0 | Dialog body, descriptions |
| `Body Regular` | Plus Jakarta Sans | 400 Regular | 12dp | 16dp | 0 | Secondary specs, sub-labels |
| `Badge / Tag` | Rajdhani | 700 Bold | 10dp | 12dp | +0.08em | MAX, NEW, LEVEL UP, 120 FPS |

**Implementation note (Android):** Rajdhani and Plus Jakarta Sans are not Android system
fonts. The implementation substitutes the closest system families — `sans-serif-condensed`
Bold for Rajdhani-scale instrument type, `sans-serif` / `sans-serif-medium` for body type —
with the same sizes, weights and letter tracking. Bundling the licensed fonts later only
requires swapping the typeface objects in `UiPainter`.

### 2.3 Layout & Safe-Area Grids (16:9 Landscape Benchmark)
- **Base Canvas:** `1920 × 1080 px` (Aspect Ratio 16:9). Implemented as a `640 × 360 dp`
  design space (`density 3`), so every value in this document maps 1:1 to dp on device.
- **Adaptive Horizontal Margins:** Left & Right padding `48dp` (accommodates Android punch-hole cameras, curved bezel corners, and gesture exclusion zones).
- **Top Bar Safe Area:** `24dp` top offset clear of the Android status bar.
- **Bottom Gesture Safe Area:** `16dp` bottom padding clear of the Android navigation bar.
- **Minimum Touch Target:** `48 × 48dp` (all primary buttons designed at 52–64dp touch area).
- **Adaptation:** height is fixed at 360dp; width expands with the device ratio (640dp at
  16:9, 800dp at 20:9). Left content anchors left, right content anchors right, centre
  content centres. Nothing stretches.

---

## 3. THE 5 COMPLETE REALISTIC NATURAL ECOSYSTEMS (Brief Sections 3, 4, 5, 6, 7)

Every ecosystem is built as an actual, living piece of natural land. Each features 4 distinct visual depth layers (Foreground, Midground, Background, Sky), authentic geological undulation, high-definition textures, physically based lighting, and natural atmospheric depth without unnecessary clutter.

### 3.1 Forest / Green Landscape Ecosystem (Temperate Montane Forest)
- **Geological Terrain & Ground:**
  - Naturally undulating terrain with varied elevations from gentle 5° grassy slopes to 35° rocky depressions.
  - Multi-layer ground materials: rich dark humus earth, patches of damp loam, coarse river gravel, and moss-encrusted granite outcroppings.
  - Meandering mountain stream featuring smooth river stones, clear running water with realistic reflections, and natural water-carved erosion banks.
- **Vegetation Density & Species:**
  - Multi-tier canopy: Mature Scots Pines and Birch trees (8m–22m) distributed with generous natural spacing.
  - Understory layer: Wild ferns, berry bushes, heather, and textured grass tufts.
  - Balanced composition: Sun-dappled open clearings with wildflowers contrasting against shadowy timber corridors.
- **Atmosphere, Sky & Sunlight:**
  - Sky: Morning crystalline azure sky with soft, non-uniform cumulus fractus clouds.
  - Sunlight: Sun shafts filtering through the canopy.
  - Atmospheric Haze: Soft emerald-tinted distance fog providing depth separation beyond 600m.
- **Living Environment & Ambient Motion:**
  - Flocks of alpine swallows circling thermals; subtle flutter of butterflies over sunny clearings.
  - Wind sway across leafy branches and tall grasses; natural water ripple shaders along the stream.

### 3.2 Desert Ecosystem (Grand Sand Dunes & Canyon Badlands)
- **Geological Terrain & Ground:**
  - Monumental barchan sand dunes featuring natural curved wind-sculpted ripples and sharp crescent slipfaces.
  - Deep dry wadi riverbeds with sun-baked cracked clay mud tiles and scattered desert gravel.
  - Massive red sandstone buttes, weathered mesas, natural arch cliff formations, and vertical canyon walls with authentic strata.
- **Vegetation & Desert Flora:**
  - Clustered Giant Saguaro and Prickly Pear cacti with realistic fibrous skin textures and needle clusters.
  - Sparse desert sagebrush shrubs, tumbleweeds, and hardy bunchgrass emerging from rock fissures with ample breathing room.
- **Atmosphere, Sky & Sunlight:**
  - Sky: Deep cobalt sky grading smoothly into pale gold near the horizon, with faint wispy cirrus clouds.
  - Sunlight: Intense golden sunlight casting crisp, deep natural shadows under overhangs and dunes.
  - Atmospheric Depth: Subtle convective thermal heat shimmer above the dunes, and wind-blown dust curtains.
- **Living Environment & Ambient Motion:**
  - Desert hawks soaring on thermal air currents; tiny horned lizards darting between shaded rock crevices.

### 3.3 Snow / Glacier Ecosystem (Sub-Zero Alpine Glacier & Ice Shelf)
- **Geological Terrain & Ground:**
  - Massive jagged tectonic granite peaks partially cloaked in eternal blue glacial ice.
  - Deep glacial crevasses and ice falls exhibiting rich cerulean and cyan subsurface light scattering.
  - Partially frozen alpine lake featuring subsurface stress fractures, milky air bubbles, and mirror-like dark ice patches contrasting against drifted powder snow layers.
  - Windblown snow accumulation naturally drifting against the leeward face of rocks and trees.
- **Vegetation:**
  - Hardy Subarctic Siberian Larches and dwarf mountain pines heavily weighed down with realistic crusty snow.
  - Exposed dark lichen-covered cliffs and arctic moss clinging to windswept rock faces.
- **Atmosphere, Sky & Sunlight:**
  - Sky: Crisp winter sky transitioning from steel-blue overcast to brilliant sub-zero sun breaks.
  - Lighting: High-albedo crystalline snow reflections with subtle lens flare, and specular sparkle shaders on fresh powder.
  - Atmosphere: Swirling subarctic blowing spindrift, crystalline ice fog, and cold purple distance mountain haze.
- **Living Environment & Ambient Motion:**
  - High-altitude snow eagles soaring over mountain passes; subtle drifting snowflakes carried by turbulent wind.

### 3.4 Ocean / Coastal Ecosystem (Oceanic Cliffs, Shoreline & Sea Stacks)
- **Geological Terrain & Ground:**
  - Sheer basalt and limestone coastal cliffs with carved natural sea arches, wave-cut platforms, and sea caves.
  - Expansive shoreline showing a realistic natural gradient: dry pale quartz dunes transitioning smoothly to wet, dark, reflective tidal sands.
  - Craggy isolated sea stacks and emergent offshore reefs with foaming wave breakers.
- **Water Bodies & Oceanic Realism:**
  - Deep sapphire offshore water grading into aquamarine and turquoise surf zones.
  - Physically based wave trains with foam lines, realistic sea spray particle bursts on cliff impact, and sky reflections.
- **Vegetation & Wildlife:**
  - Wind-sculpted coastal pines, salt-tolerant dune grasses, and sea lavender along cliff edges.
  - Flocks of seabirds banking on coastal updrafts; visible schools of silver fish below clear tidal pools.
- **Atmosphere, Sky & Sunlight:**
  - Sky: Bright marine sky with billowing maritime cumulus clouds.
  - Lighting: Brilliant shimmering sun path (glitter path) across the ocean surface; coastal sea spray mist.

### 3.5 Mountain / Countryside Ecosystem (Alpine Terraces & Highland Valleys)
- **Geological Terrain & Ground:**
  - Sweeping alpine valleys, jagged ridge peaks, terraced hillside green pastures, and steep scree/talus slopes with authentic geological strata.
  - Naturally carved river gorges with whitewater rapids leading to broad pastoral highland flats and gravel riverbanks.
  - Exposed limestone strata and boulder fields resting on lush green mountain pastures.
- **Vegetation:**
  - Diverse emerald meadow grasses, alpine clover, golden buttercups, and scattered stands of silver firs and oak clusters with generous open breathing room.
- **Atmosphere, Sky & Sunlight:**
  - Sky: Crisp mountain blue sky with rolling fair-weather cumulus casting moving cloud shadows across valleys.
  - Lighting: Warm golden-hour afternoon sunlight bathing ridges and highlighting fine terrain undulations.
  - Atmosphere: Classic blue Rayleigh scattering distance haze giving realistic scale to distant mountain tiers.
- **Living Environment & Ambient Motion:**
  - Soaring falcons; fluttering swallowtail butterflies; gentle ripples across mountain tarns.

---

## 4. ECOSYSTEM COMPOSITION & DEPTH ARCHITECTURE (Brief Sections 4, 5, 6, 7)

### 4.1 The 4-Layer Visual Depth Model
To ensure natural immersion without overcrowding or flat backdrop planes, every ecosystem enforces a strict 4-layer composition:

```
┌────────────────────────────────────────────────────────────────────────┐
│ Layer 4: Dynamic Sky & Atmosphere (Clouds, Rayleigh Haze, Sun Path)    │
├────────────────────────────────────────────────────────────────────────┤
│ Layer 3: Background Silhouette (Distant Peaks, Buttes, Ocean Horizon)  │
├────────────────────────────────────────────────────────────────────────┤
│ Layer 2: Midground Landforms (Valleys, Cliffs, Dunes, Tree Stands)     │
├────────────────────────────────────────────────────────────────────────┤
│ Layer 1: Foreground Detail (Undulating Soil, Rocks, Grass, Flora)      │
└────────────────────────────────────────────────────────────────────────┘
```

1. **Layer 1 (Foreground):** High-definition ground textures, individual grass tufts, exposed loam, cracked clay, stones, and riverbanks.
2. **Layer 2 (Midground):** Primary terrain formations (5°–35° slopes, dunes, river gorges, cliffs) and naturally clustered tree stands with open breathing room.
3. **Layer 3 (Background):** Distant mountain ridges, sea stacks, or canyon mesas graded with atmospheric distance haze.
4. **Layer 4 (Sky & Atmosphere):** Realistic non-uniform cloud formations, volumetric sun shafts, and weather-appropriate lighting.

### 4.2 Contrast & Aircraft Readability
- The player's aircraft must always remain visually distinguishable against all terrain surfaces.
- Ambient occlusion, edge highlighting, and dynamic sun specular reflections on the aircraft fuselage ensure the craft never blends into dark forests, white snow, or red sandstone.


---

## 5. AIRCRAFT & SLINGSHOT PROGRESSION ARCHITECTURE (Brief Section 8)

### 5.1 Strict Aircraft Progression Stages (Exact Sequence)
The aircraft is designed as a progressively developing physical object across 6 distinct visual stages:

```
Stage 1: Initial Wingless Aircraft Body
   │     (Streamlined monocoque fuselage, cockpit canopy, nose cone)
   ▼
Stage 2: First Wing Installation
   │     (Port-side high-lift NACA 2412 wing attached via visible titanium spar)
   ▼
Stage 3: Second Wing Installation
   │     (Starboard wing installed, completing the symmetrical dual dihedral airfoil)
   ▼
Stage 4: Tail Assembly
   │     (Empennage section: vertical stabilizer fin and dual horizontal rudders)
   ▼
Stage 5: Functional Rotating Propeller
   │     (3-Blade variable-pitch propeller hub with high-speed rotation shader)
   ▼
Stage 6: Further Visual Aircraft Upgrades
         (Streamlined canopy, dihedral winglets, low-drag aerodynamic fairings)
```

- **Material & PBR Shaders:**
  - **Fuselage Chassis:** Aerospace composite lacquer (Roughness: `0.35`, Metallic: `0.15`) with subtle panel lines and visible structural rivets.
  - **Cockpit Canopy:** Tinted polarized aeronautical glass (Opacity: `85%`, Specular: `0.95`, Fresnel reflectance).
  - **Propeller:** Forged carbon-fiber blades with high-speed motion-blur alpha disc shader for rotation state.

**Implementation status:** Stages 1–5 and the stage-6 geometry (canopy fairing, winglets,
cowling, tail fairings) are modelled procedurally and rebuilt whenever parts change. The
fuselage material uses the specified roughness/metallic values; the canopy is a separate
mesh with its own low-roughness tinted-glass material. A true alpha motion-blur disc is not
yet rendered (documented gap).

### 5.2 Kinetic Slingshot Visual System
- **Frame & Mechanics:**
  - Heavy-duty dual-pylon launch towers constructed from anodized aerospace titanium with visible hydraulic recoil dampers.
  - Heavy vulcanized latex launch band showing dynamic elastomeric stretching and physical sag when tensioned.
  - Padded leather/carbon aircraft carriage cradle with quick-release magnetic release latch.
- **Launch States:**
  - **Rest State:** Elastic bands rest with gentle catenary curve; launch trajectory guide laser inactive.
  - **Draw State:** User drags aircraft backward; elastic bands stretch taut, tension meter glows from Amber to Neon Cyan, hydraulic pistons compress.
  - **Release State:** Instant snappy spring-back, dynamic elastic recoil wobble, launch smoke puff, and camera impulse shake.

**Implementation status:** bands are drawn as dynamically stretched tubes from both prongs
to the cradle, their tint interpolating Amber → Neon Cyan with draw tension; the HUD tension
meter matches the same ramp; release triggers a camera impulse shake.

---

## 6. COMPLETE LANDSCAPE UI/UX DESIGN SYSTEM (Brief Section 9)

### Screen 1: Main Menu
- **Top Header Bar:**
  - Left: Pilot Profile Pill (Rank avatar, `Capt. Alexander`, `Level 24`, Rank Title: `Ace Aeronaut`, XP bar).
  - Right: Resource Cluster: Aero Coins (`48,250`), Settings gear button (`48 × 48dp`).
- **Main Viewport Stage:**
  - 3D interactive preview of current player aircraft resting on the polished launch pad against the chosen ecosystem backdrop.
  - Aircraft inspection hotspot tags (`[Mk-IV Propeller]`, `[Carbon Dihedral Wing]`).
- **Bottom Navigation Deck:**
  - **Assembly / Plane Workshop** button (Plane icon).
  - **Impulse Core / Slingshot Lab** button (Slingshot icon).
  - **5 Worlds / Ecosystems** button (Globe icon, displays current active biome: *Glacier Valley*).
  - **Primary CTA: "CHOOSE REALM & LAUNCH"** (Large high-contrast amber button `280 × 64dp`, animated play chevron, pulse glow).

### Screen 2: Aircraft Workshop
- **Layout:** Full 3D aircraft turntable with free 360° touch-orbit rotation and pinch-to-zoom.
- **Component Category Tabs (Top Floating):**
  - `[Fuselage]` • `[Wings]` • `[Tail & Rudders]` • `[Propeller]` • `[Upgrades]`
- **Right Side Drawer (Part Specs & Upgrade Card):**
  - Component Title: `Mk-IV Carbon Dihedral Wing`.
  - Tier & Level: `Tier 3 - Level 4/5`.
  - Aerodynamic Stats Delta:
    - **Lift Coefficient ($C_L$):** `+18%` (Green comparative bar)
    - **Aerodynamic Drag ($C_D$):** `-12%` (Lower resistance)
    - **Gliding Stability:** `+24%`
  - Upgrade Button: `"UPGRADE COMPONENT"` (Cost: `6,500 Aero Coins`).

### Screen 3: Slingshot Workshop
- **Layout:** Side-elevation 3D view of the kinetic slingshot apparatus with dynamic tension grid.
- **Interactive Mechanical Controls:**
  - **Band Tension Capacity:** Upgrades elastic thickness (Increases initial launch velocity $V_0$).
  - **Launch Elevation Arc:** Mechanical angle adjustment (`20° to 55°` optimal ballistic arc).
  - **Hydraulic Recoil Damper:** Minimizes launch vibrational turbulence.
- **Stat Telemetry Display:** Base Velocity: `142 km/h` → `168 km/h` (with upgrade); Impulse Force: `8,400 N`.

### Screen 4: Environment Selection (5 Ecosystems Biome Carousel)
- **Layout:** Horizon card carousel presenting all 5 biomes with animated environmental parallax cards:
  1. **Glacier Valley (Ice / Snow)** — Active / Unlocked. Weather: Light Spindrift, Wind: `12 km/h E`.
  2. **Verdant Canopy (Forest)** — Unlocked. Weather: Mild Sun Shafts, Wind: `4 km/h S`.
  3. **Canyon Spires (Desert)** — Unlocked. Weather: Thermal Updrafts, Wind: `18 km/h W`.
  4. **Cerulean Coast (Ocean)** — Unlocked. Weather: Ocean Squalls, Wind: `22 km/h NE`.
  5. **Highland Peaks (Mountain)** — Master Level Unlock (Requires `Level 25`).
- **Per-Biome Metrics Display:** Personal Best Distance record, Wind conditions, and `"SELECT ECOSYSTEM"` button.

### Screen 5: In-Flight Gameplay HUD (Clean & Non-Obstructive)
- **Ergonomics:** Core central viewport remains 100% unobstructed; all instruments positioned on extreme edges.
- **Top Center Telemetry Bar:**
  - Distance Traveled: `2,842 m` (Large high-contrast display in Rajdhani Bold).
  - Altitude: `415 m` (Mini altimeter ladder).
  - Airspeed: `184 km/h` (Pitot tube speed gauge).
- **Top Left:** Pause button (`48 × 48dp`), Flight Time (`01:24.8`).
- **Center-Left Attitude Horizon Gimbal:** Subtle semi-transparent artificial horizon ladder showing aircraft pitch attitude and bank angle.
- **Dynamic Contextual Flight Alerts:**
  - `"THERMAL CAUGHT! +50m ALTITUDE"` (Animated upward cyan banner).
  - `"STALL HAZARD — PITCH NOSE DOWN"` (Flashing amber caution alert).

### Screen 6: Flight Results & Debrief
- **Layout:** Semi-transparent glass modal overlaying the final landing position in the ecosystem.
- **Headline Banner:** `"FLIGHT COMPLETED"` / `"NEW PERSONAL RECORD!"`.
- **Primary Metric:** `4,892 METERS` (With animated distance odometer counter).
- **Performance Breakdown Ledger:**
  - Airtime Duration: `02:14.3`
  - Max Altitude Reached: `612 m`
  - Top Airspeed: `248 km/h`
  - Thermals Caught: `6`
- **Action Buttons:** `"RETRY FLIGHT"`, `"AIRCRAFT WORKSHOP"`, `"CLAIM & CONTINUE"`.

### Screen 7: Upgrade & Reward Screens
- **Layout:** Cinematic reward pedestal showcasing unlocked components (e.g. `Stage 5: 3-Blade Propeller Hub`).
- **Component Card:** High-definition 3D turntable preview, stats delta, and `"EQUIP TO AIRCRAFT"` button.

### Screen 8: Settings (Graphics & Audio Preferences)
- **Graphics Quality Segmented Picker:** `[Power Saver 30FPS]` • `[Balanced 60FPS]` • `[High Fidelity 90FPS]` • `[Ultra Extreme 120FPS]`.
- **Visual Fidelity Toggles:** Dynamic Water Reflections (`ON/OFF`), Atmospheric Volumetric Fog (`ON/OFF`), High-Resolution Textures (`ON/OFF`).
- **Audio Sliders:** Master Volume, Propeller Engine FX, Natural Environmental Ambiance.

### Screen 9: Progress & Achievement Screens
- **Badge Showcase Grid:**
  - *Cloud Piercer:* Reach 1,000m Altitude (`UNLOCKED`).
  - *Desert Falcon:* Fly 5,000m in Canyon Spires (`PROGRESS: 4,200 / 5,000m`).
  - *Glacier Master:* Cross frozen crevasse zone (`COMPLETED`).
- **Claim Rewards:** Interactive claim buttons for unlocked achievement tiers.

### Screen 10: Pilot Career Profile
- **Career Stats:** Total Flights (`148`), Total Distance Flown (`412,650 m`), Favorite Aircraft Configuration, Favorite Biome (`Glacier Valley`).

**Implementation note:** every screen above is implemented in the `ui` package and is
driven by live game data (profile, aircraft build, slingshot levels, telemetry, results).
Numbers in this document are illustrative mock values; the running game shows real values.

---

## 7. ASSET EXPORT & DEVELOPER HAND-OFF MANIFEST (Brief Section 10)

### 7.1 UI Slices & 9-Patch Assets (`res/drawable-nodpi/`)
- `btn_primary_amber_9patch.9.png` (Stretchable center, fixed rounded corners 12dp).
- `panel_hud_glass_bg_9patch.9.png` (72% dark glass, 1px cyan stroke, 16dp radius).
- `gauge_horizon_gimbal_ring.svg` (256 × 256px).
- `icon_altitude_ladder.svg` (48 × 48dp).
- `icon_airspeed_indicator.svg` (48 × 48dp).
- `icon_currency_coin.png` (96 × 96px, 3D rendered gold coin with alpha).

**Implementation note:** the UI is drawn procedurally on a `Canvas` overlay instead of
9-patch bitmaps, so panels, buttons, gauges and icons are resolution-independent vector
drawing at any density. When authored 9-patch/SVG assets are supplied they can replace the
primitives in `ui/UiPainter.kt` without touching screen layout.

### 7.2 3D Model Hierarchy & Bone Rigging Convention (`assets/models/`)
All models authored in FBX/glTF formats, centered at $(0, 0, 0)$ origin in real-world metric units ($1\text{ unit} = 1\text{ meter}$):

```
Aircraft_Assembly_Root
├── Meshes
│   ├── Mesh_Fuselage_Stage1 (Tris: 4,200, PBR 2K Texture)
│   ├── Mesh_Wing_Left_Stage2 (Tris: 2,100)
│   ├── Mesh_Wing_Right_Stage3 (Tris: 2,100)
│   ├── Mesh_Tail_Assembly_Stage4 (Tris: 1,400)
│   └── Mesh_Propeller_Stage5 (Tris: 850)
└── Bones / Attachment Locators
    ├── Bone_Fuselage_Root
    ├── Bone_Wing_Left_Mount
    ├── Bone_Wing_Right_Mount
    ├── Bone_Tail_Mount
    └── Bone_Propeller_Shaft (Anim Channel: Rot_Z, continuous 360°)
```

**Implementation note:** the current build generates this hierarchy procedurally in
`aircraft/AircraftMesh.kt` using the same local convention (forward = −Z, up = +Y,
right = +X, 1 unit = 1 m) and the same part slots, so authored glTF meshes can drop into the
existing mount points later.

### 7.3 Android Kotlin Data Contracts Reference
```kotlin
// Android UI State Binding Data Contract
data class FlightTelemetryState(
    val currentDistanceMeters: Float = 0.0f,
    val currentAltitudeMeters: Float = 0.0f,
    val currentAirspeedKmh: Float = 0.0f,
    val pitchAngleDegrees: Float = 0.0f,
    val rollAngleDegrees: Float = 0.0f,
    val isStallWarningActive: Boolean = false,
    val activeThermalAlert: String? = null
)

data class EcosystemProfile(
    val id: String,
    val displayName: String,
    val themeColorHex: String,
    val unlocked: Boolean,
    val personalBestDistanceMeters: Float,
    val windConditionDescription: String,
    val atmosphericModifierDescription: String
)
```

**Implementation note:** `game/FlightTelemetry.kt` provides the live telemetry object and
`world/WorldDefinition.kt` + `PlayerProfile.bestPerWorld` provide the ecosystem rows the
carousel binds to.

---

## 8. VERIFICATION MATRIX AGAINST ASSIGNMENT REQUIREMENTS

| Brief Requirement | Status | Implementation in Master Design Suite |
|---|---|---|
| **Fully Editable Files** (Sec 1, 10) | Verified | Design tokens in `gfx/DesignTokens.kt`, UI primitives in `ui/`, complete screen suite |
| **Realistic Quality Benchmark** (Sec 2) | Partial | High-definition PBR material tokens, natural lighting, shadow treatments, atmospheric depth |
| **Landscape 16:9 Android Format** (Sec 2, 9) | Verified | 640×360 dp design space scaling to 1920×1080, safe zones for punch-holes & gestures |
| **Forest Ecosystem** (Sec 3.1) | Partial | Undulating slopes, multi-tier canopy, visible soil, haze, water body |
| **Desert Ecosystem** (Sec 3.2) | Partial | Dunes with slipfaces, wadis, sparse flora, red rock, dust |
| **Snow/Glacier Ecosystem** (Sec 3.3) | Partial | Jagged peaks, ice, frozen water, conifers, blowing spindrift |
| **Ocean/Coastal Ecosystem** (Sec 3.4) | Partial | Coastal cliffs, quartz beaches, wave shading, sea stacks, coastal pines |
| **Mountain/Countryside Ecosystem** (Sec 3.5) | Partial | Alpine terraces, valleys, talus slopes, Rayleigh haze |
| **No Simple Flat Planes / Natural Composition** (Sec 4, 5) | Partial | 4-layer composition: Foreground, Midground, Background, Sky |
| **Sky, Sunlight, Clouds & Atmosphere** (Sec 6) | Partial | Distinct atmospheric properties, light scattering, cloud formations for all 5 realms |
| **Aircraft Progressive Development** (Sec 8) | Partial | Exact 6-stage progression from wingless body to wings, tail, rotating propeller, upgrades |
| **Kinetic Slingshot Workshop** (Sec 8) | Partial | Stretched elastics with amber→cyan tension tint, heavy launch frame, angle/elevation controls |
| **Complete UI Screens Suite** (Sec 9) | Partial | All 10 mandatory landscape screens implemented with live data and tokens |
| **Developer Hand-Off Clarity** (Sec 10, 11, 12) | Verified | Clean separation of design vs code; 3D locator conventions and Kotlin models |

**Verification note:** `Partial` means source structures or procedural implementations exist, but full requirement-by-requirement visual validation on target Android hardware has not been completed. `Verified` must not be interpreted as proof of AAA visual fidelity or device-level acceptance. The aircraft now has a dedicated high-RPM alpha motion-blur disc pass, gated by propeller RPM. The three blades are separate meshes and receive an animated pitch angle from an RPM-governor response, while rotating around the shaft. Hardware-level fidelity, blur appearance and blade-pitch behaviour still require target-device visual and gameplay validation; source implementation alone is not acceptance.
