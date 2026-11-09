package com.buildlaunchfly.game.ui

import android.graphics.Paint
import android.graphics.RectF
import com.buildlaunchfly.game.gfx.DesignTokens
import com.buildlaunchfly.game.world.WorldCatalog
import com.buildlaunchfly.game.world.WorldDefinition
import com.buildlaunchfly.game.world.WorldId

/** Screen 4 — Environment Selection, a portrait list of the five realms. */
object WorldSelectScreen {

    fun draw(f: UiFrame) {
        val contentTop = f.header("ENVIRONMENT SELECTION")
        val worlds = WorldCatalog.all
        val cardHeight = 78f
        val gap = 8f

        worlds.forEachIndexed { index, definition ->
            val r = f.rect(
                DesignTokens.MARGIN_H,
                contentTop + 4f + index * (cardHeight + gap),
                f.width - DesignTokens.MARGIN_H * 2f,
                cardHeight
            )
            drawCard(f, r, index, worlds, definition)
        }

        val selected = worlds[f.nav.worldCursor.coerceIn(0, worlds.size - 1)]
        val unlocked = f.app.playerProfile.unlockedWorlds.contains(selected.id)
        val button = f.rect(
            DesignTokens.MARGIN_H,
            f.height - DesignTokens.SAFE_BOTTOM - 48f,
            f.width - DesignTokens.MARGIN_H * 2f,
            48f
        )

        if (unlocked) {
            f.button(button, "LAUNCH IN THIS REALM", UiButtonStyle.PRIMARY) {
                f.app.requestSelectWorld(selected.id)
                f.nav.go(UiScreen.LAUNCH)
            }
        } else {
            f.button(
                r = button,
                text = "LOCKED \u2014 REACH ${selected.unlockDistanceMeters.toInt()} m",
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

        f.badge(
            r.right - 60f,
            r.top + 9f,
            if (unlocked) "OPEN" else "LOCKED",
            if (unlocked) DesignTokens.COLOR_SUCCESS else DesignTokens.COLOR_HAZARD
        )

        f.label(
            truncate(f, definition.displayName.uppercase(), r.width() - 80f, UiTextStyle.H2),
            r.left + 10f,
            r.top + 26f,
            UiTextStyle.H2,
            DesignTokens.TEXT_PRIMARY
        )
        f.label(
            truncate(f, weatherFor(definition.id), r.width() - 20f, UiTextStyle.BODY),
            r.left + 10f,
            r.top + 44f,
            UiTextStyle.BODY,
            DesignTokens.TEXT_MUTED
        )

        f.label("WIND ${windFor(definition.id)}", r.left + 10f, r.bottom - 10f, UiTextStyle.BADGE, DesignTokens.TEXT_MUTED)
        f.label(
            "BEST ${UiFormat.meters(profile.bestForWorld(definition.id))}",
            r.right - 10f,
            r.bottom - 10f,
            UiTextStyle.BADGE,
            DesignTokens.COLOR_PRIMARY_GLOW,
            Paint.Align.RIGHT
        )

        if (!unlocked) {
            f.solid(r, 12f, DesignTokens.withAlpha(0x000000, 0.55f))
            f.label(
                "REACH ${definition.unlockDistanceMeters.toInt()} m",
                r.centerX(),
                r.centerY() + 4f,
                UiTextStyle.BADGE,
                DesignTokens.TEXT_PRIMARY,
                Paint.Align.CENTER
            )
        }

        f.hit(r, { f.nav.worldCursor = index.coerceIn(0, worlds.size - 1) })
    }

    private fun truncate(f: UiFrame, text: String, maxWidth: Float, style: UiTextStyle): String {
        if (f.textWidth(text, style) <= maxWidth) return text
        var result = text
        while (result.length > 4 && f.textWidth("$result\u2026", style) > maxWidth) {
            result = result.substring(0, result.length - 1)
        }
        return "$result\u2026"
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
