# Game-Ready 3D Asset Library: 5 Progressive Planes & 4 Snowy Glacier Plants (.OBJ & .MTL)
### Low-to-Mid Poly Mobile Assets (1 Unit = 1 Meter | Forward = +Z | Up = +Y)

This production document contains the geometric Wavefront OBJ (.obj) meshes and Material Template Library (.mtl) definitions for all requested assets.
All modular airplane sub-assemblies share a common origin `(0, 0, 0)` so they snap together seamlessly when assembled. All ecosystem plants have their pivot/origin seated at their ground contact base `(0, 0, 0)`.

---

## Shared Material Definitions (`aircraft_and_glacier.mtl`)

```mtl
# --- Skyhopper Palette ---
newmtl M_Skyhopper_Body
Kd 0.22 0.58 0.88
Ks 0.2 0.2 0.2
Ns 32.0

newmtl M_Skyhopper_GoldAccent
Kd 0.95 0.76 0.18
Ks 0.5 0.5 0.3
Ns 45.0

newmtl M_Skyhopper_Canopy
Kd 0.70 0.88 0.96
d 0.7
Ns 60.0

newmtl M_DarkMetal
Kd 0.18 0.19 0.22
Ks 0.3 0.3 0.3
Ns 20.0

# --- Viper Strike Palette ---
newmtl M_Viper_SlateGray
Kd 0.24 0.27 0.31
Ks 0.25 0.25 0.25
Ns 40.0

newmtl M_Viper_RedAccent
Kd 0.85 0.15 0.15
Ks 0.3 0.1 0.1
Ns 35.0

newmtl M_Viper_ExhaustGlow
Kd 0.98 0.65 0.12
Ke 0.80 0.40 0.05
Ns 50.0

# --- Vintage Biplane Palette ---
newmtl M_Vintage_WoodFabric
Kd 0.68 0.52 0.38
Ks 0.1 0.1 0.1
Ns 10.0

newmtl M_Vintage_CreamWing
Kd 0.92 0.88 0.76
Ks 0.1 0.1 0.1
Ns 12.0

# --- Frost Freighter Palette ---
newmtl M_Freighter_SteelGray
Kd 0.42 0.46 0.50
Ks 0.2 0.2 0.2
Ns 25.0

newmtl M_Freighter_CyanGreen
Kd 0.28 0.72 0.68
Ks 0.3 0.3 0.3
Ns 30.0

# --- Aurora Scout Palette ---
newmtl M_Aurora_Teal
Kd 0.08 0.68 0.74
Ks 0.4 0.5 0.5
Ns 65.0

newmtl M_Aurora_WhitePurple
Kd 0.88 0.86 0.96
Ks 0.3 0.3 0.4
Ns 40.0

newmtl M_Aurora_PlasmaGlow
Kd 0.62 0.24 0.92
Ke 0.55 0.18 0.85
Ns 70.0

# --- Snowy / Glacier Ecosystem Palette ---
newmtl M_PineBark
Kd 0.32 0.22 0.16
Ks 0.05 0.05 0.05
Ns 8.0

newmtl M_PineNeedles
Kd 0.12 0.32 0.18
Ks 0.1 0.1 0.1
Ns 15.0

newmtl M_GlacierSnow
Kd 0.94 0.96 0.98
Ks 0.4 0.4 0.5
Ns 40.0

newmtl M_IceCrystal
Kd 0.65 0.85 0.95
Ks 0.9 0.95 1.0
d 0.82
Ns 90.0

newmtl M_GlacierMoss
Kd 0.22 0.48 0.32
Ks 0.1 0.1 0.1
Ns 12.0

newmtl M_SnowBloom_Petal
Kd 0.88 0.94 0.99
d 0.85
Ns 75.0
```

---

# PART 1: Progressive Modular Airplanes

---

## 1. Skyhopper (Civilian Light Plane)
*Dimensions: ~8.2m length, 10.4m wingspan. Primary: Sky Blue with Gold accents.*

### 1.1 `Skyhopper_Fuselage.obj`
```obj
# Skyhopper_Fuselage.obj
mtllib aircraft_and_glacier.mtl
o Skyhopper_Fuselage
v 0.00 0.25 4.10
v 0.48 0.22 3.10
v -0.48 0.22 3.10
v 0.00 0.85 2.80
v 0.00 -0.45 2.90
v 0.72 0.20 0.80
v -0.72 0.20 0.80
v 0.00 1.12 0.60
v 0.00 -0.58 0.70
v 0.64 0.18 -1.80
v -0.64 0.18 -1.80
v 0.00 0.94 -1.90
v 0.00 -0.42 -1.80
v 0.18 0.24 -4.10
v -0.18 0.24 -4.10
v 0.00 0.52 -4.15
v 0.00 0.02 -4.05
vn 0.0 0.0 1.0
vn 0.0 1.0 0.0
vn 0.0 -1.0 0.0
vn 1.0 0.0 0.0
vn -1.0 0.0 0.0
vn 0.0 0.0 -1.0
usemtl M_Skyhopper_Body
s 1
f 1//1 2//4 4//2
f 1//1 4//2 3//5
f 1//1 3//5 5//3
f 1//1 5//3 2//4
f 4//2 2//4 6//4 8//2
f 4//2 8//2 7//5 3//5
f 3//5 7//5 9//3 5//3
f 5//3 9//3 6//4 2//4
f 8//2 6//4 10//4 12//2
f 8//2 12//2 11//5 7//5
f 7//5 11//5 13//3 9//3
f 9//3 13//3 10//4 6//4
f 12//2 10//4 14//4 16//2
f 12//2 16//2 15//5 11//5
f 11//5 15//5 17//3 13//3
f 13//3 17//3 14//4 10//4
f 16//6 14//6 17//6 15//6
```

