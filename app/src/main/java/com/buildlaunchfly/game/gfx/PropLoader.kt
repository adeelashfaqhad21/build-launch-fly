package com.buildlaunchfly.game.gfx

import android.content.res.AssetManager
import android.util.Log

/**
 * Reads a single-texture OBJ (the fantasy RTS buildings and mountains): all faces go into one
 * mesh, the PNG next to the OBJ is the albedo, and the result is normalised to a target height
 * with its base at y = 0 and its centre on the origin so a placement matrix only has to put it
 * on the ground.
 */
object PropLoader {

    private const val TAG = "BLF-PROPS"

    fun load(assets: AssetManager, objPath: String, heightMeters: Float): MeshData? = try {
        assets.open(objPath).bufferedReader().use { reader ->
            parseLines(reader.lineSequence(), heightMeters)
        }
    } catch (error: Throwable) {
        Log.e(TAG, "Failed to load prop $objPath", error)
        null
    }

    /** Same parser for OBJ text held in memory (props read out of a zip archive). */
    fun loadFromText(text: String, label: String, heightMeters: Float): MeshData? = try {
        parseLines(text.lineSequence(), heightMeters)
    } catch (error: Throwable) {
        Log.e(TAG, "Failed to parse prop $label", error)
        null
    }

    private fun parseLines(lines: Sequence<String>, heightMeters: Float): MeshData? {
        val positions = ArrayList<Float>(8192)
        val texCoords = ArrayList<Float>(8192)
        val normals = ArrayList<Float>(8192)
        val vertices = FloatList()
        val indices = IntList()

        for (line in lines) {
            when {
                line.startsWith("v ") -> {
                    val p = line.trim().split(WHITESPACE)
                    if (p.size >= 4) {
                        positions.add(p[1].toFloatOrNull() ?: 0f)
                        positions.add(p[2].toFloatOrNull() ?: 0f)
                        positions.add(p[3].toFloatOrNull() ?: 0f)
                    }
                }
                line.startsWith("vt ") -> {
                    val p = line.trim().split(WHITESPACE)
                    if (p.size >= 2) {
                        texCoords.add(p[1].toFloatOrNull() ?: 0f)
                        texCoords.add(if (p.size >= 3) p[2].toFloatOrNull() ?: 0f else 0f)
                    }
                }
                line.startsWith("vn ") -> {
                    val p = line.trim().split(WHITESPACE)
                    if (p.size >= 4) {
                        normals.add(p[1].toFloatOrNull() ?: 0f)
                        normals.add(p[2].toFloatOrNull() ?: 0f)
                        normals.add(p[3].toFloatOrNull() ?: 0f)
                    }
                }
                line.startsWith("f ") -> addFace(line, positions, texCoords, normals, vertices, indices)
            }
        }

        if (vertices.size == 0) return null
        normalise(vertices, heightMeters)

        val vertexArray = FloatArray(vertices.size)
        System.arraycopy(vertices.data(), 0, vertexArray, 0, vertices.size)
        return MeshData(vertexArray, indices.toArray())
    }

    private fun addFace(
        line: String,
        positions: ArrayList<Float>,
        texCoords: ArrayList<Float>,
        normals: ArrayList<Float>,
        vertices: FloatList,
        indices: IntList
    ) {
        val tokens = line.trim().split(WHITESPACE)
        val corners = tokens.size - 1
        if (corners < 3) return

        val vertexCount = positions.size / 3
        val uvCount = texCoords.size / 2
        val normalCount = normals.size / 3

        val v = IntArray(corners)
        val t = IntArray(corners)
        val n = IntArray(corners)
        for (i in 0 until corners) {
            val c = tokens[i + 1].split('/')
            v[i] = resolve(c.getOrNull(0), vertexCount)
            t[i] = if (c.size > 1) resolve(c[1], uvCount) else -1
            n[i] = if (c.size > 2) resolve(c[2], normalCount) else -1
        }

        for (i in 1 until corners - 1) {
            emit(vertices, indices, positions, texCoords, normals, v[0], t[0], n[0])
            emit(vertices, indices, positions, texCoords, normals, v[i], t[i], n[i])
            emit(vertices, indices, positions, texCoords, normals, v[i + 1], t[i + 1], n[i + 1])
        }
    }

    private fun emit(
        vertices: FloatList,
        indices: IntList,
        positions: ArrayList<Float>,
        texCoords: ArrayList<Float>,
        normals: ArrayList<Float>,
        vertexIndex: Int,
        uvIndex: Int,
        normalIndex: Int
    ) {
        if (vertexIndex < 0 || vertexIndex * 3 + 2 >= positions.size) return
        val index = vertices.size / 8
        vertices.add(positions[vertexIndex * 3])
        vertices.add(positions[vertexIndex * 3 + 1])
        vertices.add(positions[vertexIndex * 3 + 2])
        if (uvIndex >= 0 && uvIndex * 2 + 1 < texCoords.size) {
            vertices.add(texCoords[uvIndex * 2])
            vertices.add(1f - texCoords[uvIndex * 2 + 1])
        } else {
            vertices.add(0f)
            vertices.add(0f)
        }
        if (normalIndex >= 0 && normalIndex * 3 + 2 < normals.size) {
            vertices.add(normals[normalIndex * 3])
            vertices.add(normals[normalIndex * 3 + 1])
            vertices.add(normals[normalIndex * 3 + 2])
        } else {
            vertices.add(0f)
            vertices.add(1f)
            vertices.add(0f)
        }
        indices.add(index)
    }

    private fun resolve(token: String?, count: Int): Int {
        if (token.isNullOrEmpty()) return -1
        val value = token.toIntOrNull() ?: return -1
        val resolved = if (value > 0) value - 1 else count + value
        return if (resolved in 0 until count) resolved else -1
    }

    private fun normalise(vertices: FloatList, heightMeters: Float) {
        val data = vertices.data()
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        var i = 0
        while (i + 7 < vertices.size) {
            val x = data[i]
            val y = data[i + 1]
            val z = data[i + 2]
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
            i += 8
        }
        if (minX > maxX) return

        val scale = heightMeters / (maxY - minY).coerceAtLeast(0.0001f)
        val cx = (minX + maxX) * 0.5f
        val cz = (minZ + maxZ) * 0.5f
        i = 0
        while (i + 7 < vertices.size) {
            data[i] = (data[i] - cx) * scale
            data[i + 1] = (data[i + 1] - minY) * scale
            data[i + 2] = (data[i + 2] - cz) * scale
            i += 8
        }
    }

    private class FloatList {
        private var buffer = FloatArray(8192)
        var size: Int = 0
            private set

        fun add(value: Float) {
            if (size == buffer.size) buffer = buffer.copyOf(buffer.size * 2)
            buffer[size++] = value
        }

        fun data(): FloatArray = buffer
    }

    private class IntList {
        private var buffer = IntArray(4096)
        private var count = 0

        fun add(value: Int) {
            if (count == buffer.size) buffer = buffer.copyOf(buffer.size * 2)
            buffer[count++] = value
        }

        fun toArray(): IntArray = buffer.copyOf(count)
    }

    private val WHITESPACE = Regex("\\s+")
}
