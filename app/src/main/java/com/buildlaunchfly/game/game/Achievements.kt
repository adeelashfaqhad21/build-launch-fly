package com.buildlaunchfly.game.game

import com.buildlaunchfly.game.world.WorldId

/** What an achievement measures. */
enum class AchievementMetric {
    BEST_DISTANCE,
    BEST_ALTITUDE,
    TOTAL_DISTANCE,
    FLIGHTS,
    WORLD_DISTANCE,
    WORLDS_UNLOCKED,
    GLACIER_CREVASSE_CROSSED
}

class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val metric: AchievementMetric,
    val target: Float,
    val reward: Int,
    val world: WorldId? = null,
    val unit: String = "m"
)

object AchievementCatalog {

    val all: List<Achievement> = listOf(
        Achievement(
            id = "cloud_piercer",
            title = "Cloud Piercer",
            description = "Reach 1,000 m above the terrain in any realm.",
            metric = AchievementMetric.BEST_ALTITUDE,
            target = 1000f,
            reward = 250
        ),
        Achievement(
            id = "desert_falcon",
            title = "Desert Falcon",
            description = "Fly 5,000 m in the amber dunes.",
            metric = AchievementMetric.WORLD_DISTANCE,
            target = 5000f,
            reward = 300,
            world = WorldId.DESERT
        ),
        Achievement(
            id = "glacier_master",
            title = "Glacier Master",
            description = "Cross an exposed glacier crevasse zone.",
            metric = AchievementMetric.GLACIER_CREVASSE_CROSSED,
            target = 1f,
            reward = 300,
            unit = ""
        ),
        Achievement(
            id = "long_hauler",
            title = "Long Hauler",
            description = "Cover 10,000 m in total flight distance.",
            metric = AchievementMetric.TOTAL_DISTANCE,
            target = 10000f,
            reward = 400
        ),
        Achievement(
            id = "frequent_flyer",
            title = "Frequent Flyer",
            description = "Complete 25 flights.",
            metric = AchievementMetric.FLIGHTS,
            target = 25f,
            reward = 350,
            unit = ""
        ),
        Achievement(
            id = "realm_explorer",
            title = "Realm Explorer",
            description = "Unlock all five ecosystems.",
            metric = AchievementMetric.WORLDS_UNLOCKED,
            target = 5f,
            reward = 600,
            unit = ""
        )
    )

    fun byId(id: String): Achievement? = all.firstOrNull { it.id == id }

    fun progressValue(achievement: Achievement, profile: PlayerProfile): Float {
        return when (achievement.metric) {
            AchievementMetric.BEST_DISTANCE -> profile.bestDistance
            AchievementMetric.BEST_ALTITUDE -> profile.bestAltitude
            AchievementMetric.TOTAL_DISTANCE -> profile.totalDistance
            AchievementMetric.FLIGHTS -> profile.flights.toFloat()
            AchievementMetric.WORLD_DISTANCE ->
                profile.bestPerWorld[achievement.world ?: WorldId.FOREST] ?: 0f
            AchievementMetric.WORLDS_UNLOCKED -> profile.unlockedWorlds.size.toFloat()
            AchievementMetric.GLACIER_CREVASSE_CROSSED ->
                if (profile.glacierCrevasseCrossed) 1f else 0f
        }
    }

    fun progressFraction(achievement: Achievement, profile: PlayerProfile): Float {
        if (achievement.target <= 0f) return 1f
        return (progressValue(achievement, profile) / achievement.target).coerceIn(0f, 1f)
    }

    fun isComplete(achievement: Achievement, profile: PlayerProfile): Boolean =
        progressValue(achievement, profile) >= achievement.target

    fun progressText(achievement: Achievement, profile: PlayerProfile): String {
        val value = progressValue(achievement, profile).coerceAtMost(achievement.target)
        val current = if (achievement.unit.isEmpty()) {
            value.toInt().toString()
        } else {
            "${value.toInt()} ${achievement.unit}"
        }
        val target = if (achievement.unit.isEmpty()) {
            achievement.target.toInt().toString()
        } else {
            "${achievement.target.toInt()} ${achievement.unit}"
        }
        return "$current / $target"
    }
}
