# World rendering audit — 2026-11-09

Branch: `working` (baseline cloned from the latest `main`)

## Confirmed findings and changes

| Area | Finding | Action |
|---|---|---|
| Fog | The previous linear fog expression caused distant ground to blend too strongly into the blue-grey fog palette. | Changed the shared fog function to exponential-squared attenuation and reduced the five realm fog densities. |
| Terrain ambient colour | Upward-facing terrain normals received the full blue sky-ambient value, tinting otherwise natural grass, soil and rock maps cyan in every non-desert realm. | Added a terrain-only ambient sky-fill clamp: blue is limited relative to green. Other props keep their original world sky lighting. |
| Glacier Reach | Surface C is rock, but the snow realm previously used a negative snow-line threshold, causing the high-altitude layer to cover nearly all terrain. | Raised the snow-line threshold so the snow base and rock/cliff slope layer remain visible. |
| CI cadence | Pushes to `working` were not covered by the Android workflow. | Added `working` to the push branch filter. |
| Imported vegetation coverage | Quaternius/Kenney models were imported but Quaternius scatter was limited to a 18–140 m ring around the launch pad. Procedural vegetation therefore dominated most visible ground. | Expanded imported-prop distribution into the first flight region and increased imported species density, with extra grass clumps in forest, alpine and coastal realms. |

## Remaining audit work

- Verify the terrain-only ambient correction and each realm's terrain layer mapping / photographic ground-map colour balance in an installed build.
- Verify the exact image content of the five supplied Poly Haven diffuse maps against the existing runtime files `terrain/grass.jpg`, `terrain/sand.jpg`, `terrain/rock.jpg`, `terrain/snow.jpg`, and `terrain/forest_floor.jpg`. All five runtime files already exist and are loaded by `TextureFactory`; the snow file is byte-identical to the supplied diffuse map, while the other four have different Git blob hashes and still need visual/content verification before replacement. The archives also contain displacement, roughness and normal maps; only diffuse maps are currently consumed by the terrain shader.
- Rebalance procedural fallback trees, shrubs and grass against the imported realm-specific species after visual inspection.
- Verify grass visibility, vegetation silhouettes and density at the actual gameplay camera height.
- Re-audit aircraft scale, hangar orientation, launch rail alignment and collision clearance against the six original reference screenshots.
- Inspect shadow-pass ordering and ensure every visible world prop receives the intended shadow treatment.

## Verification policy

Do not run builds for individual edits. Run one Android debug/release build after the current grouped terrain, vegetation, aircraft and launch-system batch is ready.