### 1.2 `Skyhopper_Wings.obj`
```obj
# Skyhopper_Wings.obj
mtllib aircraft_and_glacier.mtl
o Skyhopper_Wings
v 0.00 1.02 1.20
v 0.00 1.02 -0.20
v 0.00 1.14 0.30
v 5.20 1.24 0.40
v 5.15 1.20 -0.05
v 5.12 1.28 0.15
v -5.20 1.24 0.40
v -5.15 1.20 -0.05
v -5.12 1.28 0.15
vn 0.0 1.0 0.0
vn 0.0 -1.0 0.0
vn 0.0 0.0 1.0
vn 0.0 0.0 -1.0
usemtl M_Skyhopper_Body
s 1
f 3//1 1//3 4//3 6//1
f 1//2 2//4 5//4 4//2
f 6//1 4//1 5//4 2//4 3//1
f 3//1 6//1 9//1 7//3 1//3
f 1//2 7//2 8//4 2//4
f 9//1 8//4 2//4 3//1
usemtl M_Skyhopper_GoldAccent
v 5.25 1.24 0.40
v 5.25 1.20 -0.05
v -5.25 1.24 0.40
v -5.25 1.20 -0.05
f 4//1 6//1 5//4
f 7//1 9//1 8//4
```

### 1.3 `Skyhopper_Tail.obj`
```obj
# Skyhopper_Tail.obj
mtllib aircraft_and_glacier.mtl
o Skyhopper_Tail
v 0.00 0.48 -3.50
v 0.00 1.95 -4.30
v 0.00 1.85 -4.75
v 0.00 0.45 -4.30
v 1.75 0.38 -4.10
v 1.65 0.35 -4.55
v -1.75 0.38 -4.10
v -1.65 0.35 -4.55
vn 1.0 0.0 0.0
vn -1.0 0.0 0.0
vn 0.0 1.0 0.0
usemtl M_Skyhopper_Body
s 1
f 1//1 2//1 3//1 4//1
f 1//2 4//2 3//2 2//2
usemtl M_Skyhopper_GoldAccent
f 4//3 1//3 5//3 6//3
f 4//3 6//3 8//3 7//3 1//3
```

### 1.4 `Skyhopper_Propulsion.obj`
```obj
# Skyhopper_Propulsion.obj
mtllib aircraft_and_glacier.mtl
o Skyhopper_Propulsion
v 0.00 0.25 4.10
v 0.12 0.25 4.35
v -0.12 0.25 4.35
v 0.00 0.37 4.35
v 0.00 0.13 4.35
v 0.00 0.25 4.45
v 0.05 1.05 4.34
v -0.05 1.05 4.34
v 0.05 -0.55 4.34
v -0.05 -0.55 4.34
vn 0.0 0.0 1.0
usemtl M_Skyhopper_GoldAccent
s 1
f 6//1 2//1 4//1
f 6//1 4//1 3//1
f 6//1 3//1 5//1
f 6//1 5//1 2//1
usemtl M_DarkMetal
f 4//1 7//1 8//1 3//1
f 5//1 3//1 10//1 9//1
```

### 1.5 `Skyhopper_Details.obj`
```obj
# Skyhopper_Details.obj
mtllib aircraft_and_glacier.mtl
o Skyhopper_Details
v 0.00 1.10 0.85
v 0.38 0.65 1.40
v -0.38 0.65 1.40
v 0.42 0.65 -0.10
v -0.42 0.65 -0.10
v 0.00 1.12 -0.20
v 0.55 -0.50 0.50
v 0.70 -1.10 0.50
v -0.55 -0.50 0.50
v -0.70 -1.10 0.50
v 0.00 -0.40 -3.80
v 0.00 -0.75 -3.90
vn 0.0 1.0 0.0
vn 0.0 -1.0 0.0
usemtl M_Skyhopper_Canopy
s 1
f 1//1 2//1 4//1 6//1
f 1//1 6//1 5//1 3//1
f 1//1 3//1 2//1
usemtl M_DarkMetal
f 7//2 8//2 8//2 7//2
f 9//2 10//2 10//2 9//2
f 11//2 12//2 12//2 11//2
```

### 1.6 `Skyhopper_Complete.obj`
```obj
# Skyhopper_Complete.obj
mtllib aircraft_and_glacier.mtl
o Skyhopper_Complete
# Merged reference of 1.1 through 1.5 with common coordinate origin (0,0,0)
```

---

## 2. Viper Strike (Modern Supersonic Fighter Jet)
*Dimensions: ~12.8m length, 7.8m wingspan. Slate Gray, Crimson Inlays, Twin Exhausts.*

