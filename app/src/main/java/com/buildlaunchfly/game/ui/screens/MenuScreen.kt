package com.buildlaunchfly.game.ui.screens

import android.graphics.Paint
import android.graphics.RectF
import com.buildlaunchfly.game.aircraft.AircraftCatalog
import com.buildlaunchfly.game.aircraft.PartSlot
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.slingshot.SlingUpgrade
import com.buildlaunchfly.game.ui.UiButtonStyle
import com.buildlaunchfly.game.ui.UiFormat
import com.buildlaunchfly.game.ui.UiFrame
import com.buildlaunchfly.game.ui.UiIcon
import com.buildlaunchfly.game.ui.UiIcons
import com.buildlaunchfly.game.ui.UiScreen
import com.buildlaunchfly.game.ui.UiTextStyle
import com.buildlaunchfly.game.world.WorldCatalog

/**
 * Screen 2 — Hangar home, following Ref-2:
 * currency pills, plaque, promo rail, chest/VIP/rocket, TAP TO FLY and three upgrade cards.
 */
object MenuScreen {

    fun draw(f: UiFrame) {
        val w = f.width
        val h = f.height
        val profile = f.app.playerProfile
        val world = WorldCatalog.byId(profile.selectedWorld)

        drawCurrencies(f)
        drawPromos(f)

        val plaque = f.rect(80f, 62f, 200f, 30f)
        f.solid(plaque, 10f, DesignTokens.PLAQUE)
        f.ring(plaque, 10f, DesignTokens.withAlpha(DesignTokens.INK, 0.35f), 2f)
        f.labelIn(plaque, "VINTAGE PLANE", UiTextStyle.HUD_LABEL, DesignTokens.PLAQUE_TEXT)

        val tapY = h * 0.52f
        f.label(
            "TAP TO FLY",
            w / 2f,
            tapY,
            UiTextStyle.DISPLAY_HERO,
            DesignTokens.WHITE,
            Paint.Align.CENTER,
            outline = true
        )
        f.label(
            world.displayName.uppercase(),
            w / 2f,
            tapY + 20f,
            UiTextStyle.BADGE,
            DesignTokens.WHITE,
            Paint.Align.CENTER,
            outline = true
        )
        f.hit(f.rect(w / 2f - 90f, tapY - 36f, 180f, 60f)) { f.nav.go(UiScreen.LAUNCH) }

        val cardY = h - DesignTokens.SAFE_BOTTOM - 104f
        val cardWidth = (w - DesignTokens.MARGIN_H * 2f - 16f) / 3f

        upgradeCard(
            f = f,
            r = f.rect(DesignTokens.MARGIN_H, cardY, cardWidth, 104f),
            header = DesignTokens.ORANGE,
            icon = UiIcon.SLINGSHOT,
            title = "SLINGSHOT",
            level = slingshotLevel(f),
            progress = (slingshotLevel(f) % 5) / 5f,
            price = slingshotPrice(f)
        ) { f.nav.go(UiScreen.SLINGSHOT) }

        upgradeCard(
            f = f,
            r = f.rect(DesignTokens.MARGIN_H + cardWidth + 8f, cardY, cardWidth, 104f),
            header = DesignTokens.CYAN,
            icon = UiIcon.PLANE,
            title = "PLANE",
            level = planeLevel(f),
            progress = (planeLevel(f) % 5) / 5f,
            price = planePrice(f)
        ) { f.nav.go(UiScreen.AIRCRAFT) }

        upgradeCard(
            f = f,
            r = f.rect(DesignTokens.MARGIN_H + (cardWidth + 8f) * 2f, cardY, cardWidth, 104f),
            header = DesignTokens.PURPLE,
            icon = UiIcon.COIN,
            title = "INCOME",
            level = profile.incomeLevel,
            progress = (profile.incomeLevel % 5) / 5f,
            price = profile.incomeUpgradePrice()
        ) { f.app.requestUpgradeIncome() }
    }

    private fun drawCurrencies(f: UiFrame) {
        val w = f.width
        val profile = f.app.playerProfile
        f.currencyPill(
            f.rect(16f, 20f, 130f, 30f),
            DesignTokens.GOLD,
            UiIcon.COIN,
            UiFormat.coins(profile.coins)
        )
        f.currencyPill(
            f.rect(154f, 20f, 86f, 30f),
            DesignTokens.GEM,
            UiIcon.GEM,
            profile.gems.toString()
        )
        f.iconButton(f.rect(w - 16f - 32f, 20f, 32f, 32f), UiIcon.GEAR) {
            f.nav.go(UiScreen.SETTINGS)
        }
    }

