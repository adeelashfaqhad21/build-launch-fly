package com.buildlaunchfly.game.gfx

import android.content.res.AssetManager
import android.util.Log
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal glTF 2.0 binary (.glb) reader: enough for a single static mesh.
 *
 * Reads the JSON and BIN chunks, walks `meshes[].primitives[]`, decodes the POSITION,
 * NORMAL and TEXCOORD_0 accessors (respecting bufferView byteStride) and the optional index
 * accessor, then emits the engine's interleaved layout, centred on the origin and scaled to
 * [heightMetres]. Node transforms, skinning and animation are ignored — the birds fly on
 * instance transforms, so a static pose is what is needed.
 */
object GlbLoader {

    private const val TAG = "BLF-GLB"
    private const val GLB_MAGIC = 0x46546C67
    private const val CHUNK_JSON = 0x4E4F534A
    private const val CHUNK_BIN = 0x004E4942

    /**
     * Largest binary the loader will read. The whole blob is held in memory while it is
     * parsed into boxed lists, so a multi-megabyte file can exhaust a phone's heap before the
     * first frame. Anything larger is skipped and the caller falls back.
     */
    private const val MAX_BYTES = 4 * 1024 * 1024

    fun load(assets: AssetManager, path: String, heightMetres: Float): MeshData? = try {
        assets.open(path).use { stream ->
            val size = stream.available()
            if (size > MAX_BYTES) {
                Log.w(
                    TAG,
                    "Skipping $path: $size bytes is more than the startup budget of $MAX_BYTES"
                )
                null
            } else {
                parse(stream.readBytes(), path, heightMetres)
            }
        }
    } catch (error: Throwable) {
        Log.e(TAG, "Failed to load $path", error)
        null
    }

    fun parse(bytes: ByteArray, label: String, heightMetres: Float): MeshData? {
        return try {
        if (bytes.size < 12) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (buffer.int != GLB_MAGIC) return null
        buffer.int // version
        buffer.int // total length

        var json: JSONObject? = null
        var bin: ByteArray? = null
        while (buffer.remaining() >= 8) {
            val chunkLength = buffer.int
            val chunkType = buffer.int
            if (chunkLength <= 0 || chunkLength > buffer.remaining()) break
            val chunk = ByteArray(chunkLength)
            buffer.get(chunk)
            when (chunkType) {
                CHUNK_JSON -> json = JSONObject(String(chunk, Charsets.UTF_8))
                CHUNK_BIN -> bin = chunk
            }
        }

        val root = json ?: return null
        val binary = bin ?: return null
        val accessors = root.optJSONArray("accessors") ?: return null
        val views = root.optJSONArray("bufferViews") ?: return null
        val meshes = root.optJSONArray("meshes") ?: return null

        val positions = ArrayList<Float>()
        val normals = ArrayList<Float>()
        val uvs = ArrayList<Float>()
        val indices = ArrayList<Int>()

        for (m in 0 until meshes.length()) {
            val primitives = meshes.optJSONObject(m)?.optJSONArray("primitives") ?: continue
            for (p in 0 until primitives.length()) {
                val primitive = primitives.optJSONObject(p) ?: continue
                val attributes = primitive.optJSONObject("attributes") ?: continue
                val positionAccessor = attributes.optInt("POSITION", -1)
                if (positionAccessor < 0) continue

                val pos = readFloats(root, accessors, views, binary, positionAccessor, 3)
                    ?: continue
                val normal = readFloats(
                    root, accessors, views, binary,
                    attributes.optInt("NORMAL", -1), 3
                )
                val uv = readFloats(
                    root, accessors, views, binary,
                    attributes.optInt("TEXCOORD_0", -1), 2
                )
                val count = pos.size / 3

                val base = positions.size / 3
                for (i in 0 until count) {
                    positions.add(pos[i * 3])
                    positions.add(pos[i * 3 + 1])
                    positions.add(pos[i * 3 + 2])

                    if (normal != null && normal.size >= (i + 1) * 3) {
                        normals.add(normal[i * 3])
                        normals.add(normal[i * 3 + 1])
                        normals.add(normal[i * 3 + 2])
                    } else {
                        normals.add(0f)
                        normals.add(1f)
                        normals.add(0f)
                    }

                    if (uv != null && uv.size >= (i + 1) * 2) {
                        uvs.add(uv[i * 2])
                        uvs.add(uv[i * 2 + 1])
                    } else {
                        uvs.add(0f)
                        uvs.add(0f)
                    }
                }

                val primitiveIndices = readInts(
                    root, accessors, views, binary,
                    primitive.optInt("indices", -1)
                )
                if (primitiveIndices != null) {
                    for (index in primitiveIndices) indices.add(base + index)
                } else {
                    for (i in 0 until count) indices.add(base + i)
                }
            }
        }

        val vertexCount = positions.size / 3
        if (vertexCount == 0) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        for (i in 0 until vertexCount) {
            val x = positions[i * 3]
            val y = positions[i * 3 + 1]
            val z = positions[i * 3 + 2]
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
        }

        val height = (maxY - minY).coerceAtLeast(0.0001f)
        val scale = heightMetres / height
        val centreX = (minX + maxX) * 0.5f
        val centreY = (minY + maxY) * 0.5f
        val centreZ = (minZ + maxZ) * 0.5f

        val vertices = FloatArray(vertexCount * MeshData.VERTEX_FLOATS)
        var v = 0
        for (i in 0 until vertexCount) {
            vertices[v++] = (positions[i * 3] - centreX) * scale
            vertices[v++] = (positions[i * 3 + 1] - centreY) * scale
            vertices[v++] = (positions[i * 3 + 2] - centreZ) * scale
            vertices[v++] = normals[i * 3]
            vertices[v++] = normals[i * 3 + 1]
            vertices[v++] = normals[i * 3 + 2]
            vertices[v++] = uvs[i * 2]
            vertices[v++] = uvs[i * 2 + 1]
        }

        MeshData(vertices, indices.toIntArray())
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to parse $label", error)
            null
        }
    }

