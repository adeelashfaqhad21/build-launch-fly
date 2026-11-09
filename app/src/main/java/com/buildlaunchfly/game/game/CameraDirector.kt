package com.buildlaunchfly.game.game

import com.buildlaunchfly.game.gfx.Camera
import com.buildlaunchfly.game.math.Vec3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Camera behaviour per session state: launches show the rig, flights trail the aircraft,
 * results orbit the landing point, and the menus orbit the aircraft or the slingshot for
 * the workshop screens.
 *
 * A short decaying impulse shake is applied on slingshot release.
 */
class CameraDirector(private val camera: Camera) {

    private val desiredPosition = Vec3()
    private val desiredTarget = Vec3()
    private val smoothPosition = Vec3()
    private val smoothTarget = Vec3()

    private var shake = 0f
    private var shakeTime = 0f

    fun addShake(amount: Float) {
        if (amount > shake) shake = amount
    }

    fun snapTo(position: Vec3, target: Vec3) {
        smoothPosition.set(position)
        smoothTarget.set(target)
        camera.position.set(position)
        camera.target.set(target)
        camera.update()
    }

    /**
     * Launch setup (Ref-3/Ref-4): directly behind the rig and the plane, ~3.3 m up, centred
     * on the aircraft. Drawing brings the camera about a quarter closer, as specified.
     */
    fun updateSeated(padPosition: Vec3, aircraftPosition: Vec3, pull: Float, dt: Float) {
        val distance = SEATED_DISTANCE - pull.coerceIn(0f, 1f) * SEATED_DISTANCE * 0.25f
        desiredPosition.set(
            aircraftPosition.x,
            padPosition.y + SEATED_HEIGHT,
            padPosition.z + distance
        )
        desiredTarget.set(
            aircraftPosition.x,
            aircraftPosition.y - 0.25f,
            aircraftPosition.z - 1.1f
        )
        follow(dt, 4.5f)
    }

    fun updateFlight(aircraftPosition: Vec3, velocity: Vec3, speed: Float, dt: Float) {
        var dirX = 0f
        var dirY = 0f
        var dirZ = -1f
        if (speed > 0.6f) {
            dirX = velocity.x / speed
            dirY = velocity.y / speed
            dirZ = velocity.z / speed
        }
        // Keep the aircraft large and the terrain readable, matching the low rear-chase
        // framing in DESIGN.md: roughly 3–4 m behind, with a modest downward view.
        desiredPosition.set(
            aircraftPosition.x - dirX * FLIGHT_CAMERA_DISTANCE,
            aircraftPosition.y - dirY * FLIGHT_CAMERA_DISTANCE + FLIGHT_CAMERA_HEIGHT,
            aircraftPosition.z - dirZ * FLIGHT_CAMERA_DISTANCE
        )
        desiredTarget.set(
            aircraftPosition.x + dirX * FLIGHT_TARGET_LEAD,
            aircraftPosition.y + FLIGHT_TARGET_HEIGHT,
            aircraftPosition.z + dirZ * FLIGHT_TARGET_LEAD
        )
        follow(dt, 4.0f)
    }

    fun updateResults(focus: Vec3, time: Float, dt: Float) {
        val angle = time * 0.32f
        desiredPosition.set(
            focus.x + cos(angle) * 19f,
            focus.y + 10f,
            focus.z + sin(angle) * 19f
        )
        desiredTarget.set(focus.x, focus.y + 0.6f, focus.z)
        follow(dt, 2.2f)
    }

    /**
     * Wide rear-side view of the aircraft and full slingshot assembly inside the hangar.
     * The 8.4 m post span must remain in frame; the 26 m shed depth keeps this wider orbit
     * inside the structure. The view only turns when the player drags.
     */
    fun updateOrbitAircraft(center: Vec3, time: Float, dt: Float, radius: Float, yawOffset: Float) {
        val angle = HANGAR_BASE_ANGLE + yawOffset
        desiredPosition.set(
            center.x + sin(angle) * radius,
            center.y + 1.2f,
            center.z + cos(angle) * radius
        )
        desiredTarget.set(center.x, center.y + 0.30f, center.z + 0.40f)
        follow(dt, 3.6f)
    }

    /** Side view of the launching rig (slingshot lab); orbit is driven by drag only. */
    fun updateOrbitSlingshot(pad: Vec3, time: Float, dt: Float, yawOffset: Float) {
        val angle = 0.35f + yawOffset
        val radius = 9.5f
        desiredPosition.set(
            pad.x + sin(angle) * radius,
            pad.y + 3.6f,
            pad.z + cos(angle) * radius
        )
        desiredTarget.set(pad.x, pad.y + 2.0f, pad.z)
        follow(dt, 3.6f)
    }

    private fun follow(dt: Float, rate: Float) {
        val t = (rate * dt).coerceIn(0f, 1f)
        smoothPosition.lerpTo(desiredPosition, t)
        smoothTarget.lerpTo(desiredTarget, t)

        shakeTime += dt
        if (shake > 0.002f) {
            camera.position.set(
                smoothPosition.x + sin(shakeTime * 53f) * shake,
                smoothPosition.y + sin(shakeTime * 61f + 1.3f) * shake * 0.7f,
                smoothPosition.z + sin(shakeTime * 47f) * shake * 0.4f
            )
            shake *= (1f - 4.5f * dt).coerceIn(0f, 1f)
        } else {
            shake = 0f
            camera.position.set(smoothPosition)
        }
        camera.target.set(smoothTarget)
    }

    companion object {
        /**
         * Camera height above the pad and distance behind the aircraft. Both are chosen for a
         * launch that happens inside the shed: the aircraft now stands on the floor, so the
         * camera is at eye level a few metres behind it, under the eave, and stays inside the
         * back wall during the pull.
         */
        const val SEATED_HEIGHT = 2.6f
        const val SEATED_DISTANCE = 5.5f

        /** Fixed rear-side oblique of the hangar view (Ref-2), not an animation. */
        const val HANGAR_BASE_ANGLE = -0.30f

        /** Low chase framing: the original 15.5 m offset made the aircraft and terrain tiny. */
        const val FLIGHT_CAMERA_DISTANCE = 4.0f
        const val FLIGHT_CAMERA_HEIGHT = 1.7f
        const val FLIGHT_TARGET_LEAD = 2.4f
        const val FLIGHT_TARGET_HEIGHT = 0.15f
    }
}
