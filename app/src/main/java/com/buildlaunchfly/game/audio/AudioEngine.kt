package com.buildlaunchfly.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.buildlaunchfly.game.world.AmbienceStyle
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fully procedural audio. Every sound — wind, propeller, slingshot creak and release,
 * per-world ambience and UI cues — is synthesized on a dedicated mixing thread and written
 * to a streaming [AudioTrack], so the repository needs no audio assets.
 */
class AudioEngine {

    enum class Cue { CLICK, UPGRADE, REWARD, UNLOCK, RESULT }

    @Volatile
    private var running = false

    private var track: AudioTrack? = null
    private var thread: Thread? = null

    @Volatile
    private var ambience = AmbienceStyle.FOREST

    @Volatile
    private var airspeed = 0f

    @Volatile
    private var propeller = 0f

    @Volatile
    private var flightActive = false

    @Volatile
    private var stretch = 0f

    private val cues = ConcurrentLinkedQueue<Int>()
    private val releases = ConcurrentLinkedQueue<Float>()

    fun start() {
        if (running) return
        val sampleRate = SAMPLE_RATE
        try {
            val minBuffer = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) {
                Log.w(TAG, "No usable audio buffer size ($minBuffer), audio disabled")
                return
            }

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val created = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(minBuffer * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track = created
            created.play()
            running = true

            val worker = Thread({ mixLoop(sampleRate) }, "blf-audio")
            thread = worker
            worker.start()
        } catch (error: Throwable) {
            Log.e(TAG, "Audio engine unavailable", error)
            running = false
        }
    }

    fun stop() {
        running = false
        val worker = thread
        if (worker != null) {
            try {
                worker.join(300)
            } catch (ignored: InterruptedException) {
                // Nothing to do.
            }
        }
        thread = null
        val audioTrack = track
        track = null
        if (audioTrack != null) {
            try {
                audioTrack.pause()
                audioTrack.flush()
                audioTrack.stop()
                audioTrack.release()
            } catch (ignored: Throwable) {
                // Already dead.
            }
        }
    }

    fun setAmbience(style: AmbienceStyle) {
        ambience = style
    }

    fun setFlight(airspeed01: Float, prop01: Float, active: Boolean) {
        airspeed = airspeed01.coerceIn(0f, 1f)
        propeller = prop01.coerceIn(0f, 1f)
        flightActive = active
    }

    fun setStretch(amount: Float) {
        stretch = amount.coerceIn(0f, 1f)
    }

    fun playRelease(power: Float) {
        releases.add(power.coerceIn(0f, 1f))
    }

    fun playUi(cue: Cue) {
        cues.add(cue.ordinal)
    }

    private fun mixLoop(sampleRate: Int) {
        val frames = 1024
        val buffer = ShortArray(frames)
        val rng = Random(20240517)
        val twoPi = 6.2831855f

        var windLow = 0f
        var windHigh = 0f
        var propPhase = 0f
        var creakPhase = 0f
        var chirpPhase = 0f
        var chirpEnvelope = 0f
        var releaseEnvelope = 0f
        var releasePower = 0f
        var cueEnvelope = 0f
        var cuePhase = 0f
        var cueFrequency = 760f
        var swellPhase = 0f

        while (running) {
            val audioTrack = track ?: break

            while (true) {
                val release = releases.poll() ?: break
                releaseEnvelope = 1f
                releasePower = release
            }
            while (true) {
                val cue = cues.poll() ?: break
                when (cue) {
                    Cue.CLICK.ordinal -> {
                        cueEnvelope = 0.30f
                        cueFrequency = 780f
                    }
                    Cue.UPGRADE.ordinal -> {
                        cueEnvelope = 0.45f
                        cueFrequency = 520f
                    }
                    Cue.REWARD.ordinal -> {
                        cueEnvelope = 0.45f
                        cueFrequency = 660f
                    }
                    Cue.UNLOCK.ordinal -> {
                        cueEnvelope = 0.50f
                        cueFrequency = 440f
                    }
                    else -> {
                        cueEnvelope = 0.55f
                        cueFrequency = 320f
                    }
                }
            }

            for (i in 0 until frames) {
                val white = rng.nextFloat() * 2f - 1f
                windLow += (white - windLow) * 0.010f
                windHigh += (white - windHigh) * 0.060f

                var sample = 0f

                when (ambience) {
                    AmbienceStyle.FOREST -> {
                        sample += windLow * 0.22f
                        if (rng.nextFloat() < 0.00035f) {
                            chirpEnvelope = 0.5f
                            chirpPhase = 0f
                        }
                        if (chirpEnvelope > 0.0005f) {
                            val frequency = 1500f + 700f * sin(chirpPhase * 0.35f)
                            sample += sin(chirpPhase) * chirpEnvelope * 0.12f
                            chirpPhase += twoPi * frequency / sampleRate
                            chirpEnvelope *= 0.99920f
                        }
                    }
                    AmbienceStyle.DESERT -> {
                        sample += windLow * 0.30f + windHigh * 0.05f
                    }
                    AmbienceStyle.SNOW -> {
                        sample += (white - windHigh) * 0.05f + windLow * 0.16f
                    }
                    AmbienceStyle.OCEAN -> {
                        swellPhase += twoPi * 0.55f / sampleRate
                        if (swellPhase > twoPi) swellPhase -= twoPi
                        val swell = sin(swellPhase) * 0.5f + 0.5f
                        sample += windLow * (0.18f + swell * 0.20f) + windHigh * 0.03f
                    }
                    AmbienceStyle.MOUNTAIN -> {
                        sample += windLow * 0.24f + windHigh * 0.02f
                    }
                }

                if (flightActive) {
                    sample += windHigh * airspeed * 0.32f
                    if (propeller > 0.01f) {
                        propPhase += (90f + propeller * 150f) / sampleRate
                        if (propPhase > 1f) propPhase -= 1f
                        val saw = 2f * propPhase - 1f
                        sample += saw * propeller * 0.09f
                        sample += sin(propPhase * twoPi * 2f) * propeller * 0.03f
                    }
                }

                if (stretch > 0.01f) {
                    creakPhase += (70f + stretch * 90f) / sampleRate
                    if (creakPhase > 1f) creakPhase -= 1f
                    val creak = if (creakPhase < 0.5f) 0.5f else -0.5f
                    sample += creak * stretch * 0.06f + white * stretch * 0.02f
                }

                if (releaseEnvelope > 0.0005f) {
                    sample += windHigh * releaseEnvelope * (0.45f + releasePower * 0.55f)
                    releaseEnvelope *= 0.99980f
                }

                if (cueEnvelope > 0.0005f) {
                    cuePhase += twoPi * cueFrequency / sampleRate
                    if (cuePhase > twoPi) cuePhase -= twoPi
                    sample += sin(cuePhase) * cueEnvelope * 0.12f
                    cueEnvelope *= 0.99975f
                }

                val clamped = sample.coerceIn(-1f, 1f) * MASTER_GAIN
                buffer[i] = (clamped * 32767f).toInt().toShort()
            }

            val written = audioTrack.write(buffer, 0, frames)
            if (written < 0) break
        }
    }

    companion object {
        private const val TAG = "BLF-AUDIO"
        private const val SAMPLE_RATE = 44100
        private const val MASTER_GAIN = 0.55f
    }
}
