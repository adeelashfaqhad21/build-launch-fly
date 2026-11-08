package com.buildlaunchfly.game.gfx

import android.content.res.AssetManager
import android.util.Log

/**
 * Minimal OBJ reader for the Quaternius nature models shipped in `assets/quaternius`.
 *
 * Blender exports these as n-gon faces with `v/vt/vn` indices and splits the geometry across
 * two materials — bark and leaves — so the reader triangulates the faces, buckets them into a
 * bark group and a foliage group, and normalises both with one shared transform (1 unit tall,
 * base at y = 0, centred in XZ) so the two meshes stay aligned when scaled per instance.
 */
object ObjLoader {

    private const val TAG = "BLF-ASSETS"

    /** Bark/rock geometry plus the foliage group, already normalised. */
    class Model(val bark: MeshData?, val foliage: MeshData?)

    fun load(assets: AssetManager, path: String): Model? = try {
        parse(assets, path)
    } catch (error: Throwable) {
        Log.e(TAG, "Failed to load OBJ $path", error)
        null
    }

    private fun parse(assets: AssetManager, path: String): Model? {
        val positions = ArrayList<Float>(8192)
        val texCoords = ArrayList<Float>(8192)
        val normals = ArrayList<Float>(8192)

        val barkVertices = FloatList()
        val barkIndices = IntList()
        val foliageVertices = FloatList()
        val foliageIndices = IntList()

        var inFoliage = false

        assets.open(path).bufferedReader().use { reader ->
            var line = reader.readLine()
            while (line != null) {
                when {
                    line.startsWith("v ") -> {
                        val parts = line.trim().split(WHITESPACE)
                        if (parts.size >= 4) {
                            positions.add(parts[1].toFloatOrNull() ?: 0f)
                            positions.add(parts[2].toFloatOrNull() ?: 0f)
                            positions.add(parts[3].toFloatOrNull() ?: 0f)
                        }
                    }
                    line.startsWith("vt ") -> {
                        val parts = line.trim().split(WHITESPACE)
                        if (parts.size >= 2) {
                            texCoords.add(parts[1].toFloatOrNull() ?: 0f)
                            texCoords.add(if (parts.size >= 3) parts[2].toFloatOrNull() ?: 0f else 0f)
                        }
                    }
                    line.startsWith("vn ") -> {
                        val parts = line.trim().split(WHITESPACE)
                        if (parts.size >= 4) {
                            normals.add(parts[1].toFloatOrNull() ?: 0f)
                            normals.add(parts[2].toFloatOrNull() ?: 0f)
                            normals.add(parts[3].toFloatOrNull() ?: 0f)
                        }
                    }
                    line.startsWith("usemtl ") -> {
                        inFoliage = isFoliage(line.substring(7).trim(), path)
                    }
                    line.startsWith("f ") -> {
                        addFace(
                            line, positions, texCoords, normals, inFoliage,
                            barkVertices, barkIndices, foliageVertices, foliageIndices
                        )
                    }
                }
                line = reader.readLine()
            }
        }

        if (barkVertices.size == 0 && foliageVertices.size == 0) return null

        normalise(barkVertices, foliageVertices)

        return Model(
            bark = if (barkVertices.size == 0) null else barkVertices.toMeshData(barkIndices),
            foliage = if (foliageVertices.size == 0) null else foliageVertices.toMeshData(foliageIndices)
        )
    }

    /**
     * Leaves, flowers, grass and bushes go to the foliage group; bark and rock do not.
     * Some Quaternius grass OBJ files export their only material as "None", so use the
     * asset filename as a fallback only when the material itself carries no useful category.
     */
    private fun isFoliage(material: String, path: String): Boolean {
        val name = material.lowercase()
        if (name.contains("bark") || name.contains("trunk") || name.contains("rock")) return false
        if (name.contains("leave") || name.contains("flower") ||
            name.contains("grass") || name.contains("bush") || name.contains("plant")) return true

        val materialIsUnspecified = name.isBlank() || name == "none" || name == "default"
        if (!materialIsUnspecified) return false

        val assetName = path.substringAfterLast('/').substringBeforeLast('.').lowercase()
        return assetName.contains("grass") || assetName.contains("flower") ||
            assetName.contains("bush") || assetName.contains("plant")
    }

