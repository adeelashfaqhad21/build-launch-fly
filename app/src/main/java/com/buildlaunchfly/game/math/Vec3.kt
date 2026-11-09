package com.buildlaunchfly.game.math

/** Mutable 3-component vector. All mutation helpers return `this` for chaining. */
class Vec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {

    fun set(x: Float, y: Float, z: Float): Vec3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(o: Vec3): Vec3 = set(o.x, o.y, o.z)

    fun copy(): Vec3 = Vec3(x, y, z)

    fun add(o: Vec3): Vec3 {
        x += o.x
        y += o.y
        z += o.z
        return this
    }

    fun add(x: Float, y: Float, z: Float): Vec3 {
        this.x += x
        this.y += y
        this.z += z
        return this
    }

    fun addScaled(o: Vec3, s: Float): Vec3 {
        x += o.x * s
        y += o.y * s
        z += o.z * s
        return this
    }

    fun sub(o: Vec3): Vec3 {
        x -= o.x
        y -= o.y
        this.z -= o.z
        return this
    }

    fun scale(s: Float): Vec3 {
        x *= s
        y *= s
        z *= s
        return this
    }

    fun lengthSq(): Float = x * x + y * y + z * z

    fun length(): Float = kotlin.math.sqrt(lengthSq())

    fun horizontalLength(): Float = kotlin.math.sqrt(x * x + z * z)

    fun normalize(): Vec3 {
        val len = length()
        if (len > 1e-6f) {
            val inv = 1f / len
            x *= inv
            y *= inv
            z *= inv
        } else {
            set(0f, 0f, 0f)
        }
        return this
    }

    fun setLength(target: Float): Vec3 {
        val len = length()
        if (len > 1e-6f) scale(target / len) else set(0f, target, 0f)
        return this
    }

    fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z

    /** this = a x b */
    fun cross(a: Vec3, b: Vec3): Vec3 {
        val cx = a.y * b.z - a.z * b.y
        val cy = a.z * b.x - a.x * b.z
        val cz = a.x * b.y - a.y * b.x
        return set(cx, cy, cz)
    }

    /** Linear interpolation from the current value towards [o]. */
    fun lerpTo(o: Vec3, t: Float): Vec3 {
        x += (o.x - x) * t
        y += (o.y - y) * t
        z += (o.z - z) * t
        return this
    }

    override fun toString(): String = "Vec3(%.3f, %.3f, %.3f)".format(x, y, z)

    companion object {
        fun of(x: Float, y: Float, z: Float): Vec3 = Vec3(x, y, z)
        fun zero(): Vec3 = Vec3(0f, 0f, 0f)
        fun up(): Vec3 = Vec3(0f, 1f, 0f)
    }
}
