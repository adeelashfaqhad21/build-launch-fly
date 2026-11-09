# Asset audit

Written after the second wave of imports (City Kit Industrial, Fantasy Town Kit, Fantasy UI
Borders, UI Pack, Interface Sounds, Impact Sounds) was unzipped into `assets-source/`.

The audit has two jobs: say honestly what each part of the game draws with, and name the
defect behind the broken ground.

## The ground defect

The terrain was generating three albedos and blending them in `TERRAIN_FRAG`. When the
generated maps were swapped for Poly Haven photographs, the rest of the material kept the
settings that had been tuned for procedural noise:

| Setting | Was (noise) | Problem with a photo | Now |
|---|---|---|---|
| `uUvScale` | `1.0` | `TerrainMesh` writes one UV unit per 8 m, so a 512² photo repeated **every 8 m** — reads as gravel shimmer, not grass | constants in `SceneRenderer`: `TERRAIN_UV_SCALE = 0.75f` (≈10.7 m per tile) |
| Layer B | `vUv * 0.37` | 21 m tile, fine | `0.50` (≈21 m) |
| Layer C (snow/tint) | `vUv * 2.13` | 3.8 m tile of a photo, aliasing hard in the distance | `0.35` (≈30 m) |
| Soil D | `vUv * 1.7` + a warm cast | warm cast fought the photo's own colour | `0.70`, no cast |
| Detail term | `texture(uTexC, vUv * 0.07).r` | a 114 m sample of a photo's red channel — large muddy blotches | `texture(uTexA, vUv * 0.12).r` at ±6 % |
| Realm tints | untouched | `tintA = (0.86, 1.02, 0.74)` over a green photo compounds to olive mush | `muted()` pulls each tint 80 % back to white |

The tints stay in `WorldDefinition` as design intent; `SceneRenderer.muted()` decides how
much of them a photographic map can carry.

## Inventory

| System | Imported (CC0 unless noted) | Still generated | Verdict |
|---|---|---|---|
| Aircraft | `samolet` biplane, CC BY 4.0, split by material | full procedural airframe as fallback | **done** |
| Ground textures | 13 Poly Haven 1K maps + 2 ambientCG maps | `leaf`, `waterNormals` | done except two maps |
| Vegetation, near field | Quaternius (21 trees, 10 plants, 4 rocks), Kenney Nature Kit (9 models) | `Vegetation.kt` for the 140–400 m scatter | near field done |
| Wildlife | Quaternius animals — 12 shipped, 2–3 per realm | birds (`buildBirdData`) | done except birds |
| Ships | 5 Quaternius ships | — | **done** |
| Landmarks | Quaternius fantasy RTS (10 buildings + mountains) | — | **done** |
| Ruins | Kenney Castle Kit (tower + walls) | — | **done** |
| Hangar props | Kenney Furniture Kit (5 props) | — | **done** |
| Hangar shell + 6 m ramp | nothing wired yet | `world/HangarMesh.kt` | **open** — City Industrial is on disk |
| Slingshot | nothing wired yet | `slingshot/SlingshotMesh.kt` | **open** — Castle/Nature timber is on disk |
| Realm-specific buildings | Quaternius houses (read wrong in sand and snow) | — | **open** — Fantasy Town is on disk |
| Flight obstacles | buildings, trees, rocks only | — | **open** — Castle gates/rocks/flags on disk |
| UI | vector only | `ui/UiFrame.kt`, `ui/UiIcons.kt`, `ui/UiPainter.kt` | **open** — Fantasy UI Borders + UI Pack on disk |
| Audio | synthesised | `audio/AudioEngine.kt` | **open** — Interface/Impact sounds on disk |

## Shipped files

```
app/src/main/assets/
  aircraft/samolet/             model.obj, materials.mtl
  animals/                      12 × (.obj + .mtl)
  buildings/                    10 Quaternius buildings + PNGs
  models/castle/                7 × .obj + colormap.png
  models/furniture/             5 × (.obj + .mtl)
  models/nature/                9 × (.obj + .mtl)
  quaternius/                   22 models + 14 maps
  ships/                        5 × (.obj + .mtl)
  terrain/                      15 JPEG diffuse maps
  kits/aircraft-kit.zip         windsock + cone, read at runtime
```

Two loaders cover all of it: `gfx/ObjLoader` for models with a PNG (`buildings`,
`quaternius`, `ships`), and `gfx/AnimalLoader` for models with only flat MTL colours
(`animals`, `models/nature`, `models/furniture`, `samolet`). `gfx/PropLoader` handles the
modular Kenney kits on their shared 1-unit grid.

## Gaps that no download fixes

- **Birds** — no pack in use has one; needs a bird model whose format we can read.
- **Skeletal animation** — needs a skinning pass in the renderer.
- **Split airframe** — `samolet` is one welded mesh, so build stages cannot add wings.

## Next, in order

1. Hangar shell and ramp from City Kit Industrial (replaces the last generated hero object).
2. Slingshot posts from the Castle/Nature timber, band stays the imported hazard map.
3. Fantasy Town building clusters for desert, coast and alpine.
4. Obstacles from the Castle Kit on the flight path.
5. UI frames from Fantasy UI Borders; audio from Interface/Impact Sounds last.
