# Imported-asset gap list

Purpose: everything the game still draws with generated (procedural) code, what imported
asset would replace it, and what we already have. This is the shopping list that has to be
filled before the generated geometry can be deleted.

Status today: **110 imported files** ship in `app/src/main/assets/` —

| Pack | Files | Licence |
|---|---|---|
| Quaternius Ultimate Stylized Nature | 21 trees, 9 bushes/plants/flowers/grass, 4 rocks + textures | CC0 |
| Quaternius Ultimate Animated Animals | Deer, Donkey, Husky, Cow, Stag (.obj + .mtl) | CC0 |
| Quaternius Ships | VikingBoat, BoatWSail, Lifeboat, SailShip, CruiseShip | CC0 |
| Quaternius Fantasy RTS | 8 buildings, 2 mountains, Crate, Barrel | CC0 |
| heyheythere Low-Poly Aircraft Kit | 1 archive (2 props used) | CC BY 4.0 |

Everything else in the game is generated at runtime by code, and that is what the list below
covers.

---

## 1. Aircraft — the biggest gap

There is **no imported aircraft**. The biplane in `aircraft/AircraftMesh.kt` is built from
boxes, cylinders and rods in code (362 lines).

Needed, one low-poly vintage biplane in OBJ/glTF split by material, plus separate meshes for
each build stage so upgrades stay visible:

| Slot | Imported asset needed | Notes |
|---|---|---|
| BODY | fuselage + cockpit + wheels, one mesh | ~1,500–4,000 tris, canvas + timber + metal materials |
| WING_LEFT | left wing pair, own mesh | must mirror cleanly for Mk II span |
| WING_RIGHT | right wing pair, own mesh | same, mirrored |
| TAIL | fin + stabiliser, own mesh | |
| PROPELLER | 2-blade prop + hub, own mesh | pivot at hub so it can spin |
| ENGINE | engine block + exhaust, own mesh | |
| Extras | spinning-blur disc, pilot figure (optional) | |

Style: tan dodged canvas, orange-brown ribs, dark timber, flat shading — matches Ref-3.

## 2. Hangar and launch ramp

Generated in `world/HangarMesh.kt` (plank wall, studs, gable roof, door frame, floor, bales,
crates, barrel, bench, lamp, ramp).

Needed imported set, either as one shed model or as parts:

- hangar shell: left plank wall, open right side with posts, back wall, gable roof with exposed
  rafters/ties/purlins, front door frame;
- interior props: hay bales, crates, barrel, workbench, stool, hanging lamp;
- **launch ramp**: 6 m deck, 1 m rise, timber substructure and kerb rails.

Suggested sources: Quaternius' *Ultimate Modular Buildings*, *Farm* and *Medieval Village*
packs (CC0), or Kenney's *Castle Kit* / *Furniture Kit* (CC0). Nothing in the current packs
covers a timber hangar.

## 3. Slingshot

Generated in `slingshot/SlingshotMesh.kt`: two wide squared posts plus a unit-cylinder band
stretched along a curve each frame.

Needed:

- two wide timber posts (or one modular timber-beam model used twice);
- a band that can be drawn as a stretchable tube — any unit cylinder/rope asset works, or a
  dedicated slingshot model with two prongs and a band;
- 4 upgrade states for the band (thinner/rougher to thicker/cleaner).

No imported slingshot exists in the current packs.

## 4. Terrain and ground textures — **partly imported**

`app/src/main/assets/terrain/` now ships five Poly Haven CC0 diffuse maps (1K JPG):
`grass.jpg` (Aerial Grass Rock), `rock.jpg` (Gray Rocks), `sand.jpg` (Gravelly Sand),
`snow.jpg` (Snow 02) and `soil.jpg` (Brown Mud Dry). `TextureFactory` loads them and keeps
the generated versions only as a fallback — see `docs/licenses/PolyHaven-CC0.txt`.

