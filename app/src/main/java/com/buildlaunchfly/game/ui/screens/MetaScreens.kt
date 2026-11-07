package com.buildlaunchfly.game.ui.screens

import android.graphics.Paint
import android.graphics.RectF
import com.buildlaunchfly.game.aircraft.AircraftCatalog
import com.buildlaunchfly.game.aircraft.PartSlot
import com.buildlaunchfly.game.game.AchievementCatalog
import com.buildlaunchfly.game.game.GraphicsQuality
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.ui.UiButtonStyle
import com.buildlaunchfly.game.ui.UiFormat
import com.buildlaunchfly.game.ui.UiFrame
import com.buildlaunchfly.game.ui.UiScreen
import com.buildlaunchfly.game.ui.UiTextStyle
import com.buildlaunchfly.game.world.WorldCatalog

/** Screen 7 — Upgrade & reward confirmation (design.md §6). */
object RewardScreen {

    fun draw(f: UiFrame) {
        val w = f.width
        val h = f.height
        f.solid(RectF(0f, 0f, w, h), 0f, DesignTokens.withAlpha(0x000000, 0.55f))

        val r = RectF((w - 400f) / 2f, (h - 236f) / 2f, (w + 400f) / 2f, (h + 236f) / 2f)
        f.panel(
            r,
            DesignTokens.RADIUS_PANEL,
            DesignTokens.SURFACE_MODAL_BG,
            DesignTokens.withAlpha(DesignTokens.COLOR_COIN_GOLD, 0.45f)
        )

        f.label("UPGRADE INSTALLED", r.centerX(), r.top + 34f, UiTextStyle.H1, DesignTokens.COLOR_COIN_GOLD, Paint.Align.CENTER)

        val slot = f.nav.rewardSlot
        val tier = if (slot != null) f.app.aircraftBuild.tierOf(slot) else 0
        val part = if (slot != null) AircraftCatalog.upgrade(slot, tier) else null

        f.label(
            part?.displayName ?: "COMPONENT READY",
            r.centerX(),
            r.top + 64f,
            UiTextStyle.H2,
            DesignTokens.TEXT_PRIMARY,
            Paint.Align.CENTER
        )

        val stats = f.app.aircraftBuild.stats()
        var y = r.top + 96f
        y = statRow(f, r, "LIFT COEFFICIENT", "%.2f".format(stats.lift), y)
        y = statRow(f, r, "AERODYNAMIC DRAG", "%.2f".format(stats.dragArea), y)
        y = statRow(f, r, "GLIDING STABILITY", "%.2f".format(stats.stability), y)
        row(f, r, "AIRFRAME MASS", "%.0f kg".format(stats.mass), y)

        val button = RectF(r.left + 20f, r.bottom - 48f, r.right - 20f, r.bottom - 12f)
        f.button(button, "EQUIP TO AIRCRAFT", UiButtonStyle.PRIMARY) {
            f.nav.go(UiScreen.AIRCRAFT)
        }
    }

    private fun statRow(f: UiFrame, panel: RectF, label: String, value: String, y: Float): Float {
        row(f, panel, label, value, y)
        return y + 20f
    }

    private fun row(f: UiFrame, panel: RectF, label: String, value: String, y: Float) {
        f.label(label, panel.left + 20f, y, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)
        f.label(value, panel.right - 20f, y, UiTextStyle.BODY_LARGE, DesignTokens.TEXT_PRIMARY, Paint.Align.RIGHT)
    }
}

/** Screen 8 — Settings (design.md §6). */
object SettingsScreen {

