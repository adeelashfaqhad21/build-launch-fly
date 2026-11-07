package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.math.Mat4
import kotlin.math.cos
import kotlin.math.sin

/**
 * Birds and ground animals. Both are instanced procedural meshes with simple but readable
 * motion so the worlds feel inhabited.
 */
object Wildlife {

    class Birds(
        private val count: Int,
        private val terrain: Terrain,
        private val definition: WorldDefinition
    ) {
        private val matrices = FloatArray(count.coerceAtLeast(1) * 16)
        private val homeX = FloatArray(count)
        private val homeZ = FloatArray(count)
        private val angle = FloatArray(count)
        private val radius = FloatArray(count)
        private val heightOffset = FloatArray(count)
        private val speed = FloatArray(count)
        private val phase = FloatArray(count)
        private val scale = FloatArray(count)
        private val matrix = Mat4()

        var instanceCount: Int = count
            private set

        init {
            val rng = kotlin.random.Random(definition.seed + 307)
            for (i in 0 until count) {
                val z = -2550f + rng.nextFloat() * 2700f
                val shoreline = -285f + 78f * sin(z * 0.00145f) +
                    24f * sin(z * 0.0031f + 0.8f)
                val x = when (definition.id) {
                    WorldId.OCEAN -> shoreline - 110f + rng.nextFloat() * 250f
                    WorldId.DESERT -> -500f + rng.nextFloat() * 1200f
                    else -> -720f + rng.nextFloat() * 1500f
                }
                homeX[i] = x
                homeZ[i] = z
                angle[i] = rng.nextFloat() * 6.2831855f
                radius[i] = 18f + rng.nextFloat() * 68f
                heightOffset[i] = when (definition.id) {
                    WorldId.OCEAN -> 18f + rng.nextFloat() * 38f
                    WorldId.SNOW -> 35f + rng.nextFloat() * 75f
                    WorldId.MOUNTAIN -> 45f + rng.nextFloat() * 100f
                    WorldId.DESERT -> 35f + rng.nextFloat() * 85f
                    WorldId.FOREST -> 24f + rng.nextFloat() * 62f
                }
                speed[i] = (0.045f + rng.nextFloat() * 0.11f) *
                    (if (rng.nextBoolean()) 1f else -1f)
                phase[i] = rng.nextFloat() * 6.2831855f
                scale[i] = 0.9f + rng.nextFloat() * 0.9f
            }
        }

        fun update(dt: Float, time: Float) {
            for (i in 0 until count) {
                angle[i] += speed[i] * dt
                val a = angle[i]
                val x = homeX[i] + cos(a) * radius[i]
                val z = homeZ[i] + sin(a) * radius[i]
                val ground = terrain.height(homeX[i], homeZ[i])
                val y = ground + heightOffset[i] + sin(time * 0.55f + phase[i]) * 3.5f
                val yaw = a + 1.5707964f
                val roll = if (speed[i] >= 0f) -0.22f else 0.22f
                matrix.setTrs(x, y, z, 0f, yaw, roll, scale[i], scale[i], scale[i])
                System.arraycopy(matrix.m, 0, matrices, i * 16, 16)
            }
        }

        fun matrices(): FloatArray = matrices
    }

    class GroundAnimals(
        private val count: Int,
        private val terrain: Terrain,
        private val definition: WorldDefinition
    ) {
        private val matrices = FloatArray(count.coerceAtLeast(1) * 16)
        private val homeX = FloatArray(count)
        private val homeZ = FloatArray(count)
        private val angle = FloatArray(count)
        private val radius = FloatArray(count)
        private val speed = FloatArray(count)
        private val scale = FloatArray(count)
        private val phase = FloatArray(count)
        private val matrix = Mat4()

        var instanceCount: Int = count
            private set

        init {
            val rng = kotlin.random.Random(definition.seed + 601)
            val normal = com.buildlaunchfly.game.math.Vec3()
            for (i in 0 until count) {
                var x = 0f
                var z = -90f
                var found = false
                // Place wildlife in the biome's actual habitat, spread along the playable
                // landscape rather than orbiting in a small ring around the launch pad.
                repeat(180) {
                    if (found) return@repeat
                    val candidateX = -760f + rng.nextFloat() * 1660f
                    val candidateZ = -2600f + rng.nextFloat() * 2800f
                    val ground = terrain.height(candidateX, candidateZ)
                    terrain.normal(candidateX, candidateZ, normal)
                    val relativeHeight = ground - definition.heightOffset
                    val suitable = when (definition.id) {
                        WorldId.FOREST ->
                            ground > definition.waterLevel + 1.5f && normal.y > 0.82f
                        WorldId.DESERT -> {
                            val wadiX = 95f * kotlin.math.sin(candidateZ * 0.0018f) +
                                38f * kotlin.math.sin(candidateZ * 0.0041f + 1.2f)
                            ground > definition.waterLevel + 1f && normal.y > 0.78f &&
                                kotlin.math.abs(candidateX - wadiX) < 190f
                        }
                        WorldId.SNOW ->
                            relativeHeight in 2f..82f && normal.y > 0.82f &&
                                !terrain.isCrevasseZone(candidateX, candidateZ)
                        WorldId.OCEAN -> {
                            val shoreline = -285f + 78f * kotlin.math.sin(candidateZ * 0.00145f) +
                                24f * kotlin.math.sin(candidateZ * 0.0031f + 0.8f)
                            val inland = candidateX - shoreline
                            inland in 3f..62f && ground > definition.waterLevel + 0.4f &&
                                normal.y > 0.78f
                        }
                        WorldId.MOUNTAIN ->
                            relativeHeight in 16f..112f && normal.y in 0.68f..0.96f
                    }
                    if (suitable) {
                        x = candidateX
                        z = candidateZ
                        found = true
                    }
                }
                // Sparse fallback only protects unusual terrain seeds from uninitialised
                // coordinates; it is not the normal habitat placement path.
                if (!found) {
                    x = 0f
                    z = -90f - i * 12f
                }
                homeX[i] = x
                homeZ[i] = z
                angle[i] = rng.nextFloat() * 6.2831855f
                radius[i] = 2f + rng.nextFloat() * 9f
                speed[i] = (0.025f + rng.nextFloat() * 0.065f) *
                    (if (rng.nextBoolean()) 1f else -1f)
                scale[i] = 0.8f + rng.nextFloat() * 0.7f
                phase[i] = rng.nextFloat() * 6.2831855f
            }
        }

        fun update(dt: Float, time: Float, terrain: Terrain) {
            for (i in 0 until count) {
                // Animals pause and graze intermittently, then wander within their habitat.
                val graze = sin(time * 0.31f + phase[i]) > 0.55f
                if (!graze) angle[i] += speed[i] * dt
                val a = angle[i]
                val x = homeX[i] + cos(a) * radius[i]
                val z = homeZ[i] + sin(a) * radius[i]
                val y = terrain.height(x, z)
                val yaw = a + 1.5707964f
                matrix.setTrs(x, y, z, 0f, yaw, 0f, scale[i], scale[i], scale[i])
                System.arraycopy(matrix.m, 0, matrices, i * 16, 16)
            }
        }

        fun matrices(): FloatArray = matrices
    }
}
