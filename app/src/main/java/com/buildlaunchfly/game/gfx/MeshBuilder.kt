package com.buildlaunchfly.game.gfx

import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.sin

/**
 * Builds procedural geometry (boxes, cylinders, cones, spheres, quads) with a transform
 * stack so complex objects (aircraft, slingshot, trees) can be composed from primitives.
 *
 * All vertices are transformed on the CPU by the active matrix, and normals use the
 * inverse-transpose so non-uniform scaling stays correct.
 */
class MeshBuilder {

    private val vertices = ArrayList<Float>(4096)
    private val indices = ArrayList<Int>(2048)

    private val stack = ArrayList<Mat4>(8)
    private val transform = Mat4()
    private val normalMatrix = Mat4()

    init {
        updateNormalMatrix()
    }

    fun clear(): MeshBuilder {
        vertices.clear()
        indices.clear()
        stack.clear()
        transform.identity()
        updateNormalMatrix()
        return this
    }

    private fun updateNormalMatrix() {
        transform.normalMatrix(normalMatrix)
    }

    fun push(m: Mat4): MeshBuilder {
        stack.add(transform.copy())
        transform.set(m)
        updateNormalMatrix()
        return this
    }

    fun pushTrs(px: Float, py: Float, pz: Float, rx: Float, ry: Float, rz: Float, scale: Float): MeshBuilder {
        return push(Mat4().setTrs(px, py, pz, rx, ry, rz, scale))
    }

    fun pushTrs(
        px: Float, py: Float, pz: Float,
        rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float
    ): MeshBuilder {
        return push(Mat4().setTrs(px, py, pz, rx, ry, rz, sx, sy, sz))
    }

    fun pop(): MeshBuilder {
        if (stack.isNotEmpty()) {
            transform.set(stack.removeAt(stack.size - 1))
            updateNormalMatrix()
        }
        return this
    }

