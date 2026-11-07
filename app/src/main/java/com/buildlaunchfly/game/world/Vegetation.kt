package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.InstanceSet
import com.buildlaunchfly.game.gfx.Noise
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3
import kotlin.random.Random

/** Scatters trees, rocks and grass instances over a world's terrain. */
object Vegetation {

    fun scatterTrees(terrain: Terrain, def: WorldDefinition, count: Int, seed: Int): InstanceSet {
        if (count <= 0) return InstanceSet.EMPTY
        val out = FloatArray(count * 16)
        val rng = Random(seed)
        val normal = Vec3()
        val matrix = Mat4()
        var placed = 0
        var attempts = 0
        val maxAttempts = count * 16

        while (placed < count && attempts < maxAttempts) {
            attempts++
            val x = -760f + rng.nextFloat() * 1660f
            val z = -2600f + rng.nextFloat() * 3000f
            val h = terrain.height(x, z)
            if (h < def.waterLevel + 1.6f) continue
            terrain.normal(x, z, normal)
            val density = Noise.fbm01(x * 0.0035f, z * 0.0035f, 2, seed + 5)
            val relativeHeight = h - def.heightOffset
            val habitat = when (def.id) {
                WorldId.FOREST -> {
                    // Mixed temperate woodland: clustered canopy with deliberate meadow gaps.
                    normal.y >= 0.74f && density > 0.39f &&
                        Noise.fbm01(x * 0.0015f, z * 0.0015f, 2, seed + 19) > 0.32f
                }
                WorldId.DESERT -> {
                    // Saguaro stands favour rocky fans and wadi margins, not every dune crest.
                    val wadiX = 95f * kotlin.math.sin(z * 0.0018f) +
                        38f * kotlin.math.sin(z * 0.0041f + 1.2f)
                    normal.y >= 0.72f && density > 0.50f &&
                        kotlin.math.abs(x - wadiX) < 210f
                }
                WorldId.SNOW -> {
                    // Sparse conifers remain below the exposed alpine/glacier tree line.
                    normal.y >= 0.76f && relativeHeight in 4f..92f && density > 0.47f
                }
                WorldId.OCEAN -> {
                    // Coastal woodland sits inland from the beach and away from sea cliffs.
                    val shoreline = -285f + 78f * kotlin.math.sin(z * 0.00145f) +
                        24f * kotlin.math.sin(z * 0.0031f + 0.8f)
                    val inland = x - shoreline
                    normal.y >= 0.76f && inland in 210f..760f && density > 0.43f
                }
                WorldId.MOUNTAIN -> {
                    // Forest transitions into open alpine meadow as elevation increases.
                    normal.y >= 0.72f && relativeHeight in 6f..118f && density > 0.38f
                }
            }
            if (!habitat) continue

            val height = when (def.id) {
                WorldId.DESERT -> 2.2f + rng.nextFloat() * 3.2f
                WorldId.SNOW -> 3.2f + rng.nextFloat() * 4.8f
                WorldId.OCEAN -> 3.0f + rng.nextFloat() * 5.2f
                WorldId.MOUNTAIN -> 3.6f + rng.nextFloat() * 6.0f
                WorldId.FOREST -> 4.2f + rng.nextFloat() * 6.8f
            }
            // Independent crown footprint prevents a repeated row of identical tree stamps.
            // Broadleaf stands vary from narrow birches to wide Scots-pine-like crowns;
            // coastal trees receive a little more directional lean from persistent sea wind.
            val widthX = height * (0.22f + rng.nextFloat() * 0.30f)
            val widthZ = height * (0.20f + rng.nextFloat() * 0.32f)
            val rotY = rng.nextFloat() * 6.2831855f
            val windLean = if (def.id == WorldId.OCEAN) 0.095f else 0.055f
            val leanX = (rng.nextFloat() - 0.5f) * windLean
            val leanZ = (rng.nextFloat() - 0.5f) * windLean
            matrix.setTrs(x, h, z, leanX, rotY, leanZ, widthX, height, widthZ)
            System.arraycopy(matrix.m, 0, out, placed * 16, 16)
            placed++
        }

        if (placed == 0) return InstanceSet.EMPTY
        return InstanceSet(placed, out)
    }