Still needed (for the generated ones that remain in `gfx/TextureFactory.kt`): a proper ice
map, a wood-plank set for the hangar, bark for the posts, painted metal, and the water
normal map. Normal/roughness maps for the five above would also sharpen them.

## 5. Vegetation still generated

Generated in `world/Vegetation.kt` (264 lines) and `WorldScene.kt` — procedural trunk/canopy
meshes, shrubs, cacti, reeds, grass clumps, and the far-field tree ring.

Already imported (kept): Birch 1-2, Maple 1-2/4, Normal 1/5, Pine 1-3/5, Palm 1-5,
Dead 1/2/3/5/9, Bush/Bush_Large/Bush_Small/Bush_Flowers, Flower 1-3 clumps, Grass L/S,
Plant_1, Rock 1-4.

Still needed as imports:

- **desert**: cacti (saguaro, barrel), desert shrubs, dry grass tufts — none in the nature pack;
- **alpine**: snow-capped conifers (the pines work, but a snow variant would help);
- **coast**: reeds/marsh grass, driftwood;
- **far ring** (90–400 m): 2–3 billboard or low-poly tree cards so the procedural far scatter
  can be removed.

## 6. Animals and birds

Imported: Deer, Donkey, Husky, Cow, Stag (static bind pose).

Needed:

- **birds**: no bird model at all — birds are procedural triangles today. A single low-poly
  bird with 2–3 flap poses (or glTF animation) would replace `Wildlife.Birds` geometry;
- desert camel or lizard, alpine ibex, coastal seagull/gull — optional, one per realm;
- animated versions of the existing five animals if we want walking (needs skinning support).

## 7. Buildings and landmarks

Imported: Houses, Windmill, WatchTower, TownCenter, TowerHouse, Dock, Mine, Crate, Barrel,
2 mountains.

Still needed:

- a **desert** set (cliff dwelling, oil derrick, dead-town shell) — the fantasy houses look
  wrong in sand;
- **alpine** chalet/ski-lift pieces;
- **coast** lighthouse, fishing huts, piers (dock exists);
- fence posts and field boundaries for the mountain valley.

## 8. Obstacles for the flight course

The flight is meant to weave through obstacles. Currently only buildings and trees serve.
Needed imported props: hot-air balloon, wind turbine, rock arch, stone pillars, ruins, floating
rings/markers (for a course), and a target/flag marker.

## 9. UI and 2D

Generated as Canvas vector drawing (`ui/UiIcons.kt`, `ui/UiFrame.kt`, `ui/UiPainter.kt`).

Needed if we want imported 2D: coin, gem, pros, chest, VIP cap, rocket, 9-slice card frame,
panel/button skins, gear/trophy/pilot icons, and the plaque plate. These are best delivered as
SVG/PNG slices (Kenney *UI Pack*, CC0).

## 10. Audio

Fully procedural (`audio/AudioEngine.kt`). Needed if we import sound: wind loop, propeller
loop, elastic creak, release whoosh, per-realm ambience beds, UI pops, coin cascade.

---

## Suggested import order

1. **Aircraft** — it is the player's hero object and the only pack that is completely absent.
2. **Hangar shell + ramp** — the launch screen is built around it.
3. **Terrain texture set** — replaces 13 generated textures in one go and fixes the ground
   look independently of geometry.
4. **Slingshot** — small, quick win.
5. **Desert/coast/alpine props** — fills the realm gaps.
6. **Birds and extra animals** — animation work, do after the static scene is stable.

## What removing the generated half will take

`AircraftMesh`, `HangarMesh`, `SlingshotMesh`, `Vegetation`, `TerrainMesh` geometry helpers,
`MeshBuilder` primitives and `TextureFactory` are all procedural. None of them can be deleted
until the pack for that slot is in `assets/` and has a loader. The safe sequence is:

1. import pack → add loader (`ObjLoader`/`PropLoader` already exist);
2. switch the feature to the imported mesh behind a fallback;
3. delete the procedural builder for that feature in a separate commit;
4. repeat per feature.

That keeps the APK buildable and playable at every step, which a big-bang deletion would not.
