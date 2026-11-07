package com.buildlaunchfly.game.ui

import android.graphics.Paint
import android.graphics.Typeface
import com.buildlaunchfly.game.gfx.DesignTokens

/** Font families from design.md §2.2, mapped to the closest Android system families. */
enum class UiFont { DISPLAY, BODY, BODY_MEDIUM }

/** Typography scale from design.md §2.2. */
enum class UiTextStyle(
    val sizeDp: Float,
    val font: UiFont,
    val tracking: Float
) {
    DISPLAY_HERO(DesignTokens.TYPE_DISPLAY_HERO, UiFont.DISPLAY, 0.02f),
    H1(DesignTokens.TYPE_HEADER_1, UiFont.DISPLAY, 0.03f),
    H2(DesignTokens.TYPE_HEADER_2, UiFont.DISPLAY, 0.02f),
    HUD_LABEL(DesignTokens.TYPE_HUD_LABEL, UiFont.DISPLAY, 0.05f),
    BODY_LARGE(DesignTokens.TYPE_BODY_LARGE, UiFont.BODY_MEDIUM, 0f),
    BODY(DesignTokens.TYPE_BODY, UiFont.BODY, 0f),
    BADGE(DesignTokens.TYPE_BADGE, UiFont.DISPLAY, 0.08f)
}

/** Reusable Paint set. The canvas is scaled to dp, so text sizes are dp values. */
class UiPainter {

    val fill: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    val stroke: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    val text: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isSubpixelText = true
        style = Paint.Style.FILL
    }

    private val displayTypeface: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    private val bodyTypeface: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    private val bodyMediumTypeface: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    fun typeface(font: UiFont): Typeface = when (font) {
        UiFont.DISPLAY -> displayTypeface
        UiFont.BODY -> bodyTypeface
        UiFont.BODY_MEDIUM -> bodyMediumTypeface
    }
}

/** Number/string formatting shared by all screens. */
object UiFormat {

    fun meters(value: Float): String {
        return if (value >= 1000f) {
            String.format("%.2f km", value / 1000f)
        } else {
            "${value.toInt()} m"
        }
    }

    fun metersInt(value: Float): String = "${value.toInt()} m"

    fun kmh(value: Float): String = "${value.toInt()} km/h"

    fun coins(value: Int): String = String.format("%,d", value)

    fun time(seconds: Float): String {
        val total = seconds.toInt().coerceAtLeast(0)
        val minutes = total / 60
        val secs = total % 60
        val tenths = ((seconds - total) * 10f).toInt().coerceIn(0, 9)
        return String.format("%02d:%02d.%d", minutes, secs, tenths)
    }

    fun percent(fraction: Float): String = "${(fraction.coerceIn(0f, 1f) * 100f).toInt()}%"

    fun signed(value: Float, unit: String = ""): String {
        val rounded = if (kotlin.math.abs(value) >= 10f) value.toInt() else value
        return if (value >= 0f) "+$rounded$unit" else "$rounded$unit"
    }
}