    private fun vertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        u: Float, v: Float
    ): Int {
        val tm = transform.m
        val px = tm[0] * x + tm[4] * y + tm[8] * z + tm[12]
        val py = tm[1] * x + tm[5] * y + tm[9] * z + tm[13]
        val pz = tm[2] * x + tm[6] * y + tm[10] * z + tm[14]

        val nm = normalMatrix.m
        var tnx = nm[0] * nx + nm[4] * ny + nm[8] * nz
        var tny = nm[1] * nx + nm[5] * ny + nm[9] * nz
        var tnz = nm[2] * nx + nm[6] * ny + nm[10] * nz
        val len = sqrt(tnx * tnx + tny * tny + tnz * tnz)
        if (len > 1e-6f) {
            val inv = 1f / len
            tnx *= inv
            tny *= inv
            tnz *= inv
        } else {
            tnx = 0f
            tny = 1f
            tnz = 0f
        }

        vertices.add(px)
        vertices.add(py)
        vertices.add(pz)
        vertices.add(tnx)
        vertices.add(tny)
        vertices.add(tnz)
        vertices.add(u)
        vertices.add(v)
        return vertices.size / MeshData.VERTEX_FLOATS - 1
    }

    fun addTriangle(a: Int, b: Int, c: Int): MeshBuilder {
        indices.add(a)
        indices.add(b)
        indices.add(c)
        return this
    }

    /** Adds a quad from four corners (in order) with an explicit face normal. */
    fun addFace(
        p0: Vec3, p1: Vec3, p2: Vec3, p3: Vec3,
        nx: Float, ny: Float, nz: Float,
        uvScale: Float = 1f
    ): MeshBuilder {
        val a = vertex(p0.x, p0.y, p0.z, nx, ny, nz, 0f, 0f)
        val b = vertex(p1.x, p1.y, p1.z, nx, ny, nz, uvScale, 0f)
        val c = vertex(p2.x, p2.y, p2.z, nx, ny, nz, uvScale, uvScale)
        val d = vertex(p3.x, p3.y, p3.z, nx, ny, nz, 0f, uvScale)
        addTriangle(a, b, c)
        addTriangle(a, c, d)
        return this
    }

    /** Adds a quad, deriving the normal from the corner order. */
    fun addQuad(p0: Vec3, p1: Vec3, p2: Vec3, p3: Vec3, uvScale: Float = 1f): MeshBuilder {
        val e1x = p1.x - p0.x
        val e1y = p1.y - p0.y
        val e1z = p1.z - p0.z
        val e2x = p2.x - p0.x
        val e2y = p2.y - p0.y
        val e2z = p2.z - p0.z
        var nx = e1y * e2z - e1z * e2y
        var ny = e1z * e2x - e1x * e2z
        var nz = e1x * e2y - e1y * e2x
        val len = sqrt(nx * nx + ny * ny + nz * nz)
        if (len > 1e-6f) {
            nx /= len
            ny /= len
            nz /= len
        } else {
            ny = 1f
        }
        return addFace(p0, p1, p2, p3, nx, ny, nz, uvScale)
    }

    fun addBox(
        cx: Float, cy: Float, cz: Float,
        hx: Float, hy: Float, hz: Float,
        uvScale: Float = 1f
    ): MeshBuilder {
        val x0 = cx - hx
        val x1 = cx + hx
        val y0 = cy - hy
        val y1 = cy + hy
        val z0 = cz - hz
        val z1 = cz + hz

        // +X
        addFace(
            Vec3(x1, y0, z1), Vec3(x1, y0, z0), Vec3(x1, y1, z0), Vec3(x1, y1, z1),
            1f, 0f, 0f, uvScale
        )
        // -X
        addFace(
            Vec3(x0, y0, z0), Vec3(x0, y0, z1), Vec3(x0, y1, z1), Vec3(x0, y1, z0),
            -1f, 0f, 0f, uvScale
        )
        // +Y
        addFace(
            Vec3(x0, y1, z1), Vec3(x1, y1, z1), Vec3(x1, y1, z0), Vec3(x0, y1, z0),
            0f, 1f, 0f, uvScale
        )
        // -Y
        addFace(
            Vec3(x0, y0, z0), Vec3(x1, y0, z0), Vec3(x1, y0, z1), Vec3(x0, y0, z1),
            0f, -1f, 0f, uvScale
        )
        // +Z
        addFace(
            Vec3(x0, y0, z1), Vec3(x1, y0, z1), Vec3(x1, y1, z1), Vec3(x0, y1, z1),
            0f, 0f, 1f, uvScale
        )
        // -Z
        addFace(
            Vec3(x1, y0, z0), Vec3(x0, y0, z0), Vec3(x0, y1, z0), Vec3(x1, y1, z0),
            0f, 0f, -1f, uvScale
        )
        return this
    }

    /** Cylinder aligned with the Y axis, centred at (cx, cy, cz). */
    fun addCylinder(
        cx: Float, cy: Float, cz: Float,
        radiusBottom: Float, radiusTop: Float, height: Float,
        segments: Int, capBottom: Boolean = true, capTop: Boolean = true,
        uvScale: Float = 1f
    ): MeshBuilder {
        val seg = segments.coerceAtLeast(3)
        val halfH = height * 0.5f
        val slopeNy = if (height > 1e-5f) (radiusBottom - radiusTop) / height else 0f

        val ringBottom = IntArray(seg + 1)
        val ringTop = IntArray(seg + 1)

        for (i in 0..seg) {
            val a = (i.toFloat() / seg) * M.TWO_PI
            val ca = cos(a)
            val sa = sin(a)
            var nx = ca
            var ny = slopeNy
            var nz = sa
            val len = sqrt(nx * nx + ny * ny + nz * nz)
            if (len > 1e-6f) {
                nx /= len
                ny /= len
                nz /= len
            }
            val u = i.toFloat() / seg * uvScale
            ringBottom[i] = vertex(
                cx + ca * radiusBottom, cy - halfH, cz + sa * radiusBottom,
                nx, ny, nz, u, uvScale
            )
            ringTop[i] = vertex(
                cx + ca * radiusTop, cy + halfH, cz + sa * radiusTop,
                nx, ny, nz, u, 0f
            )
        }

        for (i in 0 until seg) {
            addTriangle(ringBottom[i], ringTop[i], ringTop[i + 1])
            addTriangle(ringBottom[i], ringTop[i + 1], ringBottom[i + 1])
        }

        if (capBottom && radiusBottom > 1e-4f) {
            val center = vertex(cx, cy - halfH, cz, 0f, -1f, 0f, 0.5f, 0.5f)
            for (i in 0 until seg) {
                val a = (i.toFloat() / seg) * M.TWO_PI
                val b = ((i + 1).toFloat() / seg) * M.TWO_PI
                val v0 = vertex(
                    cx + cos(a) * radiusBottom, cy - halfH, cz + sin(a) * radiusBottom,
                    0f, -1f, 0f, 0.5f + cos(a) * 0.5f, 0.5f + sin(a) * 0.5f
                )
                val v1 = vertex(
                    cx + cos(b) * radiusBottom, cy - halfH, cz + sin(b) * radiusBottom,
                    0f, -1f, 0f, 0.5f + cos(b) * 0.5f, 0.5f + sin(b) * 0.5f
                )
                addTriangle(center, v1, v0)
            }
        }

        if (capTop && radiusTop > 1e-4f) {
            val center = vertex(cx, cy + halfH, cz, 0f, 1f, 0f, 0.5f, 0.5f)
            for (i in 0 until seg) {
                val a = (i.toFloat() / seg) * M.TWO_PI
                val b = ((i + 1).toFloat() / seg) * M.TWO_PI
                val v0 = vertex(
                    cx + cos(a) * radiusTop, cy + halfH, cz + sin(a) * radiusTop,
                    0f, 1f, 0f, 0.5f + cos(a) * 0.5f, 0.5f + sin(a) * 0.5f
                )
                val v1 = vertex(
                    cx + cos(b) * radiusTop, cy + halfH, cz + sin(b) * radiusTop,
                    0f, 1f, 0f, 0.5f + cos(b) * 0.5f, 0.5f + sin(b) * 0.5f
                )
                addTriangle(center, v0, v1)
            }
        }
        return this
    }

    /** Cone pointing up (+Y), base centred at (cx, cy, cz). */
    fun addCone(
        cx: Float, cy: Float, cz: Float,
        radius: Float, height: Float, segments: Int, uvScale: Float = 1f
    ): MeshBuilder {
        return addCylinder(
            cx, cy + height * 0.5f, cz, radius, 0.02f, height, segments,
            capBottom = true, capTop = false, uvScale = uvScale
        )
    }

    /** UV sphere centred at (cx, cy, cz). */
    fun addSphere(
        cx: Float, cy: Float, cz: Float,
        radius: Float, rings: Int = 8, segments: Int = 12,
        uvScale: Float = 1f
    ): MeshBuilder {
        val r = rings.coerceAtLeast(2)
        val s = segments.coerceAtLeast(3)
        val grid = Array(r + 1) { IntArray(s + 1) }

        for (i in 0..r) {
            val phi = (i.toFloat() / r) * M.PI.toFloat()
            val y = cos(phi)
            val sinPhi = sin(phi)
            for (j in 0..s) {
                val theta = (j.toFloat() / s) * M.TWO_PI
                val nx = sinPhi * cos(theta)
                val nz = sinPhi * sin(theta)
                grid[i][j] = vertex(
                    cx + nx * radius, cy + y * radius, cz + nz * radius,
                    nx, y, nz,
                    j.toFloat() / s * uvScale, i.toFloat() / r * uvScale
                )
            }
        }

        for (i in 0 until r) {
            for (j in 0 until s) {
                val a = grid[i][j]
                val b = grid[i][j + 1]
                val c = grid[i + 1][j + 1]
                val d = grid[i + 1][j]
                addTriangle(a, b, c)
                addTriangle(a, c, d)
            }
        }
        return this
    }

    /** Ellipsoid helper implemented as a scaled sphere. */
    fun addEllipsoid(
        cx: Float, cy: Float, cz: Float,
        rx: Float, ry: Float, rz: Float,
        rings: Int = 8, segments: Int = 12,
        uvScale: Float = 1f
    ): MeshBuilder {
        pushTrs(cx, cy, cz, 0f, 0f, 0f, rx, ry, rz)
        addSphere(0f, 0f, 0f, 1f, rings, segments, uvScale)
        pop()
        return this
    }

    /**
     * Adds a thin rod (a cylinder without end caps) between two arbitrary points in the
     * current transform space. Bracing wires, struts and landing-gear legs need a direction
     * the axis-aligned primitives cannot give, so the rod builds its own orthonormal basis
     * with +Y mapped onto the segment.
     */
    fun addRod(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        radius: Float, segments: Int = 6
    ): MeshBuilder {
        val dx = bx - ax
        val dy = by - ay
        val dz = bz - az
        val length = sqrt(dx * dx + dy * dy + dz * dz)
        if (length < 1e-4f) return this

        val ux = dx / length
        val uy = dy / length
        val uz = dz / length

        // Right = world-up x direction, unless the rod is vertical.
        var rx = uz
        var rz = -ux
        val rightLength = sqrt(rx * rx + rz * rz)
        if (rightLength < 1e-4f) {
            rx = 1f
            rz = 0f
        } else {
            rx /= rightLength
            rz /= rightLength
        }

        // Back = right x direction completes a right-handed basis (right's Y is always 0).
        val backX = -rz * uy
        val backY = rz * ux - rx * uz
        val backZ = rx * uy

        val m = Mat4()
        m.m[0] = rx
        m.m[1] = 0f
        m.m[2] = rz
        m.m[3] = 0f
        m.m[4] = ux
        m.m[5] = uy
        m.m[6] = uz
        m.m[7] = 0f
        m.m[8] = backX
        m.m[9] = backY
        m.m[10] = backZ
        m.m[11] = 0f
        m.m[12] = (ax + bx) * 0.5f
        m.m[13] = (ay + by) * 0.5f
        m.m[14] = (az + bz) * 0.5f
        m.m[15] = 1f

        push(m)
        addCylinder(0f, 0f, 0f, radius, radius, length, segments, false, false)
        pop()
        return this
    }

    fun build(): MeshData {
        val vertexArray = FloatArray(vertices.size)
        for (i in vertices.indices) vertexArray[i] = vertices[i]
        val indexArray = IntArray(indices.size)
        for (i in indices.indices) indexArray[i] = indices[i]
        return MeshData(vertexArray, indexArray)
    }
}