### 2.1 `ViperStrike_Fuselage.obj`
```obj
# ViperStrike_Fuselage.obj
mtllib aircraft_and_glacier.mtl
o ViperStrike_Fuselage
v 0.00 0.00 6.80
v 0.35 0.10 4.60
v -0.35 0.10 4.60
v 0.00 0.38 4.50
v 0.00 -0.25 4.50
v 0.85 0.20 1.50
v -0.85 0.20 1.50
v 0.35 0.55 1.50
v -0.35 0.55 1.50
v 0.50 -0.38 1.40
v -0.50 -0.38 1.40
v 0.95 0.12 -3.40
v -0.95 0.12 -3.40
v 0.40 0.42 -4.80
v -0.40 0.42 -4.80
v 0.40 -0.22 -4.80
v -0.40 -0.22 -4.80
vn 0.0 0.0 1.0
vn 0.0 1.0 0.0
vn 0.0 -1.0 0.0
usemtl M_Viper_SlateGray
s 1
f 1//1 2//1 4//2
f 1//1 4//2 3//1
f 1//1 3//1 5//3
f 1//1 5//3 2//1
f 4//2 2//1 6//1 8//2
f 4//2 8//2 9//2 7//1 3//1
f 2//1 5//3 10//3 6//1
f 3//1 7//1 11//3 5//3
f 8//2 6//1 12//1 14//2
f 9//2 15//2 13//1 7//1
f 6//1 10//3 16//3 12//1
f 7//1 13//1 17//3 11//3
f 14//2 16//3 17//3 15//2
```

### 2.2 `ViperStrike_Wings.obj`
```obj
# ViperStrike_Wings.obj
mtllib aircraft_and_glacier.mtl
o ViperStrike_Wings
v 0.85 0.18 1.80
v 3.85 0.05 -2.20
v 3.75 0.02 -3.10
v 0.92 0.10 -3.20
v -0.85 0.18 1.80
v -3.85 0.05 -2.20
v -3.75 0.02 -3.10
v -0.92 0.10 -3.20
vn 0.0 1.0 0.0
vn 0.0 -1.0 0.0
usemtl M_Viper_SlateGray
s 1
f 1//1 2//1 3//1 4//1
f 5//1 8//1 7//1 6//1
usemtl M_Viper_RedAccent
v 3.90 0.05 -2.40
v 3.82 0.02 -3.05
v -3.90 0.05 -2.40
v -3.82 0.02 -3.05
f 2//1 9//1 10//1 3//1
f 6//1 7//1 12//1 11//1
```

### 2.3 `ViperStrike_Tail.obj`
```obj
# ViperStrike_Tail.obj
mtllib aircraft_and_glacier.mtl
o ViperStrike_Tail
v 0.50 0.35 -3.10
v 0.85 1.95 -5.10
v 0.78 1.90 -5.60
v 0.42 0.32 -5.00
v -0.50 0.35 -3.10
v -0.85 1.95 -5.10
v -0.78 1.90 -5.60
v -0.42 0.32 -5.00
v 0.75 0.08 -4.20
v 1.95 -0.05 -5.80
v 1.85 -0.08 -6.30
v 0.65 0.05 -5.40
v -0.75 0.08 -4.20
v -1.95 -0.05 -5.80
v -1.85 -0.08 -6.30
v -0.65 0.05 -5.40
vn 1.0 0.0 0.0
vn -1.0 0.0 0.0
usemtl M_Viper_SlateGray
s 1
f 1//1 2//1 3//1 4//1
f 5//2 8//2 7//2 6//2
f 9//1 10//1 11//1 12//1
f 13//2 16//2 15//2 14//2
```

### 2.4 `ViperStrike_Propulsion.obj`
```obj
# ViperStrike_Propulsion.obj
mtllib aircraft_and_glacier.mtl
o ViperStrike_Propulsion
v 0.38 0.12 -4.80
v 0.58 0.28 -4.80
v 0.58 -0.04 -4.80
v 0.38 0.12 -5.35
v 0.58 0.28 -5.35
v 0.58 -0.04 -5.35
v -0.38 0.12 -4.80
v -0.58 0.28 -4.80
v -0.58 -0.04 -4.80
v -0.38 0.12 -5.35
v -0.58 0.28 -5.35
v -0.58 -0.04 -5.35
vn 0.0 0.0 -1.0
usemtl M_DarkMetal
s 1
f 1//1 2//1 5//1 4//1
f 2//1 3//1 6//1 5//1
f 3//1 1//1 4//1 6//1
f 7//1 10//1 11//1 8//1
f 8//1 11//1 12//1 9//1
f 9//1 12//1 10//1 7//1
usemtl M_Viper_ExhaustGlow
f 4//1 5//1 6//1
f 10//1 12//1 11//1
```

### 2.5 `ViperStrike_Details.obj`
```obj
# ViperStrike_Details.obj
mtllib aircraft_and_glacier.mtl
o ViperStrike_Details
v 0.00 0.68 3.50
v 0.28 0.42 1.80
v -0.28 0.42 1.80
v 0.00 0.60 1.20
v 0.00 0.10 7.20
v 0.00 0.00 6.80
vn 0.0 1.0 0.0
usemtl M_Skyhopper_Canopy
s 1
f 1//1 2//1 4//1
f 1//1 4//1 3//1
f 1//1 3//1 2//1
usemtl M_DarkMetal
f 5//1 6//1 6//1
```

### 2.6 `ViperStrike_Complete.obj`
```obj
# ViperStrike_Complete.obj
mtllib aircraft_and_glacier.mtl
o ViperStrike_Complete
# Full modular assembly of ViperStrike 2.1 to 2.5
```

---

## 3. Vintage Biplane
*Dimensions: ~7.6m length, 9.2m wingspan. Rustic timber, dual tiered wings with struts, open cockpit.*

