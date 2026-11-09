package com.buildlaunchfly.game.game

/**
 * Live flight instrument values. Mirrors the FlightTelemetryState contract in
 * docs/DESIGN.md §7.3 and is refreshed every frame from GameApp.
 */
class FlightTelemetry(
    val distanceMeters: Float = 0f,
    val altitudeMeters: Float = 0f,
    val airspeedKmh: Float = 0f,
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val stallWarning: Boolean = false,
    val verticalSpeed: Float = 0f,
    val flightTimeSeconds: Float = 0f,
    /** Airspeed at which this build's wing stalls, in km/h; 0 when unknown. */
    val stallAirspeedKmh: Float = 0f
) {
    val climbing: Boolean get() = verticalSpeed > 4f

    /** True when the aircraft is slow enough that a stall is imminent. */
    val lowSpeed: Boolean
        get() = stallAirspeedKmh > 1f && airspeedKmh < stallAirspeedKmh * 1.15f
}
