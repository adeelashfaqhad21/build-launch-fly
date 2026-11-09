package com.buildlaunchfly.game.aircraft

import com.buildlaunchfly.game.math.M

/** Flight-relevant numbers derived from installed parts. */
class AircraftStats(
    val mass: Float,
    val wingArea: Float,
    val lift: Float,
    val dragArea: Float,
    val thrust: Float,
    val stability: Float,
    val maxRpm: Float
) {
    val canFly: Boolean get() = lift > 0.5f
    val hasPropeller: Boolean get() = maxRpm > 0f
    val hasEngine: Boolean get() = thrust > 0f

    companion object {
        fun from(tiers: Map<PartSlot, Int>): AircraftStats {
            val body = AircraftCatalog.bareBody
            var mass = body.mass
            var lift = body.lift
            var drag = body.dragArea
            var thrust = body.thrust
            var stability = body.stability
            var maxRpm = body.maxRpm
            var wingArea = 0f

            for ((slot, tier) in tiers) {
                if (slot == PartSlot.BODY || tier <= 0) continue
                val upgrade = AircraftCatalog.upgrade(slot, tier) ?: continue
                mass += upgrade.mass
                lift += upgrade.lift
                drag += upgrade.dragArea
                thrust += upgrade.thrust
                stability += upgrade.stability
                maxRpm = maxOf(maxRpm, upgrade.maxRpm)
                if (slot == PartSlot.WING_LEFT || slot == PartSlot.WING_RIGHT) {
                    // Real wing area in square metres: a stage-1 pair is about 7 m², which
                    // puts the stall speed of a 340 kg airframe near 24 m/s.
                    wingArea += 2.6f + upgrade.lift * 0.9f
                }
            }

            return AircraftStats(
                mass = M.clamp(mass, 20f, 900f),
                wingArea = wingArea,
                lift = lift,
                dragArea = M.clamp(drag, 0.12f, 2.5f),
                thrust = thrust,
                stability = M.clamp(stability, 0.05f, 3f),
                maxRpm = maxRpm
            )
        }
    }
}

/** Which parts are installed, and at what tier. */
class AircraftBuild(val tiers: MutableMap<PartSlot, Int> = LinkedHashMap()) {

    init {
        if (!tiers.containsKey(PartSlot.BODY)) tiers[PartSlot.BODY] = 1
    }

    fun tierOf(slot: PartSlot): Int = tiers[slot] ?: 0

    fun isInstalled(slot: PartSlot): Boolean = tierOf(slot) > 0

    fun install(slot: PartSlot, tier: Int) {
        tiers[slot] = tier
    }

    fun stats(): AircraftStats = AircraftStats.from(tiers)

    /** Human-readable summary, used by logs and (later) by the workshop UI. */
    fun summary(): String {
        val builder = StringBuilder()
        for (slot in PartSlot.values()) {
            val tier = tierOf(slot)
            val name = AircraftCatalog.upgrade(slot, tier)?.displayName ?: "none"
            builder.append(slot.name).append(": ").append(name).append('\n')
        }
        val stats = stats()
        builder.append("mass=").append(stats.mass)
        builder.append(" lift=").append(stats.lift)
        builder.append(" thrust=").append(stats.thrust)
        builder.append(" stability=").append(stats.stability)
        builder.append(" rpm=").append(stats.maxRpm)
        return builder.toString()
    }
}
