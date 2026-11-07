package com.buildlaunchfly.game.game

import com.buildlaunchfly.game.gfx.Camera
import com.buildlaunchfly.game.math.Vec3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Camera behaviour per session state: launches show the rig, flights trail the aircraft and
 * results orbit the landing point. All transitions are smoothed so nothing snaps.
 */
class CameraDirector(private val camera: Camera) {

    private val desiredPosition = Vec3()
    private val desiredTarget = Vec3()
    private val smoothPosition = Vec3()
    private val smoothTarget = Vec3()

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

    private fun follow(dt: Float, rate: Float) {
        val t = (rate * dt).coerceIn(0f, 1f)
        smoothPosition.lerpTo(desiredPosition, t)
        smoothTarget.lerpTo(desiredTarget, t)
        camera.position.set(smoothPosition)
        camera.target.set(smoothTarget)
    }
}
