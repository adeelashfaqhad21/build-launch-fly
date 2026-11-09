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
        private val center: com.buildlaunchfly.game.math.Vec3,
        private val seed: Int
    ) {
        private val matrices = FloatArray(count.coerceAtLeast(1) * 16)
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
            val rng = kotlin.random.Random(seed)
            for (i in 0 until count) {
                angle[i] = rng.nextFloat() * 6.2831855f
                radius[i] = 60f + rng.nextFloat() * 180f
                heightOffset[i] = 25f + rng.nextFloat() * 55f
                speed[i] = (0.08f + rng.nextFloat() * 0.16f) * (if (rng.nextBoolean()) 1f else -1f)
                phase[i] = rng.nextFloat() * 6.2831855f
                scale[i] = 0.9f + rng.nextFloat() * 0.9f
            }
        }

        fun update(dt: Float, time: Float) {
            for (i in 0 until count) {
                angle[i] += speed[i] * dt
                val a = angle[i]
                val x = center.x + cos(a) * radius[i]
                val z = center.z + sin(a) * radius[i]
                val y = center.y + heightOffset[i] + sin(time * 0.55f + phase[i]) * 3.5f
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
        private val center: com.buildlaunchfly.game.math.Vec3,
        private val seed: Int,
        private val isExcluded: (Float, Float) -> Boolean = { _, _ -> false }
    ) {
        private val matrices = FloatArray(count.coerceAtLeast(1) * 16)
        private val angle = FloatArray(count)
        private val radius = FloatArray(count)
        private val speed = FloatArray(count)
        private val scale = FloatArray(count)
        private val phase = FloatArray(count)
        private val matrix = Mat4()

        var instanceCount: Int = count
            private set

        init {
            val rng = kotlin.random.Random(seed + 601)
            for (i in 0 until count) {
                angle[i] = rng.nextFloat() * 6.2831855f
                radius[i] = 12f + rng.nextFloat() * 58f
                speed[i] = (0.05f + rng.nextFloat() * 0.12f) * (if (rng.nextBoolean()) 1f else -1f)
                scale[i] = 0.8f + rng.nextFloat() * 0.7f
                phase[i] = rng.nextFloat() * 6.2831855f
            }
        }

        fun update(dt: Float, time: Float, terrain: Terrain) {
            for (i in 0 until count) {
                // Animals pause and graze intermittently.
                val graze = (sin(time * 0.31f + phase[i]) > 0.55f)
                if (!graze) angle[i] += speed[i] * dt
                var a = angle[i]
                var x = center.x + cos(a) * radius[i]
                var z = center.z + sin(a) * radius[i]
                // Ground wildlife is world dressing, so it must respect the same reserved
                // launch-facility exclusion as vegetation and imported props.
                var exclusionAttempts = 0
                while (isExcluded(x, z) && exclusionAttempts < 24) {
                    a += if (speed[i] >= 0f) 0.25f else -0.25f
                    x = center.x + cos(a) * radius[i]
                    z = center.z + sin(a) * radius[i]
                    exclusionAttempts++
                }
                angle[i] = a
                val y = terrain.height(x, z)
                val yaw = if (graze) a + 1.5707964f else a + 1.5707964f
                matrix.setTrs(x, y, z, 0f, yaw, 0f, scale[i], scale[i], scale[i])
                System.arraycopy(matrix.m, 0, matrices, i * 16, 16)
            }
        }

        fun matrices(): FloatArray = matrices
    }
}
