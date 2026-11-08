# DESIGN — Build • Launch • Fly

**Status:** reference-derived. `Ref-1` … `Ref-6` were inspected frame by frame; they are the
authoritative source for UI and engine look. `Ref-7` (a 39 MB recording) is committed but
could not be opened in the browser, so nothing in this document is derived from it.

**Supersedes:** the earlier glassmorphism / PUBG-realism design. The references show a
stylised low-poly 3D game with a bright hypercasual UI, and that is the target.

Companion documents: [`BRIEF.md`](BRIEF.md), [`MASTER_BRIEF.md`](MASTER_BRIEF.md) (original
commission), [`reference/README.md`](reference/README.md) (plate index).

---

## 0. Sources and precedence

| Plate | What it actually shows |
|---|---|
| `Ref-1` | Splash / loading: toy-room illustration, "Voodoo" mark, `PLANE EVOLUTION` logo, loading ring + `Loading level: 99%`. No controls. |
| `Ref-2` | Hangar home: currency pills (`28.15K` coins, `0` gems), gear, `VINTAGE PLANE` plaque, promo rail (store, remove-ads, gems `x1 3/3`), chest `OPEN`, `VIP`, rocket `UNLOCK`, `TAP TO FLY`, three upgrade cards (`SLINGSHOT 41 / 69.55K`, `PLANE 58 / 421.04K`, `INCOME 18 x38.4 / 98.52K`). |
| `Ref-3` | Launch setup: plane from behind inside the slingshot, striped bands from two posts, desert valley, red `BACK` button. No other UI. |
| `Ref-4` | Aim / tension: tighter camera, curved segmented gauge (green centre, yellow/orange/red flanks). No text. |
| `Ref-5` | Flight, good launch: speedometer `94 KM/H` top-left, pause, `PERFECT!`, `167M`, vertical progress track with checkered flag and `4%`, green speed trails. |
| `Ref-6` | Flight, slow: `37 KM/H`, red `LOW SPEED` under the dial, `370M`, `9%`. |

Precedence: plates → this document → `BRIEF.md` → `MASTER_BRIEF.md`.

Originality: the reference game's branding, logo, artwork and copy are not reused. The
layout language, palette relationships and component inventory are matched; our name, logo,
art, numbers and text are our own.

---

## 1. Global design system

### 1.1 Palette

| Token | Hex | Use |
|---|---|---|
| `sky` | `#40B9E8` | sky, primary HUD accent |
| `sand` | `#C5A078` | terrain base |
| `sandLight` | `#D4B183` | lit terrain, dunes |
| `card` | `#FFF9EF` | every white card / panel |
| `blue` | `#1975D1` | primary buttons, HUD fills |
| `navy` | `#185AB7` | text outlines, deep strokes |
| `green` | `#35D52D` | progress, positive, coin "+" |
| `gold` | `#FFB51F` | coins, price text highlights |
| `yellow` | `#FFD52B` | launch bands, markers, VIP |
| `red` | `#F33135` | BACK, LOW SPEED, destructive |
| `purple` | `#9A4BEA` | income card, UNLOCK button |
| `lavender` | `#E7B5F3` | price panel on cards |
| `cyan` | `#19A9ED` | plane card header |
| `orange` | `#FF9D12` | slingshot card header |
| `gem` | `#22B7F0` | gems |
| `plaque` | `#A58B70` | hangar title plaque |
| `plaqueText` | `#583B1E` | plaque lettering |
| `scrim` | `#000000` @ 55 % | modal backdrop |
| `shadow` | `#000000` @ 25 % | card/button drop shadows |

Rules: 3–5 saturated colours per screen, never more. Cards are always `card` white with a
light bevel border; saturated colour appears in headers, buttons and pills only. Text on
colour is white with a navy outline; text on white is navy/plaque brown.

### 1.2 Typography

| Role | Spec |
|---|---|
| Display / numbers / buttons | Rounded heavy sans, uppercase, tight tracking, white fill + 2–3 dp navy outline + 1 dp shadow. Bundled face: **Fredoka** (`res/font/fredoka.ttf`, OFL). |
| HUD numerals | Same rounded face, tabular, 28–40 dp |
| Body / helper text | Plus Jakarta Sans Medium, 12–14 dp, `#583B1E` on white, white on colour |
| Card headings | 14–16 dp heavy uppercase, white |
| Price | 12–14 dp bold, `#583B1E` on `lavender` |

The repo currently bundles Rajdhani + Plus Jakarta Sans (OFL). Rajdhani is an instrument
face, not a rounded casual face — a checklist item below covers adding a rounded display
face so headings match the plates.

