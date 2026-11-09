package com.buildlaunchfly.game.ui.screens

import android.graphics.Paint
import android.graphics.RectF
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.slingshot.SlingUpgrade
import com.buildlaunchfly.game.slingshot.SlingshotState
import com.buildlaunchfly.game.ui.UiFormat
import com.buildlaunchfly.game.ui.UiFrame
import com.buildlaunchfly.game.ui.UiTextStyle

/** Screen 3 — Slingshot Workshop / Impulse Core lab (portrait). */
object SlingshotWorkshopScreen {

    fun draw(f: UiFrame) {
        val contentTop = f.header("SLINGSHOT LAB")
        val w = f.width
        val h = f.height

        drawTelemetry(f, f.rect(DesignTokens.MARGIN_H, contentTop + 6f, w - DesignTokens.MARGIN_H * 2f, 138f))
        drawUpgrades(f, f.rect(DesignTokens.MARGIN_H, contentTop + 152f, w - DesignTokens.MARGIN_H * 2f, 200f))

        val angle = f.app.playerProfile.launchAngleDegrees
        val labelY = contentTop + 364f
        f.label("LAUNCH ELEVATION ARC", DesignTokens.MARGIN_H, labelY, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)
        f.label(
            "${angle.toInt()}\u00b0",
            w - DesignTokens.MARGIN_H,
            labelY,
            UiTextStyle.BADGE,
            DesignTokens.COLOR_PRIMARY_GLOW,
            Paint.Align.RIGHT
        )
        f.slider(
            f.rect(DesignTokens.MARGIN_H, labelY + 8f, w - DesignTokens.MARGIN_H * 2f, 16f),
            ((angle - 20f) / 35f).coerceIn(0f, 1f)
        ) { fraction ->
            f.app.playerProfile.launchAngleDegrees = 20f + fraction * 35f
            f.app.requestSaveProfile()
        }

        f.label(
            "UPGRADES RAISE DRAW LENGTH, TENSION AND RELEASE EFFICIENCY",
            w / 2f,
            h - DesignTokens.SAFE_BOTTOM - 4f,
            UiTextStyle.BADGE,
            DesignTokens.TEXT_MUTED,
            Paint.Align.CENTER
        )
    }

    private fun drawTelemetry(f: UiFrame, r: RectF) {
        f.panel(r)
        f.label("LAUNCH TELEMETRY", r.left + 12f, r.top + 20f, UiTextStyle.HUD_LABEL, DesignTokens.TEXT_PRIMARY)

        val state = f.app.slingshotState
        val stats = f.app.aircraftBuild.stats()
        val speed = state.launchSpeed(1f, stats.mass)
        val impulse = speed * stats.mass

        var y = r.top + 42f
        y = row(f, r, "BASE VELOCITY", "%d km/h".format((speed * 3.6f).toInt()), y)
        y = row(f, r, "IMPULSE FORCE", "%,d N".format(impulse.toInt()), y)
        y = row(f, r, "DRAW LENGTH", "%.2f m".format(state.drawLengthMeters()), y)
        y = row(f, r, "BAND THICKNESS", "x%.2f".format(state.bandThickness()), y)
        row(f, r, "POWER RATING", UiFormat.percent(state.powerRating()), y)
    }

    private fun row(f: UiFrame, panel: RectF, label: String, value: String, y: Float): Float {
        f.label(label, panel.left + 12f, y, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)
        f.label(
            value,
            panel.right - 12f,
            y,
            UiTextStyle.BODY,
            DesignTokens.COLOR_PRIMARY_GLOW,
            Paint.Align.RIGHT
        )
        return y + 18f
    }

    private fun drawUpgrades(f: UiFrame, r: RectF) {
        f.panel(r)
        f.label("MECHANICAL UPGRADES", r.left + 12f, r.top + 20f, UiTextStyle.HUD_LABEL, DesignTokens.TEXT_PRIMARY)

        val state = f.app.slingshotState
        val coins = f.app.playerProfile.coins
        var y = r.top + 30f
        val rowHeight = 34f

        for (type in SlingUpgrade.values()) {
            drawRow(f, r, type, state, coins, y, rowHeight)
            y += rowHeight
        }
    }

    private fun drawRow(
        f: UiFrame,
        panel: RectF,
        type: SlingUpgrade,
        state: SlingshotState,
        coins: Int,
        y: Float,
        height: Float
    ) {
        val level = state.levelOf(type)
        val maxed = state.isMaxed(type)
        val cost = state.costToUpgrade(type)

        f.label(type.displayName, panel.left + 12f, y + 12f, UiTextStyle.BODY_LARGE, DesignTokens.TEXT_PRIMARY)

        val pipX = panel.left + 12f
        for (i in 0 until SlingshotState.MAX_LEVEL) {
            val pip = RectF(pipX + i * 5.5f, y + 18f, pipX + i * 5.5f + 3.5f, y + 25f)
            f.solid(
                pip,
                1f,
                if (i < level) DesignTokens.COLOR_PRIMARY_GLOW else DesignTokens.withAlpha(0xFFFFFFFF.toInt(), 0.16f)
            )
        }

        val action = RectF(panel.right - 86f, y + 5f, panel.right - 10f, y + 27f)
        if (maxed) {
            f.label("MAX LEVEL", action.centerX(), action.centerY() + 4f, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED, Paint.Align.CENTER)
        } else {
            val affordable = coins >= cost
            f.solid(
                action,
                6f,
                if (affordable) DesignTokens.COLOR_PRIMARY_AERO else DesignTokens.withAlpha(0xFFFFFFFF.toInt(), 0.10f)
            )
            f.labelIn(action, UiFormat.coins(cost), UiTextStyle.BADGE, DesignTokens.TEXT_PRIMARY)
            if (affordable) {
                f.hit(action, { f.app.requestUpgradeSlingshot(type) })
            }
        }
    }
}
