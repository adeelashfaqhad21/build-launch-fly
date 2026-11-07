package com.buildlaunchfly.game.aircraft

/** Aircraft build slots. Tier 0 means "not installed". */
enum class PartSlot { BODY, WING_LEFT, WING_RIGHT, TAIL, PROPELLER, ENGINE }

/**
 * One purchasable tier of one slot. Deltas are additive on top of the bare fuselage, so a
 * full build is simply the sum of everything installed.
 */
class PartUpgrade(
    val slot: PartSlot,
    val tier: Int,
    val id: String,
    val displayName: String,
    val description: String,
    val price: Int,
    val lift: Float,
    val dragArea: Float,
    val thrust: Float,
    val mass: Float,
    val stability: Float,
    val maxRpm: Float
)

object AircraftCatalog {

    /** The free, always-installed bare fuselage: no wings, no tail, no propeller. */
    val bareBody = PartUpgrade(
        slot = PartSlot.BODY,
        tier = 1,
        id = "body_stage1",
        displayName = "Stage 1 — Fuselage",
        description = "Bare airframe. No wings, no tail, no propulsion.",
        price = 0,
        lift = 0.04f,
        dragArea = 0.32f,
        thrust = 0f,
        mass = 42f,
        stability = 0.15f,
        maxRpm = 0f
    )

    val upgrades: List<PartUpgrade> = listOf(
        bareBody,
        PartUpgrade(
            PartSlot.WING_LEFT, 1, "wing_left_1", "Left Wing — Stage 2",
            "First wing. Adds real lift and a visible panel.",
            price = 180, lift = 1.05f, dragArea = 0.10f, thrust = 0f,
            mass = 4.5f, stability = 0.10f, maxRpm = 0f
        ),
        PartUpgrade(
            PartSlot.WING_RIGHT, 1, "wing_right_1", "Right Wing — Stage 3",
            "Second wing. Balanced lift for stable glides.",
            price = 240, lift = 1.05f, dragArea = 0.10f, thrust = 0f,
            mass = 4.5f, stability = 0.14f, maxRpm = 0f
        ),
        PartUpgrade(
            PartSlot.TAIL, 1, "tail_1", "Tail Assembly — Stage 4",
            "Vertical fin and stabiliser. Much better pitch control.",
            price = 360, lift = 0.10f, dragArea = 0.06f, thrust = 0f,
            mass = 2.4f, stability = 0.55f, maxRpm = 0f
        ),
        PartUpgrade(
            PartSlot.PROPELLER, 1, "propeller_1", "Propeller — Stage 5",
            "Two-blade propeller on a polished hub. Windmills in the airflow.",
            price = 420, lift = 0f, dragArea = 0.03f, thrust = 0f,
            mass = 2.0f, stability = 0.02f, maxRpm = 900f
        ),
        PartUpgrade(
            PartSlot.ENGINE, 1, "engine_1", "Engine — Stage 6",
            "Light aircraft engine. Turns the propeller into real thrust.",
            price = 900, lift = 0f, dragArea = 0.05f, thrust = 340f,
            mass = 9.0f, stability = 0.05f, maxRpm = 2400f
        ),
        PartUpgrade(
            PartSlot.WING_LEFT, 2, "wing_left_2", "Left Wing Mk II",
            "Longer spar with improved aerofoil and finish.",
            price = 1500, lift = 0.55f, dragArea = -0.02f, thrust = 0f,
            mass = 1.2f, stability = 0.12f, maxRpm = 0f
        ),
        PartUpgrade(
            PartSlot.WING_RIGHT, 2, "wing_right_2", "Right Wing Mk II",
            "Matched Mk II wing for clean, symmetric lift.",
            price = 1500, lift = 0.55f, dragArea = -0.02f, thrust = 0f,
            mass = 1.2f, stability = 0.12f, maxRpm = 0f
        ),
        PartUpgrade(
            PartSlot.TAIL, 2, "tail_2", "Tail Assembly Mk II",
            "Enlarged surfaces tuned for high-speed flight.",
            price = 2100, lift = 0.12f, dragArea = -0.01f, thrust = 0f,
            mass = 0.8f, stability = 0.45f, maxRpm = 0f
        ),
        PartUpgrade(
            PartSlot.PROPELLER, 2, "propeller_2", "Three-Blade Propeller",
            "Better blade loading and smoother thrust delivery.",
            price = 2400, lift = 0f, dragArea = -0.01f, thrust = 40f,
            mass = 0.6f, stability = 0.02f, maxRpm = 400f
        ),
        PartUpgrade(
            PartSlot.ENGINE, 2, "engine_2", "Tuned Engine",
            "Higher compression, more thrust, same weight class.",
            price = 3900, lift = 0f, dragArea = 0f, thrust = 210f,
            mass = 1.4f, stability = 0.03f, maxRpm = 500f
        )
    )

    fun tiersFor(slot: PartSlot): List<PartUpgrade> {
        val list = ArrayList<PartUpgrade>()
        for (upgrade in upgrades) {
            if (upgrade.slot == slot) list.add(upgrade)
        }
        list.sortBy { it.tier }
        return list
    }

    fun upgrade(slot: PartSlot, tier: Int): PartUpgrade? {
        for (upgrade in upgrades) {
            if (upgrade.slot == slot && upgrade.tier == tier) return upgrade
        }
        return null
    }

    fun nextUpgrade(slot: PartSlot, currentTier: Int): PartUpgrade? {
        return upgrade(slot, currentTier + 1)
    }
}
