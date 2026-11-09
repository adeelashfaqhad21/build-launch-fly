package com.buildlaunchfly.game.math

import kotlin.math.cos
import kotlin.math.sin

/**
 * Column-major 4x4 matrix, laid out exactly like an OpenGL uniform matrix so [m] can be
 * uploaded with `glUniformMatrix4fv(..., transpose = false, m, 0)`.
 */
class Mat4 {

    val m = FloatArray(16)

    init {
        identity()
    }

    fun identity(): Mat4 {
        for (i in 0 until 16) {
            m[i] = if (i % 5 == 0) 1f else 0f
        }
        return this
    }

    fun set(o: Mat4): Mat4 {
        System.arraycopy(o.m, 0, m, 0, 16)
        return this
    }

    fun copy(): Mat4 = Mat4().set(this)

    /** this = this * o */
    fun multiply(o: Mat4): Mat4 {
        val a = m.copyOf()
        val b = o.m
        for (c in 0 until 4) {
            for (r in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += a[k * 4 + r] * b[c * 4 + k]
                }
                m[c * 4 + r] = sum
            }
        }
        return this
    }

    /** this = a * b, safe even when `a` or `b` is `this`. */
    fun setProduct(a: Mat4, b: Mat4): Mat4 {
        val am = if (a === this) a.m.copyOf() else a.m
        val bm = if (b === this) b.m.copyOf() else b.m
        for (c in 0 until 4) {
            for (r in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += am[k * 4 + r] * bm[c * 4 + k]
                }
                m[c * 4 + r] = sum
            }
        }
        return this
    }

    /** this = this * T(x, y, z) */
    fun translate(x: Float, y: Float, z: Float): Mat4 {
        val a = m.copyOf()
        for (r in 0 until 4) {
            val row0 = a[r]
            val row1 = a[4 + r]
            val row2 = a[8 + r]
            val row3 = a[12 + r]
            m[r] = row0
            m[4 + r] = row1
            m[8 + r] = row2
            m[12 + r] = row0 * x + row1 * y + row2 * z + row3
        }
        return this
    }

    /** this = this * S(sx, sy, sz) */
    fun scale(sx: Float, sy: Float, sz: Float): Mat4 {
        val a = m.copyOf()
        for (r in 0 until 4) {
            m[r] = a[r] * sx
            m[4 + r] = a[4 + r] * sy
            m[8 + r] = a[8 + r] * sz
            m[12 + r] = a[12 + r]
        }
        return this
    }

    fun scale(s: Float): Mat4 = scale(s, s, s)

    /** this = this * Rx(rad) */
    fun rotateX(rad: Float): Mat4 {
        val c = cos(rad)
        val s = sin(rad)
        mulRotation(
            col0 = floatArrayOf(1f, 0f, 0f, 0f),
            col1 = floatArrayOf(0f, c, s, 0f),
            col2 = floatArrayOf(0f, -s, c, 0f)
        )
        return this
    }

    /** this = this * Ry(rad) */
    fun rotateY(rad: Float): Mat4 {
        val c = cos(rad)
        val s = sin(rad)
        mulRotation(
            col0 = floatArrayOf(c, 0f, -s, 0f),
            col1 = floatArrayOf(0f, 1f, 0f, 0f),
            col2 = floatArrayOf(s, 0f, c, 0f)
        )
        return this
    }

    /** this = this * Rz(rad) */
    fun rotateZ(rad: Float): Mat4 {
        val c = cos(rad)
        val s = sin(rad)
        mulRotation(
            col0 = floatArrayOf(c, s, 0f, 0f),
            col1 = floatArrayOf(-s, c, 0f, 0f),
            col2 = floatArrayOf(0f, 0f, 1f, 0f)
        )
        return this
    }

    private fun mulRotation(col0: FloatArray, col1: FloatArray, col2: FloatArray) {
        val a = m.copyOf()
        for (r in 0 until 4) {
            val a0 = a[r]
            val a1 = a[4 + r]
            val a2 = a[8 + r]
            val a3 = a[12 + r]
            m[r] = a0 * col0[0] + a1 * col0[1] + a2 * col0[2] + a3 * col0[3]
            m[4 + r] = a0 * col1[0] + a1 * col1[1] + a2 * col1[2] + a3 * col1[3]
            m[8 + r] = a0 * col2[0] + a1 * col2[1] + a2 * col2[2] + a3 * col2[3]
        }
    }

    /** Convenience: identity, translate, then Y-X-Z rotation, then uniform scale. */
    fun setTrs(px: Float, py: Float, pz: Float, rx: Float, ry: Float, rz: Float, scale: Float): Mat4 {
        identity()
        translate(px, py, pz)
        rotateY(ry)
        rotateX(rx)
        rotateZ(rz)
        scale(sx = scale, sy = scale, sz = scale)
        return this
    }

    fun setTrs(
        px: Float,
        py: Float,
        pz: Float,
        rx: Float,
        ry: Float,
        rz: Float,
        sx: Float,
        sy: Float,
        sz: Float
    ): Mat4 {
        identity()
        translate(px, py, pz)
        rotateY(ry)
        rotateX(rx)
        rotateZ(rz)
        scale(sx, sy, sz)
        return this
    }

    fun setPerspective(fovYRadians: Float, aspect: Float, near: Float, far: Float): Mat4 {
        identity()
        val f = 1f / kotlin.math.tan(fovYRadians * 0.5f)
        val safeAspect = if (aspect <= 0.0001f) 1f else aspect
        m[0] = f / safeAspect
        m[5] = f
        m[10] = (far + near) / (near - far)
        m[11] = -1f
        m[14] = 2f * far * near / (near - far)
        m[15] = 0f
        return this
    }

    fun setOrtho(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Mat4 {
        identity()
        m[0] = 2f / (right - left)
        m[5] = 2f / (top - bottom)
        m[10] = -2f / (far - near)
        m[12] = -(right + left) / (right - left)
        m[13] = -(top + bottom) / (top - bottom)
        m[14] = -(far + near) / (far - near)
        return this
    }

    fun setLookAt(eye: Vec3, center: Vec3, up: Vec3): Mat4 {
        val fx = center.x - eye.x
        val fy = center.y - eye.y
        val fz = center.z - eye.z
        val fLen = kotlin.math.sqrt(fx * fx + fy * fy + fz * fz)
        val fn = if (fLen > 1e-6f) 1f / fLen else 0f
        val ffx = fx * fn
        val ffy = fy * fn
        val ffz = fz * fn

        // s = normalize(f x up)
        var sx = ffy * up.z - ffz * up.y
        var sy = ffz * up.x - ffx * up.z
        var sz = ffx * up.y - ffy * up.x
        val sLen = kotlin.math.sqrt(sx * sx + sy * sy + sz * sz)
        val sn = if (sLen > 1e-6f) 1f / sLen else 0f
        sx *= sn
        sy *= sn
        sz *= sn

        // u = s x f
        val ux = sy * ffz - sz * ffy
        val uy = sz * ffx - sx * ffz
        val uz = sx * ffy - sy * ffx

        m[0] = sx
        m[1] = ux
        m[2] = -ffx
        m[3] = 0f
        m[4] = sy
        m[5] = uy
        m[6] = -ffy
        m[7] = 0f
        m[8] = sz
        m[9] = uz
        m[10] = -ffz
        m[11] = 0f
        m[12] = -(sx * eye.x + sy * eye.y + sz * eye.z)
        m[13] = -(ux * eye.x + uy * eye.y + uz * eye.z)
        m[14] = ffx * eye.x + ffy * eye.y + ffz * eye.z
        m[15] = 1f
        return this
    }

    fun transformPoint(v: Vec3): Vec3 {
        val px = m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12]
        val py = m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13]
        val pz = m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14]
        v.x = px
        v.y = py
        v.z = pz
        return v
    }

    fun transformDirection(v: Vec3): Vec3 {
        val px = m[0] * v.x + m[4] * v.y + m[8] * v.z
        val py = m[1] * v.x + m[5] * v.y + m[9] * v.z
        val pz = m[2] * v.x + m[6] * v.y + m[10] * v.z
        v.x = px
        v.y = py
        v.z = pz
        return v
    }

    fun invert(): Mat4 {
        val src = m.copyOf()
        val inv = FloatArray(16)

        inv[0] = src[5] * src[10] * src[15] - src[5] * src[11] * src[14] -
            src[9] * src[6] * src[15] + src[9] * src[7] * src[14] +
            src[13] * src[6] * src[11] - src[13] * src[7] * src[10]
        inv[4] = -src[4] * src[10] * src[15] + src[4] * src[11] * src[14] +
            src[8] * src[6] * src[15] - src[8] * src[7] * src[14] -
            src[12] * src[6] * src[11] + src[12] * src[7] * src[10]
        inv[8] = src[4] * src[9] * src[15] - src[4] * src[11] * src[13] -
            src[8] * src[5] * src[15] + src[8] * src[7] * src[13] +
            src[12] * src[5] * src[11] - src[12] * src[7] * src[9]
        inv[12] = -src[4] * src[9] * src[14] + src[4] * src[10] * src[13] +
            src[8] * src[5] * src[14] - src[8] * src[6] * src[13] -
            src[12] * src[5] * src[10] + src[12] * src[6] * src[9]
        inv[1] = -src[1] * src[10] * src[15] + src[1] * src[11] * src[14] +
            src[9] * src[2] * src[15] - src[9] * src[3] * src[14] -
            src[13] * src[2] * src[11] + src[13] * src[3] * src[10]
        inv[5] = src[0] * src[10] * src[15] - src[0] * src[11] * src[14] -
            src[8] * src[2] * src[15] + src[8] * src[3] * src[14] +
            src[12] * src[2] * src[11] - src[12] * src[3] * src[10]
        inv[9] = -src[0] * src[9] * src[15] + src[0] * src[11] * src[13] +
            src[8] * src[1] * src[15] - src[8] * src[3] * src[13] -
            src[12] * src[1] * src[11] + src[12] * src[3] * src[9]
        inv[13] = src[0] * src[9] * src[14] - src[0] * src[10] * src[13] -
            src[8] * src[1] * src[14] + src[8] * src[2] * src[13] +
            src[12] * src[1] * src[10] - src[12] * src[2] * src[9]
        inv[2] = src[1] * src[6] * src[15] - src[1] * src[7] * src[14] -
            src[5] * src[2] * src[15] + src[5] * src[3] * src[14] +
            src[13] * src[2] * src[7] - src[13] * src[3] * src[6]
        inv[6] = -src[0] * src[6] * src[15] + src[0] * src[7] * src[14] +
            src[4] * src[2] * src[15] - src[4] * src[3] * src[14] -
            src[12] * src[2] * src[7] + src[12] * src[3] * src[6]
        inv[10] = src[0] * src[5] * src[15] - src[0] * src[7] * src[13] -
            src[4] * src[1] * src[15] + src[4] * src[3] * src[13] +
            src[12] * src[1] * src[7] - src[12] * src[3] * src[5]
        inv[14] = -src[0] * src[5] * src[14] + src[0] * src[6] * src[13] +
            src[4] * src[1] * src[14] - src[4] * src[2] * src[13] -
            src[12] * src[1] * src[6] + src[12] * src[2] * src[5]
        inv[3] = -src[1] * src[6] * src[11] + src[1] * src[7] * src[10] +
            src[5] * src[2] * src[11] - src[5] * src[3] * src[10] -
            src[9] * src[2] * src[7] + src[9] * src[3] * src[6]
        inv[7] = src[0] * src[6] * src[11] - src[0] * src[7] * src[10] -
            src[4] * src[2] * src[11] + src[4] * src[3] * src[10] +
            src[8] * src[2] * src[7] - src[8] * src[3] * src[6]
        inv[11] = -src[0] * src[5] * src[11] + src[0] * src[7] * src[9] +
            src[4] * src[1] * src[11] - src[4] * src[3] * src[9] -
            src[8] * src[1] * src[7] + src[8] * src[3] * src[5]
        inv[15] = src[0] * src[5] * src[10] - src[0] * src[6] * src[9] -
            src[4] * src[1] * src[10] + src[4] * src[2] * src[9] +
            src[8] * src[1] * src[6] - src[8] * src[2] * src[5]

        var det = src[0] * inv[0] + src[1] * inv[4] + src[2] * inv[8] + src[3] * inv[12]
        if (det == 0f || !det.isFinite()) {
            return identity()
        }
        det = 1f / det
        for (i in 0 until 16) {
            m[i] = inv[i] * det
        }
        return this
    }

    fun transpose(): Mat4 {
        for (c in 0 until 4) {
            for (r in c + 1 until 4) {
                val a = m[c * 4 + r]
                m[c * 4 + r] = m[r * 4 + c]
                m[r * 4 + c] = a
            }
        }
        return this
    }

    /** out = inverse-transpose of this, suitable as a normal matrix (uploaded as mat4). */
    fun normalMatrix(out: Mat4): Mat4 {
        out.set(this)
        out.invert()
        out.transpose()
        return out
    }
}
