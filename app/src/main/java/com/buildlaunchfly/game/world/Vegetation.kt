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
        val maxAttempts = count * 6

        while (placed < count && attempts < maxAttempts) {
            attempts++
            val x = -760f + rng.nextFloat() * 1660f
            val z = -2600f + rng.nextFloat() * 3000f
            val h = terrain.height(x, z)
            if (h < def.waterLevel + 1.6f) continue
            terrain.normal(x, z, normal)
            if (normal.y < 0.74f) continue
            val density = Noise.fbm01(x * 0.0035f, z * 0.0035f, 2, seed + 5)
            if (density < 0.40f) continue

            val height = 2.6f + rng.nextFloat() * 4.4f
            val width = height * (0.30f + rng.nextFloat() * 0.16f)
            val rotY = rng.nextFloat() * 6.2831855f
            matrix.setTrs(x, h, z, 0f, rotY, 0f, width, height, width)
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
            val h = terrain.height(x, z)
            if (h < def.waterLevel + 0.8f) continue
            terrain.normal(x, z, normal)
            // Rocks prefer slopes and ridges.
            val slopeBias = 1.0f - normal.y
            if (rng.nextFloat() > 0.25f + slopeBias * 2.4f) continue

            val size = 0.9f + rng.nextFloat() * 3.4f
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
        while (placed < count && attempts < count * 10) {
            attempts++
            val x = -920f + rng.nextFloat() * 1900f
            val z = -2750f + rng.nextFloat() * 3150f
            val h = terrain.height(x, z)
            if (h < def.waterLevel + 0.8f) continue
            terrain.normal(x, z, normal)
            if (normal.y < 0.78f) continue
            val patch = Noise.fbm01(x * 0.006f, z * 0.006f, 2, seed + 31)
            if (patch < 0.34f) continue
            val scale = when (def.id) {
                WorldId.DESERT -> 0.65f + rng.nextFloat() * 0.8f
                WorldId.SNOW -> 0.55f + rng.nextFloat() * 0.65f
                else -> 0.8f + rng.nextFloat() * 1.2f
            }
            val sx = scale * (0.85f + rng.nextFloat() * 0.45f)
            val sz = scale * (0.85f + rng.nextFloat() * 0.45f)
            matrix.setTrs(x, h, z, 0f, rng.nextFloat() * 6.2831855f, 0f, sx, scale, sz)
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
        centerX: Float,
        centerZ: Float,
        radius: Float
    ): InstanceSet {
        if (count <= 0) return InstanceSet.EMPTY
        val out = FloatArray(count * 16)
        val rng = Random(seed + 1543)
        val normal = Vec3()
        val matrix = Mat4()
        var placed = 0
        var attempts = 0
        val maxAttempts = count * 4

        while (placed < count && attempts < maxAttempts) {
            attempts++
            val x = centerX + (rng.nextFloat() * 2f - 1f) * radius
            val z = centerZ + (rng.nextFloat() * 2f - 1f) * radius
            val h = terrain.height(x, z)
            if (h < def.waterLevel + 0.4f) continue
            terrain.normal(x, z, normal)
            if (normal.y < 0.80f) continue

            val size = 0.5f + rng.nextFloat() * 0.9f
            matrix.setTrs(x, h, z, 0f, rng.nextFloat() * 6.2831855f, 0f, size, size * 1.3f, size)
            System.arraycopy(matrix.m, 0, out, placed * 16, 16)
            placed++
        }

        if (placed == 0) return InstanceSet.EMPTY
        return InstanceSet(placed, out)
    }
}
