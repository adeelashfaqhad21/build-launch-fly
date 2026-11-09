package com.buildlaunchfly.game.gfx

import android.opengl.GLES30

/** Full-screen analytic sky: gradient, sun disk, glow and a cheap cloud band. */
class SkyRenderer {

    private val program = ShaderProgram.create(
        "sky", ShaderSources.SKY_VERT, ShaderSources.SKY_FRAG
    )
    private var vao = 0
    private var vbo = 0

    init {
        val vaoIds = IntArray(1)
        GLES30.glGenVertexArrays(1, vaoIds, 0)
        vao = vaoIds[0]
        GLES30.glBindVertexArray(vao)

        val quad = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        vbo = GLUtil.uploadFloatBuffer(quad)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glEnableVertexAttribArray(GLUtil.ATTR_POSITION)
        GLES30.glVertexAttribPointer(GLUtil.ATTR_POSITION, 2, GLES30.GL_FLOAT, false, 8, 0)

        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLUtil.checkError("SkyRenderer.init")
    }

    fun render(camera: Camera, env: EnvUniforms) {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(false)
        GLES30.glDisable(GLES30.GL_BLEND)

        program.use()
        program.set("uInvViewProj", camera.invViewProj)
        program.set("uCameraPos", camera.position)
        program.set("uSunDir", env.sunDir)
        program.set("uSunColor", env.sunColor)
        program.set("uZenith", env.zenithColor)
        program.set("uHorizon", env.horizonColor)
        program.set("uTime", env.time)

        GLES30.glBindVertexArray(vao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)

        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
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
