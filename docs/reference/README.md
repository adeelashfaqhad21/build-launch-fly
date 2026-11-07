# Reference image slots

Visual reference images for the five ecosystems. The design specification
(`docs/DESIGN.md` §3 and the verification matrix in §8) refers to five reference
plates, mapped as follows:

| Slot | File name to use | Biome / world | Design token in `design.md` |
|---|---|---|---|
| 1 | `biome-mountain.png` | Mountain / countryside (alpine terraces, highland valleys) | `{{DATA:IMAGE:IMAGE_1}}` |
| 2 | `biome-ocean.png` | Ocean / coastal (cliffs, sea stacks, quartz shoreline) | `{{DATA:IMAGE:IMAGE_2}}` |
| 3 | `slot-desert.png` → rename to `biome-desert.png` | Desert (barchan dunes, wadis, buttes) | `{{DATA:IMAGE:IMAGE_3}}` |
| 4 | `biome-snow.png` | Snow / glacier (crevasses, frozen lake, spindrift) | `{{DATA:IMAGE:IMAGE_4}}` |
| 5 | `biome-forest.png` | Forest / green landscape (canopy, stream, sun shafts) | `{{DATA:IMAGE:IMAGE_5}}` |

Optional extras, if available:

- `aircraft-stages.png` — the six-stage aircraft progression reference
- `slingshot-rig.png` — the kinetic slingshot / launch rig reference
- `ui-screens.png` — any UI/UX reference board

## How these are used

- **Art direction:** target look for each biome's terrain shaping, palette, atmosphere,
  vegetation density and depth layering (`world/WorldDefinition.kt`,
  `gfx/TextureFactory.kt`).
- **Review aid:** compare a build screenshot against the plate for that biome.
- **Not shipped in the APK.** This folder is documentation only; nothing here is packaged
  into the app and no code references these files.

## Adding more

Drop PNG/JPG/WebP files into this folder using the names above (or add new rows to the
table), then commit. Keep images reasonably sized (long edge ≤ 2048 px) so the repository
stays light.
