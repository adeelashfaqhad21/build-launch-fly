package com.buildlaunchfly.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

/**
 * Plays the imported Kenney cue samples (OGG in `assets/audio`). It is a front layer over the
 * synthesised engine: every play call reports whether a sample really played, so the caller
 * can fall back to the synth when a file is missing or still decoding.
 */
class SoundBank(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = HashMap<String, Int>()
    private val loaded = HashSet<Int>()

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
        load("click_001")
        load("confirmation_001")
        load("switch_001")
        load("error_001")
        load("release")
    }

    private fun load(name: String) {
        try {
            val descriptor = context.assets.openFd("audio/$name.ogg")
            val id = pool.load(descriptor, 1)
            descriptor.close()
            if (id != 0) ids[name] = id
        } catch (error: Throwable) {
            Log.e(TAG, "Missing cue sample $name", error)
        }
    }

    /** Returns true when a real sample played, false to let the synth take the cue. */
    fun playUi(cue: AudioEngine.Cue, volume: Float): Boolean = when (cue) {
        AudioEngine.Cue.CLICK -> play("click_001", volume)
        AudioEngine.Cue.UPGRADE -> play("confirmation_001", volume)
        AudioEngine.Cue.REWARD -> play("confirmation_001", volume * 0.9f)
        AudioEngine.Cue.UNLOCK -> play("switch_001", volume)
        AudioEngine.Cue.RESULT -> play("error_001", volume * 0.8f)
    }

    fun playRelease(power: Float, volume: Float): Boolean =
        play("release", volume * (0.55f + 0.45f * power.coerceIn(0f, 1f)))

    private fun play(name: String, volume: Float): Boolean {
        val id = ids[name] ?: return false
        if (id !in loaded) return false
        val v = volume.coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, 1f)
        return true
    }

    fun release() {
        pool.release()
        ids.clear()
        loaded.clear()
    }

    private companion object {
        const val TAG = "BLF-SOUND"
    }
}
