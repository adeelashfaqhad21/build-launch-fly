package com.buildlaunchfly.game.ui.screens

import android.graphics.Paint
import android.graphics.RectF
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.ui.UiButtonStyle
import com.buildlaunchfly.game.ui.UiFormat
import com.buildlaunchfly.game.ui.UiFrame
import com.buildlaunchfly.game.ui.UiIcon
import com.buildlaunchfly.game.ui.UiIcons
import com.buildlaunchfly.game.ui.UiScreen
import com.buildlaunchfly.game.ui.UiTextStyle
import com.buildlaunchfly.game.world.WorldCatalog
import com.buildlaunchfly.game.world.WorldDefinition
import com.buildlaunchfly.game.world.WorldId

/** Screen 4 — Environment Selection carousel (design.md §6). */
object WorldSelectScreen {

    fun draw(f: UiFrame) {
        val contentTop = f.header("ENVIRONMENT SELECTION")
        val worlds = WorldCatalog.all
        val gap = 10f
        val cardWidth = (f.width - DesignTokens.MARGIN_H * 2f - gap * 4f) / 5f
        val cardHeight = 170f

        worlds.forEachIndexed { index, definition ->
            val x = DesignTokens.MARGIN_H + index * (cardWidth + gap)
            val r = f.rect(x, contentTop + 14f, cardWidth, cardHeight)
            drawCard(f, r, index, worlds, worlds[index])
        }

        val selected = worlds[f.nav.worldCursor.coerceIn(0, worlds.size - 1)]
        val unlocked = f.app.playerProfile.unlockedWorlds.contains(selected.id)
        val buttonWidth = 240f
        val button = f.rect((f.width - buttonWidth) / 2f, f.height - DesignTokens.SAFE_BOTTOM - 44f, buttonWidth, 44f)

        if (unlocked) {
            f.button(button, "LAUNCH IN THIS REALM", UiButtonStyle.PRIMARY) {
                f.app.requestSelectWorld(selected.id)
                f.nav.go(UiScreen.LAUNCH)
            }
        } else {
            f.button(
                r = button,
                text = "LOCKED \u2014 REACH ${worlds[f.nav.worldCursor].unlockDistanceMeters.toInt()} m",
                style = UiButtonStyle.SECONDARY,
                enabled = false,
                onClick = {}
            )
        }
    }

    private fun drawCard(
        f: UiFrame,
        r: RectF,
        index: Int,
        worlds: List<WorldDefinition>,
        definition: WorldDefinition
    ) {
        val profile = f.app.playerProfile
        val selected = index == f.nav.worldCursor
        val unlocked = profile.unlockedWorlds.contains(definition.id)

        f.panel(
            r,
            12f,
            if (selected) DesignTokens.withAlpha(DesignTokens.COLOR_PRIMARY_AERO, 0.28f) else DesignTokens.SURFACE_HUD_BG,
            if (selected) DesignTokens.COLOR_PRIMARY_GLOW else DesignTokens.GLASS_BORDER
        )
        if (selected) {
            f.panelGlow(r, 12f, DesignTokens.withAlpha(DesignTokens.COLOR_PRIMARY_GLOW, 0.55f), 2f)
        }

        f.badge(r.left + 10f, r.top + 10f, if (unlocked) "OPEN" else "LOCKED",
            if (unlocked) DesignTokens.COLOR_SUCCESS else DesignTokens.COLOR_HAZARD)

        var y = r.top + 34f
        y = f.paragraph(
            definition.displayName.uppercase(),
            r.left + 10f,
            y,
            r.width() - 20f,
            UiTextStyle.H2,
            DesignTokens.TEXT_PRIMARY,
            19f
        )
        y = f.paragraph(
            weatherFor(definition.id),
            r.left + 10f,
            y + 6f,
            r.width() - 20f,
            UiTextStyle.BODY,
            DesignTokens.TEXT_MUTED,
            14f
        )

        f.label("WIND", r.left + 10f, y + 10f, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)
        f.label(windFor(definition.id), r.left + 10f, y + 24f, UiTextStyle.BODY, DesignTokens.TEXT_PRIMARY)

        f.divider(r.left + 10f, r.right - 10f, r.bottom - 44f)
        f.label("PERSONAL BEST", r.left + 10f, r.bottom - 26f, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)
        f.label(
            UiFormat.meters(profile.bestForWorld(definition.id)),
            r.right - 10f,
            r.bottom - 26f,
            UiTextStyle.HUD_LABEL,
            DesignTokens.COLOR_PRIMARY_GLOW,
            Paint.Align.RIGHT
        )

        if (!unlocked) {
            f.solid(r, 12f, DesignTokens.withAlpha(0x000000, 0.55f))
            UiIcons.draw(
                f.canvas,
                f.painter,
                UiIcon.LOCK,
                RectF(r.centerX() - 14f, r.centerY() - 20f, r.centerX() + 14f, r.centerY() + 8f),
                DesignTokens.TEXT_PRIMARY
            )
            f.label(
                "REACH ${definition.unlockDistanceMeters.toInt()} m",
                r.centerX(),
                r.centerY() + 28f,
                UiTextStyle.BADGE,
                DesignTokens.TEXT_PRIMARY,
                Paint.Align.CENTER
            )
        }

        f.hit(r, { f.nav.worldCursor = index.coerceIn(0, worlds.size - 1) })
    }

    private fun weatherFor(id: WorldId): String = when (id) {
        WorldId.FOREST -> "Mild sun shafts, emerald haze"
        WorldId.DESERT -> "Thermal updrafts, blowing dust"
        WorldId.SNOW -> "Light spindrift, ice fog"
        WorldId.OCEAN -> "Ocean squalls, sea spray"
        WorldId.MOUNTAIN -> "Fair-weather cumulus"
    }

    private fun windFor(id: WorldId): String = when (id) {
        WorldId.FOREST -> "4 km/h S"
        WorldId.DESERT -> "18 km/h W"
        WorldId.SNOW -> "12 km/h E"
        WorldId.OCEAN -> "22 km/h NE"
        WorldId.MOUNTAIN -> "9 km/h NW"
    }
}