    fun draw(f: UiFrame) {
        val contentTop = f.header("SETTINGS")
        val w = f.width
        val h = f.height
        val settings = f.app.settings

        val r = f.rect(
            DesignTokens.MARGIN_H,
            contentTop + 6f,
            w - DesignTokens.MARGIN_H * 2f,
            h - contentTop - 6f - DesignTokens.SAFE_BOTTOM
        )
        f.panel(r)

        var y = r.top + 28f
        f.label("GRAPHICS QUALITY", r.left + 16f, y, UiTextStyle.HUD_LABEL, DesignTokens.TEXT_PRIMARY)
        y += 10f

        val qualities = GraphicsQuality.values()
        val gap = 6f
        val segmentWidth = (r.width() - 32f - gap * (qualities.size - 1)) / qualities.size
        qualities.forEachIndexed { index, quality ->
            val b = RectF(
                r.left + 16f + index * (segmentWidth + gap),
                y,
                r.left + 16f + index * (segmentWidth + gap) + segmentWidth,
                y + 26f
            )
            val active = settings.quality == quality
            f.solid(
                b,
                8f,
                if (active) DesignTokens.COLOR_PRIMARY_AERO else DesignTokens.withAlpha(0xFFFFFFFF.toInt(), 0.10f)
            )
            f.labelIn(
                b,
                quality.label.uppercase(),
                UiTextStyle.BADGE,
                if (active) DesignTokens.TEXT_PRIMARY else DesignTokens.TEXT_MUTED
            )
            f.hit(b) { f.app.requestSetQuality(quality) }
        }
        y += 38f

        y = toggleRow(f, r, "DYNAMIC WATER REFLECTIONS", settings.waterReflections, y) {
            settings.waterReflections = !settings.waterReflections
            f.app.requestReapplyGraphics(false)
        }
        y = toggleRow(f, r, "ATMOSPHERIC VOLUMETRIC FOG", settings.volumetricFog, y) {
            settings.volumetricFog = !settings.volumetricFog
            f.app.requestReapplyGraphics(false)
        }
        y = toggleRow(f, r, "HIGH-RESOLUTION TEXTURES", settings.highResTextures, y) {
            settings.highResTextures = !settings.highResTextures
            f.app.requestReapplyGraphics(true)
        }

        y += 8f
        f.divider(r.left + 16f, r.right - 16f, y)
        y += 16f

        y = sliderRow(f, r, "MASTER VOLUME", settings.masterVolume, y) { value ->
            settings.masterVolume = value
            f.app.requestApplyAudio()
        }
        y = sliderRow(f, r, "PROPELLER / ENGINE FX", settings.engineVolume, y) { value ->
            settings.engineVolume = value
            f.app.requestApplyAudio()
        }
        sliderRow(f, r, "NATURAL AMBIENCE", settings.ambienceVolume, y) { value ->
            settings.ambienceVolume = value
            f.app.requestApplyAudio()
        }
    }

    private fun toggleRow(f: UiFrame, panel: RectF, label: String, on: Boolean, y: Float, onToggle: () -> Unit): Float {
        val row = RectF(panel.left + 16f, y, panel.right - 16f, y + 20f)
        f.label(label, panel.left + 16f, y + 14f, UiTextStyle.BODY, DesignTokens.TEXT_PRIMARY)
        f.label(
            if (on) "ON" else "OFF",
            panel.right - 52f,
            y + 14f,
            UiTextStyle.BADGE,
            if (on) DesignTokens.COLOR_SUCCESS else DesignTokens.TEXT_MUTED,
            Paint.Align.RIGHT
        )
        f.toggle(row, on, onToggle)
        return y + 28f
    }

    private fun sliderRow(
        f: UiFrame,
        panel: RectF,
        label: String,
        value: Float,
        y: Float,
        onChange: (Float) -> Unit
    ): Float {
        f.label(label, panel.left + 16f, y + 10f, UiTextStyle.BODY, DesignTokens.TEXT_PRIMARY)
        f.label(
            UiFormat.percent(value),
            panel.right - 16f,
            y + 10f,
            UiTextStyle.BADGE,
            DesignTokens.COLOR_PRIMARY_GLOW,
            Paint.Align.RIGHT
        )
        f.slider(RectF(panel.left + 16f, y + 20f, panel.right - 16f, y + 32f), value, onChange)
        return y + 42f
    }
}

/** Screen 9 — Progress & achievements (design.md §6). */
object AchievementsScreen {

