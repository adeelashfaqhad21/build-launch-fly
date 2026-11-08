package com.buildlaunchfly.game.ui.screens

import android.graphics.Paint
import android.graphics.RectF
import com.buildlaunchfly.game.game.FlightResult
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.ui.UiButtonStyle
import com.buildlaunchfly.game.ui.UiFormat
import com.buildlaunchfly.game.ui.UiFrame
import com.buildlaunchfly.game.ui.UiIcon
import com.buildlaunchfly.game.ui.UiIcons
import com.buildlaunchfly.game.ui.UiScreen
import com.buildlaunchfly.game.ui.UiTextStyle

/**
 * Screen 6 — flight debrief (docs/DESIGN.md §3.13), drawn in the reference card language:
 * a saturated header band, the distance as the hero number, an itemised reward ledger and
 * a coin pill that counts up as the panel settles.
 *
 * The optional rewarded DOUBLE slot stays hidden: there is no rewarded-ad integration in
 * the build yet, so the button would be a dead end.
 */
object ResultsScreen {

    fun draw(f: UiFrame, result: FlightResult) {
        val w = f.width
        val h = f.height

        f.solid(RectF(0f, 0f, w, h), 0f, DesignTokens.withAlpha(0x000000, 0.55f))

        val panelHeight = 372f
        val top = (h - panelHeight) / 2f
        val panel = RectF(
            DesignTokens.MARGIN_H,
            top,
            w - DesignTokens.MARGIN_H,
            top + panelHeight
        )
        f.panel(panel, DesignTokens.RADIUS_PANEL, DesignTokens.CARD, DesignTokens.CARD_EDGE)

        val headerColor = if (result.isRecord) DesignTokens.GOLD else DesignTokens.NAVY
        val band = RectF(panel.left + 2f, panel.top + 2f, panel.right - 2f, panel.top + 50f)
        f.solid(band, 18f, headerColor)
        f.labelIn(
            band,
            if (result.isRecord) "NEW RECORD!" else "FLIGHT COMPLETE",
            UiTextStyle.H1,
            DesignTokens.WHITE,
            outline = true
        )

        f.label(
            "${result.distanceMeters.toInt()}M",
            panel.centerX(),
            panel.top + 108f,
            UiTextStyle.DISPLAY_HERO,
            DesignTokens.INK,
            Paint.Align.CENTER
        )

        val best = maxOf(f.app.playerProfile.bestDistance, result.distanceMeters)
        f.label(
            "BEST ${best.toInt()}M",
            panel.centerX(),
            panel.top + 130f,
            UiTextStyle.BADGE,
            DesignTokens.TEXT_MUTED,
            Paint.Align.CENTER
        )

        drawRewardPill(f, panel, result)

        var y = panel.top + 200f
        y = row(f, panel, "DISTANCE", UiFormat.coins(result.distanceCoins), y)
        y = row(f, panel, "SPEED BONUS", UiFormat.coins(result.speedBonus), y)
        y = row(f, panel, "MILESTONES", UiFormat.coins(result.milestoneBonus), y)
        if (result.recordBonus > 0) {
            y = row(f, panel, "RECORD BONUS", UiFormat.coins(result.recordBonus), y, DesignTokens.GOLD)
        }

        // Everything the economy folds in beyond the visible rows (launch power, landing).
        val launchAndLanding = result.coins - result.distanceCoins -
            result.speedBonus - result.milestoneBonus - result.recordBonus
        if (launchAndLanding > 0) {
            y = row(f, panel, "LAUNCH & LANDING", UiFormat.coins(launchAndLanding), y)
        }
        row(f, panel, "TOTAL", UiFormat.coins(result.coins), y + 2f, DesignTokens.INK)

        val gap = 10f
        val buttonHeight = 40f
        val buttonY = panel.bottom - 56f
        val buttonWidth = (panel.width() - 32f - gap) / 2f

        val retry = RectF(panel.left + 16f, buttonY, panel.left + 16f + buttonWidth, buttonY + buttonHeight)
        val hangar = RectF(retry.right + gap, buttonY, retry.right + gap + buttonWidth, buttonY + buttonHeight)

        f.button(retry, "RETRY", UiButtonStyle.BLUE) {
            f.app.requestReturnToMenu()
            f.nav.go(UiScreen.LAUNCH)
        }
        f.button(hangar, "HANGAR", UiButtonStyle.SECONDARY) {
            f.app.requestReturnToMenu()
            f.nav.go(UiScreen.MENU)
        }
    }

    private fun drawRewardPill(f: UiFrame, panel: RectF, result: FlightResult) {
        val pill = RectF(panel.left + 24f, panel.top + 146f, panel.right - 24f, panel.top + 184f)
        f.pill(pill, DesignTokens.GOLD)
        UiIcons.draw(
            f.canvas,
            f.painter,
            UiIcon.COIN,
            RectF(pill.left + 14f, pill.centerY() - 11f, pill.left + 36f, pill.centerY() + 11f),
            DesignTokens.WHITE
        )
        f.label(
            "+${UiFormat.coins(countedCoins(f, result))}",
            pill.left + 46f,
            pill.centerY() + 8f,
            UiTextStyle.H1,
            DesignTokens.WHITE,
            outline = true
        )
        f.label(
            "COINS",
            pill.right - 14f,
            pill.centerY() + 4f,
            UiTextStyle.BADGE,
            DesignTokens.WHITE,
            Paint.Align.RIGHT,
            outline = true
        )
    }

    private fun row(
        f: UiFrame,
        panel: RectF,
        label: String,
        value: String,
        y: Float,
        valueColor: Int = DesignTokens.INK
    ): Float {
        f.label(label, panel.left + 20f, y, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)
        f.label(value, panel.right - 20f, y, UiTextStyle.BODY_LARGE, valueColor, Paint.Align.RIGHT)
        f.divider(panel.left + 20f, panel.right - 20f, y + 6f)
        return y + 21f
    }

    /** Coins tick up over the first 0.9 s so the reward lands with a beat. */
    private fun countedCoins(f: UiFrame, result: FlightResult): Int {
        val t = (f.app.resultsElapsed() / 0.9f).coerceIn(0f, 1f)
        val eased = 1f - (1f - t) * (1f - t)
        return (result.coins * eased).toInt()
    }
}
