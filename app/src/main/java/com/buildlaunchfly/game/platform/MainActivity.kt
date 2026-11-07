package com.buildlaunchfly.game.platform

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.buildlaunchfly.game.game.GameApp

/**
 * Native Android entry point: landscape, fullscreen, immersive, screen kept awake. The whole
 * game lives in [GameApp]; this class only owns the platform wiring.
 */
class MainActivity : Activity() {

    private lateinit var input: InputState
    private lateinit var app: GameApp
    private lateinit var surfaceView: GameSurfaceView
    private lateinit var root: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setVolumeControlStream(AudioManager.STREAM_MUSIC)

        input = InputState()
        app = GameApp(this, input)
        surfaceView = GameSurfaceView(this, app, input)

        root = FrameLayout(this)
        root.addView(
            surfaceView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)
        hideSystemUi()
    }

    override fun onResume() {
        super.onResume()
        surfaceView.onResume()
        app.audio.start()
        hideSystemUi()
    }

    override fun onPause() {
        app.audio.stop()
        surfaceView.onPause()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }
}
