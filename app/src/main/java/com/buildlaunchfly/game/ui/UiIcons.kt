package com.buildlaunchfly.game.ui

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import com.buildlaunchfly.game.gfx.DesignTokens

/** Vector icons from design.md §7.1, drawn as paths so they scale at any density. */
enum class UiIcon { BACK, GEAR, PLANE, SLINGSHOT, GLOBE, COIN, TROPHY, PLAY, LOCK, CHECK, PAUSE, USER, GEM, CHEST, ROCKET, STORE, VIDEO }

object UiIcons {

    fun draw(
        canvas: Canvas,
        painter: UiPainter,
        icon: UiIcon,
        bounds: RectF,
        color: Int
    ) {
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val size = minOf(bounds.width(), bounds.height()) * 0.42f

        painter.stroke.color = color
        painter.stroke.strokeWidth = size * 0.16f
        painter.fill.color = color

        when (icon) {
            UiIcon.BACK -> {
                canvas.drawLine(cx + size * 0.5f, cy - size, cx - size * 0.4f, cy, painter.stroke)
                canvas.drawLine(cx - size * 0.4f, cy, cx + size * 0.5f, cy + size, painter.stroke)
            }

            UiIcon.GEAR -> {
                canvas.drawCircle(cx, cy, size * 0.5f, painter.stroke)
                canvas.drawCircle(cx, cy, size * 0.2f, painter.stroke)
                for (i in 0 until 6) {
                    val angle = i * 60.0 * 0.0174533
                    val c = kotlin.math.cos(angle).toFloat()
                    val s = kotlin.math.sin(angle).toFloat()
                    canvas.drawLine(
                        cx + c * size * 0.58f, cy + s * size * 0.58f,
                        cx + c * size * 0.95f, cy + s * size * 0.95f,
                        painter.stroke
                    )
                }
            }

            UiIcon.PLANE -> {
                val path = Path()
                path.moveTo(cx, cy - size)
                path.lineTo(cx - size * 1.1f, cy + size * 0.5f)
                path.lineTo(cx - size * 0.2f, cy + size * 0.3f)
                path.lineTo(cx - size * 0.35f, cy + size)
                path.lineTo(cx, cy + size * 0.6f)
                path.lineTo(cx + size * 0.35f, cy + size)
                path.lineTo(cx + size * 0.2f, cy + size * 0.3f)
                path.lineTo(cx + size * 1.1f, cy + size * 0.5f)
                path.close()
                canvas.drawPath(path, painter.fill)
            }

            UiIcon.SLINGSHOT -> {
                canvas.drawLine(cx, cy + size, cx, cy, painter.stroke)
                canvas.drawLine(cx, cy, cx - size * 0.8f, cy - size * 0.85f, painter.stroke)
                canvas.drawLine(cx, cy, cx + size * 0.8f, cy - size * 0.85f, painter.stroke)
                canvas.drawLine(
                    cx - size * 0.8f, cy - size * 0.85f,
                    cx + size * 0.8f, cy - size * 0.85f,
                    painter.stroke
                )
            }

            UiIcon.GLOBE -> {
                canvas.drawCircle(cx, cy, size * 0.92f, painter.stroke)
                canvas.drawOval(
                    RectF(cx - size * 0.45f, cy - size * 0.92f, cx + size * 0.45f, cy + size * 0.92f),
                    painter.stroke
                )
                canvas.drawLine(cx - size * 0.92f, cy, cx + size * 0.92f, cy, painter.stroke)
            }

            UiIcon.COIN -> {
                canvas.drawCircle(cx, cy, size * 0.92f, painter.fill)
                painter.stroke.color = DesignTokens.TEXT_INVERSE
                painter.stroke.strokeWidth = size * 0.14f
                canvas.drawCircle(cx, cy, size * 0.58f, painter.stroke)
            }

            UiIcon.TROPHY -> {
                canvas.drawArc(
                    RectF(cx - size * 0.7f, cy - size * 0.9f, cx + size * 0.7f, cy + size * 0.3f),
                    0f, 180f, false, painter.stroke
                )
                canvas.drawLine(cx, cy + size * 0.25f, cx, cy + size * 0.65f, painter.stroke)
                canvas.drawLine(cx - size * 0.5f, cy + size * 0.85f, cx + size * 0.5f, cy + size * 0.85f, painter.stroke)
            }

            UiIcon.PLAY -> {
                val path = Path()
                path.moveTo(cx - size * 0.55f, cy - size * 0.9f)
                path.lineTo(cx + size * 0.9f, cy)
                path.lineTo(cx - size * 0.55f, cy + size * 0.9f)
                path.close()
                canvas.drawPath(path, painter.fill)
            }

            UiIcon.LOCK -> {
                canvas.drawRoundRect(
                    RectF(cx - size * 0.75f, cy - size * 0.15f, cx + size * 0.75f, cy + size * 0.9f),
                    4f, 4f, painter.stroke
                )
                canvas.drawArc(
                    RectF(cx - size * 0.5f, cy - size * 0.9f, cx + size * 0.5f, cy + size * 0.2f),
                    180f, 180f, false, painter.stroke
                )
            }

            UiIcon.CHECK -> {
                canvas.drawLine(cx - size * 0.8f, cy, cx - size * 0.15f, cy + size * 0.6f, painter.stroke)
                canvas.drawLine(cx - size * 0.15f, cy + size * 0.6f, cx + size * 0.9f, cy - size * 0.7f, painter.stroke)
            }

            UiIcon.PAUSE -> {
                canvas.drawRoundRect(
                    RectF(cx - size * 0.6f, cy - size * 0.9f, cx - size * 0.15f, cy + size * 0.9f),
                    2f, 2f, painter.fill
                )
                canvas.drawRoundRect(
                    RectF(cx + size * 0.15f, cy - size * 0.9f, cx + size * 0.6f, cy + size * 0.9f),
                    2f, 2f, painter.fill
                )
            }

            UiIcon.USER -> {
                canvas.drawCircle(cx, cy - size * 0.4f, size * 0.42f, painter.fill)
                canvas.drawArc(
                    RectF(cx - size * 0.85f, cy + size * 0.05f, cx + size * 0.85f, cy + size * 1.5f),
                    180f, 180f, true, painter.fill
                )
            }

            UiIcon.GEM -> {
                val path = Path()
                path.moveTo(cx, cy - size)
                path.lineTo(cx + size * 0.85f, cy - size * 0.1f)
                path.lineTo(cx, cy + size)
                path.lineTo(cx - size * 0.85f, cy - size * 0.1f)
                path.close()
                canvas.drawPath(path, painter.fill)
                painter.stroke.color = DesignTokens.WHITE
                painter.stroke.strokeWidth = size * 0.1f
                canvas.drawLine(cx - size * 0.5f, cy - size * 0.1f, cx + size * 0.5f, cy - size * 0.1f, painter.stroke)
            }

            UiIcon.CHEST -> {
                canvas.drawRoundRect(
                    RectF(cx - size, cy - size * 0.35f, cx + size, cy + size),
                    3f, 3f, painter.fill
                )
                painter.stroke.color = DesignTokens.WHITE
                painter.stroke.strokeWidth = size * 0.12f
                canvas.drawLine(cx - size, cy - size * 0.35f, cx + size, cy - size * 0.35f, painter.stroke)
                canvas.drawRoundRect(
                    RectF(cx - size * 0.2f, cy + size * 0.05f, cx + size * 0.2f, cy + size * 0.5f),
                    2f, 2f, painter.stroke
                )
            }

            UiIcon.ROCKET -> {
                val path = Path()
                path.moveTo(cx, cy - size)
                path.lineTo(cx + size * 0.45f, cy + size * 0.15f)
                path.lineTo(cx + size * 0.2f, cy + size * 0.85f)
                path.lineTo(cx - size * 0.2f, cy + size * 0.85f)
                path.lineTo(cx - size * 0.45f, cy + size * 0.15f)
                path.close()
                canvas.drawPath(path, painter.fill)
                painter.stroke.color = DesignTokens.WHITE
                painter.stroke.strokeWidth = size * 0.12f
                canvas.drawCircle(cx, cy - size * 0.25f, size * 0.22f, painter.stroke)
            }

            UiIcon.STORE -> {
                canvas.drawRoundRect(
                    RectF(cx - size, cy - size * 0.55f, cx + size, cy + size),
                    3f, 3f, painter.fill
                )
                painter.stroke.color = DesignTokens.WHITE
                painter.stroke.strokeWidth = size * 0.12f
                canvas.drawLine(cx - size, cy - size * 0.2f, cx + size, cy - size * 0.2f, painter.stroke)
                canvas.drawLine(cx - size * 0.3f, cy + size, cx - size * 0.3f, cy + size * 0.2f, painter.stroke)
                canvas.drawLine(cx + size * 0.3f, cy + size, cx + size * 0.3f, cy + size * 0.2f, painter.stroke)
            }

            UiIcon.VIDEO -> {
                canvas.drawRoundRect(
                    RectF(cx - size, cy - size * 0.6f, cx + size, cy + size * 0.6f),
                    3f, 3f, painter.fill
                )
                val path = Path()
                path.moveTo(cx - size * 0.25f, cy - size * 0.35f)
                path.lineTo(cx + size * 0.45f, cy)
                path.lineTo(cx - size * 0.25f, cy + size * 0.35f)
                path.close()
                painter.fill.color = DesignTokens.WHITE
                canvas.drawPath(path, painter.fill)
            }
        }
    }
}
