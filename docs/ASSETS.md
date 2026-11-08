# Asset source review — external nature packs

Reviewed against the project's actual constraints before anything was adopted. The short
version: **no external pack was integrated**, because none of the usable ones could be
obtained with the tools available here and none of them drops into this renderer without an
importer. The visible gap they were meant to fill — variety per ecosystem — was closed with
the procedural system instead, and the adoption path is prepared below.

## What the renderer can actually consume

| Requirement | This project |
|---|---|
| Mesh input | `MeshData` (interleaved position/normal/UV floats + `Int` indices), built by `MeshBuilder` |
| Mesh formats | none loaded from disk; every mesh is generated in code |
| Textures | generated in `TextureFactory`, uploaded as `Texture2D`; no image files are read |
| Materials | one albedo texture + tint/roughness/metallic/UV scale (`gfx/Material`) |
| Animation | none — `InstancedMesh` carries rigid model matrices only; no skeleton, no skinning, no morph targets |
| Batching | instanced rendering per mesh/species; one draw call per species |
| Budget | 60 fps on mid-range Android, portrait, five realms loaded one at a time |

Anything adopted must therefore arrive as **static triangulated geometry with UVs**, in a
format we can parse, with textures we can decode, or be converted to that before it ships.

## Sources, as inspected

| # | Source | Licence | Formats | What it is | Verdict |
|---|---|---|---|---|---|
| 1 | Quaternius — Stylized Nature MegaKit (116 models) | CC0 | OBJ, FBX, glTF, .blend | Low-poly, "Ghibli-inspired" stylised trees/plants/rocks, textured, 7 leaf variants | **Visually suitable**, not obtainable here |
| 2 | Quaternius — Ultimate Stylized Nature (63 models) | CC0 | OBJ, FBX, glTF, .blend | Stylised nature with seamless textures + normal maps | **Suitable**, smaller than #1, not obtainable |
| 3 | Kenney — Nature Kit (330 files) | CC0 | OBJ, GLB, FBX (kit layout) | Flat-shaded low-poly nature, single colour atlas | **Best integration target**; direct link returned an HTML page |
| 4 | Quaternius — Ultimate Animated Animals (12 animals, 12+ clips each) | CC0 | FBX, glTF, .blend | Rigged, skeletal animation | **Not usable as-is**: needs a skinning pipeline we do not have |
| 5 | OpenGameArt — Low-poly Pigeon | CC0 | `.blend` only (798 KB) | Rigged + animated, untextured | **Not usable**: no mesh/texture export in the archive |
| 6 | OpenGameArt — Yellow-Billed Shrike (996 tris) | CC0 | `.zip` with `.blend` + 2K texture maps (11.2 MB) | Beautiful, IK-rigged bird; sized for close-up rendering | **Not usable**: Blender-only, 2K maps are far over budget for a background bird |
| 7 | Poly Haven — Nature models | CC0 | glTF/FBX + PBR texture sets | Photoscanned, high-poly, realistic | **Rejected on style and performance**: wrong art direction (references are low-poly stylised), large PBR sets |

### Why obtainability decided it

- **Kenney**: the CDN URL reported by the browser
  (`…/nature-kit/37ac38a37b-1677698939/kenney_nature-kit.zip`) is delivered through a
  download page; fetching it returned an HTML document, not the ZIP, so no archive could be
  saved into the repository.
- **Quaternius**: the free packs are served from a tokenised itch.io download page with a
  JavaScript-driven download control (99 MB Standard / 172 MB Pro / 684 MB Source). No direct
  file URL exists to fetch.
- **OpenGameArt**: the two files are `.blend` archives; nothing in this environment can
  convert Blender scenes to OBJ/glTF.
- **Poly Haven**: technically obtainable, but rejected on merit before size became relevant.

### Why the animated options still would not be adopted even if downloadable

`InstancedMesh` uploads one matrix per instance and the PBR program transforms rigid
vertices. Playing #4 or #5/#6 needs: glTF skin/joint parsing, a joint palette uniform, a
skinning vertex shader, animation clip sampling and per-instance animation phase. That is a
second renderer, not an asset import, and it would multiply draw cost for background birds
that currently cost two draw calls.

## Decision

- **Adopted: Quaternius — Ultimate Stylized Nature (CC0).** Its free OBJ set is a close match
  for the plates' low-poly stylised look, the licence is CC0, and the files are static
  (no rig) so they fit the renderer.
- Used as a **near-field hero layer**, not as the whole landscape:
  - Forest: `BirchTree_1`, `MapleTree_1`
  - Alpine: `PineTree_1`
  - Coast: `PalmTree_1`
  - All realms: `Bush`, `Flower_1_Clump`, `Grass_Large`, `Rock_1`
  - Desert: procedural saguaro/dead-tree set stays (no desert props in this pack)
