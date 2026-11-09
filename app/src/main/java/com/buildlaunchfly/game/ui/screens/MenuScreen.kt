package com.buildlaunchfly.game.ui.screens

import android.graphics.Paint
import android.graphics.RectF
import com.buildlaunchfly.game.aircraft.AircraftCatalog
import com.buildlaunchfly.game.aircraft.PartSlot
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.slingshot.SlingUpgrade
import com.buildlaunchfly.game.ui.PromoConfig
import com.buildlaunchfly.game.ui.PromoSlot
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
        f.hit(f.rect(w / 2f - 90f, tapY - 36f, 180f, 60f), { f.nav.go(UiScreen.LAUNCH) })

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
        // Left icon rail: only the slots that are switched on, stacked without gaps.
        var y = 112f
        for (slot in PromoConfig.leftRail) {
            if (!PromoConfig.isEnabled(slot)) continue
            promoIcon(f, f.rect(16f, y, 40f, 40f), iconOf(slot), promoAction(f, slot))
            y += 58f
        }

        // Right card rail: same rule, one card per enabled slot.
        y = 112f
        for (slot in PromoConfig.rightRail) {
            if (!PromoConfig.isEnabled(slot)) continue
            val height = PromoConfig.cardHeight(slot)
            promoCard(f, f.rect(244f, y, 100f, height), slot, promoAction(f, slot))
            y += height + 12f
        }
    }

    private fun iconOf(slot: PromoSlot): UiIcon = when (slot) {
        PromoSlot.STORE -> UiIcon.STORE
        PromoSlot.REWARDED_GEMS -> UiIcon.GEM
        PromoSlot.CHEST -> UiIcon.CHEST
        PromoSlot.VIP -> UiIcon.TROPHY
        PromoSlot.ROCKET -> UiIcon.ROCKET
    }

    /** A slot with no destination yet gets no hit target, so it can never be a dead tap. */
    private fun promoAction(f: UiFrame, slot: PromoSlot): (() -> Unit)? = when (slot) {
        PromoSlot.CHEST -> ({ f.app.requestClaimChest() })
        PromoSlot.ROCKET -> ({ f.nav.go(UiScreen.WORLDS) })
        PromoSlot.STORE, PromoSlot.REWARDED_GEMS, PromoSlot.VIP -> null
    }

    private fun promoIcon(f: UiFrame, r: RectF, icon: UiIcon, onClick: (() -> Unit)?) {
        f.panel(r, 12f, DesignTokens.CARD, DesignTokens.CARD_EDGE)
        UiIcons.draw(
            f.canvas,
            f.painter,
            icon,
            RectF(r.left + 10f, r.top + 10f, r.right - 10f, r.bottom - 10f),
            DesignTokens.INK
        )
        if (onClick != null) f.hit(r, onClick)
    }

    private fun promoCard(f: UiFrame, r: RectF, slot: PromoSlot, onClick: (() -> Unit)?) {
        f.panel(r, 14f)
        when (slot) {
            PromoSlot.CHEST -> {
                UiIcons.draw(
                    f.canvas,
                    f.painter,
                    UiIcon.CHEST,
                    RectF(r.left + 8f, r.top + 8f, r.left + 36f, r.top + 36f),
                    DesignTokens.GOLD
                )
                f.button(
                    f.rect(r.right - 54f, r.bottom - 24f, 46f, 18f),
                    "OPEN",
                    UiButtonStyle.SUCCESS,
                    f.app.chestReady()
                ) { f.app.requestClaimChest() }
            }
            PromoSlot.VIP -> {
                UiIcons.draw(
                    f.canvas,
                    f.painter,
                    UiIcon.TROPHY,
                    RectF(r.left + 8f, r.top + 9f, r.left + 36f, r.top + 37f),
                    DesignTokens.GOLD
                )
                f.label(
                    "VIP",
                    r.right - 10f,
                    r.centerY() + 6f,
                    UiTextStyle.H2,
                    DesignTokens.GOLD,
                    Paint.Align.RIGHT,
                    outline = true
                )
            }
            PromoSlot.ROCKET -> {
                UiIcons.draw(
                    f.canvas,
                    f.painter,
                    UiIcon.ROCKET,
                    RectF(r.left + 8f, r.top + 6f, r.left + 36f, r.top + 34f),
                    DesignTokens.BLUE
                )
                f.button(
                    f.rect(r.left + 8f, r.bottom - 26f, r.width() - 16f, 20f),
                    "UNLOCK",
                    UiButtonStyle.PURPLE
                ) { f.nav.go(UiScreen.WORLDS) }
            }
            else -> Unit
        }
        if (onClick != null) f.hit(r, onClick)
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
