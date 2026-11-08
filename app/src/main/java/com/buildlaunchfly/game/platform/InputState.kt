package com.buildlaunchfly.game.platform

import android.view.MotionEvent

/** Touch state shared between the Android UI thread and the GL thread. */
class InputState {

    private val lock = Any()

    @Volatile
    var pointerDown: Boolean = false
        private set

    private var lastX = 0f
    private var lastY = 0f
    private var hasLast = false

    private var pendingDragX = 0f
    private var pendingDragY = 0f
    private var pendingRelease = false

    private var downX = 0f
    private var downY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var viewHeight = 640f

    fun onTouch(event: MotionEvent, width: Int, height: Int) {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        synchronized(lock) {
            viewHeight = safeHeight.toFloat()
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pointerDown = true
                    lastX = event.x
                    lastY = event.y
                    downX = event.x
                    downY = event.y
                    currentX = event.x
                    currentY = event.y
                    hasLast = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!hasLast) {
                        lastX = event.x
                        lastY = event.y
                        downX = event.x
                        downY = event.y
                        hasLast = true
                    }
                    currentX = event.x
                    currentY = event.y
                    pendingDragX += (event.x - lastX) / safeWidth * 2f
                    pendingDragY += (event.y - lastY) / safeHeight * 2f
                    lastX = event.x
                    lastY = event.y
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    pointerDown = false
                    hasLast = false
                    pendingRelease = true
                }
                else -> {
                    // Additional pointers are ignored until the gesture design lands.
                }
            }
        }
    }

    /**
     * Virtual stick for flight: displacement from where the finger went down, normalised so
     * a quarter of the screen height is full deflection. x is right-positive, y is
     * down-positive; it centres itself when the finger lifts.
     */
    fun stick(out: FloatArray) {
        synchronized(lock) {
            if (!pointerDown) {
                out[0] = 0f
                out[1] = 0f
                return
            }
            val radius = (0.22f * viewHeight).coerceAtLeast(1f)
            val dx = (currentX - downX) / radius
            val dy = (currentY - downY) / radius
            out[0] = deadZone(dx)
            out[1] = deadZone(dy)
        }
    }

    private fun deadZone(value: Float): Float {
        val clamped = value.coerceIn(-1f, 1f)
        val magnitude = kotlin.math.abs(clamped)
        if (magnitude <= 0.10f) return 0f
        val scaled = (magnitude - 0.10f) / 0.90f
        return if (clamped < 0f) -scaled else scaled
    }

    /** Copies and clears the accumulated drag since the last frame into [out] (dx, dy). */
    fun consumeDrag(out: FloatArray) {
        synchronized(lock) {
            out[0] = pendingDragX
            out[1] = pendingDragY
            pendingDragX = 0f
            pendingDragY = 0f
        }
    }

    fun consumeRelease(): Boolean {
        synchronized(lock) {
            val value = pendingRelease
            pendingRelease = false
            return value
        }
    }

    /** Clears any in-flight gesture (used when the UI opens a modal). */
    fun reset() {
        synchronized(lock) {
            pointerDown = false
            hasLast = false
            pendingDragX = 0f
            pendingDragY = 0f
            pendingRelease = false
        }
    }
}
