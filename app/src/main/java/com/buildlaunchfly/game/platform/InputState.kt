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
    private var pendingCancel = false

    fun onTouch(event: MotionEvent, width: Int, height: Int) {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        synchronized(lock) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pointerDown = true
                    lastX = event.x
                    lastY = event.y
                    hasLast = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!hasLast) {
                        lastX = event.x
                        lastY = event.y
                        hasLast = true
                    }
                    pendingDragX += (event.x - lastX) / safeWidth * 2f
                    pendingDragY += (event.y - lastY) / safeHeight * 2f
                    lastX = event.x
                    lastY = event.y
                }
                MotionEvent.ACTION_UP -> {
                    pointerDown = false
                    hasLast = false
                    pendingRelease = true
                }
                MotionEvent.ACTION_CANCEL -> {
                    pointerDown = false
                    hasLast = false
                    pendingDragX = 0f
                    pendingDragY = 0f
                    pendingCancel = true
                }
                else -> {
                    // Additional pointers are ignored until the gesture design lands.
                }
            }
        }
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

    fun consumeCancel(): Boolean {
        synchronized(lock) {
            val value = pendingCancel
            pendingCancel = false
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
            pendingCancel = false
        }
    }
}