### 1.3 Shape, surface and depth

- Corner radii: cards 20 dp, buttons 22 dp (pill), small chips 12 dp, dials circular.
- Card recipe: fill `card`, 2 dp `#FFFFFF` inner border, 1 dp `navy` @ 20 % outer stroke,
  0/3 dp shadow at 25 % black, header band with 20 dp top radii.
- Button recipe: pill, saturated fill, 2 dp white border, 3 dp bottom shadow, label centred
  with outline; pressed state = 4 % scale down + shadow removed.
- Glass/blur is **not** used anywhere. Depth comes from bevels and shadows only.
- Metallic dials: outer ring in two greys (`#BFCBD4`, `#8E9AA6`), inner face `#39BCE6`,
  dark needle, tick marks, green safe zone.

### 1.4 Layout grid (portrait)

- Design space **360 × 640 dp**, width fixed, height adaptive (16:9 → ~800 dp on 20:9).
- Safe areas: top 20 dp, bottom 16 dp, horizontal margin 16 dp; 48 dp minimum touch target.
- Home: currency row y 20–52; plaque y 60–92; scene free middle; upgrade card row pinned
  16 dp above the bottom safe area, cards 104 dp tall with 8 dp gaps.
- Flight: dial top-left (x 16, y 20, 88 dp), pause under it, distance centred at 50 % height,
  progress track right edge x 336–352, aircraft lower third.
- Modal: width 328 dp, centred vertically, 16 dp padding, 16 dp radius, `scrim` behind.

### 1.5 Iconography

Chunky, glossy, single-object icons with white 2 dp strokes: coin, gem, gear, pause,
checkered flag, plane marker, chest, VIP cap, rocket, storefront, video/ad, plus. Rendered as
code-path vectors at 24/32/48 dp, or 96 px PNG for the coin and gem.

### 1.6 Motion and feedback

- Buttons: 4 % press scale, 90 ms; releases 120 ms.
- Cards: idle 1.5 % breathing scale on the primary card only.
- `PERFECT!`: 280 ms pop-in (scale 0.6 → 1.12 → 1.0) + 12 ° tilt settle; holds 900 ms.
- `LOW SPEED`: 2 Hz pulse between 80 % and 100 % opacity while speed < 45 km/h.
- Progress track marker slides with distance; checkered flag bounce on completion.
- Launch: band snap, camera shake 0.35 s, smoke puff 0.6 s.
- Screen transitions: 180 ms slide/fade; modals scale 0.92 → 1.

### 1.7 Audio language

Short, punchy, arcade: card tap "pop", purchase "coin cascade", launch "twang + whoosh",
flight loop wind + engine hum, PERFECT chime, LOW SPEED low beep every 1.2 s, results
fanfare. Ambience per biome (desert wind). All UI sounds under 300 ms.

---

## 2. Component catalog index

Each catalog below contains: purpose, layout, colours, surfaces, assets to generate, states,
data, motion, audio, an implementation checklist and acceptance criteria.

| # | Component | Source |
|---|---|---|
| 3.1 | Splash / loading | Ref-1 |
| 3.2 | Hangar home | Ref-2 |
| 3.3 | Currency bars | Ref-2 |
| 3.4 | Upgrade cards (3) | Ref-2 |
| 3.5 | Promotions (store, ads, gems, chest, VIP, rocket) | Ref-2 |
| 3.6 | Slingshot rig | Ref-3, Ref-4 |
| 3.7 | Launch setup screen | Ref-3 |
| 3.8 | Aim / tension gauge | Ref-4 |
| 3.9 | Flight HUD | Ref-5, Ref-6 |
| 3.10 | Speedometer | Ref-5, Ref-6 |
| 3.11 | Distance + progress track | Ref-5, Ref-6 |
| 3.12 | Feedback and warnings | Ref-5, Ref-6 |
| 3.13 | Results | derived |
| 3.14 | Aircraft and build stages | Ref-2 … Ref-6 |
| 3.15 | Environments | Ref-3 … Ref-6 |
| 3.16 | Camera | Ref-2 … Ref-6 |
| 3.17 | Settings | derived |
| 3.18 | Achievements / career | derived |
| 3.19 | Economy and save | Ref-2 |
| 3.20 | Performance budget | all |


---

## 3. Component catalogs

### 3.1 Splash / loading (Ref-1)

**Purpose:** brand moment while the world and save load; communicates progress.

**Layout (portrait 360×640):** full-bleed illustration; logo block y 110–210; loading ring
centred at y 560, diameter 44; progress text baseline y 610.

