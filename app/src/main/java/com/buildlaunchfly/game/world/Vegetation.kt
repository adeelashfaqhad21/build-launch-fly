package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.InstanceSet
import com.buildlaunchfly.game.gfx.Noise
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3
import kotlin.random.Random

/**
 * Scatters trees, shrubs, rocks and grass over a world's terrain.
 *
 * Every scatter returns one [InstanceSet] per species instead of a single blended population,
 * so the caller can give each species its own mesh and material. Species come from a
 * low-frequency noise field, which makes stands of one kind cluster the way real vegetation
 * does, and grass only grows inside noise-driven meadows so bare soil shows between them.
 */
object Vegetation {

    /** Tree shapes the world builds: broadleaf, conifer, poplar. */
    const val TREE_SPECIES = 3

    /** Shrub shapes: low round, wide creeping, spiky. */
    const val SHRUB_SPECIES = 3

    /** Rock shapes: rounded boulder, blocky outcrop, flat slab. */
    const val ROCK_SPECIES = 3

    /** Grass clumps: tall crossed blades, narrow tuft, low clump. */
    const val GRASS_SPECIES = 3

    fun scatterTrees(
        terrain: Terrain,
        def: WorldDefinition,
        count: Int,
        seed: Int,
        speciesCount: Int = TREE_SPECIES
    ): List<InstanceSet> {
        if (count <= 0 || speciesCount <= 0) return emptySets(speciesCount)
        val buckets = Buckets(speciesCount, count)
        val rng = Random(seed)
        val normal = Vec3()
        val matrix = Mat4()
        val coniferChance = if (def.pineTrees) 0.55f else 0.18f
        var placed = 0
        var attempts = 0
        val maxAttempts = count * 8

        while (placed < count && attempts < maxAttempts) {
            attempts++
            val x = -420f + rng.nextFloat() * 900f
            val z = -1500f + rng.nextFloat() * 1620f
            val h = terrain.height(x, z)
            if (h < def.waterLevel + 1.6f) continue
            terrain.normal(x, z, normal)
            if (normal.y < 0.74f) continue
            val density = Noise.fbm01(x * 0.0035f, z * 0.0035f, 2, seed + 5)
            if (density < 0.40f) continue

            // Full-grown timber: 5–13 m, so the aircraft flies between the crowns rather than
            // over a field of saplings.
            val height = 5.0f + rng.nextFloat() * 8.0f
            val width = height * (0.30f + rng.nextFloat() * 0.22f)
            val species = if (rng.nextFloat() < coniferChance) {
                coniferSlot(speciesCount)
            } else {
                speciesAt(x, z, seed, speciesCount, 13)
            }
            matrix.setTrs(x, h, z, 0f, rng.nextFloat() * 6.2831855f, 0f, width, height, width)
            buckets.add(species, matrix)
            placed++
        }

        return buckets.toSets()
    }

    fun scatterShrubs(
        terrain: Terrain,
        def: WorldDefinition,
        count: Int,
        seed: Int,
        speciesCount: Int = SHRUB_SPECIES
    ): List<InstanceSet> {
        if (count <= 0 || speciesCount <= 0) return emptySets(speciesCount)
        val buckets = Buckets(speciesCount, count)
        val rng = Random(seed + 613)
        val normal = Vec3()
        val matrix = Mat4()
        var placed = 0
        var attempts = 0
        val maxAttempts = count * 6

        while (placed < count && attempts < maxAttempts) {
            attempts++
            val x = -620f + rng.nextFloat() * 1400f
            val z = -2200f + rng.nextFloat() * 2500f
            val h = terrain.height(x, z)
            if (h < def.waterLevel + 0.8f) continue
            terrain.normal(x, z, normal)
            if (normal.y < 0.70f) continue
            val density = Noise.fbm01(x * 0.0055f, z * 0.0055f, 2, seed + 19)
            if (density < 0.32f) continue

            val size = 0.55f + rng.nextFloat() * 1.25f
            val species = speciesAt(x, z, seed, speciesCount, 23)
            matrix.setTrs(
                x, h, z,
                0f, rng.nextFloat() * 6.2831855f, 0f,
                size * (0.85f + rng.nextFloat() * 0.4f),
                size * (0.70f + rng.nextFloat() * 0.5f),
                size * (0.85f + rng.nextFloat() * 0.4f)
            )
            buckets.add(species, matrix)
            placed++
        }

        return buckets.toSets()
    }

