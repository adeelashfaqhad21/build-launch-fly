# Photo-derived visual design specification

**Source boundary:** This document records only what is visible in the six original application screenshots Ref-1.png–Ref-6.png. All are portrait images, 1284 × 2778 px (about 0.462 width/height). It does not import later gameplay requests or infer hidden mechanics.

## Evidence map
- **Ref-1:** loading/title illustration; toy-room diorama and polished biplane artwork.
- **Ref-2:** main menu in a timber hangar; launcher/aircraft scene and upgrade UI.
- **Ref-3:** rear launch view; aircraft, launcher, ramp and desert.
- **Ref-4:** closer rear launch/aiming view; striped elastics and coloured aiming arc.
- **Ref-5:** flight at 167 m; aircraft, chase camera, desert, HUD and PERFECT feedback.
- **Ref-6:** flight at 370 m; same flight presentation at another state, LOW SPEED warning and progress HUD.

Use direct observations as requirements. Treat perspective descriptions as visual interpretations, not measured engine settings. If the screenshots cannot establish a value or feature, leave it unspecified.

## 1. Composition and camera

### Portrait composition
- Match the tall portrait framing and vertical hierarchy of the 1284 × 2778 references.
- Interactive views place status/UI near the top and edges, the 3D world through the centre, and aircraft/launcher prominently in the lower-middle.
- Keep the forward world readable; do not allow HUD panels to cover the main flight path.
- For other aspect ratios, preserve this hierarchy and the relative size of the aircraft rather than stretching the composition.

### Flight camera (Ref-5 and Ref-6)
- Rear-follow camera, aligned close to the aircraft's longitudinal centreline.
- Camera is above and behind the aircraft, showing the upper wing, cockpit/fuselage and tail.
- View looks forward and downward toward the terrain; it is neither side-on, cockpit/first-person nor top-down.
- Aircraft sits in the lower-middle; a broad view of the route and terrain remains above it.
- Aircraft is a major foreground subject. Its wing spans approximately 67–75% of screen width in Ref-5 and 80–86% in Ref-6 (visual estimates).
- Horizon, vertical vegetation and side faces of terrain remain visible, so this is a perspective chase view rather than a map view.
- Keep nearby obstacles readable ahead of the aircraft.

No exact camera coordinates, height, distance, FOV or follow-smoothing can be measured from still images.

### Launch camera (Ref-3 and Ref-4)
- Centred behind the aircraft/launcher axis, slightly elevated, looking forward over the upper wing toward the ramp and desert.
- Ref-3 is the wider composition showing the complete aircraft and more surroundings.
- Ref-4 is closer, with wing, elastic bands and aiming arc more prominent.
- Launch ramp and distant landscape sit ahead on the centreline.

### Menu camera (Ref-2)
- Composed, slightly elevated three-quarter view inside a timber hangar.
- Large hangar opening frames bright outdoor desert and a horse.
- Aircraft and launcher occupy the lower-middle background.
- This is a menu scene, not the flight camera.

## 2. Playable aircraft (Ref-3–Ref-6)

The gameplay aircraft is a lightweight vintage open-frame flying machine:
- One broad main wing with shallow chord, mostly rectangular planform and softened/rounded outer ends.
- Wing surface is warm orange, honey or light ochre, divided into repeated rectangular panels by a darker orange-brown rib/grid pattern. This structural grid is visually distinctive; do not replace it with a smooth featureless wing.
- Thin dark bracing wires extend from the central structure to multiple points across both wing halves.
- Long, narrow, pale timber/tan fuselage or tail boom, visibly exposed rather than a large solid modern body.
- Repeated diagonal lattice/cross-bracing along the fuselage.
- Small dark open cockpit/seat recess near the forward central wing/root area; no enclosed modern canopy is visible.
- Smaller orange panelled horizontal tailplane at the rear, left and right surfaces, with a thin rear extension.
- Small dark fittings and cable attachment points around wing root and cockpit.
- Lightweight handmade/early-aircraft character; thin wing relative to its span.

**Not visible on this gameplay aircraft:** a clearly identifiable propeller, jet engine, enclosed canopy or conventional landing wheels. Do not add those to this reference model based on Ref-1.

### Separate loading-art aircraft (Ref-1)
Ref-1 depicts a different, more elaborate biplane illustration:
- Rounded warm wooden fuselage and prominent circular nose/cowling.
- Large green propeller with motion blur.
- Two stacked wings with green/dark camouflage-like patches.
- Inter-wing struts and dark landing wheels.
- Dramatic close three-quarter angle, diagonal across the frame.
- Glossy warm highlights and polished toy-like presentation.

Keep the distinction clear: Ref-1 is loading/title art; Ref-3–Ref-6 show the playable open-frame aircraft.

