package com.buildlaunchfly.game.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import com.buildlaunchfly.game.aircraft.PartSlot
import com.buildlaunchfly.game.game.CameraMode
import com.buildlaunchfly.game.game.GameApp
import com.buildlaunchfly.game.game.GameState
import com.buildlaunchfly.game.platform.InputState
import com.buildlaunchfly.game.ui.screens.AchievementsScreen
import com.buildlaunchfly.game.ui.screens.AircraftWorkshopScreen
import com.buildlaunchfly.game.ui.screens.FlightHudScreen
import com.buildlaunchfly.game.ui.screens.MenuScreen
import com.buildlaunchfly.game.ui.screens.PauseScreen
import com.buildlaunchfly.game.ui.screens.ProfileScreen
import com.buildlaunchfly.game.ui.screens.ResultsScreen
import com.buildlaunchfly.game.ui.screens.RewardScreen
import com.buildlaunchfly.game.ui.screens.SettingsScreen
import com.buildlaunchfly.game.ui.screens.SlingshotWorkshopScreen
import com.buildlaunchfly.game.world.WorldCatalog

/**
 * The whole portrait UI system from design.md §6, drawn as a transparent overlay above
 * the GL surface. One View, ten screens, immediate-mode drawing at dp scale so every
 * element stays crisp and adapts from 16:9 to 20:9 portrait phones.
 */
