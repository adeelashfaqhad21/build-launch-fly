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
