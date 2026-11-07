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

    var coins: Int = 300
    var bestDistance: Float = 0f
    var flights: Int = 0
    var totalDistance: Float = 0f

    var selectedWorld: WorldId = WorldId.FOREST
        private set

    val unlockedWorlds: MutableSet<WorldId> = mutableSetOf(WorldId.FOREST)
    val partTiers: MutableMap<PartSlot, Int> = LinkedHashMap()
    val slingshotLevels: MutableMap<SlingUpgrade, Int> = LinkedHashMap()

    init {
        partTiers[PartSlot.BODY] = 1
    }

    fun selectWorld(id: WorldId) {
        unlockedWorlds.add(WorldId.FOREST)
        if (unlockedWorlds.contains(id)) selectedWorld = id
    }

    /** Unlocks worlds whose distance gate the player has passed. */
    fun refreshUnlocks() {
        unlockedWorlds.add(WorldId.FOREST)
        for (world in WorldCatalog.all) {
            if (bestDistance >= world.unlockDistanceMeters) unlockedWorlds.add(world.id)
        }
        if (!unlockedWorlds.contains(selectedWorld)) selectedWorld = WorldId.FOREST
    }

    fun recordFlight(distance: Float, coinsEarned: Int, wasRecord: Boolean) {
        coins += coinsEarned
        flights += 1
        totalDistance += distance
        if (wasRecord && distance > bestDistance) bestDistance = distance
        refreshUnlocks()
    }

    fun toJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("coins", coins)
        root.put("bestDistance", bestDistance.toDouble())
        root.put("flights", flights)
        root.put("totalDistance", totalDistance.toDouble())
        root.put("selectedWorld", selectedWorld.name)

        val worlds = JSONArray()
        for (id in unlockedWorlds) worlds.put(id.name)
        root.put("unlockedWorlds", worlds)

        val parts = JSONObject()
        for ((slot, tier) in partTiers) parts.put(slot.name, tier)
        root.put("parts", parts)

        val slingshot = JSONObject()
        for ((type, level) in slingshotLevels) slingshot.put(type.name, level)
        root.put("slingshot", slingshot)

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
                profile.flights = root.optInt("flights", 0)
                profile.totalDistance = root.optDouble("totalDistance", 0.0).toFloat()

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
            } catch (error: Throwable) {
                Log.w("BLF-SAVE", "Could not parse save data, starting fresh", error)
                return PlayerProfile()
            }
            if (!profile.partTiers.containsKey(PartSlot.BODY)) profile.partTiers[PartSlot.BODY] = 1
            profile.refreshUnlocks()
            return profile
        }
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
