package com.buildlaunchfly.game.platform

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import com.buildlaunchfly.game.game.GameApp

/**
 * GL surface with an OpenGL ES 3.0 context. Keeps the EGL context across pauses so a
 * backgrounded game resumes without rebuilding every texture and mesh.
 */
class GameSurfaceView : GLSurfaceView {

    private var input: InputState? = null

    constructor(context: Context) : super(context) {
        init(null, null)
    }

    constructor(context: Context, app: GameApp, appInput: InputState) : super(context) {
        input = appInput
        init(app, appInput)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(null, null)
    }

    private fun init(app: GameApp?, appInput: InputState?) {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 24, 0)
        preserveEGLContextOnPause = true
        setZOrderOnTop(false)
        isFocusable = true
        isFocusableInTouchMode = true
        if (app != null && appInput != null) {
            input = appInput
            setRenderer(GameRenderer(app))
            renderMode = RENDERMODE_CONTINUOUSLY
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val localInput = input
        if (localInput != null) {
            localInput.onTouch(event, width, height)
            return true
        }
        return super.onTouchEvent(event)
    }
}
