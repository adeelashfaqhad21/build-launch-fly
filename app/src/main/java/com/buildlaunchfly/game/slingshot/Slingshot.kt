package com.buildlaunchfly.game.slingshot

import com.buildlaunchfly.game.math.M

/** Upgrade categories for the launching mechanism. */
enum class SlingUpgrade(
    val displayName: String,
    val description: String,
    val basePrice: Int,
    val priceGrowth: Float
) {
    BAND_STRENGTH(
        "Rubber Band Strength",
        "Thicker bands hold more energy before release.",
        140, 1.52f
    ),
    BAND_ELASTICITY(
        "Rubber Elasticity",
        "Snappier latex returns more of the stored energy.",
        170, 1.55f
    ),
    BAND_DURABILITY(
        "Rubber Durability",
        "Longer service life and a cleaner release.",
        120, 1.48f
    ),
    LAUNCH_POWER(
        "Launching Power",
        "Longer draw and a stronger frame geometry.",
        210, 1.58f
    ),
    LAUNCH_EFFICIENCY(
        "Launching Efficiency",
        "Less energy lost in the frame, bands and cradle.",
        190, 1.56f
    )
}

/**
 * Player-owned slingshot state plus the launch model. Higher levels stretch further and
 * transfer more of the stored energy to the aircraft.
 */
class SlingshotState(val levels: MutableMap<SlingUpgrade, Int> = LinkedHashMap()) {

    fun levelOf(type: SlingUpgrade): Int = levels[type] ?: 0

    fun costToUpgrade(type: SlingUpgrade): Int {
        val level = levelOf(type)
        return Math.round(type.basePrice * Math.pow(type.priceGrowth.toDouble(), level.toDouble())).toInt()
    }

    fun isMaxed(type: SlingUpgrade): Boolean = levelOf(type) >= MAX_LEVEL

    fun applyUpgrade(type: SlingUpgrade) {
        if (isMaxed(type)) return
        levels[type] = levelOf(type) + 1
    }

    /** Rendered draw length in metres at full stretch. */
    fun drawLengthMeters(): Float = 2.2f + levelOf(SlingUpgrade.LAUNCH_POWER) * 0.12f

    /** Visual band thickness multiplier used by the mesh. */
    fun bandThickness(): Float = 1f + levelOf(SlingUpgrade.BAND_STRENGTH) * 0.055f

    /** Overall launch quality, 0..1, for HUD and audio feedback. */
    fun powerRating(): Float {
        var sum = 0
        for (type in SlingUpgrade.values()) sum += levelOf(type)
        return M.saturate(sum.toFloat() / (SlingUpgrade.values().size * MAX_LEVEL).toFloat())
    }

    /**
     * Launch speed in m/s. The slingshot is the only power source, so this number decides
     * whether the flight is possible at all: a weak pull leaves the aircraft below its stall
     * speed and a hard pull throws it cleanly off the ramp.
     *
     * [stretch] is 0..1 of the available draw, [aircraftMass] the built airframe's mass, so a
     * heavy airframe needs a better band to keep the same launch speed.
     */
    fun launchSpeed(stretch: Float, aircraftMass: Float): Float {
        val s = stretch.coerceIn(0f, 1f)
        val strength = levelOf(SlingUpgrade.BAND_STRENGTH) * 2.1f
        val elasticity = levelOf(SlingUpgrade.BAND_ELASTICITY) * 1.7f
        val power = levelOf(SlingUpgrade.LAUNCH_POWER) * 2.4f
        val efficiency = 1f + levelOf(SlingUpgrade.LAUNCH_EFFICIENCY) * 0.020f
        val durabilityBonus = 1f + levelOf(SlingUpgrade.BAND_DURABILITY) * 0.006f

        val baseSpeed = 38f + strength + elasticity + power
        // Even a moderate pull throws the aircraft; only a very short draw is wasted.
        val pullCurve = 0.45f + 0.55f * s
        val massFactor = M.clamp(340f / aircraftMass.coerceAtLeast(1f), 0.72f, 1.25f)
        return baseSpeed * pullCurve * efficiency * durabilityBonus * massFactor
    }

    fun summary(): String {
        val builder = StringBuilder()
        for (type in SlingUpgrade.values()) {
            builder.append(type.name).append(": Lv ").append(levelOf(type)).append('\n')
        }
        return builder.toString()
    }

    companion object {
        const val MAX_LEVEL = 12
    }
}