    private fun readFloats(
        root: JSONObject,
        accessors: org.json.JSONArray,
        views: org.json.JSONArray,
        bin: ByteArray,
        accessorIndex: Int,
        components: Int
    ): FloatArray? {
        if (accessorIndex < 0) return null
        val accessor = accessors.optJSONObject(accessorIndex) ?: return null
        val view = views.optJSONObject(accessor.optInt("bufferView", -1)) ?: return null
        val count = accessor.optInt("count", 0)
        if (count <= 0) return null

        val componentSize = componentSize(accessor.optInt("componentType", 5126))
        if (componentSize <= 0) return null
        val elementSize = componentSize * components
        val stride = view.optInt("byteStride", 0).let { if (it > 0) it else elementSize }
        val start = view.optInt("byteOffset", 0) + accessor.optInt("byteOffset", 0)

        val out = FloatArray(count * components)
        val buffer = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) {
            val base = start + i * stride
            if (base + elementSize > bin.size) break
            for (c in 0 until components) {
                buffer.position(base + c * componentSize)
                out[i * components + c] = when (accessor.optInt("componentType", 5126)) {
                    5126 -> buffer.float
                    5125 -> buffer.int.toFloat()
                    5123 -> (buffer.short.toInt() and 0xFFFF).toFloat()
                    5122 -> buffer.short.toFloat()
                    5121 -> (buffer.get().toInt() and 0xFF).toFloat()
                    5120 -> buffer.get().toFloat()
                    else -> 0f
                }
            }
        }
        return out
    }

    private fun readInts(
        root: JSONObject,
        accessors: org.json.JSONArray,
        views: org.json.JSONArray,
        bin: ByteArray,
        accessorIndex: Int
    ): IntArray? {
        if (accessorIndex < 0) return null
        val accessor = accessors.optJSONObject(accessorIndex) ?: return null
        val view = views.optJSONObject(accessor.optInt("bufferView", -1)) ?: return null
        val count = accessor.optInt("count", 0)
        if (count <= 0) return null

        val componentType = accessor.optInt("componentType", 5123)
        val componentSize = componentSize(componentType)
        if (componentSize <= 0) return null
        val stride = view.optInt("byteStride", 0).let { if (it > 0) it else componentSize }
        val start = view.optInt("byteOffset", 0) + accessor.optInt("byteOffset", 0)

        val out = IntArray(count)
        val buffer = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) {
            val base = start + i * stride
            if (base + componentSize > bin.size) break
            buffer.position(base)
            out[i] = when (componentType) {
                5125 -> buffer.int
                5123 -> buffer.short.toInt() and 0xFFFF
                5121 -> buffer.get().toInt() and 0xFF
                else -> 0
            }
        }
        return out
    }

    private fun componentSize(componentType: Int): Int = when (componentType) {
        5120, 5121 -> 1
        5122, 5123 -> 2
        5125, 5126 -> 4
        else -> 0
    }
}
