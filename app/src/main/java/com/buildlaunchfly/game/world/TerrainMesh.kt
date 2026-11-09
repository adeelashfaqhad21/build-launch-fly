package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.MeshData
import kotlin.math.sqrt

/**
 * Builds the whole terrain as one mesh with a non-uniform grid: dense cells inside the
 * flight corridor, geometrically growing cells toward the horizon.
 */
object TerrainMesh {

    fun build(terrain: Terrain): MeshData {
        // The runway is only 9 m wide and 48 m long. The broad 8 m terrain grid
        // cannot represent its edges or its curved clearance reliably; add exact edge
        // samples and 1.5 m longitudinal samples so terrain triangles stay below asphalt.
        val xs = mergeSamples(
            axis(-400f, 640f, 8f, -3600f, 5200f, 1.42f),
            floatArrayOf(-5.0f, -4.5f, 4.5f, 5.0f)
        )
        val rampZ = FloatArray(33) { index -> -48f + index * 1.5f }
        val zs = mergeSamples(
            axis(-1900f, 320f, 8f, -5200f, 1600f, 1.42f),
            rampZ
        )

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

    /** Merge exact local-detail samples into the broad terrain grid without duplicates. */
    private fun mergeSamples(base: FloatArray, extra: FloatArray): FloatArray =
        (base.asList() + extra.asList()).distinct().sorted().toFloatArray()

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