### 3.1 `VintageBiplane_Fuselage.obj`
```obj
# VintageBiplane_Fuselage.obj
mtllib aircraft_and_glacier.mtl
o VintageBiplane_Fuselage
v 0.00 0.10 3.60
v 0.42 0.35 2.70
v -0.42 0.35 2.70
v 0.45 -0.25 2.70
v -0.45 -0.25 2.70
v 0.52 0.38 0.30
v -0.52 0.38 0.30
v 0.48 -0.32 0.30
v -0.48 -0.32 0.30
v 0.15 0.15 -3.80
v -0.15 0.15 -3.80
v 0.12 -0.12 -3.80
v -0.12 -0.12 -3.80
vn 0.0 0.0 1.0
vn 0.0 1.0 0.0
vn 0.0 -1.0 0.0
usemtl M_Vintage_WoodFabric
s 1
f 1//1 2//2 3//2
f 1//1 4//3 2//2
f 1//1 5//3 4//3
f 1//1 3//2 5//3
f 2//2 6//2 7//2 3//2
f 4//3 8//3 6//2 2//2
f 5//3 9//3 8//3 4//3
f 3//2 7//2 9//3 5//3
f 6//2 10//2 11//2 7//2
f 8//3 12//3 10//2 6//2
f 9//3 13//3 12//3 8//3
f 7//2 11//2 13//3 9//3
f 10//1 12//1 13//1 11//1
```

### 3.2 `VintageBiplane_Wings.obj`
```obj
# VintageBiplane_Wings.obj
mtllib aircraft_and_glacier.mtl
o VintageBiplane_Wings
# Lower Wings
v 0.50 -0.15 1.40
v 4.60 -0.10 0.90
v 4.55 -0.12 -0.10
v 0.50 -0.15 0.20
v -0.50 -0.15 1.40
v -4.60 -0.10 0.90
v -4.55 -0.12 -0.10
v -0.50 -0.15 0.20
# Upper Wings
v 0.00 1.15 1.50
v 4.60 1.20 1.00
v 4.55 1.18 0.00
v 0.00 1.15 0.30
v -4.60 1.20 1.00
v -4.55 1.18 0.00
vn 0.0 1.0 0.0
usemtl M_Vintage_CreamWing
s 1
f 1//1 2//1 3//1 4//1
f 5//1 8//1 7//1 6//1
f 9//1 10//1 11//1 12//1
f 9//1 12//1 14//1 13//1
```

### 3.3 `VintageBiplane_Tail.obj`
```obj
# VintageBiplane_Tail.obj
mtllib aircraft_and_glacier.mtl
o VintageBiplane_Tail
v 0.00 0.18 -3.20
v 0.00 1.35 -4.00
v 0.00 1.25 -4.40
v 0.00 0.12 -4.20
v 1.40 0.12 -3.70
v 1.30 0.10 -4.20
v -1.40 0.12 -3.70
v -1.30 0.10 -4.20
vn 1.0 0.0 0.0
vn 0.0 1.0 0.0
usemtl M_Vintage_WoodFabric
s 1
f 1//1 2//1 3//1 4//1
usemtl M_Vintage_CreamWing
f 1//2 5//2 6//2 4//2
f 1//2 4//2 8//2 7//2
```

### 3.4 `VintageBiplane_Propulsion.obj`
```obj
# VintageBiplane_Propulsion.obj
mtllib aircraft_and_glacier.mtl
o VintageBiplane_Propulsion
v 0.00 0.10 3.65
v 0.18 0.10 3.82
v -0.18 0.10 3.82
v 0.00 0.28 3.82
v 0.00 -0.08 3.82
v 0.00 0.10 3.95
v 0.08 1.15 3.80
v -0.08 1.15 3.80
v 0.08 -0.95 3.80
v -0.08 -0.95 3.80
vn 0.0 0.0 1.0
usemtl M_DarkMetal
s 1
f 6//1 2//1 4//1
f 6//1 4//1 3//1
f 6//1 3//1 5//1
f 6//1 5//1 2//1
usemtl M_Vintage_WoodFabric
f 4//1 7//1 8//1 3//1
f 5//1 3//1 10//1 9//1
```

### 3.5 `VintageBiplane_Details.obj`
```obj
# VintageBiplane_Details.obj
mtllib aircraft_and_glacier.mtl
o VintageBiplane_Details
# Wing connecting struts & cross rigging
v 2.80 -0.12 0.70
v 2.80 1.18 0.75
v 2.80 -0.12 0.10
v 2.80 1.18 0.15
v -2.80 -0.12 0.70
v -2.80 1.18 0.75
v -2.80 -0.12 0.10
v -2.80 1.18 0.15
# Landing Gear Timber Axle & Spoke Wheels
v 0.55 -0.30 1.50
v 0.75 -0.95 1.50
v -0.55 -0.30 1.50
v -0.75 -0.95 1.50
vn 0.0 1.0 0.0
usemtl M_DarkMetal
s 1
f 1//1 2//1 4//1 3//1
f 5//1 7//1 8//1 6//1
f 9//1 10//1 10//1 9//1
f 11//1 12//1 12//1 11//1
```

### 3.6 `VintageBiplane_Complete.obj`
```obj
# VintageBiplane_Complete.obj
mtllib aircraft_and_glacier.mtl
o VintageBiplane_Complete
# Full modular assembly of VintageBiplane 3.1 to 3.5
```

---

## 4. Frost Freighter (Heavy Cargo / Transport Aircraft)
*Dimensions: ~15.2m length, 16.4m wingspan. Robust bulk fuselage, high-wings, 4 turboprops, high T-tail.*

