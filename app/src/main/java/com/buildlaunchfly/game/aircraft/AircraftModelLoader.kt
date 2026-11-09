package com.buildlaunchfly.game.aircraft

import android.content.res.AssetManager
import android.util.Log
import com.buildlaunchfly.game.gfx.MeshData
import com.buildlaunchfly.game.math.Vec3

/**
 * Reads the imported biplane ("samolet" by vrlab development, CC BY 4.0).
 *
 * The source is one welded OBJ with no UVs and no texture: its colour comes from seven flat
 * `Kd` materials, so the file is split into one mesh per material and each part is drawn with
 * the shared white map tinted by that colour. The model is Y-up with the nose on -Z already,
 * so it only needs a uniform scale to the rig's wingspan, its base dropped to y = 0 and its
 * plan centred on the origin.
 */
object AircraftModelLoader {

    private const val TAG = "BLF-PLANE"

    private val WHITESPACE = Regex("\\s+")

    /** One material group, ready to upload, with its flat colour. */
    class Part(val data: MeshData, val color: Vec3)

    fun load(
        assets: AssetManager,
        objPath: String,
        mtlPath: String,
        targetWidth: Float
    ): List<Part> = try {
        val colors = readMaterials(assets, mtlPath)
        parse(assets, objPath, colors, targetWidth)
    } catch (error: Throwable) {
        Log.e(TAG, "Failed to load aircraft $objPath", error)
        emptyList()
    }

    private fun readMaterials(assets: AssetManager, path: String): Map<String, Vec3> {
        val colors = LinkedHashMap<String, Vec3>()
        var current: String? = null
        assets.open(path).bufferedReader().use { reader ->
            var line = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("newmtl ") -> current = trimmed.substring(7).trim()
                    trimmed.startsWith("Kd ") && current != null -> {
                        val parts = trimmed.split(WHITESPACE)
                        if (parts.size >= 4) {
                            colors[current!!] = Vec3(
                                parts[1].toFloatOrNull() ?: 1f,
                                parts[2].toFloatOrNull() ?: 1f,
                                parts[3].toFloatOrNull() ?: 1f
                            )
                        }
                    }
                }
                line = reader.readLine()
            }
        }
        return colors
    }

    private fun parse(
        assets: AssetManager,
        path: String,
        colors: Map<String, Vec3>,
        targetWidth: Float
    ): List<Part> {
        val positions = ArrayList<Float>(8192)
        val texCoords = ArrayList<Float>(8192)
        val normals = ArrayList<Float>(8192)

        val groups = LinkedHashMap<String, Group>()
        var material = "default"

        assets.open(path).bufferedReader().use { reader ->
            var line = reader.readLine()
            while (line != null) {
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
                    line.startsWith("usemtl ") -> material = line.substring(7).trim()
                    line.startsWith("f ") -> {
                        val group = groups.getOrPut(material) {
                            Group(colors[material] ?: Vec3(1f, 1f, 1f))
                        }
                        addFace(line, positions, texCoords, normals, group)
                    }
                }
                line = reader.readLine()
            }
        }

        if (groups.isEmpty()) return emptyList()
        normalise(groups, targetWidth)
        return groups.map { (_, group) ->
            Part(group.vertices.toMeshData(group.indices), group.color)
        }
    }

    private fun addFace(
        line: String,
        positions: ArrayList<Float>,
        texCoords: ArrayList<Float>,
        normals: ArrayList<Float>,
        group: Group
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
            emit(group, positions, texCoords, normals, v[0], t[0], n[0])
            emit(group, positions, texCoords, normals, v[i], t[i], n[i])
            emit(group, positions, texCoords, normals, v[i + 1], t[i + 1], n[i + 1])
        }
    }

    private fun emit(
        group: Group,
        positions: ArrayList<Float>,
        texCoords: ArrayList<Float>,
        normals: ArrayList<Float>,
        vertexIndex: Int,
        uvIndex: Int,
        normalIndex: Int
    ) {
        if (vertexIndex < 0 || vertexIndex * 3 + 2 >= positions.size) return
        val out = group.vertices
        val index = out.size / 8
        out.add(positions[vertexIndex * 3])
        out.add(positions[vertexIndex * 3 + 1])
        out.add(positions[vertexIndex * 3 + 2])
        if (uvIndex >= 0 && uvIndex * 2 + 1 < texCoords.size) {
            out.add(texCoords[uvIndex * 2])
            out.add(1f - texCoords[uvIndex * 2 + 1])
        } else {
            out.add(0f)
            out.add(0f)
        }
        if (normalIndex >= 0 && normalIndex * 3 + 2 < normals.size) {
            out.add(normals[normalIndex * 3])
            out.add(normals[normalIndex * 3 + 1])
            out.add(normals[normalIndex * 3 + 2])
        } else {
            out.add(0f)
            out.add(1f)
            out.add(0f)
        }
        group.indices.add(index)
    }

    private fun resolve(token: String?, count: Int): Int {
        if (token.isNullOrEmpty()) return -1
        val value = token.toIntOrNull() ?: return -1
        val resolved = if (value > 0) value - 1 else count + value
        return if (resolved in 0 until count) resolved else -1
    }

    /**
     * One shared transform: [targetWidth] across, centred on the origin in X and Y (the rig
     * hangs the aircraft by its middle, as the built one is), and centred in Z so the nose
     * sits where the built one does.
     */
    private fun normalise(groups: Map<String, Group>, targetWidth: Float) {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (group in groups.values) {
            val data = group.vertices.data()
            var i = 0
            while (i + 7 < group.vertices.size) {
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
        val sourceWidth = (maxX - minX).coerceAtLeast(0.0001f)
        val scale = targetWidth / sourceWidth
        val cx = (minX + maxX) * 0.5f
        val cy = (minY + maxY) * 0.5f
        val cz = (minZ + maxZ) * 0.5f

        for (group in groups.values) {
            val data = group.vertices.data()
            var i = 0
            while (i + 7 < group.vertices.size) {
                data[i] = (data[i] - cx) * scale
                data[i + 1] = (data[i + 1] - cy) * scale
                data[i + 2] = (data[i + 2] - cz) * scale
                i += 8
            }
        }
    }

    private class Group(val color: Vec3) {
        val vertices = FloatList()
        val indices = IntList()
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

        fun toMeshData(indices: IntList): MeshData {
            val vertices = FloatArray(size)
            System.arraycopy(buffer, 0, vertices, 0, size)
            return MeshData(vertices, indices.toArray())
        }
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