## 3. Slingshot / launch apparatus (Ref-2–Ref-4)
- A substantial physical launcher integrated into the launch lane, not a handheld miniature.
- Ref-3/4 show two widely spaced upright brown/timber-looking posts, one on each side.
- Two thin but visually prominent elastic bands run from the side posts toward the aircraft's central/wing-root area, converging into a broad V behind the aircraft.
- Bands have alternating diagonal yellow-and-white stripes.
- Aircraft is aligned on the launcher centreline.
- A broad dark-brown ramp/runway-like board extends directly ahead, wide at the near end and narrowing in perspective toward the far end.
- Ref-4 has a segmented semicircular aiming/power arc across the near ramp: red/orange at outer ends, yellow intermediate segments and green near the centre.
- Ref-3 shows the launch scene without the clearly visible coloured arc.
- Ref-2 shows a larger dark central support/tower-like launcher assembly with braces, partly obscured by aircraft and UI.

Screenshots do not establish exact internal mechanics, material shader, force, elastic tension formula, angle range or control gesture.

## 4. Desert world (Ref-3–Ref-6)

### Terrain and scale
- Open, broad arid landscape in warm beige, tan, ochre and sandy brown.
- Rolling sandy slopes, low rises, shallow depressions, basins and broad valleys.
- Angular/faceted rocky outcrops and hills; Ref-6 has a prominent large rocky slope/outcrop on the right, with open basin to the left.
- Ref-3/4 show sloping sandy banks framing the launch lane.
- Terrain is defined by large readable shapes and tonal planes, not dense micro-noise.
- Large areas of open sand remain visible between plants.
- Near terrain is stronger and more defined; distant terrain is paler, lower contrast and simpler.

### Vegetation
Sparse, separated desert vegetation:
- Tall branching cacti with one or more upright arms.
- Shorter rounded/clustered cactus forms.
- Leafless trees with thin grey-brown trunks and angular bare branches.
- A few small low plants/shrub silhouettes.
- Vegetation appears in scattered groups, not as continuous ground cover or repeated cloned rows.
- Near vegetation is larger and clearly shaped; distant vegetation is smaller, paler and simplified.
- Ref-3/4 show cacti on ridges and bare trees toward the left/centre; Ref-5/6 show cacti and bare trees at several distances, including a cactus cluster near the right rocky slope in Ref-6.

### Animals
- Small quadruped silhouettes occur in the desert at multiple distances.
- Ref-3/4 show two silhouettes on the right ridge; Ref-5 shows several small animals; Ref-6 shows distant and foreground animal silhouettes.
- Ref-2 shows a horse outside the hangar.
- The images are too small to establish exact species; do not assign species names from these references.

### Sky, atmosphere and light
- Bright clear cyan/light-blue sky; soft white clouds are visible in Ref-5.
- Distant land and vegetation soften into pale blue-grey atmospheric haze.
- Foreground remains clear and saturated; haze is a depth cue, not an opaque screen-filling fog.
- Bright daylight with warm illumination and soft directional shadows.
- Aircraft shadow is visible on the ground in Ref-5; vegetation and terrain also cast readable soft shadows.

## 5. Rendering style
Gameplay (Ref-3–Ref-6) uses stylized low-poly 3D:
- Large simple polygonal terrain forms and faceted rock planes.
- Simplified, recognizable vegetation silhouettes.
- Matte-looking sand with restrained surface texture.
- Broad tonal variation, soft shading and directional shadows.
- Warm earth palette contrasted with blue sky and cactus greens.
- Distant geometry progressively loses contrast and detail through atmospheric perspective.
- Readability and clear silhouettes matter more than photorealistic micro-detail.

Do not turn gameplay into photorealistic clutter, a uniform green carpet, repeated shrub clones or a fog-filled scene. The references show sparse plants and large open sand.

Ref-1 is deliberately different: glossy toy-room diorama, dramatic depth of field, warm indoor sunlight and polished reflections. Ref-2 is a stylized hangar/menu scene with bright graphic UI. Do not apply Ref-1's toy-room rendering treatment to the actual desert gameplay.

### Relative object scale
- Aircraft dominates the foreground.
- Nearby plants and rocks are large enough to recognize immediately.
- Mid-distance objects remain identifiable but smaller.
- Distant forms are reduced, paler and simpler.
- The world is not uniformly filled with objects; empty sand is an important part of the composition.
- Keep the forward route visually legible.

The screenshots cannot establish world units, polygon budgets, texture resolution, LOD thresholds or exact view distance.

## 6. Flight HUD (Ref-5 and Ref-6)
HUD is compact, edge-aligned and leaves the centre view open.

### Speed gauge
- Large circular gauge at upper-left.
- Layered metallic silver/chrome rim.
- Blue/cyan inner dial, tick marks around upper arc and dark needle.
- Large numeric readout with KM/H below/alongside.
- Ref-5 reads 94 KM/H.
- Ref-6 reads 37 KM/H and shows a red LOW SPEED warning below the gauge.