**Colours:** warm room peach `#F3B6A8`; balloon red `#E21E2C`; logo navy `#123F7B`, blue
`#2679C7`, yellow `#FFC51B`; overlay white text. Our own version uses the game palette
(`sky`, `gold`, `card`) rather than the reference's toy-room art.

**Surfaces:** no panels. Illustration + extruded logo + circular progress ring (track white
@ 30 %, fill `gold`, round cap).

**Assets to generate:** our wordmark; 1024×1024 key-art plate (or a 3D hangar still); ring.

**States:** loading 0–100 %; fade to hangar at 100 % (300 ms).

**Data:** real initialisation progress (texture generation, terrain build, save load),
mapped to 0–100 %.

**Motion:** subtle logo breathing; ring spins; particles drift.

**Audio:** soft ambience bed, no UI ticks.

**Checklist**
- [ ] Logo wordmark asset (vector or generated) in `res/drawable`
- [ ] Loading screen state before the GL surface reports ready
- [ ] Progress driven by real init stages, not a fake timer
- [ ] Skip on tap after 1.5 s minimum
- [ ] Cross-fade into hangar
- [ ] No reference branding ("Voodoo", "Plane Evolution") anywhere

**Acceptance:** first frame shows branded art within 300 ms; progress reaches 100 % only
when the hangar is ready to draw.

---

### 3.2 Hangar home (Ref-2)

**Purpose:** the hub. Shows the owned plane, currencies, mode entry and the three upgrade
tracks.

**Layout (360×640 dp):**

| Region | Rect |
|---|---|
| Coin pill | `16, 20, 150×30` |
| Gem pill | `172, 20, 90×30` |
| Gear button | `312, 20, 32×32` |
| Plaque `VINTAGE PLANE` | `80, 62, 200×32` |
| Promo rail (3 icons) | `16, 110 / 170 / 230, 40×40` |
| Chest + OPEN | `244, 112, 100×62` |
| VIP | `244, 182, 100×48` |
| Rocket + UNLOCK | `244, 240, 100×72` |
| `TAP TO FLY` | centred at `y 330`, 32 dp |
| Upgrade cards | `16, 520, 104×104` ×3, 8 dp gaps |

**Colours:** background is the live 3D hangar scene; UI `card` white, headers `orange` /
`cyan` / `purple`; plaque `plaque` with `plaqueText`; CTA text white with `navy` outline.

**Surfaces:** white cards (radius 20, 2 dp white inner border, `navy` 20 % outer stroke,
25 % shadow); pills with 2 dp white bevel; circular gear button `blue` with white rim.

**Assets to generate:** hangar interior set (roof beams, open side to sky, floor); plaque
plate; chest/VIP/rocket icons; card icons (slingshot, plane, income).

**States:** plane idle bob; chest ready/empty; promo slots enabled/disabled; card
affordable/unaffordable; `TAP TO FLY` pulses until first flight.

**Data:** coin balance, gem balance, plane name + tier, three upgrade levels, prices,
affordability, chest timer, VIP status.

**Motion:** plane slow bob ±2 cm; cards breathe 1.5 %; currency count-up on gain; gear spins
on tap.

**Audio:** hangar ambience; tap pops; purchase cascade.

**Checklist**
- [x] Hangar 3D set: tan plank floor, dark weathered plank wall down the left, open right side
  with posts, heavy beamed roof, crates and bales outside — `world/HangarMesh`
- [ ] Plane displayed in hangar, idle bob — the plane is held in the sling, no bob yet
- [x] Two currency pills with compact formatting (`28.15K`)
- [x] Gear opens settings
- [x] Title plaque bound to plane tier name
- [x] Promo rail with 3 slots (store, remove-ads, rewarded gems)
- [x] Chest / VIP / rocket slots as modular components
- [x] `TAP TO FLY` entry into launch setup
- [x] Three upgrade cards pinned to the bottom row
- [ ] Scene camera framing: rear-side, plane lower-middle, sky opening right — the camera is
  directly behind on the centre line and the roof opening is central, as the user asked; the
  rear-side variant is still open
- [x] No glass/blur panels

**Acceptance:** the whole hub reads in one glance; all four currencies/promotions and three
upgrade tracks visible without scrolling; 60 fps with the 3D scene behind.

---

### 3.3 Currency bars (Ref-2)

**Purpose:** balances + shortcuts to earn more.

**Layout:** pill `150×30` (coins), `90×30` (gems), 2 dp white bevel, 3 dp shadow, icon
24 dp at left inset 4 dp, value right-aligned inset 10 dp, green "+" circle 20 dp at right
edge overlapping the pill.

