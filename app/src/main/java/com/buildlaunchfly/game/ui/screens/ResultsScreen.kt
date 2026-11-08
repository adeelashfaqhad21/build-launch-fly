package com.buildlaunchfly.game.ui.screens

import android.graphics.Paint
import android.graphics.RectF
import com.buildlaunchfly.game.game.FlightResult
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.ui.UiButtonStyle
import com.buildlaunchfly.game.ui.UiFormat
import com.buildlaunchfly.game.ui.UiFrame
import com.buildlaunchfly.game.ui.UiScreen
import com.buildlaunchfly.game.ui.UiTextStyle

/** Screen 6 — Flight Results & debrief, portrait modal. */
object ResultsScreen {

    fun draw(f: UiFrame, result: FlightResult) {
        val w = f.width
        val h = f.height

        f.solid(RectF(0f, 0f, w, h), 0f, DesignTokens.withAlpha(0x000000, 0.55f))

        val panelHeight = 320f
        val r = RectF(
            DesignTokens.MARGIN_H,
            (h - panelHeight) / 2f,
            w - DesignTokens.MARGIN_H,
            (h + panelHeight) / 2f
        )
        f.panel(
            r,
            DesignTokens.RADIUS_PANEL,
            DesignTokens.SURFACE_MODAL_BG,
            DesignTokens.withAlpha(DesignTokens.COLOR_PRIMARY_GLOW, 0.35f)
        )

        f.label(
            if (result.isRecord) "NEW PERSONAL RECORD!" else "FLIGHT COMPLETED",
            r.centerX(),
            r.top + 30f,
            UiTextStyle.H1,
            if (result.isRecord) DesignTokens.COLOR_COIN_GOLD else DesignTokens.TEXT_PRIMARY,
            Paint.Align.CENTER
        )

        f.label(
            result.distanceMeters.toInt().toString(),
            r.centerX(),
            r.top + 84f,
            UiTextStyle.DISPLAY_HERO,
            DesignTokens.TEXT_PRIMARY,
            Paint.Align.CENTER
        )
        f.label("METERS", r.centerX(), r.top + 102f, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED, Paint.Align.CENTER)

        var y = r.top + 126f
        y = ledgerRow(f, r, "AIRTIME", UiFormat.time(result.durationSeconds), y)
        y = ledgerRow(f, r, "MAX SPEED", UiFormat.kmh(result.maxSpeed * 3.6f), y)
        y = ledgerRow(f, r, "DISTANCE COINS", UiFormat.coins(result.distanceCoins), y)
        ledgerRow(f, r, "TOTAL EARNINGS", "${UiFormat.coins(result.coins)} AC", y, DesignTokens.COLOR_COIN_GOLD)

        val gap = 6f
        val buttonWidth = (r.width() - 24f - gap * 2f) / 3f
        val buttonY = r.bottom - 46f
        val retry = RectF(r.left + 12f, buttonY, r.left + 12f + buttonWidth, buttonY + 36f)
        val workshop = RectF(retry.right + gap, buttonY, retry.right + gap + buttonWidth, buttonY + 36f)
        val claim = RectF(workshop.right + gap, buttonY, workshop.right + gap + buttonWidth, buttonY + 36f)

        f.button(retry, "RETRY", UiButtonStyle.SECONDARY) {
            f.app.requestLaunch(f.app.lastLaunchStretch)
            f.nav.go(UiScreen.LAUNCH)
        }
        f.button(workshop, "WORKSHOP", UiButtonStyle.SECONDARY) {
            f.app.requestReturnToMenu()
            f.nav.go(UiScreen.AIRCRAFT)
        }
        f.button(claim, "CLAIM", UiButtonStyle.PRIMARY) {
            f.app.requestReturnToMenu()
            f.nav.go(UiScreen.MENU)
        }
    }

    private fun ledgerRow(
        f: UiFrame,
        panel: RectF,
        label: String,
        value: String,
        y: Float,
        valueColor: Int = DesignTokens.TEXT_PRIMARY
    ): Float {
        f.label(label, panel.left + 16f, y, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)
        f.label(value, panel.right - 16f, y, UiTextStyle.BODY_LARGE, valueColor, Paint.Align.RIGHT)
        f.divider(panel.left + 16f, panel.right - 16f, y + 5f)
        return y + 19f
    }
}