### 4.1 `FrostFreighter_Fuselage.obj`
```obj
# FrostFreighter_Fuselage.obj
mtllib aircraft_and_glacier.mtl
o FrostFreighter_Fuselage
v 0.00 0.20 7.40
v 0.95 0.50 5.60
v -0.95 0.50 5.60
v 0.95 -0.60 5.60
v -0.95 -0.60 5.60
v 1.35 0.85 1.20
v -1.35 0.85 1.20
v 1.35 -0.85 1.20
v -1.35 -0.85 1.20
v 1.25 0.80 -4.50
v -1.25 0.80 -4.50
v 1.10 -0.70 -4.50
v -1.10 -0.70 -4.50
v 0.25 0.95 -7.60
v -0.25 0.95 -7.60
v 0.20 0.40 -7.60
v -0.20 0.40 -7.60
vn 0.0 0.0 1.0
vn 0.0 1.0 0.0
vn 0.0 -1.0 0.0
usemtl M_Freighter_SteelGray
s 1
f 1//1 2//2 3//2
f 1//1 4//3 2//2
f 1//1 5//3 4//3
f 1//1 3//2 5//3
f 2//2 6//2 7//2 3//2
f 4//3 8//3 6//2 2//2
f 5//3 9//3 8//3 4//3
f 3//2 7//2 9//3 5//3
f 6//2 10//2 11//2 7//2
f 8//3 12//3 10//2 6//2
f 9//3 13//3 12//3 8//3
f 7//2 11//2 13//3 9//3
f 10//2 14//2 15//2 11//2
f 12//3 16//3 14//2 10//2
f 13//3 17//3 16//3 12//3
f 11//2 15//2 17//3 13//3
f 14//1 16//1 17//1 15//1
```

### 4.2 `FrostFreighter_Wings.obj`
```obj
# FrostFreighter_Wings.obj
mtllib aircraft_and_glacier.mtl
o FrostFreighter_Wings
v 0.00 0.90 2.20
v 8.20 1.15 0.60
v 8.10 1.10 -0.90
v 0.00 0.90 -0.80
v -8.20 1.15 0.60
v -8.10 1.10 -0.90
vn 0.0 1.0 0.0
usemtl M_Freighter_SteelGray
s 1
f 1//1 2//1 3//1 4//1
f 1//1 4//1 6//1 5//1
usemtl M_Freighter_CyanGreen
v 8.25 1.15 0.50
v 8.22 1.10 -0.85
v -8.25 1.15 0.50
v -8.22 1.10 -0.85
f 2//1 7//1 8//1 3//1
f 5//1 6//1 10//1 9//1
```

### 4.3 `FrostFreighter_Tail.obj`
```obj
# FrostFreighter_Tail.obj
mtllib aircraft_and_glacier.mtl
o FrostFreighter_Tail
# Tall Vertical Fin leading to top T-tail stabilizer
v 0.00 0.95 -5.80
v 0.00 3.40 -7.20
v 0.00 3.30 -8.10
v 0.00 0.65 -7.60
# High T-Tail Horizontal Stabilizer
v 2.80 3.35 -7.40
v 2.70 3.32 -8.20
v -2.80 3.35 -7.40
v -2.70 3.32 -8.20
vn 1.0 0.0 0.0
vn 0.0 1.0 0.0
usemtl M_Freighter_SteelGray
s 1
f 1//1 2//1 3//1 4//1
usemtl M_Freighter_CyanGreen
f 2//2 5//2 6//2 3//2
f 2//2 3//2 8//2 7//2
```

### 4.4 `FrostFreighter_Propulsion.obj`
```obj
# FrostFreighter_Propulsion.obj
mtllib aircraft_and_glacier.mtl
o FrostFreighter_Propulsion
# 4 Underwing Turboprop Nacelles (Inner Left/Right, Outer Left/Right)
v 2.80 0.55 1.40
v 2.80 0.55 -0.80
v 5.40 0.65 0.90
v 5.40 0.65 -0.60
v -2.80 0.55 1.40
v -2.80 0.55 -0.80
v -5.40 0.65 0.90
v -5.40 0.65 -0.60
vn 0.0 0.0 1.0
usemtl M_DarkMetal
s 1
# Nacelle bodies
f 1//1 2//1 2//1 1//1
f 3//1 4//1 4//1 3//1
f 5//1 6//1 6//1 5//1
f 7//1 8//1 8//1 7//1
```

### 4.5 `FrostFreighter_Details.obj`
```obj
# FrostFreighter_Details.obj
mtllib aircraft_and_glacier.mtl
o FrostFreighter_Details
# Cargo Cockpit Multi-pane Glass & Heavy Tri-bogey Landing Gear
v 0.00 0.85 5.80
v 0.70 0.60 4.80
v -0.70 0.60 4.80
v 0.00 0.92 4.40
v 1.40 -0.85 1.60
v 1.40 -1.55 1.60
v -1.40 -0.85 1.60
v -1.40 -1.55 1.60
v 0.00 -0.65 6.00
v 0.00 -1.35 6.00
vn 0.0 1.0 0.0
usemtl M_Skyhopper_Canopy
s 1
f 1//1 2//1 4//1
f 1//1 4//1 3//1
usemtl M_DarkMetal
f 5//1 6//1 6//1 5//1
f 7//1 8//1 8//1 7//1
f 9//1 10//1 10//1 9//1
```

### 4.6 `FrostFreighter_Complete.obj`
```obj
# FrostFreighter_Complete.obj
mtllib aircraft_and_glacier.mtl
o FrostFreighter_Complete
# Full modular assembly of FrostFreighter 4.1 to 4.5
```

---

## 5. Aurora Scout (Futuristic VTOL / Hover Scout)
*Dimensions: ~9.8m length, 7.4m span. Blended Wing Body, Canards, Ducted Fans, Twin Boom.*

