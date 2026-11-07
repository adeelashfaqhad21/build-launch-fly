# Design rules (pending design.md)

**Status: placeholder.** The UI/UX layer is intentionally not implemented yet. When
`design.md` is supplied it becomes the authority for every screen in this game and the
values below are replaced by its decisions.

Rules that already apply regardless of the design document:

1. **Landscape only.** Target composition 16:9; all layouts must adapt to taller and wider
   aspect ratios without stretching (safe-area based anchoring, no fixed pixel positions).
2. **The 3D world is the interface.** HUD elements must never cover the aircraft or the
   central flight corridor.
3. **One design system.** Typography, colour, spacing, radii, elevation, iconography and
   motion come from a single token source (`gfx/DesignTokens.kt`), never from ad-hoc
   values in screens.
4. **No low-poly UI.** Panels, buttons and icons must be resolution-independent (vector or
   generated) and correctly scaled on every density bucket.
5. **Readability first.** Distance/speed HUD text must remain legible against bright snow,
   bright sand and dark forest simultaneously — the design must specify the scrim/outline
   strategy.

Where the design document leaves a value unspecified, prefer the world palette in
`world/WorldCatalog.kt` so that chrome and environment stay visually related.
