package com.buildlaunchfly.game.gfx

import android.opengl.GLES30
import com.buildlaunchfly.game.math.Vec3
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Point-sprite particle batch (snow, dust, spray). */
class ParticleRenderer(private val capacity: Int) {

    private val data = FloatArray(capacity.coerceAtLeast(1) * 4)
    private var count = 0

    private var vao = 0
    private var vbo = 0
    private var uploadedBytes = 0

    private val program = ShaderProgram.create(
        "particles", ShaderSources.PARTICLE_VERT, ShaderSources.PARTICLE_FRAG
    )

    init {
        val vaoIds = IntArray(1)
        GLES30.glGenVertexArrays(1, vaoIds, 0)
        vao = vaoIds[0]
        GLES30.glBindVertexArray(vao)

        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        vbo = ids[0]
        uploadedBytes = data.size * 4
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER, uploadedBytes, null, GLES30.GL_DYNAMIC_DRAW
        )
        GLES30.glEnableVertexAttribArray(GLUtil.ATTR_POSITION)
        GLES30.glVertexAttribPointer(
            GLUtil.ATTR_POSITION, 4, GLES30.GL_FLOAT, false, 16, 0
        )

        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLUtil.checkError("ParticleRenderer.init")
    }

    fun begin() {
        count = 0
    }

    fun emit(x: Float, y: Float, z: Float, size: Float) {
        if (count >= capacity) return
        val i = count * 4
        data[i] = x
        data[i + 1] = y
        data[i + 2] = z
        data[i + 3] = size
        count++
    }

    fun flush(camera: Camera, env: EnvUniforms, color: Vec3, viewportHeight: Int) {
        if (count == 0) return
        val floats = count * 4

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferSubData(
            GLES30.GL_ARRAY_BUFFER, 0, floats * 4, toBuffer(floats)
        )
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

        program.use()
        program.set("uViewProj", camera.viewProj)
        program.set("uPointScale", viewportHeight * 0.35f)
        program.set("uColor", color)
        program.set("uFogColor", env.fogColor)
        program.set("uFogDensity", env.fogDensity)

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(false)
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, count)
        GLES30.glBindVertexArray(0)
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    private fun toBuffer(floats: Int): java.nio.FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(floats * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(data, 0, floats)
        buffer.position(0)
        return buffer
    }

    fun dispose() {
        program.dispose()
        if (vao != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
            vao = 0
        }
        if (vbo != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(vbo), 0)
            vbo = 0
        }
    }
}