    fun scatterRocks(
        terrain: Terrain,
        def: WorldDefinition,
        count: Int,
        seed: Int,
        speciesCount: Int = ROCK_SPECIES
    ): List<InstanceSet> {
        if (count <= 0 || speciesCount <= 0) return emptySets(speciesCount)
        val buckets = Buckets(speciesCount, count)
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
            // Rocks prefer slopes, ridges and the edges of bare ground.
            val slopeBias = 1.0f - normal.y
            val bare = 1f - Noise.fbm01(x * 0.01f, z * 0.01f, 2, seed + 37)
            if (rng.nextFloat() > 0.18f + slopeBias * 2.2f + bare * 0.30f) continue

            val size = 0.9f + rng.nextFloat() * 3.4f
            val species = speciesAt(x, z, seed, speciesCount, 41)
            matrix.setTrs(
                x, h - size * 0.18f, z,
                (rng.nextFloat() - 0.5f) * 0.4f,
                rng.nextFloat() * 6.2831855f,
                (rng.nextFloat() - 0.5f) * 0.4f,
                size * (0.9f + rng.nextFloat() * 0.5f),
                size * (0.6f + rng.nextFloat() * 0.5f),
                size * (0.9f + rng.nextFloat() * 0.5f)
            )
            buckets.add(species, matrix)
            placed++
        }

        return buckets.toSets()
    }

    /**
     * Tall grass, confined to noise-driven meadows. [patchThreshold] raises or lowers how much
     * of the ground grows grass; everything outside the meadows stays bare soil or rock.
     */
    fun scatterGrass(
        terrain: Terrain,
        def: WorldDefinition,
        count: Int,
        seed: Int,
        centerX: Float,
        centerZ: Float,
        radius: Float,
        speciesCount: Int = GRASS_SPECIES,
        patchThreshold: Float = 0.44f
    ): List<InstanceSet> {
        if (count <= 0 || speciesCount <= 0) return emptySets(speciesCount)
        val buckets = Buckets(speciesCount, count)
        val rng = Random(seed + 1543)
        val normal = Vec3()
        val matrix = Mat4()
        var placed = 0
        var attempts = 0
        val maxAttempts = count * 8

        while (placed < count && attempts < maxAttempts) {
            attempts++
            val x = centerX + (rng.nextFloat() * 2f - 1f) * radius
            val z = centerZ + (rng.nextFloat() * 2f - 1f) * radius
            val h = terrain.height(x, z)
            if (h < def.waterLevel + 0.4f) continue
            terrain.normal(x, z, normal)
            if (normal.y < 0.82f) continue

            // A broad meadow mask plus two finer break-ups gives the patches ragged edges and
            // clears completely bare ground between them.
            val meadow = Noise.fbm01(x * 0.012f, z * 0.012f, 3, seed + 67)
            val detail = Noise.fbm01(x * 0.05f, z * 0.05f, 2, seed + 71)
            val clumps = Noise.fbm01(x * 0.21f, z * 0.21f, 2, seed + 73)
            val mask = meadow * 0.62f + detail * 0.24f + clumps * 0.14f
            if (mask < patchThreshold) continue
            // Thin the fringe out rather than cutting it off in a straight line.
            if (rng.nextFloat() > ((mask - patchThreshold) / 0.42f).coerceIn(0.08f, 1f)) continue

            // 0.6–1.6 m of blades, so a meadow is uneven instead of a repeated stamp.
            val height = 0.60f + rng.nextFloat() * 1.00f
            val width = 0.42f + rng.nextFloat() * 0.62f
            val species = speciesAt(x, z, seed, speciesCount, 29)
            matrix.setTrs(
                x, h, z,
                0f, rng.nextFloat() * 6.2831855f, 0f,
                width, height, width
            )
            buckets.add(species, matrix)
            placed++
        }

        return buckets.toSets()
    }

    // ---- helpers ----------------------------------------------------------

    private fun emptySets(count: Int): List<InstanceSet> =
        List(count.coerceAtLeast(0)) { InstanceSet.EMPTY }

    /** Conifers take the middle slot, or the last slot in a two-species world. */
    private fun coniferSlot(speciesCount: Int): Int =
        if (speciesCount >= 3) 1 else speciesCount - 1

    /**
     * Species for a position from a low-frequency noise field, so stands form instead of every
     * neighbouring plant being forced to match.
     */
    private fun speciesAt(
        x: Float,
        z: Float,
        seed: Int,
        speciesCount: Int,
        offset: Int
    ): Int {
        if (speciesCount <= 1) return 0
        val field = Noise.fbm01(x * 0.0045f + offset * 0.37f, z * 0.0045f, 2, seed + 61)
        val index = (field.coerceIn(0f, 0.9999f) * speciesCount).toInt()
        return index.coerceIn(0, speciesCount - 1)
    }

    /** Growable per-species matrix buckets. */
    private class Buckets(speciesCount: Int, expected: Int) {
        private val data = Array(speciesCount) {
            FloatArray(maxOf(16, expected / maxOf(1, speciesCount) * 16))
        }
        private val counts = IntArray(speciesCount)

        fun add(species: Int, matrix: Mat4) {
            val index = species.coerceIn(0, data.size - 1)
            val needed = (counts[index] + 1) * 16
            if (needed > data[index].size) {
                data[index] = data[index].copyOf(maxOf(needed, data[index].size * 2))
            }
            System.arraycopy(matrix.m, 0, data[index], counts[index] * 16, 16)
            counts[index]++
        }

        fun toSets(): List<InstanceSet> = data.indices.map { index ->
            if (counts[index] == 0) InstanceSet.EMPTY else InstanceSet(counts[index], data[index])
        }
    }
}
