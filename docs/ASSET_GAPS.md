# Imported-asset gap list

Purpose: everything the game still draws with generated (procedural) code, what imported
asset would replace it, and what is already in `app/src/main/assets/`. This is the shopping
list that has to be filled before the generated geometry can be deleted.

Which packs to fetch, with links and where to put them, is in
[`DOWNLOADS.md`](DOWNLOADS.md).

## What is imported today

**~129 imported files** ship in `app/src/main/assets/`:

| Pack | Shipped files | Licence |
|---|---|---|
| Quaternius Ultimate Stylized Nature | 21 trees, 10 bushes/flowers/grasses, 4 rocks + 14 maps | CC0 |
| Quaternius Ultimate Animated Animals | Deer, Donkey, Husky, Cow, Stag (.obj + .mtl) | CC0 |
| Quaternius Ships | VikingBoat, BoatWSail, Lifeboat, SailShip, CruiseShip (.obj + .mtl) | CC0 |
| Quaternius Fantasy RTS | 10 buildings/mountains/props (.obj + .png) | CC0 |
| Kenney Nature Kit | 9 models (.obj + .mtl) in `models/nature` | CC0 |
| Kenney Castle Kit | 7 models + `colormap.png` in `models/castle` | CC0 |
| heyheythere Low-Poly Aircraft Kit | 1 archive, 2 props read out of it (windsock, cone) | CC BY 4.0 |
| Poly Haven ground set | 13 diffuse maps (1K JPG) in `terrain` | CC0 |
| ambientCG painted surfaces | 2 colour maps (`rubber`, `hazard`) in `terrain` | CC0 |

Poly Pizza's `samolet` biplane is **downloaded and shipped but not loaded by any code** —
see §1.

Terrain now uses imported diffuse maps for grass, rock, sand, snow, soil, forest floor,
beach sand, cracked dry, cliff rock, gravel, planks, bark and metal. The slingshot's
warning-striped band reads the imported `hazard` map, and the propeller blur disc and the
birds read `rubber`. The generated versions remain only as a fallback so a missing file can
never break a world.

---

## 1. The aircraft — imported

The flying biplane is the imported `samolet` model (`assets/aircraft/samolet/model.obj` +
`materials.mtl`, CC BY 4.0), split by its seven flat `Kd` materials at load time
(`aircraft/AircraftModelLoader`) and drawn by `AircraftMesh`. The procedural airframe is kept
only as the fallback used when the OBJ cannot be read.

Remains open:

- **per-stage meshes**. The model is one welded mesh with no separate wing, tail or propeller
  objects, so the build stages cannot add or remove geometry; today they change paint, the
  engine and the tail. A pose-able or split airframe would restore per-stage visibility.
- **spinning propeller**. The model has a static propeller, so only the procedural spin-blur
  disc animates.
- the Poly-Google plane in `assets-source/Planes /` is a ready-made **second airframe**.

## 2. Hangar and launch ramp — shell still generated

`world/HangarMesh.kt` builds the shell (plank wall on studs, open right side, gable roof with
rafters/ties/purlins, door frame, plank floor, back wall) and the 6 m launch ramp.

**Imported now:** the furniture standing on the floor — workbench, stool, bench, two boxes and
a drum — comes from the Kenney Furniture Kit (CC0) in `assets/models/furniture`, split by its
flat material colours and placed by `HangarMesh.props`. The procedural hay bales, crates,
barrel, bench and stool were removed.

Still needed: an imported timber **shed shell and ramp** (the walls, roof and 6 m ramp are
still built in code). None of the packs in use has a barn; Kenney's Castle Kit `wall-narrow-wood*`
and `tree-log` could dress the walls, and Fantasy Town Kit has building shells.

## 3. Slingshot — still generated

`slingshot/SlingshotMesh.kt` (294 lines): two squared timber posts plus a band drawn as
eight stretched segments.

Needed: two timber post models (or one modular beam used twice) and a band/rope mesh. No
imported slingshot exists in any pack in use.

## 4. Birds and extra animals — still generated

Birds are procedural triangles (`WorldScene.buildBirdData`), and so is the fallback animal
mesh (`buildAnimalData`) used when a pack model is missing.

Needed: one low-poly bird (a few flap poses, or glTF animation). **Free win:** the
Quaternius animal pack also has `Alpaca`, `Bull`, `Fox`, `Horse`, `Horse_White`,
`ShibaInu` and `Wolf` unzipped in `assets-source/animals/` — they are not in the APK yet,
and would give every realm its own wildlife.