**Colours:** coin pill `gold` face with `#E08A00` edge; gem pill `gem` face with `#0E86B8`
edge; "+" `green` with white rim; value white with `navy` outline.

**Assets:** coin 96 px PNG/vector, gem, plus.

**States:** pulse on gain; "+" opens the relevant store tab; low-balance flash red for 200 ms
on a failed purchase.

**Data:** int balances, compact formatter (K/M, 2 decimals).

**Checklist**
- [ ] Compact number formatter shared with cards and results
- [ ] Coin and gem pill components
- [ ] "+" hit areas 48 dp with visual 20 dp
- [ ] Count-up animation on reward
- [ ] Insufficient-funds shake + red flash

---

### 3.4 Upgrade cards (Ref-2)

**Purpose:** the three progression tracks: SLINGSHOT, PLANE, INCOME.

**Layout (104×104 dp):** header band 20 dp (colour by track, white uppercase label +
level), icon 40×40 centred at y 26, five pips 8×4 dp at y 70, price panel `86×18` at
y 82 (`lavender`, coin icon, compact price).

**Colours:** slingshot `orange #FF9D12`; plane `cyan #19A9ED`; income `purple #9A4BEA`;
filled pips `green`, empty `#D9D9D9`; price text `plaqueText`.

**Assets:** three card icons; pip; coin mini.

**States:** affordable (normal), unaffordable (price greyed, 60 % opacity), max level
(pips gold, price replaced with `MAX`), just-upgraded (flash + pip fill animation).

**Data:** track id, level, max level, price, multiplier (income `x38.4`), affordability.

**Motion:** tap scale, pip fill 180 ms, icon bounce on purchase.

**Checklist**
- [ ] Card component with header colour parameter
- [ ] Five-pip progress row
- [ ] Price panel with compact formatting
- [ ] MAX state
- [ ] Income multiplier display (`x38.4`)
- [ ] Tap → upgrade or store if unaffordable
- [ ] Left/right wing upgrades kept symmetric in the PLANE track

**Acceptance:** all three cards fit the bottom row at 360 dp; each level of each track
changes a visible game value.

---

### 3.5 Promotions (Ref-2)

Slots, all modular and individually disableable:

| Slot | Visual | Behaviour |
|---|---|---|
| Store | storefront icon, 40 dp | opens shop |
| Remove ads | red "ADS" crossed badge | one-time purchase |
| Rewarded gems | gem + video mark + `x1` / `3/3` | rewarded video, cap 3/day |
| Chest | blue/silver chest + green `OPEN` | timed free reward |
| VIP | gold cap + `VIP` wordmark | VIP offer screen |
| Rocket unlock | rocket + purple `UNLOCK` + gem cost | skip to a realm/rank |

**Rules:** none of these may block play; all can be hidden by config until the economy is
validated. No pay-to-progress wall in the first 30 minutes.

**Checklist**
- [x] Generic promo-slot component (icon, label, badge, button) — `MenuScreen.promoIcon` /
  `promoCard`, driven by `PromoSlot`
- [x] Config flags per slot — `ui/PromoConfig.enabled`
- [x] Chest timer + claim state persisted — `PlayerProfile.chestReady` / `claimChest`
- [ ] Rewarded-video cap (3/3) persisted — no ad surface yet; the slot stays off
- [x] VIP/rocket screens are stubs unless enabled — VIP hidden, rocket routes to realms
- [x] All disabled slots collapse cleanly (layout without gaps) — the rail only stacks
  enabled slots

---

### 3.6 Slingshot rig (Ref-3, Ref-4)

**Purpose:** the launch device — visible, physical, readable from behind.

**Form:** two plain squared wooden posts with a slight outward splay, and **one** elastic sling
tied to both tips and meeting at a single tow point on the forward fuselage at the wing root.
No ramp, no crossbar, no braces, no metal. The posts stand about 1.2× the wingspan apart, so
the sling clears them. Bands are yellow/white striped. In Ref-3 the plane sits back; in Ref-4
the camera is closer and the sling forms a steeper V.

**Layout in world space:** posts at x ±2.15 m, height 3.15 m (about the nose-to-tail length),
squared 0.13 m section with 0.05 rad outward splay; sling radius 0.075 m; tow point 0.10 m
below the origin and 0.25 m behind it, so the arms pass behind the wings and the V meets on
the fuselage centre line; aircraft seat at 3.05 m; camera directly behind at ~3.3 m height.

**Colours:** timber `#8A6A4A` posts, sling `yellow #FFD52B` + `cream #F5F2E8` stripes, dark
leather pad.

