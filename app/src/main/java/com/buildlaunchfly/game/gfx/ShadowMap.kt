package com.buildlaunchfly.game.gfx

import android.opengl.GLES30
import android.util.Log
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3

/** Depth-only framebuffer used for the single directional shadow cascade. */
class ShadowMap(val size: Int = 1536) {

    var fbo: Int = 0
        private set
    var depthTexture: Int = 0
        private set

    val lightViewProj = Mat4()
    private val lightView = Mat4()
    private val lightProjection = Mat4()

    init {
        create()
    }

    private fun create() {
        val texIds = IntArray(1)
        GLES30.glGenTextures(1, texIds, 0)
        depthTexture = texIds[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_DEPTH_COMPONENT24,
            size, size, 0,
            GLES30.GL_DEPTH_COMPONENT, GLES30.GL_UNSIGNED_INT, null
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

        val fboIds = IntArray(1)
        GLES30.glGenFramebuffers(1, fboIds, 0)
        fbo = fboIds[0]
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT,
            GLES30.GL_TEXTURE_2D, depthTexture, 0
        )
        GLES30.glDrawBuffers(1, intArrayOf(GLES30.GL_NONE), 0)
        GLES30.glReadBuffer(GLES30.GL_NONE)

        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(GLUtil.TAG, "Shadow framebuffer incomplete: 0x${Integer.toHexString(status)}")
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLUtil.checkError("ShadowMap.create")
    }

    /** Fits the light camera around [focus] with the given orthographic [radius]. */
    fun updateLight(focus: Vec3, radius: Float, sunDir: Vec3) {
        val safeRadius = radius.coerceAtLeast(8f)
        val eye = Vec3(
            focus.x + sunDir.x * safeRadius * 2f,
            focus.y + sunDir.y * safeRadius * 2f,
            focus.z + sunDir.z * safeRadius * 2f
        )
        lightView.setLookAt(eye, focus, Vec3(0f, 1f, 0f))
        lightProjection.setOrtho(
            -safeRadius, safeRadius, -safeRadius, safeRadius,
            // Negative-domain safe near plane: the light eye is behind the focus point.
            0.5f, safeRadius * 4.5f + 60f
        )
        lightViewProj.setProduct(lightProjection, lightView)
    }

    fun dispose() {
        if (fbo != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
            fbo = 0
        }
        if (depthTexture != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(depthTexture), 0)
            depthTexture = 0
        }
    }
}
