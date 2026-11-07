package com.buildlaunchfly.game.ui.screens

import android.graphics.Paint
import android.graphics.RectF
import com.buildlaunchfly.game.aircraft.AircraftCatalog
import com.buildlaunchfly.game.aircraft.PartSlot
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.ui.UiButtonStyle
import com.buildlaunchfly.game.ui.UiFormat
import com.buildlaunchfly.game.ui.UiFrame
import com.buildlaunchfly.game.ui.UiScreen
import com.buildlaunchfly.game.ui.UiTextStyle

/** Screen 2 — Aircraft Workshop (design.md §6). */
object AircraftWorkshopScreen {

    private val tabs = listOf(
        "FUSELAGE" to PartSlot.BODY,
        "WINGS" to PartSlot.WING_LEFT,
        "TAIL" to PartSlot.TAIL,
        "PROPELLER" to PartSlot.PROPELLER,
        "UPGRADES" to PartSlot.ENGINE
    )

    fun draw(f: UiFrame) {
        val contentTop = f.header("AIRCRAFT WORKSHOP")
        val w = f.width
        val h = f.height

        val gap = 6f
        val tabWidth = (w - DesignTokens.MARGIN_H * 2f - gap * (tabs.size - 1)) / tabs.size
        tabs.forEachIndexed { index, entry ->
            val slot = entry.second
            val r = f.rect(DesignTokens.MARGIN_H + index * (tabWidth + gap), contentTop + 2f, tabWidth, 28f)
            val active = f.nav.selectedSlot == slot ||
                (slot == PartSlot.WING_LEFT && f.nav.selectedSlot == PartSlot.WING_RIGHT)
            f.button(r, entry.first, if (active) UiButtonStyle.SECONDARY else UiButtonStyle.GHOST) {
                f.nav.selectedSlot = slot
            }
            if (active) f.ring(r, DesignTokens.RADIUS_BUTTON, DesignTokens.COLOR_PRIMARY_GLOW, 1.5f)
        }

        val panelTop = contentTop + 38f
        drawStats(f, f.rect(DesignTokens.MARGIN_H, panelTop, 200f, 200f))
        drawDrawer(f, f.rect(w - DesignTokens.MARGIN_H - 268f, panelTop, 268f, 200f))

        f.label(
            "DRAG ANYWHERE TO ORBIT THE AIRCRAFT",
            w / 2f,
            h - DesignTokens.SAFE_BOTTOM - 6f,
            UiTextStyle.BADGE,
            DesignTokens.TEXT_MUTED,
            Paint.Align.CENTER
        )
    }

    private fun drawStats(f: UiFrame, r: RectF) {
        f.panel(r)
        f.label("AIRFRAME TOTALS", r.left + 12f, r.top + 22f, UiTextStyle.HUD_LABEL, DesignTokens.TEXT_PRIMARY)
        val stats = f.app.aircraftBuild.stats()
        var y = r.top + 48f
        y = statRow(f, r, "LIFT", "%.2f".format(stats.lift), (stats.lift / 3f).coerceIn(0f, 1f), y)
        y = statRow(f, r, "DRAG AREA", "%.2f".format(stats.dragArea), (stats.dragArea / 1.5f).coerceIn(0f, 1f), y)
        y = statRow(f, r, "STABILITY", "%.2f".format(stats.stability), (stats.stability / 2f).coerceIn(0f, 1f), y)
        y = statRow(f, r, "MASS", "%.0f kg".format(stats.mass), (stats.mass / 120f).coerceIn(0f, 1f), y)
        statRow(f, r, "PROP RPM", "%.0f".format(stats.maxRpm), (stats.maxRpm / 3000f).coerceIn(0f, 1f), y)
    }