**Surfaces:** flat-shaded, no gloss, no metal; sling stripes are a two-colour texture.

**Assets to generate:** two post meshes, sling tube mesh (stripable), leather tow pad, back
button plate.

**States:** rest (sling slack, slight sag), draw (sling taut, plane pulled back), release
(snap, recoil wobble, smoke).

**Data:** pull 0–1, sling length, tension, launch elevation, upgrade level → thickness/length.

**Motion:** sling stretches and sags with the pull; 0.6 s recoil after release; dust puff.

**Audio:** creak on draw (pitch rises); release whoosh.

**Checklist**
- [x] Exactly two posts and a sling — `SlingshotMesh` builds two squared posts and one elastic
  sling; there is no ramp, crossbar or metalwork
- [x] Posts about 1.2× the wingspan apart — `POLE_X = 2.15` against a 1.78 m wing half-span
- [x] Posts about the aircraft's nose-to-tail length — `POLE_HEIGHT = 3.15`
- [x] One sling tied to both tips and anchored at the wing root — `ANCHOR_Y` / `ANCHOR_Z`
- [x] Yellow/white striped sling — `TextureFactory.hazard` (yellow `#FFD52B` / cream `#F5F2E8`)
- [x] Sling articulates with draw — `holdPoint(draw)` moves the anchor back with the aircraft
- [x] Rest sag, taut draw, recoil — two-segment arms sagging to `BAND_SAG`, 0.6 s recoil
- [x] Upgrade level visibly changes sling thickness/length — `thicknessBoost` and `drawLength`
- [x] Sling casts a shadow — every segment is in the shadow pass
- [ ] Rear-view framing with both posts visible (camera path exists; needs an on-device check)

**Acceptance:** from the launch camera a first-time player understands "pull back here".

---

### 3.7 Launch setup screen (Ref-3)

**Purpose:** the pre-pull state.

**Layout:** full-bleed 3D behind; a single red `BACK` pill centred near the bottom
(`y 560`, 120×40 dp); no meter, no HUD, no other text.

**Colours:** button `red #F33135`, 2 dp white border, white uppercase label, 3 dp shadow.

**States:** idle (plane seated), first-time hint (one-shot "drag back" ghost arrow, fades
after 3 s), returning (no hint).

**Data:** selected realm, plane tier.

**Motion:** plane settles into cradle; hint arrow loops.

**Audio:** wind ambience; button pop.

**Checklist**
- [x] Launch screen uses the ref layout (single BACK at the bottom)
- [x] BACK returns to hangar
- [x] First-run drag hint — ghost chevron + `PULL BACK`, 3 s fade (`GameApp.launchHintAlpha`)
- [x] No telemetry on this screen — the tension gauge only appears while drawing
- [x] Realm-appropriate environment behind

---

### 3.8 Aim / tension gauge (Ref-4)

**Purpose:** tells the player how hard the slingshot is drawn and where the "perfect" zone is.

**Form:** a curved arc in world space or screen space just below the ramp, made of segments:
centre 5 segments green, then yellow, then orange, then red at both ends. A pointer or
highlight marks the current pull.

**Layout:** arc width 300 dp, height 28 dp, centred at y 300 (world-anchored under the
ramp), springing from the cradle.

**Colours:** green `#19C92C`, yellow `#EAC514`, orange `#F58D12`, red-orange `#E8471C`,
inactive segments 25 % white.

**States:** hidden at rest; visible while dragging; perfect zone flashes on release.

**Data:** pull 0–1 mapped to arc position; perfect band from slingshot upgrade level.

**Motion:** segment fill follows finger; perfect band pulses; on release the arc collapses
into the plane with a 150 ms flash.

**Audio:** rising creak tied to arc position; "perfect" chime if released in the green.

**Checklist**
- [ ] Arc gauge replacing the linear meter
- [ ] 5-zone colour mapping
- [ ] Perfect-release band, widened by slingshot upgrades
- [ ] Gauge tracks pull exactly (no smoothing lag)
- [ ] PERFECT trigger when release lands in the green zone
- [ ] Arc hides on release

**Acceptance:** a player can intentionally hit the green zone after two or three attempts.


---

### 3.9 Flight HUD (Ref-5, Ref-6)

**Purpose:** minimum instruments, maximum world. The references show only four things.

**Layout (360×640 dp):**

