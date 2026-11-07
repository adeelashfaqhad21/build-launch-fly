package com.buildlaunchfly.game.gfx

import android.opengl.GLES30

/** A static indexed mesh uploaded once into a VAO. */
class Mesh(val label: String, data: MeshData) {

    var vao: Int = 0
        private set
    private var vbo = 0
    private var ibo = 0
    var indexCount: Int = 0
        private set

    init {
        indexCount = data.indices.size

        val vaoIds = IntArray(1)
        GLES30.glGenVertexArrays(1, vaoIds, 0)
        vao = vaoIds[0]
        GLES30.glBindVertexArray(vao)

        vbo = GLUtil.uploadFloatBuffer(data.vertices)
        ibo = GLUtil.uploadIndexBuffer(data.indices)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLUtil.bindStandardAttributes()
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo)

        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLUtil.checkError("Mesh.upload($label)")
    }

    fun draw() {
        if (indexCount == 0) return
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindVertexArray(0)
    }

    fun dispose() {
        if (vao != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
            vao = 0
        }
        if (vbo != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(vbo), 0)
            vbo = 0
        }
        if (ibo != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(ibo), 0)
            ibo = 0
        }
        indexCount = 0
    }
}

/** A set of instance transforms (one 4x4 matrix per instance). */
class InstanceSet(val count: Int, val matrices: FloatArray) {
    companion object {
        val EMPTY = InstanceSet(0, FloatArray(0))
    }
}

/** Instanced indexed mesh: per-instance model matrices live in attributes 4..7. */
class InstancedMesh(val label: String, data: MeshData, val capacity: Int) {

    private var vao = 0
    private var vbo = 0
    private var ibo = 0
    private var instanceVbo = 0
    private val indexCount: Int = data.indices.size

    /** Scratch buffers reused for uploads; never allocate direct memory in the frame loop. */
    private val uploadBuffer = FloatArray(capacity.coerceAtLeast(1) * 16)
    private val uploadFloatBuffer = java.nio.ByteBuffer
        .allocateDirect(uploadBuffer.size * 4)
        .order(java.nio.ByteOrder.nativeOrder())
        .asFloatBuffer()

    var instanceCount: Int = 0
        private set

    private var sourceMatrices: FloatArray = FloatArray(0)
    private var sourceCount: Int = 0

    init {
        val vaoIds = IntArray(1)
        GLES30.glGenVertexArrays(1, vaoIds, 0)
        vao = vaoIds[0]
        GLES30.glBindVertexArray(vao)

        vbo = GLUtil.uploadFloatBuffer(data.vertices)
        ibo = GLUtil.uploadIndexBuffer(data.indices)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLUtil.bindStandardAttributes()

        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        instanceVbo = ids[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVbo)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            uploadBuffer.size * 4,
            null,
            GLES30.GL_DYNAMIC_DRAW
        )

        for (i in 0 until 4) {
            val loc = GLUtil.ATTR_INSTANCE + i
            GLES30.glEnableVertexAttribArray(loc)
            GLES30.glVertexAttribPointer(
                loc, 4, GLES30.GL_FLOAT, false, 64, i * 16
            )
            GLES30.glVertexAttribDivisor(4 + i, 1)
        }

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo)
        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLUtil.checkError("InstancedMesh.init($label)")
    }

    /** Uploads [count] matrices (16 floats each) from [matrices]. */
    fun setInstances(matrices: FloatArray, count: Int) {
        sourceMatrices = matrices
        sourceCount = count.coerceIn(0, capacity)
        uploadVisible(sourceCount) { index -> System.arraycopy(matrices, index * 16, uploadBuffer, index * 16, 16) }
    }

    /**
     * Camera-distance LOD for mobile: keep full instance detail nearby, progressively
     * thin distant foliage/ground clutter, and discard instances outside the useful horizon.
     * The original transforms remain cached; no per-frame arrays are allocated.
     */
    fun applyDistanceLod(camera: com.buildlaunchfly.game.math.Vec3, maxDistance: Float, fullDetailDistance: Float) {
        if (sourceCount == 0) return
        val max2 = maxDistance * maxDistance
        val full2 = fullDetailDistance * fullDetailDistance
        var visible = 0
        for (index in 0 until sourceCount) {
            val offset = index * 16
            val dx = sourceMatrices[offset + 12] - camera.x
            val dy = sourceMatrices[offset + 13] - camera.y
            val dz = sourceMatrices[offset + 14] - camera.z
            val distance2 = dx * dx + dy * dy + dz * dz
            if (distance2 > max2) continue
            // Keep all near instances. At distance, deterministically thin repeated
            // understory silhouettes; larger tree/rock forms retain their full count.
            if (distance2 > full2 && (index % 3 == 1)) continue
            System.arraycopy(sourceMatrices, offset, uploadBuffer, visible * 16, 16)
            visible++
        }
        uploadVisible(visible) { }
    }

    private inline fun uploadVisible(count: Int, fill: (Int) -> Unit) {
        instanceCount = count.coerceIn(0, capacity)
        if (instanceCount == 0) return
        val floats = instanceCount * 16
        if (fill !== {}) { /* no-op marker; source copy is performed by caller */ }
        uploadFloatBuffer.clear()
        uploadFloatBuffer.put(uploadBuffer, 0, floats)
        uploadFloatBuffer.flip()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVbo)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, floats * 4, uploadFloatBuffer)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun draw() {
        if (indexCount == 0 || instanceCount == 0) return
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawElementsInstanced(
            GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0, instanceCount
        )
        GLES30.glBindVertexArray(0)
    }

    fun dispose() {
        if (vao != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
            vao = 0
        }
        if (vbo != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(vbo), 0)
            vbo = 0
        }
        if (ibo != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(ibo), 0)
            ibo = 0
        }
        if (instanceVbo != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(instanceVbo), 0)
            instanceVbo = 0
        }
        instanceCount = 0
    }
}