    private fun addFace(
        line: String,
        positions: ArrayList<Float>,
        texCoords: ArrayList<Float>,
        normals: ArrayList<Float>,
        inFoliage: Boolean,
        barkVertices: FloatList,
        barkIndices: IntList,
        foliageVertices: FloatList,
        foliageIndices: IntList
    ) {
        val tokens = line.trim().split(WHITESPACE)
        val corners = tokens.size - 1
        if (corners < 3) return

        val vertexIndices = IntArray(corners)
        val uvIndices = IntArray(corners)
        val normalIndices = IntArray(corners)

        val vertexCount = positions.size / 3
        val uvCount = texCoords.size / 2
        val normalCount = normals.size / 3

        for (i in 0 until corners) {
            val corner = tokens[i + 1].split('/')
            vertexIndices[i] = resolveIndex(corner.getOrNull(0), vertexCount)
            uvIndices[i] = if (corner.size > 1) resolveIndex(corner[1], uvCount) else -1
            normalIndices[i] = if (corner.size > 2) resolveIndex(corner[2], normalCount) else -1
        }

        val vertices = if (inFoliage) foliageVertices else barkVertices
        val indices = if (inFoliage) foliageIndices else barkIndices

        // Triangle fan: Blender exports quads and n-gons here, GL draws triangles.
        for (i in 1 until corners - 1) {
            emitCorner(vertices, indices, positions, texCoords, normals, vertexIndices[0], uvIndices[0], normalIndices[0])
            emitCorner(vertices, indices, positions, texCoords, normals, vertexIndices[i], uvIndices[i], normalIndices[i])
            emitCorner(vertices, indices, positions, texCoords, normals, vertexIndices[i + 1], uvIndices[i + 1], normalIndices[i + 1])
        }
    }

    private fun emitCorner(
        vertices: FloatList,
        indices: IntList,
        positions: ArrayList<Float>,
        texCoords: ArrayList<Float>,
        normals: ArrayList<Float>,
        vertexIndex: Int,
        uvIndex: Int,
        normalIndex: Int
    ) {
        if (vertexIndex < 0) return
        val index = vertices.size / 8
        appendVertex(vertices, positions, texCoords, normals, vertexIndex, uvIndex, normalIndex)
        indices.add(index)
    }

    /** OBJ indices are 1-based and may be negative (relative to the end of the list). */
    private fun resolveIndex(token: String?, count: Int): Int {
        if (token.isNullOrEmpty()) return -1
        val value = token.toIntOrNull() ?: return -1
        val resolved = if (value > 0) value - 1 else count + value
        return if (resolved in 0 until count) resolved else -1
    }

    private fun appendVertex(
        out: FloatList,
        positions: ArrayList<Float>,
        texCoords: ArrayList<Float>,
        normals: ArrayList<Float>,
        vertexIndex: Int,
        uvIndex: Int,
        normalIndex: Int
    ) {
        val vertexCount = positions.size / 3
        if (vertexIndex in 0 until vertexCount) {
            out.add(positions[vertexIndex * 3])
            out.add(positions[vertexIndex * 3 + 1])
            out.add(positions[vertexIndex * 3 + 2])
        } else {
            out.add(0f)
            out.add(0f)
            out.add(0f)
        }

        // Keep the shared MeshData layout: Position (3), Normal (3), UV (2).
        // GLUtil.bindStandardAttributes() binds normals at byte offset 12 and UVs at 24.
        if (normalIndex >= 0 && normalIndex * 3 + 2 < normals.size) {
            out.add(normals[normalIndex * 3])
            out.add(normals[normalIndex * 3 + 1])
            out.add(normals[normalIndex * 3 + 2])
        } else {
            out.add(0f)
            out.add(1f)
            out.add(0f)
        }

        if (uvIndex >= 0 && uvIndex * 2 + 1 < texCoords.size) {
            // OBJ V runs bottom-up, decoded PNGs are sampled top-down.
            out.add(texCoords[uvIndex * 2])
            out.add(1f - texCoords[uvIndex * 2 + 1])
        } else {
            out.add(0f)
            out.add(0f)
        }
    }

    /** One shared transform for both groups: 1 unit tall, base at 0, centred in XZ. */
    private fun normalise(bark: FloatList, foliage: FloatList) {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (list in arrayOf(bark, foliage)) {
            val data = list.data()
            val limit = list.size
            var i = 0
            while (i + 7 < limit) {
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
        }

        if (minX > maxX) return
        val height = (maxY - minY).coerceAtLeast(0.0001f)
        val scale = 1f / height
        val cx = (minX + maxX) * 0.5f
        val cz = (minZ + maxZ) * 0.5f

        for (list in arrayOf(bark, foliage)) {
            val data = list.data()
            val limit = list.size
            var i = 0
            while (i + 7 < limit) {
                data[i] = (data[i] - cx) * scale
                data[i + 1] = (data[i + 1] - minY) * scale
                data[i + 2] = (data[i + 2] - cz) * scale
                i += 8
            }
        }
    }

    private fun FloatList.toMeshData(indices: IntList): MeshData {
        val vertices = FloatArray(size)
        System.arraycopy(data(), 0, vertices, 0, size)
        val indexArray = indices.toArray()
        return MeshData(vertices, indexArray)
    }

    private val WHITESPACE = Regex("\\s+")

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
}
