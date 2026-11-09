# Aircraft and Glacier Asset Library

Original supplied OBJ/MTL specification is preserved in this directory.

## World aircraft mapping
- Greenwood Forest: Skyhopper (civilian light aircraft)
- Amber Dunes: VintageBiplane
- Glacier Reach: FrostFreighter (heavy transport)
- Sapphire Coast: AuroraScout (VTOL-style scout)
- Highland Valley: ViperStrike (fighter)

Runtime-ready combined meshes are under `app/src/main/assets/aircraft/worlds/`.
The four glacier flora meshes and their shared material library are under
`app/src/main/assets/glacier/`.

Aircraft meshes are assembled from the five modular subassemblies in the source
document and converted from +Z-forward to the game's -Z-forward axis. The glacier
plant meshes retain the source document's ground-origin pivots.