    private fun statRow(f: UiFrame, panel: RectF, label: String, value: String, fraction: Float, y: Float): Float {
        f.label(label, panel.left + 12f, y, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)
        f.label(
            value,
            panel.right - 12f,
            y,
            UiTextStyle.BODY,
            DesignTokens.TEXT_PRIMARY,
            Paint.Align.RIGHT
        )
        f.progressBar(f.rect(panel.left + 12f, y + 5f, panel.width() - 24f, 5f), fraction)
        return y + 26f
    }

    private fun drawDrawer(f: UiFrame, r: RectF) {
        f.panel(r, DesignTokens.RADIUS_PANEL, DesignTokens.withAlpha(DesignTokens.SURFACE_MODAL_BG, 0.86f))

        val slot = f.nav.selectedSlot
        val build = f.app.aircraftBuild
        val tier = build.tierOf(slot)
        val tiers = AircraftCatalog.tiersFor(slot)
        val current = AircraftCatalog.upgrade(slot, tier)
        val next = AircraftCatalog.nextUpgrade(slot, tier)
        val maxTier = maxOf(1, tiers.size)

        val inner = r.left + 12f
        val innerWidth = r.width() - 24f
        var y = r.top + 22f

        val title = next?.displayName ?: current?.displayName ?: slot.name
        y = f.paragraph(title, inner, y, innerWidth, UiTextStyle.H2, DesignTokens.TEXT_PRIMARY, 19f)

        f.badge(
            inner,
            y + 2f,
            if (tier > 0) "TIER $tier / $maxTier" else "NOT INSTALLED",
            if (tier > 0) DesignTokens.COLOR_SUCCESS else DesignTokens.COLOR_HAZARD
        )
        y += 24f

        val description = next?.description
            ?: current?.description
            ?: "Install this component to continue building the aircraft."
        y = f.paragraph(description, inner, y, innerWidth, UiTextStyle.BODY, DesignTokens.TEXT_MUTED, 15f)

        val source = next ?: current
        if (source != null) {
            y += 4f
            y = deltaRow(f, inner, innerWidth, "LIFT", UiFormat.signed(source.lift), y)
            y = deltaRow(f, inner, innerWidth, "DRAG", UiFormat.signed(source.dragArea), y)
            deltaRow(f, inner, innerWidth, "STABILITY", UiFormat.signed(source.stability), y)
        }

        if (next != null) {
            val priceY = r.bottom - 40f
            f.label("UPGRADE COST", inner, priceY - 8f, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)
            f.label(
                "${UiFormat.coins(next.price)} AERO COINS",
                r.right - 12f,
                priceY - 8f,
                UiTextStyle.BADGE,
                DesignTokens.COLOR_COIN_GOLD,
                Paint.Align.RIGHT
            )
            val affordable = f.app.playerProfile.coins >= next.price
            val button = RectF(inner, r.bottom - 32f, r.right - 12f, r.bottom - 6f)
            f.button(
                button,
                if (affordable) "UPGRADE COMPONENT" else "NOT ENOUGH COINS",
                if (affordable) UiButtonStyle.PRIMARY else UiButtonStyle.SECONDARY,
                affordable
            ) {
                f.app.requestInstallPart(slot)
                f.nav.rewardSlot = slot
                f.nav.go(UiScreen.REWARD)
            }
        } else {
            f.label(
                "NO FURTHER PARTS FOR THIS SLOT",
                r.centerX(),
                r.bottom - 12f,
                UiTextStyle.BADGE,
                DesignTokens.TEXT_MUTED,
                Paint.Align.CENTER
            )
        }
    }

    private fun deltaRow(f: UiFrame, x: Float, width: Float, label: String, value: String, y: Float): Float {
        f.label(label, x, y, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)
        val positive = !value.startsWith("-")
        f.label(
            value,
            x + width,
            y,
            UiTextStyle.BODY,
            if (positive) DesignTokens.COLOR_SUCCESS else DesignTokens.COLOR_HAZARD,
            Paint.Align.RIGHT
        )
        return y + 15f
    }
}