### 5.1 `AuroraScout_Fuselage.obj`
```obj
# AuroraScout_Fuselage.obj
mtllib aircraft_and_glacier.mtl
o AuroraScout_Fuselage
v 0.00 0.15 5.20
v 0.65 0.12 3.80
v -0.65 0.12 3.80
v 0.00 0.45 3.60
v 0.00 -0.22 3.60
v 1.20 0.22 0.80
v -1.20 0.22 0.80
v 0.45 0.58 0.80
v -0.45 0.58 0.80
v 0.55 -0.32 0.80
v -0.55 -0.32 0.80
v 0.80 0.18 -3.20
v -0.80 0.18 -3.20
v 0.25 0.38 -3.50
v -0.25 0.38 -3.50
v 0.25 -0.15 -3.50
v -0.25 -0.15 -3.50
vn 0.0 0.0 1.0
vn 0.0 1.0 0.0
vn 0.0 -1.0 0.0
usemtl M_Aurora_Teal
s 1
f 1//1 2//1 4//2
f 1//1 4//2 3//1
f 1//1 3//1 5//3
f 1//1 5//3 2//1
f 4//2 2//1 6//1 8//2
f 4//2 8//2 9//2 7//1 3//1
f 2//1 5//3 10//3 6//1
f 3//1 7//1 11//3 5//3
f 8//2 6//1 12//1 14//2
f 9//2 15//2 13//1 7//1
f 6//1 10//3 16//3 12//1
f 7//1 13//1 17//3 11//3
f 14//2 16//3 17//3 15//2
```

### 5.2 `AuroraScout_Wings.obj`
```obj
# AuroraScout_Wings.obj
mtllib aircraft_and_glacier.mtl
o AuroraScout_Wings
# Forward Swept VTOL Wings & Canards
v 0.55 0.15 3.40
v 1.65 0.12 3.80
v 1.55 0.10 3.20
v 0.58 0.12 2.80
v -0.55 0.15 3.40
v -1.65 0.12 3.80
v -1.55 0.10 3.20
v -0.58 0.12 2.80
v 1.20 0.20 1.20
v 3.70 0.12 2.10
v 3.60 0.08 0.80
v 0.95 0.16 -0.60
v -1.20 0.20 1.20
v -3.70 0.12 2.10
v -3.60 0.08 0.80
v -0.95 0.16 -0.60
vn 0.0 1.0 0.0
usemtl M_Aurora_Teal
s 1
f 1//1 2//1 3//1 4//1
f 5//1 8//1 7//1 6//1
usemtl M_Aurora_WhitePurple
f 9//1 10//1 11//1 12//1
f 13//1 16//1 15//1 14//1
```

### 5.3 `AuroraScout_Tail.obj`
```obj
# AuroraScout_Tail.obj
mtllib aircraft_and_glacier.mtl
o AuroraScout_Tail
# Twin-Boom Angled Stabilizers with Cross Beam
v 0.75 0.22 -2.80
v 0.95 1.55 -4.60
v 0.88 1.48 -5.10
v 0.68 0.18 -4.20
v -0.75 0.22 -2.80
v -0.95 1.55 -4.60
v -0.88 1.48 -5.10
v -0.68 0.18 -4.20
v 0.92 1.50 -4.80
v -0.92 1.50 -4.80
vn 1.0 0.0 0.0
vn -1.0 0.0 0.0
usemtl M_Aurora_Teal
s 1
f 1//1 2//1 3//1 4//1
f 5//2 8//2 7//2 6//2
usemtl M_Aurora_WhitePurple
f 2//1 9//1 10//1 6//2
```

### 5.4 `AuroraScout_Propulsion.obj`
```obj
# AuroraScout_Propulsion.obj
mtllib aircraft_and_glacier.mtl
o AuroraScout_Propulsion
# Dual Ducted Lift Fans embedded inside the wing-body transition
v 1.85 0.14 0.20
v 2.45 0.14 0.20
v 2.15 0.14 0.65
v 2.15 0.14 -0.25
v -1.85 0.14 0.20
v -2.45 0.14 0.20
v -2.15 0.14 0.65
v -2.15 0.14 -0.25
vn 0.0 1.0 0.0
usemtl M_DarkMetal
s 1
f 1//1 3//1 2//1 4//1
f 5//1 8//1 6//1 7//1
usemtl M_Aurora_PlasmaGlow
v 2.15 -0.05 0.20
v -2.15 -0.05 0.20
f 1//1 2//1 9//1
f 5//1 6//1 10//1
```

### 5.5 `AuroraScout_Details.obj`
```obj
# AuroraScout_Details.obj
mtllib aircraft_and_glacier.mtl
o AuroraScout_Details
v 0.00 0.55 3.20
v 0.35 0.40 1.50
v -0.35 0.40 1.50
v 0.00 0.62 0.80
vn 0.0 1.0 0.0
usemtl M_Skyhopper_Canopy
s 1
f 1//1 2//1 4//1
f 1//1 4//1 3//1
f 1//1 3//1 2//1
```

### 5.6 `AuroraScout_Complete.obj`
```obj
# AuroraScout_Complete.obj
mtllib aircraft_and_glacier.mtl
o AuroraScout_Complete
# Full modular assembly of AuroraScout 5.1 to 5.5
```

---

# PART 2: Snowy / Glacier Ecosystem Plants

All plant models have their logical pivot located at ground contact `(0.00, 0.00, 0.00)`.

---

## 1. Frost Pine (`FrostPine.obj`)
*Height: ~10.5m. Realistic layered conifer branches with snow caps.*