class GameOverlayView(
    context: Context,
    private val app: GameApp,
    private val input: InputState
) : View(context), UiNav {

    private var screen = UiScreen.MENU

    override var selectedSlot: PartSlot = PartSlot.WING_LEFT
    override var worldCursor: Int = 0
    override var rewardSlot: PartSlot? = null

    private var paused = false

    private val lastHits = ArrayList<UiHit>(48)
    private var downHit: UiHit? = null
    private var dragHit: UiHit? = null

    private var orbitDragging = false
    private var lastOrbitX = 0f
    private var lastPinchSpan = 0f

    init {
        UiFonts.init(context.applicationContext)
        setBackgroundColor(Color.TRANSPARENT)
        isFocusable = true
        isClickable = true
        worldCursor = WorldCatalog.all.indexOfFirst { it.id == app.playerProfile.selectedWorld }
            .coerceAtLeast(0)
    }

    // ---- UiNav ------------------------------------------------------------

    override fun go(screen: UiScreen) {
        this.screen = screen
        paused = false
        app.paused = false

        if (screen == UiScreen.WORLDS) {
            worldCursor = WorldCatalog.all.indexOfFirst { it.id == app.playerProfile.selectedWorld }
                .coerceAtLeast(0)
        }
        if (screen == UiScreen.LAUNCH && app.currentState == GameState.SEATED) {
            app.requestEnterLaunch()
        }
        invalidate()
    }

    override fun setPaused(paused: Boolean) {
        this.paused = paused
        app.paused = paused
        if (paused) input.reset()
        invalidate()
    }

    override fun isPaused(): Boolean = paused

    override fun back() {
        when (screen) {
            UiScreen.MENU -> Unit
            UiScreen.LAUNCH -> go(UiScreen.MENU)
            UiScreen.REWARD -> go(UiScreen.AIRCRAFT)
            else -> go(UiScreen.MENU)
        }
    }

    // ---- drawing ----------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val density = resources.displayMetrics.density
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f || density <= 0f) return

        val designWidth = viewWidth / density
        val designHeight = viewHeight / density
        val time = (System.nanoTime() % 1_000_000_000L) / 1_000_000_000f

        val frame = UiFrame(canvas, app, this, designWidth, designHeight, density, time)

        worldCursor = worldCursor.coerceIn(0, WorldCatalog.all.size - 1)

        val state = app.currentState
        val result = app.lastFlightResult

        canvas.save()
        canvas.scale(density, density)

        when {
            state == GameState.FLIGHT -> FlightHudScreen.drawFlight(frame)
            state == GameState.RESULTS && result != null -> ResultsScreen.draw(frame, result)
            screen == UiScreen.LAUNCH -> FlightHudScreen.drawLaunch(frame)
            screen == UiScreen.AIRCRAFT -> AircraftWorkshopScreen.draw(frame)
            screen == UiScreen.SLINGSHOT -> SlingshotWorkshopScreen.draw(frame)
            screen == UiScreen.WORLDS -> WorldSelectScreen.draw(frame)
            screen == UiScreen.SETTINGS -> SettingsScreen.draw(frame)
            screen == UiScreen.ACHIEVEMENTS -> AchievementsScreen.draw(frame)
            screen == UiScreen.PROFILE -> ProfileScreen.draw(frame)
            screen == UiScreen.REWARD -> RewardScreen.draw(frame)
            else -> MenuScreen.draw(frame)
        }

        if (paused) {
            frame.hits.clear()
            PauseScreen.draw(frame)
        }

        canvas.restore()

        lastHits.clear()
        lastHits.addAll(frame.hits)

        app.cameraMode = when (screen) {
            UiScreen.SLINGSHOT -> CameraMode.ORBIT_SLINGSHOT
            UiScreen.LAUNCH -> CameraMode.LAUNCH
            else -> CameraMode.ORBIT_AIRCRAFT
        }

        postInvalidateOnAnimation()
    }

    // ---- touch ------------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val state = app.currentState
        val flightInput = state == GameState.FLIGHT ||
            (screen == UiScreen.LAUNCH && state == GameState.SEATED)

        // Two-finger pinch zooms the workshop / menu orbit.
        if (!paused && event.pointerCount >= 2) {
            val span = pinchSpan(event)
            if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                if (lastPinchSpan > 0.01f) {
                    app.requestOrbitZoom(span / lastPinchSpan)
                }
            }
            lastPinchSpan = span
            downHit = null
            dragHit = null
            orbitDragging = false
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                orbitDragging = false
                lastPinchSpan = 0f
                val hit = findHit(event.x, event.y)
                if (hit != null) {
                    downHit = hit
                    if (hit.onDrag != null) {
                        dragHit = hit
                        hit.onDrag?.invoke(fractionFor(hit, event.x))
                    }
                } else if (flightInput && !paused) {
                    input.onTouch(event, width, height)
                } else if (!paused) {
                    orbitDragging = true
                    lastOrbitX = event.x
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val slider = dragHit
                if (slider != null) {
                    slider.onDrag?.invoke(fractionFor(slider, event.x))
                    return true
                }
                if (downHit != null) return true
                if (flightInput && !paused) {
                    input.onTouch(event, width, height)
                    return true
                }
                if (orbitDragging && !paused) {
                    val density = resources.displayMetrics.density.coerceAtLeast(0.1f)
                    val delta = (event.x - lastOrbitX) / density
                    lastOrbitX = event.x
                    app.requestOrbitDelta(delta * 0.01f)
                    return true
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                lastPinchSpan = 0f
                if (dragHit != null) {
                    dragHit = null
                    return true
                }
                val hit = downHit
                downHit = null
                if (hit != null) {
                    if (hit.rect.contains(event.x, event.y)) hit.action?.invoke()
                    return true
                }
                if (flightInput && !paused) {
                    input.onTouch(event, width, height)
                }
                orbitDragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                lastPinchSpan = 0f
                downHit = null
                dragHit = null
                orbitDragging = false
                if (flightInput && !paused) input.onTouch(event, width, height)
                return true
            }
        }

        return true
    }

    private fun findHit(x: Float, y: Float): UiHit? {
        for (index in lastHits.indices.reversed()) {
            val hit = lastHits[index]
            if (hit.rect.contains(x, y)) return hit
        }
        return null
    }

    private fun fractionFor(hit: UiHit, x: Float): Float {
        val widthPx = hit.rect.width()
        if (widthPx <= 0.01f) return 0f
        return ((x - hit.rect.left) / widthPx).coerceIn(0f, 1f)
    }

    private fun pinchSpan(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        app.paused = false
        paused = false
    }
}