### Pause
- White two-bar pause glyph inside a pale translucent rounded square, below the speed gauge on the left.

### Distance
- Bold white distance value with dark shadow/outline, near horizontal centre and above the aircraft.
- Ref-5: 167M.
- Ref-6: 370M.

### Progress
- Narrow vertical rounded track near right edge.
- Pale/blue translucent fill and light outline.
- Black-and-white checkered finish marker at top.
- Yellow horizontal progress marker and yellow arrow pointing from the outer right.
- Small white aircraft marker on the track with percentage below.
- Ref-5: 4%; Ref-6: 9%.

### Feedback
- Ref-5: large playful white PERFECT! lettering with bright blue outline and darker shadow across upper-middle; bright green streaks near lateral edges and pale vertical speed trails.
- Ref-6: LOW SPEED warning is visible; the PERFECT overlay and green streaks are absent.
- Treat these as state-specific effects, not permanent scenery.

## 7. Main menu (Ref-2)
- Timber/wooden hangar interior and large opening to bright desert exterior; horse visible outside.
- Aircraft and launcher in lower-middle background.
- Top-left coin and gem balances in rounded capsules, each with green plus control.
- Blue circular settings/gear button at top-right.
- Brown/gold VINTAGE PLANE plaque with small aircraft illustration.
- Left utility stack: shop icon, crossed-out ADS icon, blue gem/reward tile with video-ticket symbol and 3/3 counter.
- Right utility stack: blue/silver chest with green OPEN button; gold VIP badge; rocket icon with purple UNLOCK button and gem cost.
- Large white rounded TAP TO FLY prompt centred over the scene.
- Three bottom cards side-by-side: SLINGSHOT (orange/yellow), PLANE (blue), INCOME (purple).
- Each card has title strip, level number, large illustrative icon, five small progression pips/slots and pale lavender price button with coin icon.
- Visible example values: Slingshot level 41 / 69.55K; Plane level 58 / 421.04K; Income level 18 / x38.4 / 98.52K. These are screenshot values, not fixed constants.

UI language: rounded corners, thick outlines, saturated orange/blue/purple/green/gold, large playful rounded lettering, white text with dark outline/shadow, glossy/bevelled icons and strong separation from subdued background.

## 8. Loading/title artwork (Ref-1)
- Full-screen portrait illustration.
- Voodoo wordmark above large PLANE EVOLUTION title.
- PLANE lettering metallic silver/blue; EVOLUTION bright yellow/gold with deep blue extrusion/backing and shadow.
- Small four-point diamond/star emblem below title.
- Close three-quarter wooden biplane with green spinning propeller, camouflage wings, struts and wheels.
- Bright child's playroom/toy-room diorama: red balloon, colourful bunting, toy train, blocks, small cars, ribbons, gift boxes and other colourful floor objects.
- Warm sunlight from right, glossy highlights and soft depth of field.
- Circular two-arrow loading symbol near bottom and “Loading level: 99%” text.

This is title/loading art, not a gameplay-world reference.

## 9. Photo-match acceptance checklist
- [ ] Portrait layout preserves the references' vertical hierarchy.
- [ ] Flight camera follows from behind and above, centred and looking forward/down.
- [ ] Aircraft remains prominent; wing spans about 67–86% of width in flight examples.
- [ ] Gameplay plane has orange/tan rectangular wing grid, thin bracing wires, exposed narrow lattice fuselage, open cockpit and smaller rear tailplane.
- [ ] Gameplay plane is not replaced by the loading-art biplane or a modern aircraft.
- [ ] Launcher has two side posts, yellow/white striped V-shaped elastics, centred aircraft and dark forward ramp.
- [ ] Ref-4 launch aiming arc has red/orange outer, yellow intermediate and green centre segments.
- [ ] Desert is warm, open and sparse, with cacti, bare trees, angular rocky slopes and small animal silhouettes.
- [ ] Nearby objects are clear; distant objects are smaller, paler and simpler.
- [ ] Blue sky and subtle distant haze; foreground is never obscured by dense fog.
- [ ] Low-poly faceted rendering and soft shadows; no photorealistic clutter or uniform vegetation carpet.
- [ ] HUD reproduces speed dial, pause control, distance, vertical progress track and state feedback.
- [ ] Menu reproduces hangar, balances, utility buttons, TAP TO FLY and three upgrade cards.
- [ ] Loading artwork remains visually distinct from gameplay.

## 10. Explicitly not established by these photos
No exact camera coordinates/FOV; aircraft dimensions or world-unit scale; aerodynamics or upgrade effects; launch force or angle range; control gesture; aircraft stage count; playable-aircraft propeller (not visible); other biomes; exact animal species; polygon/texture budgets, LOD distances or rendering API; frame-rate targets; or gameplay features such as rings, missions, collectibles, multiplayer and checkpoints. Do not invent these as photo-derived requirements.
