package com.buildlaunchfly.game.platform

import android.opengl.GLSurfaceView
import com.buildlaunchfly.game.game.GameApp
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** Drives the fixed update + render loop from the GL thread. */
class GameRenderer(private val app: GameApp) : GLSurfaceView.Renderer {

    private var lastNanos = 0L
    private var reportedVersion = false

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
        val now = System.nanoTime()
        val dt = if (lastNanos == 0L) 0f else (now - lastNanos) / 1_000_000_000f
        lastNanos = now
        app.update(dt)
        app.render()
    }
}