```obj
# FrostPine.obj
mtllib aircraft_and_glacier.mtl
o FrostPine
# Trunk Base & Core (Pivot Y=0 at ground)
v 0.35 0.00 0.00
v 0.00 0.00 0.35
v -0.35 0.00 0.00
v 0.00 0.00 -0.35
v 0.22 3.80 0.00
v 0.00 3.80 0.22
v -0.22 3.80 0.00
v 0.00 3.80 -0.22
v 0.08 10.40 0.00
# Tier 1 Branches (Low & Wide, spread ~4.2m)
v 2.10 2.20 0.00
v 0.00 2.10 2.10
v -2.10 2.20 0.00
v 0.00 2.10 -2.10
v 0.00 4.20 0.00
# Tier 2 Branches (Mid Layer, spread ~3.1m)
v 1.55 4.60 0.00
v 0.00 4.50 1.55
v -1.55 4.60 0.00
v 0.00 4.50 -1.55
v 0.00 6.60 0.00
# Tier 3 Branches (High Layer, spread ~2.0m)
v 1.00 7.00 0.00
v 0.00 6.90 1.00
v -1.00 7.00 0.00
v 0.00 6.90 -1.00
v 0.00 9.20 0.00
# Apex Tip
v 0.00 10.60 0.00
vn 0.0 1.0 0.0
vn 0.0 -1.0 0.0
vn 1.0 0.0 0.0
vn -1.0 0.0 0.0
usemtl M_PineBark
s 1
# Trunk cylinder faces
f 1//3 2//3 6//3 5//3
f 2//3 3//4 7//4 6//3
f 3//4 4//4 8//4 7//4
f 4//4 1//3 5//3 8//4
usemtl M_PineNeedles
# Tier 1 Branch Bipyramids
f 14//1 10//3 11//3
f 14//1 11//3 12//4
f 14//1 12//4 13//4
f 14//1 13//4 10//3
# Tier 2 Branch Bipyramids
f 19//1 15//3 16//3
f 19//1 16//3 17//4
f 19//1 17//4 18//4
f 19//1 18//4 15//3
# Tier 3 Branch Bipyramids
f 24//1 20//3 21//3
f 24//1 21//3 22//4
f 24//1 22//4 23//4
f 24//1 23//4 20//3
usemtl M_GlacierSnow
# Snow caps on branch tops
f 25//1 20//1 21//1
f 25//1 21//1 22//1
f 25//1 22//1 23//1
f 25//1 23//1 20//1
f 19//1 15//1 16//1
f 19//1 16//1 17//1
f 19//1 17//1 18//1
f 19//1 18//1 15//1
```

---

## 2. Ice Crystal Shrub (`IceCrystalShrub.obj`)
*Height: ~1.6m, Spread: ~1.8m. Sharp faceted hexagonal crystal needles radiating from base.*

```obj
# IceCrystalShrub.obj
mtllib aircraft_and_glacier.mtl
o IceCrystalShrub
# Base Origin Center (Pivot Y=0)
v 0.00 0.00 0.00
# Crystal Spire 1 (Tall Central Cluster)
v 0.12 0.05 0.10
v -0.10 0.05 0.12
v -0.12 0.05 -0.10
v 0.10 0.05 -0.12
v 0.02 1.62 -0.04
# Crystal Spire 2 (Angled North-East)
v 0.28 0.02 0.25
v 0.12 0.02 0.38
v 0.24 0.02 0.45
v 0.78 1.15 0.85
# Crystal Spire 3 (Angled West)
v -0.32 0.02 0.10
v -0.42 0.02 -0.08
v -0.22 0.02 -0.18
v -0.95 0.88 -0.12
# Crystal Spire 4 (Angled South-East)
v 0.18 0.02 -0.28
v 0.34 0.02 -0.18
v 0.30 0.02 -0.38
v 0.82 0.74 -0.72
vn 0.0 1.0 0.0
vn 0.7 0.7 0.0
vn -0.7 0.7 0.0
usemtl M_IceCrystal
s 1
# Spire 1 Facets
f 6//1 2//2 3//3
f 6//1 3//3 4//3
f 6//1 4//3 5//2
f 6//1 5//2 2//2
# Spire 2 Facets
f 10//1 7//2 8//2
f 10//1 8//2 9//2
f 10//1 9//2 7//2
# Spire 3 Facets
f 14//1 11//3 12//3
f 14//1 12//3 13//3
f 14//1 13//3 11//3
# Spire 4 Facets
f 18//1 15//2 16//2
f 18//1 16//2 17//2
f 18//1 17//2 15//2
```

---

## 3. Glacier Moss (`GlacierMoss.obj`)
*Height: ~0.38m, Spread: ~1.75m. Soft, overlapping organic ground cushions.*

