package com.buildlaunchfly.game.ui.screens

import android.graphics.Paint
import android.graphics.RectF
import com.buildlaunchfly.game.aircraft.AircraftCatalog
import com.buildlaunchfly.game.aircraft.PartSlot
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.ui.UiButtonStyle
import com.buildlaunchfly.game.ui.UiFormat
import com.buildlaunchfly.game.ui.UiFrame
import com.buildlaunchfly.game.ui.UiIcon
import com.buildlaunchfly.game.ui.UiIcons
import com.buildlaunchfly.game.ui.UiScreen
import com.buildlaunchfly.game.ui.UiTextStyle
import com.buildlaunchfly.game.world.WorldCatalog

/** Screen 1 — Main Menu (design.md §6). */
object MenuScreen {

    fun draw(f: UiFrame) {
        val w = f.width
        val h = f.height
        val profile = f.app.playerProfile
        val world = WorldCatalog.byId(profile.selectedWorld)

        drawProfilePill(f)
        drawTopRightCluster(f)

        f.label("BUILD \u2022 LAUNCH \u2022 FLY", DesignTokens.MARGIN_H, 136f, UiTextStyle.DISPLAY_HERO)
        f.label(
            "READY ON THE PAD \u2014 ${world.displayName.uppercase()}",
            DesignTokens.MARGIN_H,
            158f,
            UiTextStyle.BODY,
            DesignTokens.TEXT_MUTED
        )

        drawHotspot(f, w * 0.55f + 60f, 152f, propellerTag(f))
        drawHotspot(f, w * 0.46f - 40f, 200f, wingTag(f))

        val deckY = h - DesignTokens.SAFE_BOTTOM - 56f
        var x = DesignTokens.MARGIN_H
        navButton(f, f.rect(x, deckY, 96f, 56f), UiIcon.PLANE, "AIRCRAFT") {
            f.nav.go(UiScreen.AIRCRAFT)
        }
        x += 96f + 8f
        navButton(f, f.rect(x, deckY, 96f, 56f), UiIcon.SLINGSHOT, "SLINGSHOT") {
            f.nav.go(UiScreen.SLINGSHOT)
        }
        x += 96f + 8f
        navButton(f, f.rect(x, deckY, 96f, 56f), UiIcon.GLOBE, "REALMS") {
            f.nav.go(UiScreen.WORLDS)
        }

        val cta = f.rect(w - DesignTokens.MARGIN_H - 216f, deckY, 216f, 56f)
        f.button(cta, "CHOOSE REALM & LAUNCH", UiButtonStyle.PRIMARY) {
            f.nav.go(UiScreen.WORLDS)
        }
    }

    private fun drawProfilePill(f: UiFrame) {
        val profile = f.app.playerProfile
        val pill = f.rect(DesignTokens.MARGIN_H, DesignTokens.SAFE_TOP, 236f, 52f)
        f.panel(pill, 14f)
        f.circle(pill.left + 26f, pill.centerY(), 17f, DesignTokens.COLOR_PRIMARY_AERO)
        f.labelIn(
            f.rect(pill.left + 9f, pill.centerY() - 17f, 34f, 34f),
            "CPT",
            UiTextStyle.BADGE,
            DesignTokens.TEXT_PRIMARY
        )
        f.label("Capt. Alexander", pill.left + 52f, pill.top + 22f, UiTextStyle.H2)
        f.label(
            "${profile.pilotRankTitle()} \u2022 Lv ${profile.pilotLevel()}",
            pill.left + 52f,
            pill.top + 40f,
            UiTextStyle.BODY,
            DesignTokens.TEXT_MUTED
        )
        f.progressBar(
            f.rect(pill.left + 52f, pill.bottom - 8f, 168f, 4f),
            profile.levelProgressFraction()
        )
    }

    private fun drawTopRightCluster(f: UiFrame) {
        val w = f.width
        val top = DesignTokens.SAFE_TOP
        val gear = f.rect(w - DesignTokens.MARGIN_H - 36f, top, 36f, 36f)
        val trophy = f.rect(gear.left - 8f - 36f, top, 36f, 36f)
        val user = f.rect(trophy.left - 8f - 36f, top, 36f, 36f)
        val coins = f.rect(user.left - 10f - 118f, top, 118f, 36f)

        f.iconButton(gear, UiIcon.GEAR) { f.nav.go(UiScreen.SETTINGS) }
        f.iconButton(trophy, UiIcon.TROPHY) { f.nav.go(UiScreen.ACHIEVEMENTS) }
        f.iconButton(user, UiIcon.USER) { f.nav.go(UiScreen.PROFILE) }

        f.panel(coins, DesignTokens.RADIUS_PILL)
        UiIcons.draw(
            f.canvas,
            f.painter,
            UiIcon.COIN,
            RectF(coins.left + 5f, coins.centerY() - 10f, coins.left + 25f, coins.centerY() + 10f),
            DesignTokens.COLOR_COIN_GOLD
        )
        f.label(
            UiFormat.coins(f.app.playerProfile.coins),
            coins.right - 10f,
            coins.centerY() + 4f,
            UiTextStyle.HUD_LABEL,
            DesignTokens.TEXT_PRIMARY,
            Paint.Align.RIGHT
        )
    }

    private fun navButton(f: UiFrame, r: RectF, icon: UiIcon, caption: String, onClick: () -> Unit) {
        f.panel(r, 12f)
        UiIcons.draw(
            f.canvas,
            f.painter,
            icon,
            RectF(r.centerX() - 10f, r.top + 6f, r.centerX() + 10f, r.top + 26f),
            DesignTokens.TEXT_PRIMARY
        )
        f.label(caption, r.centerX(), r.bottom - 7f, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED, Paint.Align.CENTER)
        f.hit(r, onClick)
    }

    private fun drawHotspot(f: UiFrame, x: Float, y: Float, text: String) {
        val width = f.textWidth(text, UiTextStyle.BADGE) + 16f
        val r = RectF(x, y, x + width, y + 18f)
        f.solid(r, 9f, DesignTokens.withAlpha(DesignTokens.SURFACE_MODAL_BG, 0.82f))
        f.ring(r, 9f, DesignTokens.GLASS_BORDER)
        f.labelIn(r, text, UiTextStyle.BADGE, DesignTokens.COLOR_PRIMARY_GLOW)

        f.painter.stroke.color = DesignTokens.withAlpha(DesignTokens.COLOR_PRIMARY_GLOW, 0.5f)
        f.painter.stroke.strokeWidth = 1f
        f.canvas.drawLine(r.left, r.bottom, r.left + 18f, r.bottom + 14f, f.painter.stroke)
    }

    private fun propellerTag(f: UiFrame): String {
        val build = f.app.aircraftBuild
        val tier = build.tierOf(PartSlot.PROPELLER)
        val part = AircraftCatalog.upgrade(PartSlot.PROPELLER, tier)?.displayName ?: "NO PROPELLER"
        return "[ ${part.uppercase()} ]"
    }

    private fun wingTag(f: UiFrame): String {
        val build = f.app.aircraftBuild
        val tier = build.tierOf(PartSlot.WING_LEFT)
        val part = AircraftCatalog.upgrade(PartSlot.WING_LEFT, tier)?.displayName ?: "BARE AIRFRAME"
        return "[ ${part.uppercase()} ]"
    }
}
