# BRIEF — Build • Launch • Fly

Working brief derived from the authoritative reference plates (`Ref-1` … `Ref-6`,
inspected; `Ref-7` recording committed but not inspectable in the browser). Where this
brief and the references disagree, the references win; where the references are silent,
this brief and `docs/DESIGN.md` decide.

This document replaces the earlier ambition of a photoreal PUBG-class look. The references
show a **stylised, low-poly 3D game with a bright hypercasual UI**, and that is now the
target. Our own identity (name, logo, art, copy, numbers) stays original; the reference
game's branding and artwork are not copied.

---

## 1. What the game is

A portrait mobile game: the player owns a vintage biplane, upgrades it, pulls it back on a
slingshot and launches it across a desert valley for distance. Coins from each flight buy
slingshot power, plane parts and income upgrades.

## 2. The reference loop (observed)

```
Splash / loading (progress %)
        ↓
Hangar home ("TAP TO FLY", currency bars, upgrade cards, promotions)
        ↓
Launch setup (plane in the slingshot, BACK button)
        ↓
Aim / tension state (curved green-yellow-red launch gauge)
        ↓
Release → in-flight HUD (speedometer, distance, vertical progress track,
        PERFECT! on a good launch, LOW SPEED warning when slow)
        ↓
Results (not shown in references; derived) → coins
        ↓
Hangar → spend coins on SLINGSHOT / PLANE / INCOME upgrades → fly again
```

Observed numbers across two flight screenshots: **94 km/h at 167 m, 4 % progress** (good)
versus **37 km/h at 370 m, 9 % progress with LOW SPEED** — distance grows while speed
decays, and the vertical track measures progress toward a target distance.

## 3. Screen inventory

Shown in references: splash, hangar home, launch setup, aim state, flight HUD (good),
flight HUD (low speed).
Derived (required by the product, not shown in refs): results, settings, achievements /
career, shop, rewards.

## 4. Visual target (from the plates)

- **3D:** low-poly, flat-shaded pastel/desert palette; simple terrain with cacti, bare trees,
  ridges; big cyan sky with light haze; soft shadows; no PBR gloss.
- **UI:** white rounded cards with thick light borders and drop shadows; saturated headers
  (orange / cyan / purple); chunky uppercase lettering with dark outlines; glossy circular
  instruments; gold coin and blue gem pills with green “+” buttons.
- **Palette anchors:** sky `#40B9E8`, sand `#C5A078`, card `#FFF9EF`, blue `#1975D1`,
  outline navy `#185AB7`, green `#35D52D`, yellow `#FFD52B`, gold `#FFB51F`, red `#F33135`,
  purple `#9A4BEA`, lavender `#E7B5F3`, cyan card `#19A9ED`, orange card `#FF9D12`,
  gem `#22B7F0`, plaque tan `#A58B70`, plaque text `#583B1E`.
- **Orientation:** portrait (references are 1284×2778). Supersedes the earlier landscape
  requirement.

## 5. Components to build

Splash · Hangar home · Currency bars · Upgrade cards (3) · Promotions (store, remove ads,
gems, chest, VIP, rocket unlock) · Slingshot rig · Launch setup · Aim gauge · In-flight HUD
(speedometer, distance, progress track, feedback, warnings) · Results · Aircraft and build
stages · Environments · Audio · Camera · Settings · Achievements / career · Save & economy.

Each one has a full catalog — colours, layout, surfaces, assets, states, motion, data and an
implementation checklist — in [`DESIGN.md`](DESIGN.md) §3.

## 6. Economy

- Coins (`28.15K` style, compact K/M formatting) buy everything.
- Gems are the premium currency (`0` in the refs, offered by rewarded video `3/3`).
- Upgrade cards show level, five progress pips, price; `INCOME` multiplies earnings
  (`x38.4` in the refs).
- Chest, VIP, rocket-unlock and remove-ads are promotion slots. They must be modular: the
  UI can ship with them disabled while the economy is validated.

## 7. Technical constraints

- Native Android, Kotlin, OpenGL ES 3.0 renderer already in place.
- Portrait, 360 dp design width, height adaptive.
- All art generated in code or shipped as small OFL/local assets; no external CDN.
- CI builds an installable APK on every push; QA-signed release is the tester package.

## 8. Non-negotiables

1. Portrait.
2. Reference palette and card language above written style opinions.
3. The hangar must read at a glance: currencies, plane, three upgrade cards, TAP TO FLY.
4. The flight HUD carries exactly: speed dial, pause, distance, progress track, and
   situational feedback (PERFECT / LOW SPEED / stall).
5. Every component below has a checklist; nothing is "done" until its checklist is ticked
   and the build is green.

## 9. Honest status notes

- `Ref-7.mp4` (39.2 MB) is committed but the browser could not open it (20 MB CDN limit), so
  the recording's contents are **not** described anywhere in this repo. Anything that
  depends on it must be re-captured or described.
- The reference game's splash (Ref-1) is far more detailed than its gameplay; we will build
  our own splash at gameplay fidelity plus a logo treatment, not attempt that illustration.