## 5. Realm-specific buildings — partly covered

Imported: houses, windmill, watchtower, town centre, tower house, dock, mine, crate,
barrel, two mountains, plus the castle tower/wall ruin in every realm.

Still needed:

- **desert**: cliff dwelling, oil derrick, dead-town shell — the fantasy houses read wrong in
  sand;
- **alpine**: chalet, ski-lift pieces;
- **coast**: lighthouse, fishing huts, extra pier modules;
- **mountain valley**: fence posts and field boundaries. **The Kenney Nature Kit already
  ships `fence_*` models**, unused.

## 6. Flight-course obstacles

The flight is meant to weave through obstacles; today only buildings, trees and rocks serve.

Needed: hot-air balloon, rock arch, stone pillars, floating rings/course markers, target
flag. **The Kenney Castle Kit already ships `rocks-large`, `gate`, `metal-gate`, flags,
`siege-*` and `tower-hexagon-*`** — unused and immediately available.

## 7. Far-field vegetation

The 18–140 m ring is imported (Quaternius + Kenney). The 140–400 m scatter is still
procedural (`Vegetation.kt`, 264 lines).

Needed: 2–3 low-poly or billboard tree cards per biome so the far scatter can be deleted.

## 8. Textures still generated (`gfx/TextureFactory.kt`)

Two remain, plus the 1×1 `white`:

| Texture | Used by | Needed |
|---|---|---|
| `leaf` | procedural canopies | replaced when §7 lands |
| `waterNormals` | water shader | a real water normal map (Poly Haven has tiling water normals, CC0) |

`hazard` and `rubber` are now imported: `assets/terrain/rubber.jpg` and
`assets/terrain/hazard.jpg` come from ambientCG Rubber 004 and Painted Metal 016 (CC0,
`docs/licenses/AmbientCG-CC0.txt`) and are preferred over the generated maps at load time.

Normal and roughness maps for the 13 ground diffuse maps would also sharpen them, but the
shader samples one map per surface today.

## 9. UI — still vector drawing

`ui/UiIcons.kt`, `ui/UiFrame.kt`, `ui/UiPainter.kt` draw every icon, card, pill and plaque
with Canvas paths.

Needed if we want imported 2D: coin, gem, pros, chest, VIP cap, rocket, 9-slice card frame,
panel/button skins, gear/trophy/pilot icons, plaque plate. Kenney's *UI Pack* (CC0) covers
all of it.

## 10. Audio — still procedural

`audio/AudioEngine.kt` (291 lines) synthesises wind, propeller, creak, release, ambience and
UI cues.

Needed if we import sound: wind loop, propeller loop, elastic creak, release whoosh,
per-realm ambience beds, UI pops, coin cascade.

---

## Already downloaded, not wired

These cost nothing to adopt — the files are already on disk:

| Source | Unused assets | Would fill |
|---|---|---|
| `assets-source/animals/` | Alpaca, Bull, Fox, Horse, Horse_White, ShibaInu, Wolf | §4 wildlife variety |
| `assets-source/Planes /` | Airplane by Poly by Google, very cute airplane by Akash Rudra | §1 alternate airframe |
| Kenney Castle Kit | bridge, gate, metal-gate, ruins, siege engines, hexagon towers, flags, stairs, ground tiles | §2, §6 |
| Kenney Nature Kit | cliffs, fences, crops, flowers, mushrooms, logs, lilies, statues, stumps, tents, more trees | §5, §6, §7 |

## Next in order

1. **Aircraft** (§1) — the hero object; the model is already in the APK.
2. **Wildlife variety** (§4) — 7 animal models already unzipped, no new download.
3. **Obstacles from the castle/nature kits** (§6) — no new download.
4. **Hangar shell and ramp** (§2) — needs a real building pack.
5. **Slingshot** (§3), **far-field trees** (§7), **remaining textures** (§8).
6. **UI and audio** (§9, §10) — last, and only if the vector/synth versions are to go.

## What removing the generated half will take

`AircraftMesh`, `HangarMesh`, `SlingshotMesh`, `Vegetation`, `MeshBuilder` primitives and
`TextureFactory` are all procedural. None can be deleted until the pack for that slot is in
`assets/` and has a loader. The safe sequence stays:

1. import pack → add loader (`ObjLoader` / `PropLoader` / `AnimalLoader` already exist);
2. switch the feature to the imported mesh behind a fallback;
3. delete the procedural builder for that feature in a separate commit;
4. repeat per feature.

That keeps the APK buildable and playable at every step, which a big-bang deletion would not.
