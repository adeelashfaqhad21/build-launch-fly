package com.buildlaunchfly.game.ui.screens

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.ui.UiButtonStyle
import com.buildlaunchfly.game.ui.UiFrame
import com.buildlaunchfly.game.ui.UiIcon
import com.buildlaunchfly.game.ui.UiScreen
import com.buildlaunchfly.game.ui.UiTextStyle

/** Screen 3/5 — Launch setup (Ref-3/4) and in-flight HUD (Ref-5/6). */
object FlightHudScreen {

    fun drawLaunch(f: UiFrame) {
        val w = f.width
        val h = f.height

        val pull = f.app.pullAmount()

        // Ref-3 shows the rig bare: the tension gauge only appears once the player draws.
        if (pull > 0.02f) {
            drawPullGauge(f, w / 2f, h * 0.62f, minOf(w - 80f, 260f), pull)
        }

        // First visit only: a ghost arrow that fades out after a few seconds.
        val hint = f.app.launchHintAlpha()
        if (hint > 0.01f && pull < 0.05f) {
            val fade = hint * (0.7f + 0.3f * kotlin.math.abs(kotlin.math.sin(f.time * 3.4f)))
            drawPullHint(f, w / 2f, h * 0.62f + 26f, fade)
        }

        val back = f.rect(w / 2f - 60f, h - DesignTokens.SAFE_BOTTOM - 44f, 120f, 36f)
        f.button(back, "BACK", UiButtonStyle.DANGER) {
            f.nav.go(UiScreen.MENU)
        }
    }

    fun drawFlight(f: UiFrame) {
        val w = f.width
        val h = f.height
        val t = f.app.telemetry()
        val target = f.app.targetDistanceMeters().coerceAtLeast(1f)
        val fraction = (t.distanceMeters / target).coerceIn(0f, 1f)

        drawSpeedDial(f, 58f, 66f, 40f, t.airspeedKmh)
        f.iconButton(f.rect(42f, 116f, 32f, 32f), UiIcon.PAUSE) { f.nav.setPaused(true) }

        val speedKmh = t.airspeedKmh
        if (t.stallWarning) {
            warningLabel(f, "STALL", 140f, 1f)
        } else if (speedKmh < 45f) {
            warningLabel(f, "LOW SPEED", 140f, 0.65f + 0.35f * kotlin.math.abs(kotlin.math.sin(f.time * 4f)))
        }

        f.label(
            "${t.distanceMeters.toInt()}M",
            w / 2f,
            h * 0.47f,
            UiTextStyle.DISPLAY_HERO,
            DesignTokens.WHITE,
            Paint.Align.CENTER,
            outline = true
        )

        drawProgressTrack(f, w - 34f, 24f, 18f, 300f, fraction)

        if (f.app.perfectFlash() > 0f) {
            drawPerfect(f, w / 2f, h * 0.36f, f.app.perfectFlash())
        }
    }

    // ---- launch gauge -----------------------------------------------------

    private fun drawPullHint(f: UiFrame, cx: Float, y: Float, alpha: Float) {
        f.painter.stroke.color = DesignTokens.withAlpha(DesignTokens.WHITE, alpha)
        f.painter.stroke.strokeWidth = 4f
        val path = Path()
        path.moveTo(cx - 13f, y - 14f)
        path.lineTo(cx, y - 2f)
        path.lineTo(cx + 13f, y - 14f)
        f.canvas.drawPath(path, f.painter.stroke)

        f.label(
            "PULL BACK",
            cx,
            y + 18f,
            UiTextStyle.HUD_LABEL,
            DesignTokens.withAlpha(DesignTokens.NAVY, alpha),
            Paint.Align.CENTER
        )
    }

    private fun drawPullGauge(f: UiFrame, cx: Float, cy: Float, width: Float, pull: Float) {
        val radius = width / 2f
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val segments = floatArrayOf(20f, 30f, 40f, 30f, 20f)
        val colors = intArrayOf(
            DesignTokens.RED,
            DesignTokens.ORANGE,
            DesignTokens.GREEN,
            DesignTokens.YELLOW,
            DesignTokens.RED
        )
        var start = 200f
        for (i in segments.indices) {
            f.painter.stroke.color = if (pull > 0.02f) colors[i] else DesignTokens.withAlpha(colors[i], 0.35f)
            f.painter.stroke.strokeWidth = 10f
            f.painter.stroke.strokeCap = Paint.Cap.BUTT
            f.canvas.drawArc(arcRect, start, segments[i], false, f.painter.stroke)
            start += segments[i]
        }
        f.painter.stroke.strokeWidth = 2f
        f.painter.stroke.color = DesignTokens.withAlpha(DesignTokens.WHITE, 0.8f)
        val pointer = 200f + pull.coerceIn(0f, 1f) * 140f
        f.canvas.drawArc(arcRect, pointer - 5f, 10f, false, f.painter.stroke)
    }