- Bush, grass, flowers and rocks are 54–490 faces; the trees are 240 (palm), 1,092 (pine),
  1,924 (maple) and 2,299 (birch) faces. That is 2–10× the procedural proxy, which is why
  they are instanced only in the near ring (18–90 m around the launch area) and only at
  **HIGH/ULTRA** detail (`detailLevel >= 2`). The procedural scatter still fills the distance.
- The pack's `NormalTree_1` (4,260 faces) and its 19.9 MB 4K bark map were rejected: the tree
  is too heavy for background use and bark is drawn with the game's own procedural texture.
- Animation packs (Quaternius animals, OpenGameArt pigeon/shrike) are still **not** adopted:
  they need skinning, which the renderer does not have. Poly Haven stays rejected on style and
  budget.

### How it is integrated

- **The whole prop set was downloaded** from the supplied Drive folder, file by file, using
  the folder listing + direct `drive.usercontent.google.com/download?id=…` method:
  5 variants each of Birch, Maple, Normal, Pine and Palm; 10 DeadTrees; 6 bushes; 7 flowers;
  3 grasses; 3 plants; 4 petals; 5 rocks; plus the leaf, flower, grass, rock and palm maps
  (17 textures). They live in `assets-source/quaternius/` with the licence record.
- **Shipped in the APK**, mapped per realm:

  | Realm | Trees | Plants / props |
  |---|---|---|
  | Greenwood Forest | BirchTree_1/2, MapleTree_1/2, NormalTree_1 | Bush, Bush_Large, Bush_Small, Flower_1/2/3_Clump, Grass_Large, Grass_Small, Plant_1, Rock_1/2/3 |
  | Snow / Mountain | PineTree_1/2/3 | Bush, Bush_Small, Grass_Small, Plant_1, Rock_1/2/3 |
  | Ocean coast | PalmTree_1/2/3/4 | Bush_Large, Bush_Flowers, Grass_Large, Grass_Small, Plant_1, Rock_1/2/3 |
  | Desert | DeadTree_1/2/5/9 (bark-only) | Grass_Small, Rock_1/2/3 |

  Each model is 240–2,300 faces; counts are 5–10 instances per model, so the near
  ring stays in the low hundreds of thousands of triangles at HIGH/ULTRA.
- The pack's 4K bark/trunk maps are **not** shipped; the matching glTF JPEGs (321–991 KB) are
  used instead, so trunks carry the pack's own bark detail at a fraction of the size.
- `gfx/ObjLoader` parses OBJ at load time: triangulates Blender's n-gon faces, splits the
  geometry into the bark and foliage material groups, flips the OBJ V coordinate to match the
  decoded PNG orientation, and normalises both groups with one shared transform (1 unit tall,
  base at y = 0, centred in XZ) so the two meshes stay aligned.
- `gfx/AssetTextures` decodes each PNG with `inSampleSize` down to ≤512 px, uploads it
  mip-mapped, and recycles the bitmap — the source maps are much larger than a phone needs.
- `WorldScene` builds one `Mesh` + `InstancedMesh` per model and scatters it with the same
  deterministic terrain rules used by the procedural species. Their draw calls are added to
  the existing shadow pass and main pass, so no new renderer state is involved.
- Every step is wrapped: if a file is missing or a parse fails, that model is skipped and the
  procedural vegetation still fills the world.

## Inventory — what the supplied Drive folder actually contains

Verified folder by folder (`OBJ`, `glTF`, `Blends`, `FBX`, `Textures`):

- **63 meshes**: Birch 1-5, Maple 1-5, NormalTree 1-5, Pine 1-5, Palm 1-5, DeadTree 1-10,
  6 bushes, 7 flowers, 3 grasses, 3 plants, 4 petals, 5 rocks. All 63 OBJ files are downloaded
  (the used ones in `app/src/main/assets`, the rest in `assets-source`).
- **21 texture maps.** The 4K PNG bark/normal maps (up to 19.9 MB) were replaced by the
  matching JPEGs from the glTF folder (321–991 KB) — `BirchTree_Bark`, `MapleTree_Bark`,
  `NormalTree_Bark`, `PineTree_Bark`, `PalmTree_Trunk`, `Rocks`. Leaves, flowers, grass and
  bush maps are PNGs and are used as shipped.
- `License.txt` and `Preview.jpg` are kept in `assets-source/quaternius/` for provenance.
- **No fauna, no birds, no sky, no mountain or field geometry.** The pack is vegetation and
  rocks only; there is no animal or bird model anywhere in its five subfolders.

Bark maps are now used per species (birch on birch, pine on pine, palm trunk on palms, and so
on) instead of the procedural bark, which matches the models' own UV layout. Dead trees use the
NormalTree bark.

