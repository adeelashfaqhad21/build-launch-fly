package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.MeshData
import kotlin.math.sqrt

/**
 * Builds the whole terrain as one mesh with a non-uniform grid: dense cells inside the
 * flight corridor, geometrically growing cells toward the horizon.
 */
object TerrainMesh {

    fun build(terrain: Terrain, detailLevel: Int = 2): MeshData {
        // Keep the playable near field at full 8m sampling on every preset. Only the
        // far-field grid spacing changes: power-saver devices get fewer distant vertices,
        // while high-end devices retain more horizon relief.
        val farGrowth = when {
            detailLevel <= 0 -> 1.68f
            detailLevel == 1 -> 1.54f
            detailLevel == 2 -> 1.42f
            else -> 1.34f
        }
        val xs = axis(-400f, 640f, 8f, -3600f, 5200f, farGrowth)
        val zs = axis(-1900f, 320f, 8f, -5200f, 1600f, farGrowth)

        val nx = xs.size
        val nz = zs.size
        if (nx < 2 || nz < 2) {
            return MeshData(FloatArray(0), IntArray(0))
        }

        val heights = FloatArray(nx * nz)
        for (j in 0 until nz) {
            val z = zs[j]
            val row = j * nx
            for (i in 0 until nx) {
                heights[row + i] = terrain.height(xs[i], z)
            }
        }

        val vertices = FloatArray(nx * nz * MeshData.VERTEX_FLOATS)
        var v = 0
        for (j in 0 until nz) {
            for (i in 0 until nx) {
                val x = xs[i]
                val z = zs[j]
                val h = heights[j * nx + i]

                val iPrev = if (i > 0) i - 1 else i
                val iNext = if (i < nx - 1) i + 1 else i
                val jPrev = if (j > 0) j - 1 else j
                val jNext = if (j < nz - 1) j + 1 else j

                val hl = heights[j * nx + iPrev]
                val hr = heights[j * nx + iNext]
                val hd = heights[jPrev * nx + i]
                val hu = heights[jNext * nx + i]

                val dx = (xs[iNext] - xs[iPrev]).coerceAtLeast(0.001f)
                val dz = (zs[jNext] - zs[jPrev]).coerceAtLeast(0.001f)

                var nX = (hl - hr) / dx
                var nY = 1f
                var nZ = (hd - hu) / dz
                val len = sqrt(nX * nX + nY * nY + nZ * nZ)
                if (len > 1e-6f) {
                    nX /= len
                    nY /= len
                    nZ /= len
                }

                vertices[v++] = x
                vertices[v++] = h
                vertices[v++] = z
                vertices[v++] = nX
                vertices[v++] = nY
                vertices[v++] = nZ
                vertices[v++] = x / 8f
                vertices[v++] = z / 8f
            }
        }

        val indexCount = (nx - 1) * (nz - 1) * 6
        val indices = IntArray(indexCount)
        var t = 0
        for (j in 0 until nz - 1) {
            for (i in 0 until nx - 1) {
                val a = j * nx + i
                val b = j * nx + i + 1
                val c = (j + 1) * nx + i + 1
                val d = (j + 1) * nx + i
                indices[t++] = a
                indices[t++] = b
                indices[t++] = c
                indices[t++] = a
                indices[t++] = c
                indices[t++] = d
            }
        }

        return MeshData(vertices, indices)
    }

    /**
     * Ascending axis samples: a uniform run between [innerMin] and [innerMax], then
     * geometrically growing steps out to [outerMin] / [outerMax].
     */
    private fun axis(
        innerMin: Float,
        innerMax: Float,
        step: Float,
        outerMin: Float,
        outerMax: Float,
        growth: Float
    ): FloatArray {
        val positive = ArrayList<Float>(256)
        var v = innerMin
        while (v <= innerMax + 0.01f) {
            positive.add(v)
            v += step
        }

        var stepSize = step
        var p = innerMax
        var guard = 0
        while (p < outerMax - 0.5f && guard < 64) {
            stepSize *= growth
            p += stepSize
            if (p > outerMax) p = outerMax
            positive.add(p)
            guard++
        }

        val negative = ArrayList<Float>(64)
        stepSize = step
        var q = innerMin
        guard = 0
        while (q > outerMin + 0.5f && guard < 64) {
            stepSize *= growth
            q -= stepSize
            if (q < outerMin) q = outerMin
            negative.add(q)
            guard++
        }

        val result = FloatArray(negative.size + positive.size)
        var idx = 0
        for (i in negative.indices.reversed()) {
            result[idx++] = negative[i]
        }
        for (item in positive) {
            result[idx++] = item
        }
        return result
    }
}
