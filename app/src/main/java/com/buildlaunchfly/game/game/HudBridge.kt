package com.buildlaunchfly.game.game

import com.buildlaunchfly.game.world.WorldDefinition

/**
 * The seam between gameplay and the (still to be designed) UI. Once `design.md` lands the
 * screens implement this interface and the renderer keeps running untouched.
 */
interface HudBridge {

    fun onStateChanged(state: GameState) {}

    fun onDistance(distanceMeters: Float, speedMetersPerSecond: Float) {}

    fun onLaunched(stretch: Float, speedMps: Float) {}

    fun onFlightFinished(result: FlightResult) {}

    fun onCoinsChanged(coins: Int) {}

    fun onWorldChanged(world: WorldDefinition) {}
}
