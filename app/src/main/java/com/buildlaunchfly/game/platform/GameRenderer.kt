package com.buildlaunchfly.game.platform

import android.opengl.GLSurfaceView
import com.buildlaunchfly.game.game.GameApp
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** Drives the fixed update + render loop from the GL thread. */
class GameRenderer(private val app: GameApp) : GLSurfaceView.Renderer {

    private var lastNanos = 0L
    private var reportedVersion = false
    private var profileWindowStartNanos = 0L
    private var profileFrameCount = 0
    private var profileSlowFrames = 0
    private var profileTotalFrameNanos = 0L
    private var profileMaxFrameNanos = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        app.onContextCreated()
        lastNanos = 0L
        if (!reportedVersion) {
            reportedVersion = true
            android.util.Log.i(
                "BLF-GL",
                "GL_VERSION=" + android.opengl.GLES30.glGetString(android.opengl.GLES30.GL_VERSION) +
                    " RENDERER=" + android.opengl.GLES30.glGetString(android.opengl.GLES30.GL_RENDERER)
            )
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        app.onSurfaceChanged(width, height)
        lastNanos = System.nanoTime()
    }

    override fun onDrawFrame(gl: GL10?) {
        val frameStart = System.nanoTime()
        val dt = if (lastNanos == 0L) 0f else
            ((frameStart - lastNanos) / 1_000_000_000f).coerceAtMost(0.1f)
        lastNanos = frameStart

        app.update(dt)
        app.render()

        val frameNanos = System.nanoTime() - frameStart
        if (profileWindowStartNanos == 0L) profileWindowStartNanos = frameStart
        profileFrameCount++
        profileTotalFrameNanos += frameNanos
        if (frameNanos > 20_000_000L) profileSlowFrames++
        if (frameNanos > profileMaxFrameNanos) profileMaxFrameNanos = frameNanos

        val windowNanos = frameStart - profileWindowStartNanos
        if (windowNanos >= 5_000_000_000L) {
            val averageMs = profileTotalFrameNanos / profileFrameCount / 1_000_000f
            val fps = profileFrameCount * 1_000_000_000f / windowNanos
            android.util.Log.i(
                "BLF-PERF",
                "fps=%.1f avgFrameMs=%.2f maxFrameMs=%.2f slowFrames(>20ms)=%d/%d".format(
                    fps,
                    averageMs,
                    profileMaxFrameNanos / 1_000_000f,
                    profileSlowFrames,
                    profileFrameCount
                )
            )
            profileWindowStartNanos = frameStart
            profileFrameCount = 0
            profileSlowFrames = 0
            profileTotalFrameNanos = 0L
            profileMaxFrameNanos = 0L
        }
    }
}
