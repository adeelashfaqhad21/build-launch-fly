package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.Noise
import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Vec3
import kotlin.math.abs
import kotlin.math.sin

/**
 * Height field for one world. The launch pad is a flattened plateau at the origin and the
 * flight corridor (toward -Z) is gently carved so the first seconds of a flight are clear.
 */
class Terrain(val definition: WorldDefinition) {

    val padHeight: Float = definition.heightOffset + definition.hillHeight * 0.15f + 4f

    val waterLevel: Float get() = definition.waterLevel

    fun baseHeight(x: Float, z: Float): Float {
        val s = definition.seed
        val broad = Noise.fbm2(x * 0.0018f, z * 0.0018f, 4, 2f, 0.5f, s) * definition.hillHeight
        var h = broad

        if (definition.mountainHeight > 0f) {
            val ridge = Noise.ridged2(x * 0.0009f, z * 0.0009f, 3, s + 17)
            val mask = 0.35f + 0.65f * Noise.fbm01(x * 0.0006f, z * 0.0006f, 2, s + 29)
            h += ridge * definition.mountainHeight * mask
        }

        h += Noise.fbm2(x * 0.012f, z * 0.012f, 3, 2f, 0.5f, s + 41) * definition.roughness * 2.2f

        if (definition.duneStrength > 0f) {
            val broadDune = Noise.fbm2(x * 0.0032f, z * 0.0062f, 3, 2f, 0.55f, s + 53)
            val slipFace = sin((z * 0.010f + x * 0.0022f + broadDune * 1.7f).toDouble()).toFloat()
            val ripple = sin((z * 0.085f + x * 0.012f + broadDune * 5f).toDouble()).toFloat()
            h += broadDune * definition.duneStrength + slipFace * definition.duneStrength * 0.28f
            h += ripple * 0.34f
        }

        if (definition.id == WorldId.FOREST || definition.id == WorldId.MOUNTAIN) {
            val riverX = 150f * sin(z * 0.00125f) + 54f * sin(z * 0.0028f + 0.7f)
            val riverDistance = abs(x - riverX)
            val riverWidth = if (definition.id == WorldId.FOREST) 9f else 6.5f
            val riverBed = definition.waterLevel - 0.8f +
                (riverDistance / riverWidth) * (riverDistance / riverWidth) * 4.5f
            val bank = 1f - M.smoothStep(riverWidth, riverWidth * 3.2f, riverDistance)
            h = M.lerp(h, riverBed, bank * 0.94f)
        }

        if (definition.id == WorldId.DESERT) {
            // Broad, flat-topped sandstone mesas with steep eroded escarpments.
            val mesaCenters = arrayOf(
                floatArrayOf(-360f, -520f, 190f, 31f),
                floatArrayOf(310f, -1120f, 245f, 42f),
                floatArrayOf(-120f, -2020f, 175f, 27f),
                floatArrayOf(690f, -2580f, 220f, 36f)
            )
            for (mesa in mesaCenters) {
                val dx = x - mesa[0]
                val dz = z - mesa[1]
                val distance = kotlin.math.sqrt(dx * dx + dz * dz)
                val edgeNoise = Noise.fbm2(x * 0.009f, z * 0.009f, 2, 2f, 0.5f, s + 83) * 16f
                val radius = mesa[2] + edgeNoise
                val plateau = 1f - M.smoothStep(radius * 0.88f, radius, distance)
                val talus = 1f - M.smoothStep(radius, radius * 1.22f, distance)
                val mesaTop = h + mesa[3]
                h = M.lerp(h, mesaTop, plateau)
                h += talus * (1f - plateau) * 3.5f
            }

            // Dry wadi cuts through the dune field, exposing a lower clay wash.
            val wadiX = 95f * sin(z * 0.0018f) + 38f * sin(z * 0.0041f + 1.2f)
            val wadiDistance = abs(x - wadiX)
            val wadiCut = 1f - M.smoothStep(5f, 27f, wadiDistance)
            h -= wadiCut * 5.5f
        }

        if (definition.id == WorldId.SNOW) {
            // Longitudinal glacier crevasses: narrow branching grooves rather than noise bumps.
            val crackWave = abs(sin((x * 0.045f + sin(z * 0.0035f) * 1.6f).toDouble()).toFloat())
            val crackMask = 1f - M.smoothStep(0.015f, 0.105f, crackWave)
            val elevationMask = M.smoothStep(definition.heightOffset + 12f, definition.heightOffset + 42f, h)
            h -= crackMask * elevationMask * 1.15f
        }

        if (definition.id == WorldId.OCEAN) {
            // A continuous, gently curving shoreline: shallow beach shelf, then weathered sea cliffs.
            val shoreline = -285f + 78f * sin(z * 0.00145f) + 24f * sin(z * 0.0031f + 0.8f)
            val inland = x - shoreline
            if (inland in 0f..190f) {
                val beachHeight = definition.waterLevel + 1.2f + (inland / 190f) * 5.8f
                val beachBlend = 1f - M.smoothStep(0f, 190f, inland)
                h = M.lerp(h, beachHeight, beachBlend * 0.92f)
            }
            val cliffBand = M.smoothStep(185f, 260f, inland) *
                (1f - M.smoothStep(520f, 720f, inland))
            val cliffNoise = Noise.fbm2(x * 0.004f, z * 0.006f, 3, 2f, 0.55f, s + 117)
            h += cliffBand * (13f + cliffNoise * 8f)
            // Erosion gullies descend from the cliff top toward the beach.
            val gullyLine = shoreline + 230f + 22f * sin(z * 0.006f + cliffNoise * 2f)
            val gully = abs(x - gullyLine)
            h -= (1f - M.smoothStep(2.5f, 12f, gully)) * cliffBand * 4.2f
        }

        // Shallow valley along the launch corridor.
        val corridor = kotlin.math.exp((-(x * x) / (260f * 260f)).toDouble()).toFloat()
        h -= corridor * 6f

        return h + definition.heightOffset
    }

    fun height(x: Float, z: Float): Float {
        var h = baseHeight(x, z)
        val distSq = x * x + z * z
        if (distSq < 90f * 90f) {
            val distance = kotlin.math.sqrt(distSq)
            val influence = 1f - M.smoothStep(26f, 88f, distance)
            h = M.lerp(h, padHeight, influence)
        }
        return h
    }

    fun normal(x: Float, z: Float, out: Vec3): Vec3 {
        val e = 1.5f
        val hl = height(x - e, z)
        val hr = height(x + e, z)
        val hd = height(x, z - e)
        val hu = height(x, z + e)
        out.set(hl - hr, 2f * e, hd - hu)
        out.normalize()
        return out
    }

    /** Range of heights to place the world's water plane at a sensible level. */
    fun groundAt(x: Float, z: Float): Float = height(x, z)
}
