package com.buildlaunchfly.game.game

import org.json.JSONObject

/**
 * Player-facing preferences from docs/DESIGN.md §6 Screen 8, persisted inside the profile.
 */
class GameSettings {

    var quality: GraphicsQuality = GraphicsQuality.HIGH
    var waterReflections: Boolean = true
    var volumetricFog: Boolean = true
    var highResTextures: Boolean = false

    var masterVolume: Float = 0.8f
    var engineVolume: Float = 0.85f
    var ambienceVolume: Float = 0.8f

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("quality", quality.name)
        json.put("waterReflections", waterReflections)
        json.put("volumetricFog", volumetricFog)
        json.put("highResTextures", highResTextures)
        json.put("startupVersion", STARTUP_VERSION)
        json.put("masterVolume", masterVolume.toDouble())
        json.put("engineVolume", engineVolume.toDouble())
        json.put("ambienceVolume", ambienceVolume.toDouble())
        return json
    }

    companion object {
        /**
         * Bumped when the start-up cost of a setting changes. Saves written before this
         * version get the cheap 128 px textures so the first launch after an update is fast.
         */
        const val STARTUP_VERSION = 2

        fun fromJson(json: JSONObject?): GameSettings {
            val settings = GameSettings()
            if (json == null) return settings
            settings.quality = GraphicsQuality.fromName(json.optString("quality", GraphicsQuality.HIGH.name))
            settings.waterReflections = json.optBoolean("waterReflections", true)
            settings.volumetricFog = json.optBoolean("volumetricFog", true)
            settings.highResTextures = json.optBoolean("highResTextures", false)
            if (json.optInt("startupVersion", 1) < STARTUP_VERSION) {
                settings.highResTextures = false
            }
            settings.masterVolume = json.optDouble("masterVolume", 0.8).toFloat().coerceIn(0f, 1f)
            settings.engineVolume = json.optDouble("engineVolume", 0.85).toFloat().coerceIn(0f, 1f)
            settings.ambienceVolume = json.optDouble("ambienceVolume", 0.8).toFloat().coerceIn(0f, 1f)
            return settings
        }
    }
}
