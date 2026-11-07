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

    fun updateSeated(padPosition: Vec3, aircraftPosition: Vec3, pull: Float, dt: Float) {
        desiredPosition.set(
            padPosition.x + 6.4f,
            padPosition.y + 2.6f,
            padPosition.z + 7.2f + pull * 1.5f
        )
        desiredTarget.set(
            aircraftPosition.x,
            aircraftPosition.y - 0.2f,
            aircraftPosition.z - 5.0f
        )
        follow(dt, 3.2f)
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

    /** Slow hero orbit of the aircraft on the launch pad (menu / workshop). */
    fun updateOrbitAircraft(center: Vec3, time: Float, dt: Float, radius: Float, yawOffset: Float) {
        val angle = time * 0.22f + yawOffset
        desiredPosition.set(
            center.x + sin(angle) * radius,
            center.y + 2.6f,
            center.z + cos(angle) * radius
        )
        desiredTarget.set(center.x, center.y - 0.15f, center.z)
        follow(dt, 2.4f)
    }

    /** Side-elevation orbit of the launching rig (slingshot lab). */
    fun updateOrbitSlingshot(pad: Vec3, time: Float, dt: Float, yawOffset: Float) {
        val angle = 0.55f + sin(time * 0.18f) * 0.35f + yawOffset
        val radius = 9.5f
        desiredPosition.set(
            pad.x + sin(angle) * radius,
            pad.y + 3.6f,
            pad.z + cos(angle) * radius
        )
        desiredTarget.set(pad.x, pad.y + 2.2f, pad.z)
        follow(dt, 2.6f)
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
}