| Element | Rect / anchor |
|---|---|
| Speedometer dial | `16, 20, 88×88` |
| Pause | `24, 116, 32×32` |
| Warning text (`LOW SPEED`) | centred under dial at `x 60, y 132` |
| Distance (`167M`) | centred `x 180, y 300`, 30 dp |
| Progress track | `x 326–346, y 20–320` |
| Feedback (`PERFECT!`) | centred `x 180, y 236`, 40 dp |
| Aircraft | lower third, camera-framed |

**Rules:** no top telemetry bar, no artificial horizon, no altitude readout, no coins during
flight. Anything not in the reference HUD goes to results.

**Colours:** white text with `navy` outline; red warnings; blue/white track; green trails.

**Checklist**
- [x] Remove the top three-column telemetry panel
- [x] Remove the attitude gimbal from the flight HUD
- [x] Dial top-left, pause beneath
- [x] Distance centred over the world
- [x] Progress track pinned right, vertically centred on the flight window
- [x] Feedback overlays at the reference position
- [x] Safe margins respected on 20:9

**Acceptance:** screenshot parity with Ref-5/Ref-6 composition at 1284×2778.

---

### 3.10 Speedometer (Ref-5, Ref-6)

**Form:** circular gauge 88 dp: outer chrome ring (two greys), cyan face, tick marks every
10 km/h, dark needle, green "safe" zone from 45–100, digital value and `KM/H` beneath centre.

**Colours:** face `#39BCE6`, rim `#BFCBD4`/`#8E9AA6`, needle `#123`, ticks white 70 %,
value `#37DD6B`, unit white.

**States:** needle eases with real airspeed; below 45 km/h the dial rim flashes red and
`LOW SPEED` appears; above 100 the needle sits in the red.

**Data:** airspeed km/h from the flight model.

**Checklist**
- [x] Arc-dial renderer with tick marks
- [x] Needle smoothing (200 ms) — `GameApp.hudAirspeedKmh`
- [x] Green zone / red zone mapping to gameplay thresholds
- [x] Digital value + unit
- [x] Red rim flash tied to the LOW SPEED state

---

### 3.11 Distance and progress track (Ref-5, Ref-6)

**Form:** tall rounded capsule 20×300 dp with a checkered flag at the top, a yellow arrow
marker, and a small plane icon with a percentage near the bottom.

**Semantics:** the track shows progress toward the realm's target distance, not a lap; the
marker's vertical position equals `distance / target`.

**Colours:** track `#E9F3F9` with `#2F79C7` edge; marker `#FFD52B` with `#1975D1` arrow; flag
black/white; plane icon white.

**States:** marker climbs; flag bounces and the track flashes gold on target; percentage
ticks up in 1 % steps.

**Checklist**
- [x] Capsule track with rim
- [x] Checkered flag asset
- [x] Yellow marker + arrow
- [x] Plane icon + percentage readout
- [x] Percentage = distance / target (per realm)
- [x] Completion flash

---

### 3.12 Feedback and warnings (Ref-5, Ref-6)

| Trigger | Visual | Audio |
|---|---|---|
| Release in green zone | `PERFECT!` pop-in, 40 dp white with blue outline | bright chime |
| Airspeed < 45 km/h | `LOW SPEED` red, pulsing under the dial | low beep every 1.2 s |
| Stall (derived) | `STALL` red banner + nose-down arrow | warning buzz |
| New best distance | `NEW RECORD` gold banner at results | fanfare |
| Thermal/climb (derived, optional) | cyan `+X m` chip | soft whoosh |

**Checklist**
- [ ] Central feedback component with entrance/exit animation — `PERFECT!` pops in with a
  scale overshoot but currently cuts out when the timer ends
- [x] Warning component anchored under the dial
- [x] Thresholds driven by the flight model, not timers
- [x] One feedback at a time, priority: stall > low speed > perfect
- [ ] Haptics on perfect and stall (respect system setting)

---

### 3.13 Results (derived, not shown in references)

**Purpose:** convert a flight into reward and a next action.

**Layout:** modal 328 dp wide, 300 dp tall: headline (`FLIGHT COMPLETE` / `NEW RECORD`),
distance (32 dp) + best, three stat rows (top speed, flight time, perfect launch bonus),
coin reward pill with count-up, buttons `RETRY` (blue), `HANGAR` (white), and an optional
`DOUBLE` rewarded-video slot.

**Data:** distance, best, max speed, duration, coins breakdown, record flag.

**Checklist**
- [x] Results state after landing/crash
- [x] Reward calculation visible as rows, not a single number
- [x] Count-up coin pill using the currency component
- [x] RETRY restarts at the launch screen with the same realm and plane
- [ ] HANGAR returns home and plays the coin cascade (returns home; cascade cue pending)
- [x] Double-reward slot hidden unless enabled

