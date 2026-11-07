package com.buildlaunchfly.game.world

import com.buildlaunchfly.game.gfx.Camera
import com.buildlaunchfly.game.gfx.ParticleRenderer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Falling snow / blowing dust, emitted into the shared particle batch every frame. */
class WeatherSystem(private val definition: WorldDefinition) {

    private val count = 420
    private val x = FloatArray(count)
    private val y = FloatArray(count)
    private val z = FloatArray(count)
    private val speed = FloatArray(count)
    private val phase = FloatArray(count)
    private var initialized = false

    private val snowy = definition.snowfall > 0f
    private val dusty = definition.dust > 0f
    private val seaSpray = definition.id == WorldId.OCEAN
    private val rng = Random(definition.seed + 7919)

    private fun init(centerX: Float, centerY: Float, centerZ: Float) {
        for (i in 0 until count) {
            x[i] = centerX + (rng.nextFloat() * 2f - 1f) * BOX
            y[i] = if (seaSpray) definition.waterLevel + 0.6f + rng.nextFloat() * 7f else centerY + (rng.nextFloat() * 2f - 1f) * BOX * 0.6f
            z[i] = centerZ + (rng.nextFloat() * 2f - 1f) * BOX
            speed[i] = when { snowy -> 1.6f + rng.nextFloat() * 2.4f; seaSpray -> 0.8f + rng.nextFloat() * 1.7f; else -> 1.2f + rng.nextFloat() * 2.6f }
            phase[i] = rng.nextFloat() * 6.2831855f
        }
        initialized = true
    }

    fun updateAndEmit(camera: Camera, dt: Float, time: Float, particles: ParticleRenderer) {
        if (!snowy && !dusty && !seaSpray) return
        if (!initialized) init(camera.position.x, camera.position.y, camera.position.z)

        val cx = camera.position.x
        val cy = camera.position.y
        val cz = camera.position.z

        for (i in 0 until count) {
            if (snowy) {
                y[i] -= speed[i] * dt
                x[i] += sin(time * 0.7f + phase[i]) * 0.55f * dt
                z[i] += cos(time * 0.53f + phase[i]) * 0.45f * dt
                if (y[i] < cy - BOX * 0.6f) {
                    y[i] = cy + BOX * 0.6f
                    x[i] = cx + (rng.nextFloat() * 2f - 1f) * BOX
                    z[i] = cz + (rng.nextFloat() * 2f - 1f) * BOX
                }
                particles.emit(x[i], y[i], z[i], 0.35f)
            } else if (seaSpray) {
                // Fine salt mist rises from the sea surface and drifts inland in the coastal wind.
                x[i] += speed[i] * 2.2f * dt
                y[i] = definition.waterLevel + 2.3f + sin(time * 0.8f + phase[i]) * 2.2f +
                    sin(time * 1.7f + phase[i] * 1.4f) * 0.55f
                z[i] += cos(time * 0.35f + phase[i]) * 0.18f * dt
                if (x[i] > cx + BOX) {
                    x[i] = cx - BOX
                    y[i] = definition.waterLevel + rng.nextFloat() * 7f
                    z[i] = cz + (rng.nextFloat() * 2f - 1f) * BOX
                }
                particles.emit(x[i], y[i], z[i], 0.16f)
            } else {
                // Dust drifts horizontally in sheets close to the ground.
                x[i] += speed[i] * 1.6f * dt
                y[i] += sin(time * 1.3f + phase[i]) * 0.22f * dt
                if (x[i] > cx + BOX) x[i] = cx - BOX
                particles.emit(x[i], y[i], z[i], 0.22f)
            }
        }
    }

    companion object {
        private const val BOX = 46f
    }
}
