package com.buildlaunchfly.game.gfx

import android.content.res.AssetManager
import android.util.Log
import org.json.JSONObject

/**
 * Reads the BufferGeometry JSON that `emnh/rts-models` ships (one file per model in its `3d`
 * folder): flat attribute arrays for position, normal and uv, with an optional triangle index.
 *
 * The result is the engine's interleaved layout (position 3 + normal 3 + uv 2), centred on
 * the origin and scaled so the model is [heightMetres] tall, which is what the world's
 * instance matrices expect.
 */
object RtsModelLoader {

    private const val TAG = "BLF-RTS"

    fun load(assets: AssetManager, path: String, heightMetres: Float): MeshData? = try {
        val text = assets.open(path).bufferedReader().use { it.readText() }
        parse(text, path, heightMetres)
    } catch (error: Throwable) {
        Log.e(TAG, "Failed to load model $path", error)
        null
    }

    fun parse(text: String, label: String, heightMetres: Float): MeshData? {
        return try {
        val root = JSONObject(text)
        val data = root.optJSONObject("data") ?: return null
        val attributes = data.optJSONObject("attributes") ?: return null

        val positions = floats(attributes.optJSONObject("position")) ?: return null
        val normals = floats(attributes.optJSONObject("normal"))
        val uvs = floats(attributes.optJSONObject("uv"))
        val count = positions.size / 3
        if (count == 0) return null

        // Some exporters write an index buffer, some write raw triangle soup.
        val indices = ints(data.optJSONObject("index"))
            ?: IntArray(count) { it }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        for (i in 0 until count) {
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

        val vertices = FloatArray(count * MeshData.VERTEX_FLOATS)
        var v = 0
        for (i in 0 until count) {
            vertices[v++] = (positions[i * 3] - centreX) * scale
            vertices[v++] = (positions[i * 3 + 1] - centreY) * scale
            vertices[v++] = (positions[i * 3 + 2] - centreZ) * scale

            if (normals != null && normals.size >= (i + 1) * 3) {
                vertices[v++] = normals[i * 3]
                vertices[v++] = normals[i * 3 + 1]
                vertices[v++] = normals[i * 3 + 2]
            } else {
                vertices[v++] = 0f
                vertices[v++] = 1f
                vertices[v++] = 0f
            }

            if (uvs != null && uvs.size >= (i + 1) * 2) {
                vertices[v++] = uvs[i * 2]
                vertices[v++] = uvs[i * 2 + 1]
            } else {
                vertices[v++] = 0f
                vertices[v++] = 0f
            }
        }

        MeshData(vertices, indices)
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to parse model $label", error)
            null
        }
    }

    private fun floats(attribute: JSONObject?): FloatArray? {
        val array = attribute?.optJSONArray("array") ?: return null
        val out = FloatArray(array.length())
        for (i in out.indices) out[i] = array.optDouble(i, 0.0).toFloat()
        return out
    }

    private fun ints(attribute: JSONObject?): IntArray? {
        val array = attribute?.optJSONArray("array") ?: return null
        val out = IntArray(array.length())
        for (i in out.indices) out[i] = array.optInt(i, 0)
        return out
    }
}