---

### 3.14 Aircraft and build stages (Ref-2 … Ref-6)

**Reference aircraft:** vintage biplane — tan canvas wings with orange-brown rib lines, dark
struts and bracing wires, narrow wooden lattice fuselage, dark cockpit opening, two-blade
green propeller, two dark wheels, small tailplane. Flat-shaded, no gloss, soft shadow.

**Stage catalog (each stage must be visible on the model):**

| Stage | Adds | Gameplay effect |
|---|---|---|
| 1 Fuselage | lattice body, cockpit, wheels | baseline mass |
| 2 Upper wing | broad top wing + struts | lift + |
| 3 Lower wing + wires | second wing, bracing wires | lift ++, drag + |
| 4 Tail assembly | fin + stabiliser | stability + |
| 5 Propeller | 2-blade prop + spinner | windmill, prop wash |
| 6 Engine + paint | engine block, skin/paint scheme, wing rib decals | thrust +, glide + |

**Materials:** flat `#DB9854` canvas, `#8A6A4A` timber, `#3A3A3A` metal/wires, `#53683A`
prop, `#1E1E1E` tyres. No metallic reflections; roughness ~0.9.

**Checklist**
- [x] Biplane mesh: two wings, struts, wires, lattice fuselage, tail, wheels
- [x] Wing rib line decals
- [x] Stage meshes toggle with visible change
- [x] Propeller spin + blur disc
- [x] Flat-shaded material set matching the palette
- [ ] Scale and framing: plane reads at 1/3 screen height in flight (not yet verified on device)
- [x] Planes per tier change silhouette, not just colour

---

### 3.15 Environments (Ref-3 … Ref-6)

**Desert (from refs):** pale sand ridges and canyons, saguaro cacti, leafless trees, rocky
rises, distant animal silhouettes, bright cyan sky with light haze, soft directional shadows,
no water.

**Rules for the other four realms (derived):** same low-poly density, one hero landmark per
view, one saturated accent colour, matching haze and shadow softness. No realism escalation.

**Checklist**
- [ ] Desert terrain: ridges, canyon walls, sand ripples
- [x] Props: mixed species — three tree shapes (broadleaf, conifer, poplar), three shrubs,
  three rock forms; cacti and bare desert trees are still procedural stand-ins
- [x] Sky: cyan gradient + haze band; no clouds or thin wisps only
- [x] Soft shadows (short, low opacity)
- [ ] Distant skyline layer per realm
- [x] Forest / snow / coast / mountain palettes aligned to reference contrast
- [x] Flight corridor kept clear for 300 m ahead
- [x] Realm target-distance and palette live in one definition
- [x] Ground is not uniformly grass — noise-driven meadows of 0.9–1.25 m grass with bare soil
  patches and exposed rock between them
- [x] Per-realm species sets — saguaro and dead trees in the desert, three conifer forms in
  the alpine worlds, palms and reeds on the coast, forest set elsewhere
- [ ] Authored external models — reviewed in `docs/ASSETS.md`; Kenney Nature Kit is the
  preferred source once its archive can be obtained

---

### 3.16 Camera (Ref-2 … Ref-6)

| Context | Spec |
|---|---|
| Hangar | rear-side, slightly elevated, plane lower-middle, slow orbit; sky opening to the right |
| Launch setup | directly behind, ~3.3 m up, centred on the plane |
| Aim | same axis, ~25 % closer as pull increases |
| Flight | rear chase, low, 3–4 m behind, looking slightly down; FOV wide enough for 360 dp |
| Perfect / launch | 0.35 s shake, no cut |
| Results | slow orbit around the landing point |

**Checklist**
- [ ] Hangar framing matches Ref-2 (plane, roof, sky opening)
- [x] Launch framing matches Ref-3/4 (posts at the edges, plane centre) — camera sits on the
  plane's centre line, 3.3 m up and 9.6 m behind, and closes 25 % as the pull deepens
- [ ] Flight framing matches Ref-5/6 (plane lower third, terrain ahead)
- [x] No camera clipping through the slingshot posts
- [x] No automatic camera orbit — the hangar and launch views are fixed until the player drags
- [ ] Portrait vertical FOV tuned so the horizon sits ~40 % down the screen

---

### 3.17 Settings (derived)

Rows: graphics quality (4), water reflections, fog, high-res textures, master/engine/ambience
sliders, haptics, reset progress. Portrait panel, one column, grouped with white cards.

**Checklist**
- [ ] One-column portrait settings panel
- [ ] Haptics toggle wired to a single settings flag
- [ ] Reset progress with double confirm
- [ ] Values persisted in the profile JSON

