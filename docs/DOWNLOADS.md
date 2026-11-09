# What still has to be downloaded

The companion to [`ASSET_GAPS.md`](ASSET_GAPS.md): that file says *what is still generated*,
this one says *what to fetch to replace it*.

Everything here is **CC0**, so it can ship in the APK with no attribution obligation (we
credit it anyway, in the README and `docs/licenses/`).

Kenney's download button is JavaScript, so its archives cannot be fetched with a direct URL
from here — the page link is given and the zip has to be downloaded in a browser. ambientCG
and Poly Haven do have direct file URLs, so those can be fetched straight into the project.

## The short list, in the order that pays off most

| # | Pack | Link | Fills | Unzip to |
|---|---|---|---|---|
| 1 | **City Kit (Industrial)** — CC0, 3D, 40 models | <https://kenney.nl/assets/city-kit-industrial> | §2 the hangar/workshop shell: warehouse walls, roofs, roller doors, rafters, stairs, pipes, containers | `assets-source/kenney_city-industrial/` |
| 2 | **Fantasy Town Kit** — CC0, 3D, 160 models | <https://kenney.nl/assets/fantasy-town-kit> | §5 realm buildings: desert town shells, alpine chalets, coast huts, plus flags and market props | `assets-source/kenney_fantasy-town/` |
| 3 | **Fantasy UI Borders** — CC0, 2D | <https://kenney.nl/assets/fantasy-ui-borders> | §9 the nine-slice card, plaque and panel frames — the exact shape language of `Ref-1`…`Ref-7` | `assets-source/kenney_fantasy-ui-borders/` |
| 4 | **UI Pack** — CC0, 2D, 430 files | <https://kenney.nl/assets/ui-pack> | §9 buttons, sliders, toggles, arrows, progress bars for settings and the workshops | `assets-source/kenney_ui-pack/` |
| 5 | **Interface Sounds** — CC0, 100 files | <https://kenney.nl/assets/interface-sounds> | §10 taps, confirms, purchases, screen transitions | `assets-source/kenney_interface-sounds/` |
| 6 | **Impact Sounds** — CC0, 130 files | <https://kenney.nl/assets/impact-sounds> | §10 the slingshot release, the crash on landing, debris hits | `assets-source/kenney_impact-sounds/` |

Packs 1 and 2 are the ones that change how the game looks; 3–6 are polish and can follow.

Optional, lower value:

- **RPG Audio** — <https://kenney.nl/assets/rpg-audio> — wooden creaks, mechanism clicks and
  coin cascades for the launch and the results screen.
- **Water normals** — ambientCG has no dedicated water-surface set, and the ocean shader
  samples a normal map rather than a colour map; the generated one is good enough until the
  coast realm gets its own pass.

## How each one will be used

All four 3D packs above are authored the same way as the Castle, Nature and Furniture kits
already in use: a handful of models, flat per-material colours, no texture. They therefore
need no new loader — `gfx/AnimalLoader` splits them by material and `gfx/PropLoader` scales
them on the shared 1-unit grid.

1. **City Kit (Industrial)** — replaces the generated shed shell in `world/HangarMesh.kt`
   (walls, roof, door frame) and dresses the 6 m ramp. The plank deck stays generated until
   an industrial floor tile looks right.
2. **Fantasy Town Kit** — one building cluster per realm, replacing the Quaternius fantasy
   houses where they read wrong (desert, coast, alpine).
3. **Fantasy UI Borders / UI Pack** — the first step of replacing `ui/UiFrame` vector paths
   with sliced PNG frames; the layout hooks are already in place.
4. **Interface / Impact Sounds** — replaces the synthesised cues in `audio/AudioEngine.kt`.

## Already on disk — no download needed

These cost nothing and are the next wiring jobs, in this order:

| Source | Unused in the build | Fills |
|---|---|---|
| `assets-source/animals/` | the six remaining Quaternius animals — shipped in `assets/animals/` and wired per realm | §4 wildlife variety |
| Kenney Castle Kit | `gate`, `metal-gate`, `rocks-large`, `rocks-small`, `flag*`, `bridge-*`, `tree-log`, `wall-narrow-wood*`, hexagon towers, siege engines | §6 flight obstacles and §2 timber dressing |
| Kenney Nature Kit | `fence_*`, `crop_*`, `cliff_*`, `log`, `stump`, `tent`, `statue`, `mushroom`, `lily`, more trees | §5 field boundaries, §7 far-field trees |
| `assets-source/Planes /` | Airplane by Poly by Google (`PUSHILIN_Plane.obj`) | §1 a second airframe |
| `assets-source/ambientcg/` | `Rock030_1K-JPG.zip`, `Sign004_1K-JPG.zip`, `Rubber004_1K-JPG.zip` | already superseded by the colour maps shipped in `assets/terrain`; keep or delete |

## What is still missing after all of the above

- **Birds** — no pack in use has one. Poly Pizza has low-poly CC0 birds
  (<https://poly.pizza/search/bird>); the download there is a zip too, and the model would
  need a GLB/OBJ reader for its format. Until then the birds stay procedural triangles.
- **Skeletal animation** — no download fixes this; it needs a skinning pass in the renderer,
  independent of whichever animal pack is used.
- **Split airframe** — the `samolet` biplane is one welded mesh, so build stages cannot add
  or remove wing geometry. A model with separate wing/tail/propeller objects would fix it.
