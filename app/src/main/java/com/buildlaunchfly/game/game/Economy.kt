package com.buildlaunchfly.game.game

import com.buildlaunchfly.game.world.WorldDefinition
import com.buildlaunchfly.game.world.WorldId

/** High-level session states. UI screens will map one-to-one onto these. */
enum class GameState { SEATED, FLIGHT, RESULTS }

/**
 * Graphics presets. Each level scales world detail, shadow coverage and particle load so
 * weaker devices keep a stable frame time without losing the visual identity.
 *
 * Names follow docs/DESIGN.md §6 Screen 8.
 */
enum class GraphicsQuality(
    val label: String,
    val detailLevel: Int,
    val shadowRadius: Float,
    val particleScale: Float
) {
    LOW("Power Saver 30 FPS", 0, 50f, 0.4f),
    MEDIUM("Balanced 60 FPS", 1, 80f, 0.7f),
    HIGH("High Fidelity 90 FPS", 2, 120f, 1f),
    ULTRA("Ultra Extreme 120 FPS", 3, 160f, 1.4f);

    companion object {
        fun fromName(value: String?): GraphicsQuality {
            if (value == null) return HIGH
            for (quality in values()) {
                if (quality.name == value) return quality
            }
            return HIGH
        }
    }
}

/** A finished flight, ready for the results screen and the wallet. */
class FlightResult(
    val distanceMeters: Float,
    val maxSpeed: Float,
    val durationSeconds: Float,
    val landed: Boolean,
    val coins: Int,
    val distanceCoins: Int,
    val speedBonus: Int,
    val milestoneBonus: Int,
    val recordBonus: Int,
    val isRecord: Boolean
)

/** Currency rules: distance first, performance and milestones on top. */
object Economy {

    fun evaluate(
        distanceMeters: Float,
        maxSpeed: Float,
        durationSeconds: Float,
        landed: Boolean,
        world: WorldDefinition,
        bestDistance: Float,
        slingshotPower: Float
    ): FlightResult {
        val distance = distanceMeters.coerceAtLeast(0f)
        val distanceCoins = (distance * 0.85f * world.earningsMultiplier).toInt()
        val speedBonus = if (maxSpeed > 28f) {
            ((maxSpeed - 28f) * 4.5f * world.earningsMultiplier).toInt()
        } else {
            0
        }
        val milestoneBonus = (distance / 250f).toInt() * 30
        val isRecord = distance > bestDistance
        val recordBonus = if (isRecord && bestDistance > 5f) {
            (40f + bestDistance * 0.15f).toInt()
        } else {
            0
        }
        val landingBonus = if (landed) 25 else 0
        val powerBonus = (slingshotPower * 20f).toInt()

        val coins = distanceCoins + speedBonus + milestoneBonus + recordBonus + landingBonus + powerBonus
        return FlightResult(
            distanceMeters = distance,
            maxSpeed = maxSpeed,
            durationSeconds = durationSeconds,
            landed = landed,
            coins = coins,
            distanceCoins = distanceCoins,
            speedBonus = speedBonus,
            milestoneBonus = milestoneBonus,
            recordBonus = recordBonus,
            isRecord = isRecord
        )
    }

    fun worldUnlocked(world: WorldDefinition, bestDistance: Float): Boolean =
        bestDistance >= world.unlockDistanceMeters

    fun nextWorld(bestDistance: Float): WorldId? {
        for (world in com.buildlaunchfly.game.world.WorldCatalog.all) {
            if (bestDistance < world.unlockDistanceMeters) return world.id
        }
        return null
    }
}