    // ---- flight instruments ----------------------------------------------

    private fun drawSpeedDial(f: UiFrame, cx: Float, cy: Float, r: Float, kmh: Float) {
        f.circle(cx, cy, r + 3f, DesignTokens.withAlpha(DesignTokens.INK, 0.25f))
        f.circle(cx, cy, r + 2f, DesignTokens.WHITE)
        f.circle(cx, cy, r - 1f, 0xFF39BCE6.toInt())

        val dial = RectF(cx - r + 4f, cy - r + 4f, cx + r - 4f, cy + r - 4f)
        f.painter.stroke.strokeWidth = 5f
        f.painter.stroke.color = DesignTokens.withAlpha(DesignTokens.GREEN, 0.85f)
        f.canvas.drawArc(dial, -122f, 119f, false, f.painter.stroke)

        val start = -218f + (kmh / 120f).coerceIn(0f, 1f) * 256f
        f.painter.stroke.strokeWidth = 2.5f
        f.painter.stroke.color = DesignTokens.INK
        f.canvas.save()
        f.canvas.rotate(start + 90f, cx, cy)
        f.canvas.drawLine(cx, cy, cx, cy - r + 10f, f.painter.stroke)
        f.canvas.restore()

        f.label(
            kmh.toInt().toString(),
            cx,
            cy + 12f,
            UiTextStyle.H1,
            DesignTokens.WHITE,
            Paint.Align.CENTER,
            outline = true
        )
        f.label("KM/H", cx, cy + 24f, UiTextStyle.BADGE, DesignTokens.WHITE, Paint.Align.CENTER, outline = true)
    }

    private fun warningLabel(f: UiFrame, text: String, y: Float, alpha: Float) {
        f.label(
            text,
            58f,
            y,
            UiTextStyle.HUD_LABEL,
            DesignTokens.withAlpha(DesignTokens.RED, alpha),
            Paint.Align.CENTER,
            outline = true
        )
    }

    private fun drawProgressTrack(f: UiFrame, cx: Float, top: Float, width: Float, height: Float, fraction: Float) {
        val track = RectF(cx - width / 2f, top, cx + width / 2f, top + height)
        f.panel(track, width / 2f, DesignTokens.CARD, DesignTokens.CARD_EDGE)
        val inner = RectF(track.left + 4f, track.top + 4f, track.right - 4f, track.bottom - 4f)
        f.solid(inner, width / 2f, DesignTokens.withAlpha(DesignTokens.SKY, 0.25f))

        val flagY = top - 30f
        for (row in 0 until 2) {
            for (col in 0 until 4) {
                val color = if ((row + col) % 2 == 0) DesignTokens.INK else DesignTokens.WHITE
                f.solid(
                    RectF(track.left - 12f + col * 7f, flagY + row * 7f, track.left - 5f + col * 7f, flagY + (row + 1) * 7f),
                    0f,
                    color
                )
            }
        }

        val markerY = inner.bottom - inner.height() * fraction
        f.circle(cx, markerY, 7f, DesignTokens.YELLOW)
        f.ringCircle(cx, cy = markerY, radius = 7f, color = DesignTokens.BLUE, width = 2f)
        f.label(
            "${(fraction * 100f).toInt()}%",
            cx - 12f,
            markerY + 3f,
            UiTextStyle.BADGE,
            DesignTokens.WHITE,
            Paint.Align.RIGHT,
            outline = true
        )
    }

    private fun drawPerfect(f: UiFrame, cx: Float, cy: Float, remaining: Float) {
        val t = (1.4f - f.app.perfectFlash()) / 1.4f
        val scale = 0.6f + kotlin.math.sin(kotlin.math.min(t * 3.4f, 3.14159f)) * 0.5f
        f.canvas.save()
        f.canvas.translate(cx, cy)
        f.canvas.scale(scale, scale)
        f.label(
            "PERFECT!",
            0f,
            0f,
            UiTextStyle.DISPLAY_HERO,
            DesignTokens.WHITE,
            Paint.Align.CENTER,
            outline = true
        )
        f.canvas.restore()
    }
}