### Where the rest of the ecosystem comes from

| Need | Source |
|---|---|
| Animals / birds | **Not in this pack.** Quaternius' *Ultimate Animated Animals* (CC0, 12 animals) is the natural follow-up, but it is delivered through a tokenised itch.io download, and the engine has no skeletal animation — static OBJ poses would work, animation would need a skinning renderer. |
| Mountains, fields, valley | `Terrain` + `TerrainMesh` height field: ridges, meadows, bare soil. No meshes needed. |
| Sky, clouds, haze | Analytic `SkyRenderer` plus fog in the PBR shaders. |
| Water | `WaterRenderer` (normal-mapped, Fresnel). |
| Wind, snow, dust | `WeatherSystem` + `ParticleRenderer`. |

## Animals — Ultimate Animated Animals (CC0)

Second supplied Drive folder, same method. It holds 12 rigged animals — Alpaca, Bull, Cow,
Deer, Donkey, Fox, Horse, Horse_White, Husky, ShibaInu, Stag, Wolf — each with 12+ skeletal
clips, in OBJ/MTL plus glTF/FBX/Blend. **All 12 OBJ/MTL pairs are downloaded.**

- **Shipped:** Deer (forest), Donkey (desert), Husky (snow), Cow (coast), Stag (mountain).
- **No texture maps** — the MTL carries flat `Kd` colours across 5–7 materials per animal.
  `gfx/AnimalLoader` splits each OBJ by material, reads those colours, normalises all groups
  with one transform to a target height, and returns one mesh per colour, drawn with the white
  texture tinted by the MTL colour.
- They replace the procedural animal mesh in `WorldScene` while keeping the existing scripted
  grazing/wandering instance matrices, so motion and spawn rules are unchanged. The procedural
  mesh stays as the fallback.
- **Animation is not playable:** the clips are skeletal and the renderer has no skinning, so the
  animals stand in their bind pose and the scripted path moves them.
- **No birds in this pack.** Birds remain the procedural silhouettes in `Wildlife.Birds`.

Python-style "everything else" (mountains, fields, sky, weather) stays procedural — see the
table above; the packs contain no geometry for it.

## Ships — Quaternius Ships Pack (CC0)

Third supplied Drive folder, same method. Six static models: Boat, BoatWSail, CruiseShip,
Lifeboat, Sail ship, Viking boat. **All six OBJ/MTL pairs are downloaded.**

- Shipped and used in the **ocean realm only**: `VikingBoat`, `BoatWSail`, `Lifeboat`,
  `SailShip`, `CruiseShip` — five vessels anchored at the water line (18 m and 22 m tall for
  the sailing ship and cruise liner), each slowly bobbing and yawing on the swell.
- Flat MTL colours again, so they reuse the same colour-splitting loader as the animals and
  are drawn with the white texture tinted per material.
- `Boat` (the rowing boat) stays in `assets-source/ships` unused.

## What was implemented in its place

`WorldScene` now selects a plant family from the realm and builds different silhouettes with
the same scatter rules and instancing:

| Realm | Trees | Shrubs | Grass |
|---|---|---|---|
| Greenwood Forest | broadleaf, conifer, poplar | round bush, creeping scrub, spiky | tall blades, tufts, low clumps |
| Desert | saguaro cactus with arms, bare wind-worn tree, low scrub | barrel cactus, dry sage, spiky scrub | short sparse tufts (0.5× height) |
| Snow / Mountain | three conifer forms incl. low wide pine | low pine, creeping, spiky | meadow grass |
| Ocean coast | leaning palm with frond crown, broadleaf, poplar | reeds, wide creeping scrub, bush | tall reeds |
| Countryside | forest set | forest set | meadow grass |

Distribution still comes from noise fields (`Vegetation`), so species cluster into stands
rather than repeating uniformly, and grass still only grows in meadows with bare soil
between them. No new draw calls beyond the three species each category already had.

## If the Kenney ZIP is supplied

1. Drop it at `assets-source/kenney_nature-kit.zip`.
2. A Gradle task unzips `**/*.obj`, the shared atlas PNG and the licence into a generated
   assets directory (no files copied into the source tree).
3. A small OBJ reader triangulates faces, generates missing normals, normalises each model to
   a target height with its base at y = 0, and hands `MeshData` to the existing instancing.
4. Models are categorised by file name into trees / shrubs / rocks, appended to the species
   lists, and **fall back to the procedural set if anything fails to parse**.
5. Keep `docs/licenses/Kenney-Nature-Kit-CC0.txt` with the build; CC0 requires no
   attribution, but the record stays.

Step 2–4 are deliberately not written until a real archive is available, because names,
atlas layout and material assignments cannot be verified from documentation alone.
