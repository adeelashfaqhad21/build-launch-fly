package com.buildlaunchfly.game.math

import kotlin.math.abs
import kotlin.math.sign

/** Small math helpers shared by the whole engine. */
object M {

    const val PI: Double = 3.14159265358979323846
    const val TWO_PI: Float = 6.2831855f
    const val HALF_PI: Float = 1.5707964f

    /** Degrees to radians. */
    fun deg(degrees: Float): Float = (degrees * 0.017453292519943295).toFloat()

    /** Radians to degrees. */
    fun radToDeg(radians: Float): Float = (radians * 57.29577951308232).toFloat()

    fun clamp(v: Float, lo: Float, hi: Float): Float = when {
        v < lo -> lo
        v > hi -> hi
        else -> v
    }

    fun saturate(v: Float): Float = clamp(v, 0f, 1f)

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        if (edge0 == edge1) return if (x < edge0) 0f else 1f
        val t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun approach(current: Float, target: Float, maxDelta: Float): Float {
        val diff = target - current
        return if (abs(diff) <= maxDelta) target else current + maxDelta * sign(diff)
    }

    /** Wraps an angle into [-PI, PI]. */
    fun wrapAngle(radians: Float): Float {
        var a = radians
        while (a > HALF_PI * 2f) a -= TWO_PI
        while (a < -HALF_PI * 2f) a += TWO_PI
        return a
    }

    fun easeInOutCubic(t: Float): Float {
        val x = saturate(t)
        return if (x < 0.5f) {
            4f * x * x * x
        } else {
            val f = -2f * x + 2f
            1f - f * f * f / 2f
        }
    }

    /** Deterministic hash in [0, 1). */
    fun hash01(value: Int): Float {
        var h = value * 374761393 + 668265263
        h = (h xor (h shr 13)) * 1274126177
        h = h xor (h shr 16)
        return (h and 0x7FFFFFFF).toFloat() / 2147483647f
    }

    /** Deterministic hash of two integers in [0, 1). */
    fun hash01(x: Int, y: Int, seed: Int): Float {
        return hash01((x * 73856093) xor (y * 19349663) xor (seed * 83492791))
    }
}
