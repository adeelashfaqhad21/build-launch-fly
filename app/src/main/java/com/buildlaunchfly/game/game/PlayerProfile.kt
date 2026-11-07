package com.buildlaunchfly.game.game

import android.content.Context
import android.util.Log
import com.buildlaunchfly.game.aircraft.PartSlot
import com.buildlaunchfly.game.slingshot.SlingUpgrade
import com.buildlaunchfly.game.world.WorldCatalog
import com.buildlaunchfly.game.world.WorldId
import org.json.JSONArray
import org.json.JSONObject

/** Everything that persists between sessions. */
class PlayerProfile {

    var coins: Int = 320
    var bestDistance: Float = 0f
    var bestAltitude: Float = 0f
    var flights: Int = 0
    var totalDistance: Float = 0f
    var glacierCrevasseCrossed: Boolean = false

    /** Ballistic launch elevation, 20°..55° per design.md §6 Screen 3. */
    var launchAngleDegrees: Float = 24f

    var selectedWorld: WorldId = WorldId.FOREST
        private set

    val unlockedWorlds: MutableSet<WorldId> = mutableSetOf(WorldId.FOREST)
    val partTiers: MutableMap<PartSlot, Int> = LinkedHashMap()
    val slingshotLevels: MutableMap<SlingUpgrade, Int> = LinkedHashMap()
    val bestPerWorld: MutableMap<WorldId, Float> = LinkedHashMap()
    val claimedAchievements: MutableSet<String> = LinkedHashSet()
    val settings: GameSettings = GameSettings()

    init {
        partTiers[PartSlot.BODY] = 1
    }

    // ---- pilot progression ------------------------------------------------

    fun pilotLevel(): Int = 1 + (totalDistance / 2000f).toInt().coerceIn(0, 29)

    fun pilotRankTitle(): String {
        val level = pilotLevel()
        return when {
            level < 5 -> "Cadet"
            level < 10 -> "Pilot"
            level < 18 -> "Ace Aeronaut"
            level < 25 -> "Sky Captain"
            else -> "Sky Legend"
        }
    }

    fun levelProgressFraction(): Float {
        if (pilotLevel() >= 30) return 1f
        return ((totalDistance % 2000f) / 2000f).coerceIn(0f, 1f)
    }

    // ---- worlds -----------------------------------------------------------

    fun selectWorld(id: WorldId) {
        unlockedWorlds.add(WorldId.FOREST)
        if (unlockedWorlds.contains(id)) selectedWorld = id
    }

    fun refreshUnlocks() {
        unlockedWorlds.add(WorldId.FOREST)
        for (world in WorldCatalog.all) {
            if (bestDistance >= world.unlockDistanceMeters) unlockedWorlds.add(world.id)
            if (bestPerWorld[world.id] ?: 0f >= world.unlockDistanceMeters) unlockedWorlds.add(world.id)
        }
        if (!unlockedWorlds.contains(selectedWorld)) selectedWorld = WorldId.FOREST
    }

    fun bestForWorld(id: WorldId): Float = bestPerWorld[id] ?: 0f

    // ---- records ----------------------------------------------------------

    fun recordFlight(
        distance: Float,
        coinsEarned: Int,
        altitude: Float,
        worldId: WorldId
    ) {
        coins += coinsEarned
        flights += 1
        totalDistance += distance
        if (distance > bestDistance) bestDistance = distance
        if (altitude > bestAltitude) bestAltitude = altitude
        val previous = bestPerWorld[worldId] ?: 0f
        if (distance > previous) bestPerWorld[worldId] = distance
        refreshUnlocks()
    }

    fun claimAchievement(id: String): Boolean {
        if (claimedAchievements.contains(id)) return false
        val achievement = AchievementCatalog.byId(id) ?: return false
        if (!AchievementCatalog.isComplete(achievement, this)) return false
        claimedAchievements.add(id)
        coins += achievement.reward
        return true
    }

    // ---- persistence ------------------------------------------------------

    fun toJson(): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("coins", coins)
        root.put("bestDistance", bestDistance.toDouble())
        root.put("bestAltitude", bestAltitude.toDouble())
        root.put("flights", flights)
        root.put("totalDistance", totalDistance.toDouble())
        root.put("glacierCrevasseCrossed", glacierCrevasseCrossed)
        root.put("launchAngle", launchAngleDegrees.toDouble())
        root.put("selectedWorld", selectedWorld.name)

        val worlds = JSONArray()
        for (id in unlockedWorlds) worlds.put(id.name)
        root.put("unlockedWorlds", worlds)