    fun scatterRocks(terrain: Terrain, def: WorldDefinition, count: Int, seed: Int): InstanceSet {
        if (count <= 0) return InstanceSet.EMPTY
        val out = FloatArray(count * 16)
        val rng = Random(seed + 977)
        val normal = Vec3()
        val matrix = Mat4()
        var placed = 0
        var attempts = 0
        val maxAttempts = count * 6

        while (placed < count && attempts < maxAttempts) {
            attempts++
            val x = -1100f + rng.nextFloat() * 2400f
            val z = -3000f + rng.nextFloat() * 3600f
            // Keep a narrow central flight lane visually clear through the near/midground.
            // Rocks remain distributed on both sides so the terrain still reads as natural.
            if (z in -2200f..-120f && kotlin.math.abs(x) < 24f) continue
            val h = terrain.height(x, z)
            if (h < def.waterLevel + 0.8f) continue
            terrain.normal(x, z, normal)
            // Rocks prefer slopes and ridges.
            val slopeBias = 1.0f - normal.y
            if (rng.nextFloat() > 0.25f + slopeBias * 2.4f) continue

            // Foreground rocks carry readable shape detail; distant rocks shrink toward
            // the horizon to create scale separation without adding extra draw calls.
            val depth = ((-z) / 3000f).coerceIn(0f, 1f)
            val size = (0.9f + rng.nextFloat() * 3.4f) * (1.43f - depth * 0.78f)
            val rotX = (rng.nextFloat() - 0.5f) * 0.4f
            val rotY = rng.nextFloat() * 6.2831855f
            val rotZ = (rng.nextFloat() - 0.5f) * 0.4f
            matrix.setTrs(
                x, h - size * 0.18f, z, rotX, rotY, rotZ,
                size * (0.9f + rng.nextFloat() * 0.5f),
                size * (0.6f + rng.nextFloat() * 0.5f),
                size * (0.9f + rng.nextFloat() * 0.5f)
            )
            System.arraycopy(matrix.m, 0, out, placed * 16, 16)
            placed++
        }

        if (placed == 0) return InstanceSet.EMPTY
        return InstanceSet(placed, out)
    }


    /**
     * Scatters low, multi-crown shrubs independently from the tree and grass passes.
     * Forest and alpine worlds receive denser understory; desert and coastal worlds
     * receive sparse, widely spaced scrub.
     */
    fun scatterBushes(terrain: Terrain, def: WorldDefinition, count: Int, seed: Int): InstanceSet {
        if (count <= 0) return InstanceSet.EMPTY
        val out = FloatArray(count * 16)
        val rng = Random(seed + 2687)
        val normal = Vec3()
        val matrix = Mat4()
        var placed = 0
        var attempts = 0
        while (placed < count && attempts < count * 16) {
            attempts++
            val x = -920f + rng.nextFloat() * 1900f
            val z = -2750f + rng.nextFloat() * 3150f
            val h = terrain.height(x, z)
            if (h < def.waterLevel + 0.8f) continue
            terrain.normal(x, z, normal)

            val patch = Noise.fbm01(x * 0.006f, z * 0.006f, 2, seed + 31)
            val habitat = when (def.id) {
                WorldId.FOREST -> {
                    // Understory gathers in broken woodland patches, leaving sunlit clearings.
                    normal.y >= 0.78f && patch > 0.29f
                }
                WorldId.DESERT -> {
                    // Scrub follows the dry wadi and rocky washes rather than carpeting dunes.
                    val wadiX = 95f * kotlin.math.sin(z * 0.0018f) +
                        38f * kotlin.math.sin(z * 0.0041f + 1.2f)
                    val washDistance = kotlin.math.abs(x - wadiX)
                    normal.y >= 0.84f && patch > 0.47f &&
                        (washDistance < 78f || Noise.fbm01(x * 0.0022f, z * 0.0022f, 2, seed + 67) > 0.64f)
                }
                WorldId.SNOW -> {
                    // Low tundra shrubs occupy sheltered, lower slopes, not exposed glacier faces.
                    val relativeHeight = h - def.heightOffset
                    normal.y >= 0.88f && relativeHeight in 2f..76f && patch > 0.43f
                }
                WorldId.OCEAN -> {
                    // Coastal scrub and dune plants stay on the landward beach shelf.
                    val shoreline = -285f + 78f * kotlin.math.sin(z * 0.00145f) +
                        24f * kotlin.math.sin(z * 0.0031f + 0.8f)
                    val inland = x - shoreline
                    normal.y >= 0.82f && inland in 8f..178f && patch > 0.35f
                }
                WorldId.MOUNTAIN -> {
                    // Heather and alpine scrub prefer meadow benches below the bare ridgelines.
                    val relativeHeight = h - def.heightOffset
                    normal.y >= 0.80f && relativeHeight in 5f..92f && patch > 0.34f
                }
            }
            if (!habitat) continue

            val scale = when (def.id) {
                WorldId.DESERT -> 0.55f + rng.nextFloat() * 0.72f
                WorldId.SNOW -> 0.48f + rng.nextFloat() * 0.58f
                WorldId.OCEAN -> 0.62f + rng.nextFloat() * 0.82f
                WorldId.MOUNTAIN -> 0.72f + rng.nextFloat() * 1.05f
                WorldId.FOREST -> 0.78f + rng.nextFloat() * 1.18f
            }
            // Stronger independent axis variation changes each shrub's height, spread and silhouette.
            // The low-frequency habitat mask still controls clustering; transforms avoid repeated copies.
            // Establish a readable near-to-far understory scale gradient. Nearby
            // plants keep their silhouette; distant clumps recede without extra geometry.
            val depth = ((-z) / 2750f).coerceIn(0f, 1f)
            val depthScale = 1.18f - depth * 0.48f
            val sx = scale * depthScale * (0.58f + rng.nextFloat() * 0.92f)
            val sy = scale * depthScale * (0.72f + rng.nextFloat() * 0.78f)
            val sz = scale * depthScale * (0.60f + rng.nextFloat() * 0.90f)
            val leanX = (rng.nextFloat() - 0.5f) * 0.18f
            val leanZ = (rng.nextFloat() - 0.5f) * 0.18f
            matrix.setTrs(x, h, z, leanX, rng.nextFloat() * 6.2831855f, leanZ, sx, sy, sz)
            System.arraycopy(matrix.m, 0, out, placed * 16, 16)
            placed++
        }
        return if (placed == 0) InstanceSet.EMPTY else InstanceSet(placed, out)
    }