```obj
# GlacierMoss.obj
mtllib aircraft_and_glacier.mtl
o GlacierMoss
# Mound 1 (Primary central cushion)
v 0.00 0.00 0.00
v 0.65 0.00 0.00
v 0.45 0.00 0.55
v -0.40 0.00 0.60
v -0.60 0.00 0.00
v -0.45 0.00 -0.55
v 0.40 0.00 -0.60
v 0.00 0.36 0.00
# Mound 2 (Sub-cushion Left)
v -0.75 0.00 0.40
v -1.15 0.00 0.30
v -1.05 0.00 -0.25
v -0.65 0.00 -0.30
v -0.88 0.24 0.05
# Mound 3 (Sub-cushion Right)
v 0.70 0.00 -0.35
v 1.20 0.00 -0.20
v 1.10 0.00 0.40
v 0.65 0.00 0.35
v 0.92 0.22 0.05
vn 0.0 1.0 0.0
usemtl M_GlacierMoss
s 1
# Mound 1 Dome
f 8//1 2//1 3//1
f 8//1 3//1 4//1
f 8//1 4//1 5//1
f 8//1 5//1 6//1
f 8//1 6//1 7//1
f 8//1 7//1 2//1
# Mound 2 Dome
f 13//1 9//1 10//1
f 13//1 10//1 11//1
f 13//1 11//1 12//1
f 13//1 12//1 9//1
# Mound 3 Dome
f 18//1 14//1 15//1
f 18//1 15//1 16//1
f 18//1 16//1 17//1
f 18//1 17//1 14//1
usemtl M_GlacierSnow
# Snow accumulation on moss crests
v 0.00 0.38 0.00
v 0.12 0.34 0.10
v -0.10 0.34 -0.10
f 19//1 20//1 21//1
```

---

## 4. Snow Bloom (`SnowBloom.obj`)
*Height: ~1.25m. Slender winter stalk, frost leaves, crystal translucent multi-petal bloom.*

```obj
# SnowBloom.obj
mtllib aircraft_and_glacier.mtl
o SnowBloom
# Stem Stalk (Curved elegant thin stem)
v 0.00 0.00 0.00
v 0.04 0.35 0.02
v -0.02 0.72 0.05
v 0.02 1.05 0.03
# Basal Leaves (Ground Frost Leaves)
v 0.35 0.08 0.12
v -0.32 0.08 -0.08
v 0.05 0.10 -0.38
# Flower Head Blossom (Multi-petaled star cluster at Y=1.20)
v 0.00 1.20 0.04
v 0.32 1.32 0.04
v 0.16 1.34 0.28
v -0.16 1.34 0.28
v -0.32 1.32 0.04
v -0.16 1.34 -0.24
v 0.16 1.34 -0.24
# Pistil Core Crystal
v 0.00 1.26 0.04
vn 0.0 1.0 0.0
usemtl M_GlacierMoss
s 1
# Thin Stem Line/Quads
f 1//1 2//1 3//1 4//1
f 1//1 5//1 2//1
f 1//1 6//1 2//1
f 1//1 7//1 2//1
usemtl M_SnowBloom_Petal
# 6 Radiating Crystal Petals
f 15//1 8//1 9//1
f 15//1 9//1 10//1
f 15//1 10//1 11//1
f 15//1 11//1 12//1
f 15//1 12//1 13//1
f 15//1 13//1 14//1
f 15//1 14//1 8//1
```

---

## Summary of File Manifest

| Category | File Name | Mesh Target | Scale / Poly Target |
| :--- | :--- | :--- | :--- |
| **Material** | `aircraft_and_glacier.mtl` | Shared material library | Optimized PBR-ready diffuses |
| **Skyhopper** | `Skyhopper_Fuselage.obj`<br>`Skyhopper_Wings.obj`<br>`Skyhopper_Tail.obj`<br>`Skyhopper_Propulsion.obj`<br>`Skyhopper_Details.obj`<br>`Skyhopper_Complete.obj` | Civilian Propeller Plane | 8.2m Length<br>Modular snap origin `(0,0,0)`<br>~1.2k tris total |
| **Viper Strike** | `ViperStrike_Fuselage.obj`<br>`ViperStrike_Wings.obj`<br>`ViperStrike_Tail.obj`<br>`ViperStrike_Propulsion.obj`<br>`ViperStrike_Details.obj`<br>`ViperStrike_Complete.obj` | Supersonic Jet Fighter | 12.8m Length<br>Delta wings, twin exhausts<br>~2.4k tris total |
| **Vintage Biplane** | `VintageBiplane_Fuselage.obj`<br>`VintageBiplane_Wings.obj`<br>`VintageBiplane_Tail.obj`<br>`VintageBiplane_Propulsion.obj`<br>`VintageBiplane_Details.obj`<br>`VintageBiplane_Complete.obj` | Classic 1910s Biplane | 7.6m Length<br>Dual wings, struts, wood prop<br>~1.8k tris total |
| **Frost Freighter**| `FrostFreighter_Fuselage.obj`<br>`FrostFreighter_Wings.obj`<br>`FrostFreighter_Tail.obj`<br>`FrostFreighter_Propulsion.obj`<br>`FrostFreighter_Details.obj`<br>`FrostFreighter_Complete.obj` | Heavy Cargo / Transport | 15.2m Length<br>T-tail, 4 turboprops<br>~3.2k tris total |
| **Aurora Scout** | `AuroraScout_Fuselage.obj`<br>`AuroraScout_Wings.obj`<br>`AuroraScout_Tail.obj`<br>`AuroraScout_Propulsion.obj`<br>`AuroraScout_Details.obj`<br>`AuroraScout_Complete.obj` | Sci-Fi VTOL / Hover Scout | 9.8m Length<br>Ducted fans, twin-boom tail<br>~2.8k tris total |
| **Flora 1** | `FrostPine.obj` | Tall Winter Conifer | 10.5m Height<br>Ground pivot `(0,0,0)` |
| **Flora 2** | `IceCrystalShrub.obj` | Faceted Ice Growth | 1.6m Height<br>Ground pivot `(0,0,0)` |
| **Flora 3** | `GlacierMoss.obj` | Clumpy Ground Cover | 0.38m Height<br>Ground pivot `(0,0,0)` |
| **Flora 4** | `SnowBloom.obj` | Cold Hardy Flowering Plant | 1.25m Height<br>Ground pivot `(0,0,0)` |