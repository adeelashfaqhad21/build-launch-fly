# World rendering audit — 2026-11-09

Branch: `working` (baseline cloned from the latest `main`)

## Confirmed findings and changes

| Area | Finding | Action |
|---|---|---|
| Glacier Reach | Surface C is rock, but the snow realm previously used a negative snow-line threshold, causing the high-altitude layer to cover nearly all terrain. | Raised the snow-line threshold so the snow base and rock/cliff slope layer remain visible. |
| CI cadence | Pushes to `working` were not covered by the Android workflow. | Added `working` to the push branch filter. |
| Imported vegetation coverage | Imported Quaternius species were scattered in a 28–328 m circular ring around the launch pad. Since the aircraft initially travels along -Z, most instances could land outside the opening flight view. | Replaced the radial ring with a 210 m-wide forward corridor spanning the first 36–396 m of flight; retained the launch-pad exclusion and terrain / water / slope checks. Imported grass counts were also raised in forest, alpine and coastal realms. |
| Flight camera framing | The implementation trailed the aircraft by 15.5 m and sat 5.8 m above its velocity vector, while DESIGN.md specifies a low rear chase only 3–4 m behind, looking slightly down. | Brought the chase distance to 4.0 m, camera height offset to 1.7 m, and target lead to 2.4 m. Device verification remains pending. |
| Flight lift direction | Lift was applied along the fuselage's up axis. At positive angle of attack that vector contains a component opposite the flight direction, so pulling up creates an artificial loss of airspeed before the aircraft can climb. | Projected the banked wing-up vector perpendicular to the actual velocity before applying lift; also made the stall indicator report excessive angle of attack as well as low airspeed. |
| Launch rig visibility and scale | Elastic segments were generated and shadowed but omitted from the visible render pass. The posts were 4.85 m high and slightly inside the aircraft's span. | Rebuilt the elastic as flat striped webbing, lowered posts to 2.65 m, moved them outside the wing tips, and render the visible bands over the airframe. |
| Launch ramp orientation and proportions | The old ramp was a 1.5 m-wide, 3.2 m-long kicker, too small to match the broad forward launch deck in Ref-3. | Rebuilt the deck as a centered tapered wedge, 8.4 m wide at the aircraft and 18 m long along -Z, with a dark surface, side aprons, longitudinal bearers and low edge guides. |

## Remaining audit work

- Rebalance procedural fallback trees, shrubs and grass against the imported realm-specific species after visual inspection.
- Verify grass visibility, imported vegetation silhouettes / density and low-chase camera framing at the actual gameplay camera height; confirm the aircraft occupies about one-third of the portrait screen as specified.
- Verify the reconstructed hangar / ramp / slingshot proportions and aircraft ground contact against Ref-2 and Ref-3; check that the ramp is directly ahead, the posts are planted, and both straps meet the aircraft at wing-root height.
- Verify flight response on device: a sustained nose-up input should convert airspeed into a climb rather than bleed speed through a false rearward lift component; confirm the expected stall behaviour at low speed.
- Inspect shadow-pass ordering and ensure every visible world prop receives the intended shadow treatment.

## Verification policy

The blue-ground issue has been reported fixed in the latest installed main build; do not spend further changes on blue terrain or fog. Do not run builds for individual edits. Run one Android debug/release build after the grouped flight-physics and launch-assembly reconstruction is complete.
