package com.buildlaunchfly.game.aircraft

import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3

enum class FlightState { READY, FLYING, LANDED, CRASHED }

/** Player commands, both −1…1: pitch positive is nose up, roll positive is bank right. */
class FlightInput(var pitch: Float = 0f, var roll: Float = 0f)

/**
 * Glider flight model.
 *
 * The slingshot is the only source of launch power (master brief: "the player stretches the
 * rubber bands and releases the aircraft to initiate flight"). After release the aircraft
 * keeps flying on momentum and aerodynamics alone, so the two things that decide a flight
 * are exactly the two the player controls:
 *
 *  - **Attitude.** Lift comes from angle of attack, which is the difference between where the
 *    fuselage points and where it is actually going. Raising the nose buys lift but also buys
 *    a large induced-drag penalty, so airspeed bleeds away; past the stall angle the wing
 *    stops lifting and the aircraft drops. Holding a small, efficient angle of attack gives
 *    the flattest, longest glide.
 *  - **Bank.** Lift acts along the aircraft's own up axis, so banking tilts part of it
 *    sideways. That sideways force is what curves the flight path around trees and buildings.
 *    There is no separate "steer" force.
 *
 * Gravity, air density and wing area are real numbers, and the wing is defined by the
 * installed parts, so a wingless airframe really does fall out of the sky.
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

    /** True while the wing is beyond its stall angle: lift is collapsing. */
    var stalled = false
        private set

    /** Current angle of attack in radians, for instrumentation and HUD feedback. */
    var angleOfAttack = 0f
        private set

    var distance = 0f
        private set
    var maxSpeed = 0f
        private set

    var propellerAngle = 0f
        private set
    var propellerRpm = 0f
        private set

    var throttle = 1f

    /**
     * Uniform visual scale applied to [orientation] and the propeller offset. The flight
     * model works in metres; the artwork is drawn larger so the aircraft reads as the hero
     * element of the launch shot.
     */
    var visualScale = 1f

    val orientation = Mat4()

    private val start = Vec3()
    private val forward = Vec3()
    private val up = Vec3()
    private val right = Vec3()
    private val up0 = Vec3()
    private val accel = Vec3()
    private val tmp = Vec3()
    private val velocityDirection = Vec3()
    private val liftDirection = Vec3()
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
        angleOfAttack = 0f
        stalled = false
        distance = 0f
        maxSpeed = 0f
        propellerRpm = 0f
        state = FlightState.READY
        start.set(origin)
        updateOrientation()
    }

    /**
     * Fires the aircraft off the ramp. [pitchDegrees] is the launch elevation of the flight
     * path; the fuselage is pitched a few degrees above it so the wing meets the air with a
     * useful angle of attack from the first frame.
     */
    fun launch(speed: Float, pitchDegrees: Float, yawDegrees: Float) {
        val gamma = M.deg(pitchDegrees.coerceIn(-30f, 55f))
        yaw = M.deg(yawDegrees)
        pitch = (gamma + LAUNCH_ALPHA).coerceIn(-MAX_PITCH, MAX_PITCH)
        roll = 0f
        pitchRate = 0f
        rollRate = 0f
        yawRate = 0f
        stalled = false
        updateOrientation()
        velocity.set(
            kotlin.math.sin(yaw) * kotlin.math.cos(gamma),
            kotlin.math.sin(gamma),
            -kotlin.math.cos(yaw) * kotlin.math.cos(gamma)
        ).scale(speed)
        state = FlightState.FLYING
        distance = 0f
        maxSpeed = speed
    }

    fun update(dt: Float, stats: AircraftStats, terrainHeight: Float, input: FlightInput) {
        if (state != FlightState.FLYING) {
            windmill(dt)
            return
        }

        val speed = velocity.length()
        // Elevator authority grows with airspeed: a fast dive has the power to pull out, a slow
        // one does not, so "trade height for speed, then pull" is a move the model rewards.
        val controlAuthority = M.clamp(speed / 18f, 0.35f, 1.5f)

        // ---- attitude: the stick asks for an angle of attack, not a fixed horizon angle ----
        // Commanding alpha (nose relative to the airflow) keeps the launch arc intact and
        // makes "hold a nose angle" the literal thing the player is doing.
        val gammaNow = if (speed > 1e-4f) {
            kotlin.math.asin(M.clamp(velocity.y / speed, -1f, 1f))
        } else {
            0f
        }
        val pitchCommand = input.pitch.coerceIn(-1f, 1f)
        // The wing can only use so much angle before the airflow separates. Asking for more than
        // that is what used to leave the aircraft stuck nose-down: a hard pull past the stall
        // angle bought drag instead of lift, so it sank whatever its speed. The request is kept
        // inside the working part of the lift curve, which means a hard pull always produces the
        // most lift the wing has at that airspeed — and the faster the aircraft is going, the
        // harder that lift pulls the flight path back up out of a dive.
        val wantedAlpha = ALPHA_TRIM + pitchCommand * ALPHA_RANGE
        val targetAlpha = wantedAlpha.coerceIn(-ALPHA_NEG, ALPHA_LIMIT)
        val targetPitch = M.wrapAngle(gammaNow + targetAlpha)
        val pitchError = M.wrapAngle(targetPitch - pitch)
        val pitchGain = (6f + stats.stability * 2f) * controlAuthority
        val pitchLimit = PITCH_RATE * controlAuthority
        pitchRate = M.lerp(
            pitchRate,
            M.clamp(pitchError * pitchGain, -pitchLimit, pitchLimit),
            M.clamp(9f * dt, 0f, 1f)
        )
        pitch = M.clamp(pitch + pitchRate * dt, -MAX_PITCH, MAX_PITCH)

        val rollCommand = input.roll.coerceIn(-1f, 1f)
        val targetRoll = rollCommand * MAX_BANK
        val rollAuthority = M.clamp(0.35f + speed / 30f, 0.35f, 1f)
        val rollGain = (7f + stats.stability * 1.5f) * rollAuthority
        rollRate = M.lerp(
            rollRate,
            M.clamp((targetRoll - roll) * rollGain, -ROLL_RATE, ROLL_RATE),
            M.clamp(8f * dt, 0f, 1f)
        )
        roll = M.clamp(roll + rollRate * dt, -MAX_BANK, MAX_BANK)

        updateOrientation()

        // ---- aerodynamics ----
        val gamma = if (speed > 1e-4f) {
            kotlin.math.asin(M.clamp(velocity.y / speed, -1f, 1f))
        } else {
            0f
        }
        val alpha = M.wrapAngle(pitch - gamma)
        angleOfAttack = alpha

        val clMax = CL_MAX + stats.lift * 0.16f
        val cl = liftCoefficient(alpha, clMax)

        val q = 0.5f * RHO * speed * speed
        val refArea = stats.wingArea.coerceAtLeast(0.8f)
        val hasWing = stats.wingArea >= 0.5f

        // The wing itself is working; the airspeed is not. That is what the warning is for: under
        // this speed even the best angle of attack cannot hold the aircraft up, so the way to fly
        // far is to trade height for speed instead of living on the stick.
        stalled = hasWing && (speed < stallSpeed(stats) || kotlin.math.abs(alpha) > ALPHA_STALL)

        val liftAccel = if (hasWing) q * refArea * cl / stats.mass else 0f

        // Profile drag, induced drag (the price of lift) and the extra drag of a stalled wing.
        val overStall = ((kotlin.math.abs(alpha) - ALPHA_STALL) / ALPHA_STALL).coerceAtLeast(0f)
        val induced = 0.05f * cl * cl
        val stallDrag = 0.85f * overStall * overStall
        val cd = CD0 + stats.dragArea * 0.004f + induced + stallDrag
        val dragAccel = q * refArea * cd / stats.mass

        // The engine can only sustain a little height at low speed; it can never power a climb.
        val sustain = if (stats.thrust > 0f && speed < SUSTAIN_SPEED) {
            stats.thrust * 0.12f * (1f - speed / SUSTAIN_SPEED) / stats.mass
        } else {
            0f
        }

        accel.set(0f, -GRAVITY, 0f)
        if (hasWing && liftAccel > 0f) {
            // Aerodynamic lift is perpendicular to the relative airflow, not parallel to the
            // fuselage's up axis. Project the banked wing-up vector onto the plane normal to
            // velocity; this preserves bank steering without adding a false backwards force
            // every time the pilot raises the nose.
            if (speed > 1e-4f) {
                velocityDirection.set(velocity).scale(1f / speed)
                val alongAirflow = up.dot(velocityDirection)
                liftDirection.set(up).addScaled(velocityDirection, -alongAirflow).normalize()
                if (liftDirection.lengthSq() < 1e-6f) liftDirection.set(up)
            } else {
                liftDirection.set(up)
            }
            accel.addScaled(liftDirection, liftAccel)
        }
        if (speed > 1e-4f) {
            tmp.set(velocity).scale(-1f / speed)
            accel.addScaled(tmp, dragAccel)
        }
        if (sustain > 0f) accel.addScaled(forward, sustain)

        velocity.addScaled(accel, dt)

        // ---- heading follows the actual flight path, which is what banking curves ----
        val horizontalSpeed = kotlin.math.sqrt(
            velocity.x * velocity.x + velocity.z * velocity.z
        )
        if (horizontalSpeed > 0.4f) {
            val targetYaw = kotlin.math.atan2(velocity.x, -velocity.z)
            val yawError = M.wrapAngle(targetYaw - yaw)
            yawRate = M.lerp(
                yawRate,
                M.clamp(yawError * 2.4f, -YAW_RATE, YAW_RATE),
                M.clamp(6f * dt, 0f, 1f)
            )
            yaw = M.wrapAngle(yaw + yawRate * dt)
        }

        val previousX = position.x
        val previousZ = position.z
        position.addScaled(velocity, dt)

        val dx = position.x - previousX
        val dz = position.z - previousZ
        distance += kotlin.math.sqrt(dx * dx + dz * dz)

        val currentSpeed = velocity.length()
        if (currentSpeed > maxSpeed) maxSpeed = currentSpeed

        updateOrientation()
        drivePropeller(dt, stats, speed)

        // ---- ground contact: a gentle arrival is a landing, anything else is a crash ----
        val groundY = terrainHeight + 0.5f
        if (position.y <= groundY) {
            position.y = groundY
            val vertical = velocity.y
            state = when {
                kotlin.math.abs(roll) > 0.85f -> FlightState.CRASHED
                currentSpeed > 34f -> FlightState.CRASHED
                vertical < -12f -> FlightState.CRASHED
                kotlin.math.abs(pitch) > 0.45f -> FlightState.CRASHED
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

    /**
     * Ends the flight on the spot. [update] already handles arriving at the ground; this is for
     * the world's own solids — a trunk, a rooftop, a ruin — which the caller tests against the
     * scene before the aircraft can pass through them.
     */
    fun impact() {
        if (state != FlightState.FLYING) return
        state = FlightState.CRASHED
        velocity.scale(0.12f)
    }

    /**
     * Lift curve: linear to the stall angle, then falling away as the wing separates.
     * Climbing above the stall angle is exactly what costs the player their airspeed.
     */
    private fun liftCoefficient(alpha: Float, clMax: Float): Float {
        val magnitude = kotlin.math.abs(alpha)
        if (magnitude <= ALPHA_STALL) return clMax * (alpha / ALPHA_STALL)
        val over = ((magnitude - ALPHA_STALL) / ALPHA_STALL).coerceIn(0f, 1f)
        val fallen = clMax * (1f - 0.72f * over)
        return if (alpha > 0f) fallen else -fallen
    }

    /** Airspeed below which the wing can no longer hold the aircraft up, in m/s. */
    fun stallSpeed(stats: AircraftStats): Float {
        val clMax = CL_MAX + stats.lift * 0.16f
        val area = stats.wingArea.coerceAtLeast(0.8f)
        return kotlin.math.sqrt(2f * stats.mass * GRAVITY / (RHO * area * clMax))
    }

    private fun drivePropeller(dt: Float, stats: AircraftStats, speed: Float) {
        if (stats.hasPropeller) {
            val windmill = speed * 40f
            val targetRpm = maxOf(windmill, stats.maxRpm * throttle)
            propellerRpm = M.lerp(propellerRpm, targetRpm, M.clamp(2f * dt, 0f, 1f))
            propellerAngle += propellerRpm / 60f * M.TWO_PI * dt
            if (propellerAngle > M.TWO_PI) propellerAngle -= M.TWO_PI
        } else {
            propellerRpm = 0f
        }
    }

    private fun windmill(dt: Float) {
        if (propellerRpm > 1f) {
            propellerRpm *= 0.985f
            propellerAngle += propellerRpm / 60f * M.TWO_PI * dt
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

        val scale = visualScale
        orientation.m[0] = right.x * scale
        orientation.m[1] = right.y * scale
        orientation.m[2] = right.z * scale
        orientation.m[3] = 0f
        orientation.m[4] = up.x * scale
        orientation.m[5] = up.y * scale
        orientation.m[6] = up.z * scale
        orientation.m[7] = 0f
        orientation.m[8] = -forward.x * scale
        orientation.m[9] = -forward.y * scale
        orientation.m[10] = -forward.z * scale
        orientation.m[11] = 0f
        orientation.m[12] = position.x
        orientation.m[13] = position.y
        orientation.m[14] = position.z
        orientation.m[15] = 1f
    }

    fun propellerMatrix(out: Mat4) {
        propLocal.identity()
        propLocal.translate(0f, 0f, -1.62f * visualScale)
        propLocal.rotateZ(propellerAngle)
        out.setProduct(orientation, propLocal)
    }

    fun speed(): Float = velocity.length()

    fun speedFactor(): Float = M.clamp(speed() / 40f, 0f, 1f)

    fun launchOrigin(): Vec3 = start

    private companion object {
        const val GRAVITY = 9.81f
        const val RHO = 1.225f

        /** Wing lift curve: linear to 16°, then falling away. */
        const val ALPHA_STALL = 0.28f
        const val CL_MAX = 1.05f
        const val CD0 = 0.040f

        /**
         * The most angle of attack the stick may ask for, just under the stall angle, so a full
         * pull sits at the peak of the lift curve instead of beyond it.
         */
        const val ALPHA_LIMIT = ALPHA_STALL * 0.96f

        /** A few degrees of angle of attack off the ramp so the wing bites immediately. */
        const val LAUNCH_ALPHA = 0.10f

        /** Neutral stick holds this angle of attack: the efficient glide attitude. */
        const val ALPHA_TRIM = 0.13f

        /** How far the stick may take the angle of attack away from trim, either way. */
        const val ALPHA_RANGE = 0.28f

        /** The most negative angle of attack a full push may ask for, well inside the curve. */
        const val ALPHA_NEG = 0.15f

        const val MAX_PITCH = 1.50f
        const val PITCH_RATE = 1.6f

        const val MAX_BANK = 0.95f
        const val ROLL_RATE = 2.4f
        const val YAW_RATE = 1.9f

        /** Below this airspeed the engine can add a little sustain, never a climb. */
        const val SUSTAIN_SPEED = 22f
    }
}