    private fun drawPromos(f: UiFrame) {
        promoIcon(f, f.rect(16f, 112f, 40f, 40f), UiIcon.STORE, false) {}
        promoIcon(f, f.rect(16f, 170f, 40f, 40f), UiIcon.VIDEO, false) {}
        promoIcon(f, f.rect(16f, 228f, 40f, 40f), UiIcon.GEM, false) {}

        val chest = f.rect(244f, 112f, 100f, 56f)
        f.panel(chest, 14f)
        UiIcons.draw(
            f.canvas,
            f.painter,
            UiIcon.CHEST,
            RectF(chest.left + 8f, chest.top + 8f, chest.left + 36f, chest.top + 36f),
            DesignTokens.GOLD
        )
        val chestReady = f.app.chestReady()
        f.button(
            f.rect(chest.right - 54f, chest.bottom - 24f, 46f, 18f),
            "OPEN",
            UiButtonStyle.SUCCESS,
            chestReady
        ) { f.app.requestClaimChest() }

        val vip = f.rect(244f, 180f, 100f, 44f)
        f.panel(vip, 14f)
        UiIcons.draw(
            f.canvas,
            f.painter,
            UiIcon.TROPHY,
            RectF(vip.left + 8f, vip.top + 9f, vip.left + 36f, vip.top + 37f),
            DesignTokens.GOLD
        )
        f.label("VIP", vip.right - 10f, vip.centerY() + 6f, UiTextStyle.H2, DesignTokens.GOLD, Paint.Align.RIGHT, outline = true)

        val rocket = f.rect(244f, 236f, 100f, 64f)
        f.panel(rocket, 14f)
        UiIcons.draw(
            f.canvas,
            f.painter,
            UiIcon.ROCKET,
            RectF(rocket.left + 8f, rocket.top + 6f, rocket.left + 36f, rocket.top + 34f),
            DesignTokens.BLUE
        )
        f.button(
            f.rect(rocket.left + 8f, rocket.bottom - 26f, rocket.width() - 16f, 20f),
            "UNLOCK",
            UiButtonStyle.PURPLE
        ) { f.nav.go(UiScreen.WORLDS) }
    }

    private fun promoIcon(f: UiFrame, r: RectF, icon: UiIcon, enabled: Boolean, onClick: () -> Unit) {
        f.panel(r, 12f, if (enabled) DesignTokens.CARD else DesignTokens.withAlpha(DesignTokens.CARD, 0.65f))
        UiIcons.draw(
            f.canvas,
            f.painter,
            icon,
            RectF(r.left + 10f, r.top + 10f, r.right - 10f, r.bottom - 10f),
            if (enabled) DesignTokens.INK else DesignTokens.withAlpha(DesignTokens.INK, 0.4f)
        )
        if (enabled) f.hit(r, onClick)
    }

    private fun upgradeCard(
        f: UiFrame,
        r: RectF,
        header: Int,
        icon: UiIcon,
        title: String,
        level: Int,
        progress: Float,
        price: Int,
        onClick: () -> Unit
    ) {
        f.panel(r, 16f, DesignTokens.CARD, DesignTokens.CARD_EDGE)

        f.solid(RectF(r.left + 2f, r.top + 2f, r.right - 2f, r.top + 22f), 14f, header)
        f.solid(RectF(r.left + 2f, r.top + 12f, r.right - 2f, r.top + 22f), 0f, header)
        f.label(title, r.left + 8f, r.top + 15f, UiTextStyle.BADGE, DesignTokens.WHITE, outline = true)
        f.label(
            level.toString(),
            r.right - 8f,
            r.top + 15f,
            UiTextStyle.BADGE,
            DesignTokens.WHITE,
            Paint.Align.RIGHT,
            outline = true
        )

        UiIcons.draw(
            f.canvas,
            f.painter,
            icon,
            RectF(r.centerX() - 17f, r.top + 28f, r.centerX() + 17f, r.top + 62f),
            header
        )

        val pipWidth = 10f
        val pipGap = 3f
        val totalPips = 5
        val pipsWidth = totalPips * pipWidth + (totalPips - 1) * pipGap
        val pipStart = r.centerX() - pipsWidth / 2f
        val filled = (progress * totalPips).toInt().coerceIn(0, totalPips)
        for (i in 0 until totalPips) {
            val pip = RectF(
                pipStart + i * (pipWidth + pipGap),
                r.top + 68f,
                pipStart + i * (pipWidth + pipGap) + pipWidth,
                r.top + 74f
            )
            f.solid(pip, 3f, if (i < filled) DesignTokens.GREEN else DesignTokens.withAlpha(DesignTokens.INK, 0.15f))
        }

        val priceRect = RectF(r.left + 8f, r.bottom - 22f, r.right - 8f, r.bottom - 6f)
        f.solid(priceRect, 8f, DesignTokens.LAVENDER)
        UiIcons.draw(
            f.canvas,
            f.painter,
            UiIcon.COIN,
            RectF(priceRect.left + 3f, priceRect.centerY() - 6f, priceRect.left + 15f, priceRect.centerY() + 6f),
            DesignTokens.GOLD
        )
        f.label(
            UiFormat.coins(price),
            priceRect.right - 5f,
            priceRect.centerY() + 3.5f,
            UiTextStyle.BADGE,
            DesignTokens.PLAQUE_TEXT,
            Paint.Align.RIGHT
        )

        f.hit(r, onClick)
    }

    private fun slingshotLevel(f: UiFrame): Int {
        var sum = 0
        for (type in SlingUpgrade.values()) sum += f.app.slingshotState.levelOf(type)
        return sum
    }

    private fun slingshotPrice(f: UiFrame): Int {
        var best = Int.MAX_VALUE
        for (type in SlingUpgrade.values()) {
            if (!f.app.slingshotState.isMaxed(type)) {
                best = minOf(best, f.app.slingshotState.costToUpgrade(type))
            }
        }
        return if (best == Int.MAX_VALUE) 0 else best
    }

    private fun planeLevel(f: UiFrame): Int {
        var sum = 0
        for (tier in f.app.aircraftBuild.tiers.values) sum += tier
        return sum
    }

    private fun planePrice(f: UiFrame): Int {
        var best = Int.MAX_VALUE
        for (slot in PartSlot.values()) {
            val next = AircraftCatalog.nextUpgrade(slot, f.app.aircraftBuild.tierOf(slot))
            if (next != null) best = minOf(best, next.price)
        }
        return if (best == Int.MAX_VALUE) 0 else best
    }
}