    fun draw(f: UiFrame) {
        val contentTop = f.header("PROGRESS & ACHIEVEMENTS")
        val w = f.width
        val profile = f.app.playerProfile
        val list = AchievementCatalog.all
        val gap = 10f
        val cardWidth = (w - DesignTokens.MARGIN_H * 2f - gap * 2f) / 3f
        val cardHeight = 94f

        list.forEachIndexed { index, achievement ->
            val column = index % 3
            val rowIndex = index / 3
            val x = DesignTokens.MARGIN_H + column * (cardWidth + gap)
            val y = contentTop + 8f + rowIndex * (cardHeight + gap)
            val r = f.rect(x, y, cardWidth, cardHeight)
            val claimed = profile.claimedAchievements.contains(achievement.id)
            val complete = AchievementCatalog.isComplete(achievement, profile)
            val fraction = AchievementCatalog.progressFraction(achievement, profile)

            f.panel(
                r,
                12f,
                DesignTokens.SURFACE_HUD_BG,
                if (claimed) DesignTokens.withAlpha(DesignTokens.COLOR_SUCCESS, 0.4f) else DesignTokens.GLASS_BORDER
            )

            f.label(achievement.title, r.left + 12f, r.top + 20f, UiTextStyle.H2)
            f.label(
                truncate(f, achievement.description, cardWidth - 24f, UiTextStyle.BODY),
                r.left + 12f,
                y + 36f,
                UiTextStyle.BODY,
                DesignTokens.TEXT_MUTED
            )

            f.progressBar(
                f.rect(r.left + 12f, r.top + 50f, cardWidth - 24f, 6f),
                AchievementCatalog.progressFraction(achievement, profile),
                if (claimed) DesignTokens.COLOR_SUCCESS else DesignTokens.COLOR_PRIMARY_GLOW
            )

            f.label(
                AchievementCatalog.progressText(achievement, profile),
                r.left + 12f,
                r.top + 72f,
                UiTextStyle.BADGE,
                DesignTokens.TEXT_MUTED
            )

            val claim = RectF(r.right - 84f, r.bottom - 26f, r.right - 10f, r.bottom - 6f)
            when {
                claimed -> f.labelIn(claim, "CLAIMED", UiTextStyle.BADGE, DesignTokens.COLOR_SUCCESS)
                complete -> {
                    f.solid(claim, 6f, DesignTokens.COLOR_COIN_GOLD)
                    f.labelIn(claim, "+${achievement.reward} CLAIM", UiTextStyle.BADGE, DesignTokens.TEXT_INVERSE)
                    f.hit(claim) { f.app.requestClaimAchievement(achievement.id) }
                }
                else -> f.labelIn(claim, "${(fraction * 100).toInt()}%", UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)
            }
        }
    }

    private fun truncate(f: UiFrame, text: String, maxWidth: Float, style: UiTextStyle): String {
        if (f.textWidth(text, style) <= maxWidth) return text
        var result = text
        while (result.length > 4 && f.textWidth("$result\u2026", style) > maxWidth) {
            result = result.substring(0, result.length - 1)
        }
        return "$result\u2026"
    }
}

/** Screen 10 — Pilot career profile (design.md §6). */
object ProfileScreen {

