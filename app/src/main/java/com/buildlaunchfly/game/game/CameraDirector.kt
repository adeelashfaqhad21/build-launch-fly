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
        desiredPosition.set(
            aircraftPosition.x - dirX * 15.5f,
            aircraftPosition.y - dirY * 15.5f + 5.8f,
            aircraftPosition.z - dirZ * 15.5f
        )
        desiredTarget.set(
            aircraftPosition.x + dirX * 7f,
            aircraftPosition.y + 0.8f,
            aircraftPosition.z + dirZ * 7f
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
     * Rear view of the aircraft inside the hangar. The camera sits directly behind on the
     * centre line, at a low eye height, and only orbits when the player drags.
     */
    fun updateOrbitAircraft(center: Vec3, time: Float, dt: Float, radius: Float, yawOffset: Float) {
        val angle = yawOffset
        desiredPosition.set(
            center.x + sin(angle) * radius,
            center.y + 1.0f,
            center.z + cos(angle) * radius
        )
        desiredTarget.set(center.x, center.y - 0.15f, center.z)
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
        /** Camera height above the pad and distance behind the aircraft. */
        const val SEATED_HEIGHT = 3.3f
        const val SEATED_DISTANCE = 9.6f
    }
}
