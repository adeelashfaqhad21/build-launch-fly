package com.buildlaunchfly.game.aircraft

import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3

enum class FlightState { READY, FLYING, LANDED, CRASHED }

class FlightInput(var pitch: Float = 0f, var yaw: Float = 0f, var roll: Float = 0f)

/**
 * Aircraft flight model: thrust along the airframe, lift from angle of attack, quadratic
 * drag, gravity, stability terms and ground contact. A wingless airframe produces almost
 * no lift, which is exactly what makes the first upgrades feel meaningful.
 */
class FlightPhysics {

    val position = Vec3()
    val velocity = Vec3()

    var yaw = 0f
        private set
    var pitch = 0f
        private set
    var roll = 0f
        private set

    var pitchRate = 0f
        private set
    var rollRate = 0f
        private set
    var yawRate = 0f
        private set

    var state = FlightState.READY
        private set

    var distance = 0f
        private set
    var maxSpeed = 0f
        private set

    var propellerAngle = 0f
        private set
    var propellerRpm = 0f
        private set
    var propellerBladePitch = 0.12f
        private set

    var throttle = 1f

    val orientation = Mat4()

    private val start = Vec3()
    private val forward = Vec3()
    private val up = Vec3()
    private val right = Vec3()
    private val up0 = Vec3()
    private val accel = Vec3()
    private val tmp = Vec3()
    private val propLocal = Mat4()

    fun reset(origin: Vec3) {
        position.set(origin)
        velocity.set(0f, 0f, 0f)
        yaw = 0f
        pitch = 0f
        roll = 0f
        pitchRate = 0f
        rollRate = 0f
        yawRate = 0f
        distance = 0f
        maxSpeed = 0f
        propellerRpm = 0f
        propellerBladePitch = 0.12f
        state = FlightState.READY
        start.set(origin)
        updateOrientation()
    }

    /** Fires the aircraft out of the slingshot. */
    fun launch(speed: Float, pitchDegrees: Float, yawDegrees: Float) {
        pitch = M.deg(pitchDegrees)
        yaw = M.deg(yawDegrees)
        roll = 0f
        pitchRate = 0f
        rollRate = 0f
        yawRate = 0f
        updateOrientation()
        velocity.set(forward).scale(speed)
        velocity.y += speed * 0.18f
        state = FlightState.FLYING
        distance = 0f
    }

