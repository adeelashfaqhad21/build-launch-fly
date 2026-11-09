package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.MeshData
import kotlin.math.sqrt

/**
 * Builds the whole terrain as one mesh with a non-uniform grid: dense cells inside the
 * flight corridor, geometrically growing cells toward the horizon.
 */
object TerrainMesh {

    fun build(terrain: Terrain): MeshData {
        // The 9 m x 48 m runway begins beyond the hangar roof edge. Add exact edge
        // samples and 1.5 m longitudinal samples at its shifted world-space coordinates
        // so terrain triangles stay below the asphalt deck.
        val xs = mergeSamples(
            axis(-400f, 640f, 8f, -3600f, 5200f, 1.42f),
            floatArrayOf(-HangarMesh.HALF_WIDTH, -5.0f, -4.5f, 4.5f, 5.0f, HangarMesh.HALF_WIDTH)
        )
        val rampZ = FloatArray(33) { index ->
            HangarMesh.RAMP_END_Z + HangarMesh.RAMP_WORLD_OFFSET_Z + index * 1.5f
        }
        val zs = mergeSamples(
            axis(-1900f, 320f, 8f, -5200f, 1600f, 1.42f),
            rampZ + floatArrayOf(-HangarMesh.HALF_LENGTH, HangarMesh.HALF_LENGTH)
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

        // The dedicated asphalt deck is the only surface that should occupy its
        // footprint. Do not merely lower the procedural terrain there: even a lowered
        // terrain sheet remains visible as a second strip through gaps and at shallow
        // viewing angles. Omit the terrain cells under the deck entirely.
        val indices = IntArray((nx - 1) * (nz - 1) * 6)
        var t = 0
        for (j in 0 until nz - 1) {
            val z0 = zs[j]
            val z1 = zs[j + 1]
            for (i in 0 until nx - 1) {
                val x0 = xs[i]
                val x1 = xs[i + 1]
                val underRunway =
                    x0 >= -4.5f && x1 <= 4.5f &&
                    z0 >= HangarMesh.RAMP_END_Z + HangarMesh.RAMP_WORLD_OFFSET_Z &&
                    z1 <= HangarMesh.RAMP_START_Z + HangarMesh.RAMP_WORLD_OFFSET_Z
                // The plank floor is the ground surface inside the shed. Remove the
                // coincident terrain triangles beneath it to prevent depth-buffer flicker
                // and grass/terrain showing through the timber boards.
                val underHangarFloor =
                    x0 >= -HangarMesh.HALF_WIDTH && x1 <= HangarMesh.HALF_WIDTH &&
                    z0 >= -HangarMesh.HALF_LENGTH && z1 <= HangarMesh.HALF_LENGTH
                if (underRunway || underHangarFloor) continue

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

        return MeshData(vertices, indices.copyOf(t))
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
