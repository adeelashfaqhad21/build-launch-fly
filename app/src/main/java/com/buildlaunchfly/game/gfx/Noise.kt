package com.buildlaunchfly.game.gfx

import com.buildlaunchfly.game.math.M
import kotlin.math.abs
import kotlin.math.floor

/** Deterministic value-noise / FBM used for terrain shaping and procedural textures. */
object Noise {

    /** Hash in [-1, 1]. */
    fun hash2(x: Int, y: Int, seed: Int): Float = M.hash01(x, y, seed) * 2f - 1f

    private fun fade(t: Float): Float = t * t * (3f - 2f * t)

    /** Smoothly interpolated value noise in [-1, 1]. */
    fun value2(x: Float, y: Float, seed: Int): Float {
        val xi = floor(x)
        val yi = floor(y)
        val xf = x - xi
        val yf = y - yi
        val ix = xi.toInt()
        val iy = yi.toInt()

        val v00 = hash2(ix, iy, seed)
        val v10 = hash2(ix + 1, iy, seed)
        val v01 = hash2(ix, iy + 1, seed)
        val v11 = hash2(ix + 1, iy + 1, seed)

        val u = fade(xf)
        val v = fade(yf)

        val a = v00 + (v10 - v00) * u
        val b = v01 + (v11 - v01) * u
        return a + (b - a) * v
    }

    fun fbm2(
        x: Float,
        y: Float,
        octaves: Int = 4,
        lacunarity: Float = 2.0f,
        gain: Float = 0.5f,
        seed: Int = 0
    ): Float {
        var amplitude = 1f
        var frequency = 1f
        var sum = 0f
        var norm = 0f
        for (i in 0 until octaves) {
            sum += value2(x * frequency, y * frequency, seed + i * 131) * amplitude
            norm += amplitude
            amplitude *= gain
            frequency *= lacunarity
        }
        return if (norm > 1e-6f) sum / norm else 0f
    }

    /** Ridged multifractal in [-1, 1] — good for mountain ridges. */
    fun ridged2(x: Float, y: Float, octaves: Int = 3, seed: Int = 0): Float {
        var amplitude = 1f
        var frequency = 1f
        var sum = 0f
        var norm = 0f
        for (i in 0 until octaves) {
            val n = 1f - abs(value2(x * frequency, y * frequency, seed + i * 277))
            sum += (n * 2f - 1f) * amplitude
            norm += amplitude
            amplitude *= 0.5f
            frequency *= 2.1f
        }
        return if (norm > 1e-6f) sum / norm else 0f
    }

    /** Convenience wrapper returning [0, 1]. */
    fun fbm01(x: Float, y: Float, octaves: Int, seed: Int): Float {
        return (fbm2(x, y, octaves, 2f, 0.5f, seed) * 0.5f + 0.5f).coerceIn(0f, 1f)
    }
}