    fun update(dt: Float, stats: AircraftStats, terrainHeight: Float, input: FlightInput) {
        if (state != FlightState.FLYING) {
            if (propellerRpm > 1f) {
                propellerRpm *= 0.985f
                propellerAngle += propellerRpm / 60f * M.TWO_PI * dt
            }
            return
        }

        val speed = velocity.length()

        if (speed > 1e-4f) {
            tmp.set(velocity).scale(1f / speed)
        } else {
            tmp.set(forward)
        }
        val flightPathAngle = kotlin.math.asin(M.clamp(tmp.y, -1f, 1f))

        // Angle of attack drives lift; a stalled alpha is clamped.
        val alpha = M.wrapAngle(pitch - flightPathAngle)
        val liftCoefficient = M.clamp(2f * M.PI.toFloat() * alpha * 0.55f, -1.1f, 1.1f)

        val rho = 1.14f
        val v2 = speed * speed

        val liftAccel = 0.5f * rho * v2 * stats.wingArea * liftCoefficient * stats.lift / stats.mass
        val dragAccel = 0.5f * rho * v2 * stats.dragArea / stats.mass
        val thrustAccel = stats.thrust / stats.mass * throttle

        // Stability: the airframe drifts toward the flight path angle.
        val stabilityGain = 0.8f + stats.stability
        val targetPitchRate = input.pitch * 1.15f + (flightPathAngle - pitch) * stabilityGain * 0.9f
        pitchRate = M.lerp(pitchRate, targetPitchRate, M.clamp(6f * dt, 0f, 1f))
        pitch = M.clamp(pitch + pitchRate * dt, -1.1f, 1.1f)

        val targetRollRate = input.roll * 2.4f - roll * (1.6f + stats.stability)
        rollRate = M.lerp(rollRate, targetRollRate, M.clamp(5f * dt, 0f, 1f))
        roll = M.clamp(roll + rollRate * dt, -1.0f, 1.0f)

        val speedFactor = M.clamp(speed / 35f, 0.15f, 1.6f)
        val targetYawRate = -kotlin.math.sin(roll) * 0.55f * speedFactor + input.yaw * 1.25f
        yawRate = M.lerp(yawRate, targetYawRate, M.clamp(3f * dt, 0f, 1f))
        yaw = M.wrapAngle(yaw + yawRate * dt)

        updateOrientation()

        // Integrate accelerations.
        accel.set(0f, -9.81f, 0f)
        accel.addScaled(forward, thrustAccel)
        accel.addScaled(up, liftAccel)
        if (speed > 1e-4f) {
            tmp.set(velocity).scale(-1f / speed)
            accel.addScaled(tmp, dragAccel)
        }

        velocity.addScaled(accel, dt)

        val previousX = position.x
        val previousZ = position.z
        position.addScaled(velocity, dt)

        val dx = position.x - previousX
        val dz = position.z - previousZ
        distance += kotlin.math.sqrt(dx * dx + dz * dz)

        val currentSpeed = velocity.length()
        if (currentSpeed > maxSpeed) maxSpeed = currentSpeed

        // Propeller: engine driven, with windmilling when the airflow is fast enough.
        if (stats.hasPropeller) {
            val windmill = speed * 40f
            val targetRpm = maxOf(windmill, stats.maxRpm * throttle)
            propellerRpm = M.lerp(propellerRpm, targetRpm, M.clamp(2f * dt, 0f, 1f))
            val rpmFraction = M.clamp(propellerRpm / maxOf(stats.maxRpm, 1f), 0f, 1f)
            val targetBladePitch = 0.10f + rpmFraction * 0.34f
            propellerBladePitch = M.lerp(propellerBladePitch, targetBladePitch, M.clamp(2.8f * dt, 0f, 1f))
            propellerAngle += propellerRpm / 60f * M.TWO_PI * dt
            if (propellerAngle > M.TWO_PI) propellerAngle -= M.TWO_PI
        } else {
            propellerRpm = 0f
            propellerBladePitch = M.lerp(propellerBladePitch, 0.12f, M.clamp(2f * dt, 0f, 1f))
        }

        // Ground contact: decide between a landing and a crash.
        val groundY = terrainHeight + 0.5f
        if (position.y <= groundY) {
            position.y = groundY
            val vertical = velocity.y
            state = when {
                kotlin.math.abs(roll) > 0.85f -> FlightState.CRASHED
                currentSpeed > 34f -> FlightState.CRASHED
                vertical < -12f -> FlightState.CRASHED
                else -> FlightState.LANDED
            }
            velocity.scale(0.12f)
        }


        // Soft ceiling keeps the aircraft inside the rendered world.
        val ceiling = terrainHeight + 300f
        if (position.y > ceiling) {
            position.y = terrainHeight + 260f
            if (velocity.y > 0f) velocity.y = 0f
        }
    }

    private fun updateOrientation() {
        val cp = kotlin.math.cos(pitch)
        forward.set(
            kotlin.math.sin(yaw) * cp,
            kotlin.math.sin(pitch),
            -kotlin.math.cos(yaw) * cp
        )
        forward.normalize()

        right.set(kotlin.math.cos(yaw), 0f, kotlin.math.sin(yaw))
        right.normalize()

        up0.cross(right, forward).normalize()

        val c = kotlin.math.cos(roll)
        val s = kotlin.math.sin(roll)

        up.set(
            up0.x * c - right.x * s,
            up0.y * c - right.y * s,
            up0.z * c - right.z * s
        ).normalize()

        right.set(
            right.x * c + up0.x * s,
            right.y * c + up0.y * s,
            right.z * c + up0.z * s
        ).normalize()

        orientation.m[0] = right.x
        orientation.m[1] = right.y
        orientation.m[2] = right.z
        orientation.m[3] = 0f
        orientation.m[4] = up.x
        orientation.m[5] = up.y
        orientation.m[6] = up.z
        orientation.m[7] = 0f
        orientation.m[8] = -forward.x
        orientation.m[9] = -forward.y
        orientation.m[10] = -forward.z
        orientation.m[11] = 0f
        orientation.m[12] = position.x
        orientation.m[13] = position.y
        orientation.m[14] = position.z
        orientation.m[15] = 1f
    }

    fun propellerMatrix(out: Mat4) {
        propLocal.identity()
        propLocal.translate(0f, 0f, -1.62f)
        propLocal.rotateZ(propellerAngle)
        out.setProduct(orientation, propLocal)
    }

    fun speed(): Float = velocity.length()

    fun speedFactor(): Float = M.clamp(speed() / 40f, 0f, 1f)

    fun launchOrigin(): Vec3 = start
}
