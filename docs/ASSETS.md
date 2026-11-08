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

- **Not adopted**: no pack is integrated, so nothing ships that we cannot verify, and the
  zero-binary-asset build stays intact.
- **Recorded as the preferred future source**: Kenney Nature Kit (CC0, static, single atlas,
  low-poly) — best style/format/performance match. Quaternius MegaKit is the strongest
  alternative if a zip is supplied.
- **Delivered instead**: per-ecosystem species sets in the existing procedural system, which
  is what the packs were wanted for.

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
