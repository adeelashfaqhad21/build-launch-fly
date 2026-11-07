package com.buildlaunchfly.game.ui.screens

import android.graphics.Paint
import android.graphics.RectF
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.slingshot.SlingUpgrade
import com.buildlaunchfly.game.slingshot.SlingshotState
import com.buildlaunchfly.game.ui.UiFormat
import com.buildlaunchfly.game.ui.UiFrame
import com.buildlaunchfly.game.ui.UiTextStyle

/** Screen 3 — Slingshot Workshop / Impulse Core lab (design.md §6). */
object SlingshotWorkshopScreen {

    fun draw(f: UiFrame) {
        val contentTop = f.header("SLINGSHOT LAB")
        val w = f.width
        val h = f.height

        drawTelemetry(f, f.rect(DesignTokens.MARGIN_H, contentTop + 8f, 196f, 190f))
        drawUpgrades(f, f.rect(w - DesignTokens.MARGIN_H - 320f, contentTop + 8f, 320f, 190f))

        val angle = f.app.playerProfile.launchAngleDegrees
        f.label("LOW-ALTITUDE LAUNCH ELEVATION", DesignTokens.MARGIN_H, contentTop + 214f, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)
        f.label(
            "${angle.toInt()}\u00b0",
            DesignTokens.MARGIN_H + 196f,
            contentTop + 214f,
            UiTextStyle.BADGE,
            DesignTokens.COLOR_PRIMARY_GLOW,
            Paint.Align.RIGHT
        )
        f.slider(
            f.rect(DesignTokens.MARGIN_H, contentTop + 222f, 196f, 16f),
            ((angle - 2f) / 6f).coerceIn(0f, 1f)
        ) { fraction ->
            f.app.playerProfile.launchAngleDegrees = 2f + fraction * 6f
            f.app.requestSaveProfile()
        }

        f.label(
            "LEVELS IMPROVE DRAW LENGTH, BAND TENSION AND RELEASE EFFICIENCY",
            w / 2f,
            h - DesignTokens.SAFE_BOTTOM - 6f,
            UiTextStyle.BADGE,
            DesignTokens.TEXT_MUTED,
            Paint.Align.CENTER
        )
    }

    private fun drawTelemetry(f: UiFrame, r: RectF) {
        f.panel(r)
        f.label("LAUNCH TELEMETRY", r.left + 12f, r.top + 22f, UiTextStyle.HUD_LABEL, DesignTokens.TEXT_PRIMARY)

        val state = f.app.slingshotState
        val stats = f.app.aircraftBuild.stats()
        val speed = state.launchSpeed(1f, stats.mass)
        val momentum = speed * stats.mass

        var y = r.top + 48f
        y = row(f, r, "BASE VELOCITY", "%d km/h".format((speed * 3.6f).toInt()), y)
        y = row(f, r, "LAUNCH MOMENTUM", "%,d kg·m/s".format(momentum.toInt()), y)
        y = row(f, r, "DRAW LENGTH", "%.2f m".format(state.drawLengthMeters()), y)
        y = row(f, r, "BAND THICKNESS", "x%.2f".format(state.bandThickness()), y)
        row(f, r, "POWER RATING", UiFormat.percent(state.powerRating()), y)
        drawTensionGrid(f, r, state)
    }

    /** Compact live upgrade readout: each cell represents one of twelve capacity steps. */
    private fun drawTensionGrid(f: UiFrame, panel: RectF, state: SlingshotState) {
        val strength = state.levelOf(SlingUpgrade.BAND_STRENGTH).coerceIn(0, SlingshotState.MAX_LEVEL)
        val elasticity = state.levelOf(SlingUpgrade.BAND_ELASTICITY).coerceIn(0, SlingshotState.MAX_LEVEL)
        val left = panel.left + 12f
        val right = panel.right - 12f
        val cellGap = 2f
        val cellWidth = (right - left - cellGap * (SlingshotState.MAX_LEVEL - 1)) / SlingshotState.MAX_LEVEL
        val labelY = panel.bottom - 28f
        f.label("TENSION CAPACITY", left, labelY, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)

        val strengthY = panel.bottom - 19f
        val returnY = panel.bottom - 9f
        for (index in 0 until SlingshotState.MAX_LEVEL) {
            val x = left + index * (cellWidth + cellGap)
            val activeStrength = index < strength
            val activeReturn = index < elasticity
            val strengthColor = if (activeStrength) DesignTokens.COLOR_COIN_GOLD
                else DesignTokens.withAlpha(0xFFFFFFFF.toInt(), 0.12f)
            val returnColor = if (activeReturn) DesignTokens.COLOR_PRIMARY_GLOW
                else DesignTokens.withAlpha(0xFFFFFFFF.toInt(), 0.12f)
            f.solid(RectF(x, strengthY, x + cellWidth, strengthY + 4f), 1.5f, strengthColor)
            f.solid(RectF(x, returnY, x + cellWidth, returnY + 4f), 1.5f, returnColor)
        }
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
        return y + 22f
    }

    private fun drawUpgrades(f: UiFrame, r: RectF) {
        f.panel(r)
        f.label("MECHANICAL UPGRADES", r.left + 12f, r.top + 22f, UiTextStyle.HUD_LABEL, DesignTokens.TEXT_PRIMARY)

        val state = f.app.slingshotState
        val coins = f.app.playerProfile.coins
        var y = r.top + 34f
        val rowHeight = 24f

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
            val pip = RectF(pipX + i * 5.5f, y + 17f, pipX + i * 5.5f + 3.5f, y + 24f)
            f.solid(
                pip,
                1f,
                if (i < level) DesignTokens.COLOR_PRIMARY_GLOW else DesignTokens.withAlpha(0xFFFFFFFF.toInt(), 0.16f)
            )
        }

        val action = RectF(panel.right - 82f, y + 4f, panel.right - 10f, y + 26f)
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
