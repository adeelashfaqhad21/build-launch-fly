package com.buildlaunchfly.game.ui.screens

import android.graphics.Paint
import android.graphics.RectF
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.ui.UiFrame
import com.buildlaunchfly.game.ui.UiTextStyle

/**
 * Screen 1 — splash / loading (Ref-1). Drawn over the surface while the renderer builds its
 * textures and the first world, so the wait is branded instead of a black window.
 */
object SplashScreen {

    fun draw(f: UiFrame) {
        val w = f.width
        val h = f.height
        val progress = f.app.loadProgress().coerceIn(0f, 1f)

        f.solid(RectF(0f, 0f, w, h), 0f, DesignTokens.SKY)
        f.solid(RectF(0f, h * 0.72f, w, h), 0f, DesignTokens.SAND)
        f.circle(w * 0.80f, h * 0.18f, 46f, DesignTokens.withAlpha(DesignTokens.YELLOW, 0.35f))
        f.circle(w * 0.80f, h * 0.18f, 30f, DesignTokens.YELLOW)

        f.label(
            "BUILD",
            w / 2f,
            h * 0.36f,
            UiTextStyle.DISPLAY_HERO,
            DesignTokens.WHITE,
            Paint.Align.CENTER,
            outline = true
        )
        f.label(
            "LAUNCH • FLY",
            w / 2f,
            h * 0.36f + 34f,
            UiTextStyle.H1,
            DesignTokens.WHITE,
            Paint.Align.CENTER,
            outline = true
        )

        val radius = 30f
        val cx = w / 2f
        val cy = h * 0.60f
        f.circle(cx, cy, radius + 4f, DesignTokens.withAlpha(DesignTokens.INK, 0.20f))
        f.circle(cx, cy, radius, DesignTokens.CARD)
        val arc = RectF(cx - radius + 5f, cy - radius + 5f, cx + radius - 5f, cy + radius - 5f)
        f.painter.stroke.strokeWidth = 6f
        f.painter.stroke.color = DesignTokens.GREEN
        f.canvas.drawArc(arc, -90f, 360f * progress.coerceAtLeast(0.04f), false, f.painter.stroke)

        f.label(
            "${(progress * 100f).toInt()}%",
            cx,
            cy + 6f,
            UiTextStyle.H2,
            DesignTokens.INK,
            Paint.Align.CENTER
        )
        f.label(
            "LOADING",
            cx,
            cy + radius + 26f,
            UiTextStyle.HUD_LABEL,
            DesignTokens.INK,
            Paint.Align.CENTER
        )
    }
}