        val bestPerWorldJson = JSONObject()
        for ((id, distance) in bestPerWorld) bestPerWorldJson.put(id.name, distance.toDouble())
        root.put("bestPerWorld", bestPerWorldJson)

        val parts = JSONObject()
        for ((slot, tier) in partTiers) parts.put(slot.name, tier)
        root.put("parts", parts)

        val slingshot = JSONObject()
        for ((type, level) in slingshotLevels) slingshot.put(type.name, level)
        root.put("slingshot", slingshot)

        val claimed = JSONArray()
        for (id in claimedAchievements) claimed.put(id)
        root.put("claimedAchievements", claimed)

        root.put("settings", settings.toJson())

        return root.toString()
    }

    companion object {

        fun fromJson(text: String?): PlayerProfile {
            val profile = PlayerProfile()
            if (text.isNullOrBlank()) return profile
            try {
                val root = JSONObject(text)
                profile.coins = root.optInt("coins", profile.coins)
                profile.bestDistance = root.optDouble("bestDistance", 0.0).toFloat()
                profile.bestAltitude = root.optDouble("bestAltitude", 0.0).toFloat()
                profile.flights = root.optInt("flights", 0)
                profile.totalDistance = root.optDouble("totalDistance", 0.0).toFloat()
                profile.glacierCrevasseCrossed = root.optBoolean("glacierCrevasseCrossed", false)
                profile.launchAngleDegrees =
                    root.optDouble("launchAngle", 24.0).toFloat().coerceIn(20f, 55f)

                val selected = root.optString("selectedWorld", WorldId.FOREST.name)
                for (id in WorldId.values()) {
                    if (id.name == selected) profile.selectedWorld = id
                }

                val worlds = root.optJSONArray("unlockedWorlds")
                if (worlds != null) {
                    for (i in 0 until worlds.length()) {
                        val name = worlds.optString(i, "")
                        for (id in WorldId.values()) {
                            if (id.name == name) profile.unlockedWorlds.add(id)
                        }
                    }
                }

                val perWorld = root.optJSONObject("bestPerWorld")
                if (perWorld != null) {
                    for (id in WorldId.values()) {
                        if (perWorld.has(id.name)) {
                            profile.bestPerWorld[id] = perWorld.optDouble(id.name, 0.0).toFloat()
                        }
                    }
                }

                val parts = root.optJSONObject("parts")
                if (parts != null) {
                    for (slot in PartSlot.values()) {
                        if (parts.has(slot.name)) {
                            profile.partTiers[slot] = parts.optInt(slot.name, 0)
                        }
                    }
                }

                val slingshot = root.optJSONObject("slingshot")
                if (slingshot != null) {
                    for (type in SlingUpgrade.values()) {
                        if (slingshot.has(type.name)) {
                            profile.slingshotLevels[type] = slingshot.optInt(type.name, 0)
                        }
                    }
                }

                val claimed = root.optJSONArray("claimedAchievements")
                if (claimed != null) {
                    for (i in 0 until claimed.length()) {
                        val id = claimed.optString(i, "")
                        if (id.isNotEmpty()) profile.claimedAchievements.add(id)
                    }
                }

                val settings = GameSettings.fromJson(root.optJSONObject("settings"))
                profile.copySettings(settings)
            } catch (error: Throwable) {
                Log.w("BLF-SAVE", "Could not parse save data, starting fresh", error)
                return PlayerProfile()
            }
            if (!profile.partTiers.containsKey(PartSlot.BODY)) profile.partTiers[PartSlot.BODY] = 1
            profile.refreshUnlocks()
            return profile
        }
    }

    private fun copySettings(other: GameSettings) {
        settings.quality = other.quality
        settings.waterReflections = other.waterReflections
        settings.volumetricFog = other.volumetricFog
        settings.highResTextures = other.highResTextures
        settings.masterVolume = other.masterVolume
        settings.engineVolume = other.engineVolume
        settings.ambienceVolume = other.ambienceVolume
    }
}

/** Small wrapper around SharedPreferences storage of the JSON profile. */
class ProfileStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): PlayerProfile = PlayerProfile.fromJson(prefs.getString(KEY_PROFILE, null))

    fun save(profile: PlayerProfile) {
        prefs.edit().putString(KEY_PROFILE, profile.toJson()).apply()
    }

    companion object {
        private const val PREFS_NAME = "build_launch_fly_profile"
        private const val KEY_PROFILE = "profile_json"
    }
}