    fun scatterGrass(
        terrain: Terrain,
        def: WorldDefinition,
        count: Int,
        seed: Int,
    ): InstanceSet {
        if (count <= 0) return InstanceSet.EMPTY
        val out = FloatArray(count * 16)
        val rng = Random(seed + 1543)
        val normal = Vec3()
        val matrix = Mat4()
        var placed = 0
        var attempts = 0
        val maxAttempts = count * 4

        while (placed < count && attempts < maxAttempts * 3) {
            attempts++
            // Scatter across the traversable world footprint, not just the launch-pad
            // clearing. The coastline keeps its own narrow landward marram band.
            val z = -2700f + rng.nextFloat() * 3050f
            val randomX = -760f + rng.nextFloat() * 1660f
            val shoreline = -285f + 78f * kotlin.math.sin(z * 0.00145f) +
                24f * kotlin.math.sin(z * 0.0031f + 0.8f)
            val x = if (def.id == WorldId.OCEAN) shoreline + 18f + rng.nextFloat() * 150f else randomX
            val h = terrain.height(x, z)
            if (h < def.waterLevel + 0.4f) continue
            terrain.normal(x, z, normal)
            if (normal.y < 0.80f) continue

            val patch = Noise.fbm01(x * 0.008f, z * 0.008f, 2, seed + 103)
            // Broad low-frequency gaps create visible bare soil between grass clumps;
            // the second field adds irregular edges instead of a uniform green carpet.
            val groundCover = Noise.fbm01(x * 0.0019f, z * 0.0019f, 3, seed + 149)
            val barePatch = Noise.fbm01(x * 0.00085f, z * 0.00085f, 2, seed + 211)
            val habitat = when (def.id) {
                WorldId.FOREST -> patch > 0.58f && groundCover > 0.46f && barePatch > 0.28f
                WorldId.DESERT -> {
                    val wadiX = 95f * kotlin.math.sin(z * 0.0018f) +
                        38f * kotlin.math.sin(z * 0.0041f + 1.2f)
                    kotlin.math.abs(x - wadiX) < 62f && patch > 0.68f &&
                        groundCover > 0.54f && barePatch > 0.36f
                }
                WorldId.SNOW -> false // glacier world uses exposed ice, snow and tundra shrubs
                WorldId.OCEAN -> patch > 0.62f && groundCover > 0.50f && barePatch > 0.30f
                WorldId.MOUNTAIN -> {
                    val relativeHeight = h - def.heightOffset
                    relativeHeight in 4f..86f && patch > 0.60f &&
                        groundCover > 0.48f && barePatch > 0.28f
                }
            }
            if (!habitat) continue

            val size = when (def.id) {
                WorldId.DESERT -> 0.30f + rng.nextFloat() * 0.62f
                WorldId.OCEAN -> 0.58f + rng.nextFloat() * 0.94f
                WorldId.MOUNTAIN -> 0.42f + rng.nextFloat() * 1.02f
                WorldId.FOREST -> 0.46f + rng.nextFloat() * 1.12f
                WorldId.SNOW -> 0.35f + rng.nextFloat() * 0.60f
            }
            // Give exposed grass a shared prevailing-wind direction, then layer per-clump
            // variation over it. This avoids the uniformly random, upright lawn appearance.
            val prevailingLean = when (def.id) {
                WorldId.DESERT -> -0.19f
                WorldId.OCEAN -> -0.27f
                WorldId.MOUNTAIN -> -0.11f
                WorldId.FOREST -> -0.035f
                WorldId.SNOW -> 0f
            }
            val lean = prevailingLean + (rng.nextFloat() - 0.5f) * 0.18f
            val depthScale = size * (0.68f + rng.nextFloat() * 0.64f)
            val heightScale = size * (1.08f + rng.nextFloat() * 0.52f)
            matrix.setTrs(x, h, z, lean, rng.nextFloat() * 6.2831855f, -lean * 0.55f,
                size, heightScale, depthScale)
            System.arraycopy(matrix.m, 0, out, placed * 16, 16)
            placed++
        }

        if (placed == 0) return InstanceSet.EMPTY
        return InstanceSet(placed, out)
    }
}