    fun draw(f: UiFrame) {
        val contentTop = f.header("PILOT CAREER")
        val w = f.width
        val h = f.height
        val profile = f.app.playerProfile
        val world = WorldCatalog.byId(profile.selectedWorld)

        val r = f.rect(
            DesignTokens.MARGIN_H,
            contentTop + 6f,
            w - DesignTokens.MARGIN_H * 2f,
            h - contentTop - 6f - DesignTokens.SAFE_BOTTOM
        )
        f.panel(r)

        f.label("Capt. Alexander", r.left + 20f, r.top + 30f, UiTextStyle.H1)
        f.label(
            "${profile.pilotRankTitle()} \u2022 Level ${profile.pilotLevel()}",
            r.left + 20f,
            r.top + 50f,
            UiTextStyle.BODY,
            DesignTokens.TEXT_MUTED
        )
        f.progressBar(f.rect(r.left + 20f, r.top + 58f, 200f, 5f), profile.levelProgressFraction())

        var y = r.top + 84f
        y = statRow(f, r, "TOTAL FLIGHTS", profile.flights.toString(), y)
        y = statRow(f, r, "TOTAL DISTANCE FLOWN", UiFormat.meters(profile.totalDistance), y)
        y = statRow(f, r, "BEST DISTANCE", UiFormat.meters(profile.bestDistance), y)
        y = statRow(f, r, "BEST ALTITUDE", UiFormat.metersInt(profile.bestAltitude), y)
        y = statRow(f, r, "FAVOURITE REALM", world.displayName, y)
        y = statRow(f, r, "AIRCRAFT CONFIGURATION", installedParts(f).toString() + " / 6 PARTS", y)
        statRow(f, r, "ACHIEVEMENTS CLAIMED", "${profile.claimedAchievements.size} / ${AchievementCatalog.all.size}", y)

        val gap = 10f
        val buttonWidth = (r.width() - 40f - gap * 2f) / 3f
        val buttonY = r.bottom - 44f
        val achievements = RectF(r.left + 20f, buttonY, r.left + 20f + buttonWidth, buttonY + 32f)
        val settings = RectF(achievements.right + gap, buttonY, achievements.right + gap + buttonWidth, buttonY + 32f)
        val workshop = RectF(settings.right + gap, buttonY, settings.right + gap + buttonWidth, buttonY + 32f)

        f.button(achievements, "ACHIEVEMENTS", UiButtonStyle.SECONDARY) { f.nav.go(UiScreen.ACHIEVEMENTS) }
        f.button(settings, "SETTINGS", UiButtonStyle.SECONDARY) { f.nav.go(UiScreen.SETTINGS) }
        f.button(workshop, "WORKSHOP", UiButtonStyle.SECONDARY) { f.nav.go(UiScreen.AIRCRAFT) }
    }

    private fun installedParts(f: UiFrame): Int {
        var count = 0
        for (slot in PartSlot.values()) {
            if (slot != PartSlot.BODY && f.app.aircraftBuild.tierOf(slot) > 0) count++
        }
        return count
    }

    private fun statRow(f: UiFrame, panel: RectF, label: String, value: String, y: Float): Float {
        f.label(label, panel.left + 20f, y, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)
        f.label(value, panel.right - 20f, y, UiTextStyle.BODY_LARGE, DesignTokens.TEXT_PRIMARY, Paint.Align.RIGHT)
        f.divider(panel.left + 20f, panel.right - 20f, y + 6f)
        return y + 22f
    }
}

/** Transient pause modal. */
object PauseScreen {

    fun draw(f: UiFrame) {
        val w = f.width
        val h = f.height
        f.solid(RectF(0f, 0f, w, h), 0f, DesignTokens.withAlpha(0x000000, 0.6f))

        val r = RectF((w - 300f) / 2f, (h - 172f) / 2f, (w + 300f) / 2f, (h + 172f) / 2f)
        f.panel(r, DesignTokens.RADIUS_PANEL, DesignTokens.SURFACE_MODAL_BG, DesignTokens.GLASS_BORDER)

        f.label("PAUSED", r.centerX(), r.top + 36f, UiTextStyle.H1, DesignTokens.TEXT_PRIMARY, Paint.Align.CENTER)
        f.label(
            "FLIGHT SUSPENDED",
            r.centerX(),
            r.top + 56f,
            UiTextStyle.BADGE,
            DesignTokens.TEXT_MUTED,
            Paint.Align.CENTER
        )

        val buttonWidth = r.width() - 40f
        f.button(f.rect(r.left + 20f, r.top + 76f, buttonWidth, 30f), "RESUME", UiButtonStyle.PRIMARY) {
            f.nav.setPaused(false)
        }
        f.button(f.rect(r.left + 20f, r.top + 112f, buttonWidth, 26f), "SETTINGS", UiButtonStyle.SECONDARY) {
            f.nav.setPaused(false)
            f.nav.go(UiScreen.SETTINGS)
        }
        f.button(f.rect(r.left + 20f, r.top + 142f, buttonWidth, 26f), "QUIT TO MENU", UiButtonStyle.DANGER) {
            f.nav.setPaused(false)
            f.app.requestReturnToMenu()
            f.nav.go(UiScreen.MENU)
        }
    }
}
