package com.buildlaunchfly.game.ui.screens

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.ui.UiFormat
import com.buildlaunchfly.game.ui.UiFrame
import com.buildlaunchfly.game.ui.UiIcon
import com.buildlaunchfly.game.ui.UiScreen
import com.buildlaunchfly.game.ui.UiTextStyle
import com.buildlaunchfly.game.world.WorldCatalog

/** Screen 5 — Launch and in-flight HUD (portrait). */
object FlightHudScreen {

    fun drawLaunch(f: UiFrame) {
        val w = f.width
        val h = f.height
        val world = WorldCatalog.byId(f.app.playerProfile.selectedWorld)

        f.iconButton(f.rect(DesignTokens.MARGIN_H, DesignTokens.SAFE_TOP, 36f, 36f), UiIcon.BACK) {
            f.nav.go(UiScreen.MENU)
        }

        f.label(
            world.displayName.uppercase(),
            w / 2f,
            DesignTokens.SAFE_TOP + 24f,
            UiTextStyle.HUD_LABEL,
            DesignTokens.TEXT_MUTED,
            Paint.Align.CENTER
        )

        val pull = f.app.pullAmount()
        val speedMs = f.app.slingshotState.launchSpeed(pull, f.app.aircraftBuild.stats().mass)

        f.label(
            "PULL BACK AND RELEASE",
            w / 2f,
            h - DesignTokens.SAFE_BOTTOM - 74f,
            UiTextStyle.HUD_LABEL,
            DesignTokens.TEXT_PRIMARY,
            Paint.Align.CENTER
        )

        val meter = f.rect(
            DesignTokens.MARGIN_H,
            h - DesignTokens.SAFE_BOTTOM - 58f,
            w - DesignTokens.MARGIN_H * 2f,
            12f
        )
        f.progressBar(
            meter,
            pull,
            DesignTokens.mix(DesignTokens.COLOR_COIN_GOLD, DesignTokens.COLOR_PRIMARY_GLOW, pull)
        )

        f.label(
            "${UiFormat.percent(pull)}  \u2022  ${UiFormat.kmh(speedMs * 3.6f)}",
            w / 2f,
            h - DesignTokens.SAFE_BOTTOM - 14f,
            UiTextStyle.BADGE,
            DesignTokens.TEXT_MUTED,
            Paint.Align.CENTER
        )
    }

    fun drawFlight(f: UiFrame) {
        val w = f.width
        val t = f.app.telemetry()

        f.iconButton(f.rect(DesignTokens.MARGIN_H, 10f, 36f, 36f), UiIcon.PAUSE) {
            f.nav.setPaused(true)
        }
        f.label(UiFormat.time(t.flightTimeSeconds), DesignTokens.MARGIN_H + 8f, 58f, UiTextStyle.BADGE)

        val panel = f.rect(DesignTokens.MARGIN_H + 44f, 10f, w - DesignTokens.MARGIN_H * 2f - 44f, 62f)
        f.panel(panel, 12f)

        column(f, panel.left + 46f, "DIST", UiFormat.metersInt(t.distanceMeters))
        column(f, panel.centerX(), "ALT", UiFormat.metersInt(t.altitudeMeters))
        column(f, panel.right - 46f, "SPD", "${t.airspeedKmh.toInt()}")

        val banner = f.rect(DesignTokens.MARGIN_H, 80f, w - DesignTokens.MARGIN_H * 2f, 22f)
        if (t.stallWarning) {
            f.panel(banner, 8f, DesignTokens.withAlpha(DesignTokens.COLOR_HAZARD, 0.8f), DesignTokens.COLOR_HAZARD)
            f.labelIn(banner, "STALL \u2014 PITCH NOSE DOWN", UiTextStyle.BADGE, DesignTokens.TEXT_PRIMARY)
        } else if (t.climbing && t.verticalSpeed > 5f) {
            f.panel(banner, 8f, DesignTokens.withAlpha(DesignTokens.COLOR_PRIMARY_AERO, 0.78f), DesignTokens.COLOR_PRIMARY_GLOW)
            val pulse = 0.65f + 0.35f * kotlin.math.sin(f.time * 5f)
            f.labelIn(
                banner,
                "THERMAL CAUGHT  +${t.verticalSpeed.toInt()} m/s",
                UiTextStyle.BADGE,
                DesignTokens.withAlpha(DesignTokens.TEXT_PRIMARY, pulse)
            )
        }

        drawGimbal(f, DesignTokens.MARGIN_H + 46f, 180f, 38f, t.pitchDegrees, t.rollDegrees)
    }

    private fun column(f: UiFrame, x: Float, caption: String, value: String) {
        f.label(caption, x, 30f, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED, Paint.Align.CENTER)
        f.label(value, x, 54f, UiTextStyle.H1, DesignTokens.TEXT_PRIMARY, Paint.Align.CENTER)
    }

    private fun drawGimbal(
        f: UiFrame,
        cx: Float,
        cy: Float,
        radius: Float,
        pitchDegrees: Float,
        rollDegrees: Float
    ) {
        f.ringCircle(cx, cy, radius, DesignTokens.withAlpha(DesignTokens.COLOR_PRIMARY_GLOW, 0.4f), 1f)

        val clip = Path()
        clip.addCircle(cx, cy, radius - 2f, Path.Direction.CW)

        f.canvas.save()
        f.canvas.clipPath(clip)
        f.canvas.rotate(-rollDegrees, cx, cy)

        val pitchOffset = pitchDegrees.coerceIn(-45f, 45f) * 0.9f

        f.painter.stroke.color = DesignTokens.withAlpha(DesignTokens.COLOR_SUCCESS, 0.85f)
        f.painter.stroke.strokeWidth = 1.5f
        f.canvas.drawLine(cx - radius, cy + pitchOffset, cx + radius, cy + pitchOffset, f.painter.stroke)

        f.painter.stroke.color = DesignTokens.withAlpha(DesignTokens.TEXT_PRIMARY, 0.4f)
        f.painter.stroke.strokeWidth = 1f
        for (i in -3..3) {
            val y = cy + pitchOffset + i * 12f
            f.canvas.drawLine(cx - 12f, y, cx + 12f, y, f.painter.stroke)
        }

        f.canvas.restore()
        f.circle(cx, cy, 2f, DesignTokens.COLOR_PRIMARY_GLOW)
        f.ringCircle(cx, cy, radius, DesignTokens.withAlpha(DesignTokens.COLOR_PRIMARY_GLOW, 0.25f), 1f)
    }
}
