package com.buildlaunchfly.game.ui

/**
 * Modular hangar promotions, per DESIGN §3.5. Every slot can be switched off and the rail
 * closes up around it, so the hangar never shows a promotion whose flow does not exist yet.
 */
enum class PromoSlot { STORE, REWARDED_GEMS, CHEST, VIP, ROCKET }

object PromoConfig {

    /**
     * Live slots. The store, rewarded gems and VIP offers stay off until their flows and the
     * economy behind them exist; nothing here may block play.
     */
    val enabled: Set<PromoSlot> = linkedSetOf(PromoSlot.CHEST, PromoSlot.ROCKET)

    /** Icon slots down the left edge, in drawing order. */
    val leftRail: List<PromoSlot> = listOf(PromoSlot.STORE, PromoSlot.REWARDED_GEMS)

    /** Card slots down the right edge, in drawing order. */
    val rightRail: List<PromoSlot> = listOf(PromoSlot.CHEST, PromoSlot.VIP, PromoSlot.ROCKET)

    fun isEnabled(slot: PromoSlot): Boolean = slot in enabled

    /** Height of a card slot; icon slots use their own 40 dp square. */
    fun cardHeight(slot: PromoSlot): Float = when (slot) {
        PromoSlot.CHEST -> 56f
        PromoSlot.VIP -> 44f
        PromoSlot.ROCKET -> 64f
        PromoSlot.STORE, PromoSlot.REWARDED_GEMS -> 40f
    }
}