---

### 3.18 Achievements / career (derived)

Badge grid (2 columns), six badges, progress bars, claim buttons; plus career totals
(flights, distance, best, favourite realm, configuration). Same card language as the hangar.

**Checklist**
- [ ] Badge card component reusing the upgrade-card surface
- [ ] Claim state persisted
- [ ] Career totals derived from the profile, not stored twice

---

### 3.19 Economy and save (Ref-2)

- Coins: earned per metre + speed + perfect bonus + record bonus, multiplied by the INCOME
  track (`x1.0` → `x38.4` style).
- Gems: premium, earned from rewarded slots; not required for the first 30 minutes.
- Costs: each upgrade level costs more than the last; balance so the first upgrade is
  affordable after 1–2 flights.
- Save: JSON in SharedPreferences; schema versioned; corrupt save falls back to defaults.

**Checklist**
- [ ] Compact formatter (`1.2K`, `28.15K`, `1.2M`)
- [ ] Income multiplier applied to all coin sources
- [ ] Save migration from earlier schemas
- [ ] No gem-gated progression in the first hour
- [ ] Chest/rewarded caps persisted
- [ ] All numbers visible in the UI come from one economy model

---

### 3.20 Performance budget (all)

- 60 fps on mid-range; 30 fps floor on the low preset.
- Flight: ≤ 120 draw calls, ≤ 150 k triangles, one 1024² shadow map.
- Hangar: ≤ 60 draw calls, plane ≤ 25 k triangles.
- UI: one overlay pass, ≤ 120 draw ops per frame, no allocations in the draw loop where
  avoidable.
- Textures: ≤ 60 MB total, procedural where possible.
- Load to hangar: ≤ 4 s on mid-range.

**Checklist**
- [ ] Frame-time overlay in debug builds
- [ ] Quality presets that actually change draw distance, shadows, texture size
- [ ] No allocation in per-frame UI paths
- [ ] Thermal test: 10-minute flight, no frame collapse

---

## 4. Implementation sequence

Follow this order; each phase ends with a green CI build.

**Phase A — Design system swap**
- [ ] Replace the glass palette in `gfx/DesignTokens.kt` with the reference palette (§1.1)
- [ ] Add a rounded display font (Fredoka One / Baloo 2 Bold, OFL) for headings
- [ ] Card, pill and outlined-text primitives in `ui/UiFrame.kt`
- [ ] Icon set: coin, gem, gear, pause, flag, plane marker, chest, VIP, rocket, video, plus

**Phase B — Hangar**
- [ ] Hangar 3D set and framing
- [ ] Currency bars + gear
- [ ] Plaque, promo rail, chest/VIP/rocket slots
- [ ] Three upgrade cards bound to slingshot / plane / income
- [ ] TAP TO FLY → launch setup

**Phase C — Launch**
- [ ] Slingshot rig rebuild with striped bands and ramp
- [ ] Launch setup screen with bottom BACK
- [ ] Pull interaction and arc gauge with perfect zone
- [ ] Release: snap, recoil, smoke, shake

**Phase D — Flight HUD**
- [ ] Speedometer dial
- [ ] Distance readout
- [ ] Progress track with flag and percentage
- [ ] PERFECT / LOW SPEED / STALL feedback
- [ ] Remove legacy telemetry panel and gimbal

**Phase E — Results and economy**
- [ ] Results modal with breakdown and count-up
- [ ] Income multiplier across all sources
- [ ] Rewarded slots (configurable)

**Phase F — Content and polish**
- [ ] Biplane stage meshes
- [ ] Five realms aligned to reference density
- [ ] Settings, achievements, career
- [ ] Audio pass
- [ ] Performance pass and presets

---

## 5. Acceptance matrix

| Requirement | Evidence |
|---|---|
| Portrait 360 dp UI | screenshots at 1284×2778 match Ref-2/3/5/6 composition |
| Reference palette | tokens match §1.1; no glass panels remain |
| Hangar completeness | currencies, plaque, promos, chest/VIP/rocket, 3 cards, TAP TO FLY |
| Launch readability | posts at frame edges, plane centred, BACK at bottom |
| Perfect-release mechanic | arc gauge with green zone, PERFECT! feedback |
| Flight HUD parity | dial, pause, distance, progress track only |
| Speed state feedback | LOW SPEED below 45 km/h |
| Aircraft stages | each stage visibly changes the biplane |
| Economy | income multiplier applies, values formatted compactly |
| Build | CI green, installable APK artifact |
