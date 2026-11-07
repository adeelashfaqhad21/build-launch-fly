package com.buildlaunchfly.game.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.buildlaunchfly.game.game.GameApp
import com.buildlaunchfly.game.gfx.DesignTokens

/** A clickable (and optionally draggable) region, stored in screen pixels for hit testing. */
class UiHit(
    val rect: RectF,
    val action: (() -> Unit)?,
    val onDrag: ((Float) -> Unit)? = null
)

enum class UiButtonStyle { PRIMARY, SECONDARY, GHOST, DANGER }

/**
 * Immediate-mode drawing surface for one frame. The canvas is already scaled so all
 * coordinates and sizes are in design dp (640 x 360 baseline, width adaptive).
 */
class UiFrame(
    val canvas: Canvas,
    val app: GameApp,
    val nav: UiNav,
    val width: Float,
    val height: Float,
    val scale: Float,
    val time: Float
) {

    val painter = UiPainter()
    val hits = ArrayList<UiHit>(48)

    fun rect(x: Float, y: Float, w: Float, h: Float): RectF = RectF(x, y, x + w, y + h)

    // ---- surfaces ---------------------------------------------------------

    fun panel(
        r: RectF,
        radius: Float = DesignTokens.RADIUS_PANEL,
        fill: Int = DesignTokens.SURFACE_HUD_BG,
        border: Int = DesignTokens.GLASS_BORDER
    ) {
        painter.fill.color = fill
        canvas.drawRoundRect(r, radius, radius, painter.fill)

        painter.stroke.color = DesignTokens.GLASS_HIGHLIGHT
        painter.stroke.strokeWidth = 1f
        canvas.drawLine(r.left + radius, r.top + 1f, r.right - radius, r.top + 1f, painter.stroke)

        painter.stroke.color = border
        painter.stroke.strokeWidth = 1f
        canvas.drawRoundRect(r, radius, radius, painter.stroke)
    }

    fun panelGlow(r: RectF, radius: Float, color: Int, width: Float = 2f) {
        painter.stroke.color = color
        painter.stroke.strokeWidth = width
        val expanded = RectF(r.left - 1.5f, r.top - 1.5f, r.right + 1.5f, r.bottom + 1.5f)
        canvas.drawRoundRect(expanded, radius + 1.5f, radius + 1.5f, painter.stroke)
    }

    fun solid(r: RectF, radius: Float, color: Int) {
        painter.fill.color = color
        canvas.drawRoundRect(r, radius, radius, painter.fill)
    }

    fun ring(r: RectF, radius: Float, color: Int, width: Float = 1f) {
        painter.stroke.color = color
        painter.stroke.strokeWidth = width
        canvas.drawRoundRect(r, radius, radius, painter.stroke)
    }

    fun circle(cx: Float, cy: Float, radius: Float, color: Int) {
        painter.fill.color = color
        canvas.drawCircle(cx, cy, radius, painter.fill)
    }

    fun ringCircle(cx: Float, cy: Float, radius: Float, color: Int, width: Float = 1f) {
        painter.stroke.color = color
        painter.stroke.strokeWidth = width
        canvas.drawCircle(cx, cy, radius, painter.stroke)
    }

    fun divider(x1: Float, x2: Float, y: Float) {
        painter.stroke.color = DesignTokens.withAlpha(0xFFFFFFFF.toInt(), 0.08f)
        painter.stroke.strokeWidth = 1f
        canvas.drawLine(x1, y, x2, y, painter.stroke)
    }

    // ---- text -------------------------------------------------------------

    fun label(
        text: String,
        x: Float,
        y: Float,
        style: UiTextStyle,
        color: Int = DesignTokens.TEXT_PRIMARY,
        align: Paint.Align = Paint.Align.LEFT
    ) {
        val p = painter.text
        p.typeface = painter.typeface(style.font)
        p.textSize = style.sizeDp
        p.color = color
        p.textAlign = Paint.Align.LEFT

        val tracking = style.sizeDp * style.tracking
        val rawWidth = p.measureText(text)
        val total = if (tracking <= 0.01f) rawWidth else rawWidth + tracking * (text.length - 1).coerceAtLeast(0)

        var cursor = when (align) {
            Paint.Align.CENTER -> x - total / 2f
            Paint.Align.RIGHT -> x - total
            else -> x
        }

        if (tracking <= 0.01f) {
            canvas.drawText(text, cursor, y, p)
            return
        }

        for (index in text.indices) {
            val ch = text[index].toString()
            canvas.drawText(ch, cursor, y, p)
            cursor += p.measureText(ch) + tracking
        }
    }

    fun labelIn(
        r: RectF,
        text: String,
        style: UiTextStyle,
        color: Int = DesignTokens.TEXT_PRIMARY,
        align: Paint.Align = Paint.Align.CENTER
    ) {
        val baseline = r.centerY() + style.sizeDp * 0.35f
        val x = when (align) {
            Paint.Align.CENTER -> r.centerX()
            Paint.Align.RIGHT -> r.right
            else -> r.left
        }
        label(text, x, baseline, style, color, align)
    }

    fun textWidth(text: String, style: UiTextStyle): Float {
        val p = painter.text
        p.typeface = painter.typeface(style.font)
        p.textSize = style.sizeDp
        return p.measureText(text)
    }

    /** Word-wrapped text. Returns the y of the next free line. */
    fun paragraph(
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        style: UiTextStyle,
        color: Int = DesignTokens.TEXT_MUTED,
        lineHeight: Float = style.sizeDp * 1.35f
    ): Float {
        val p = painter.text
        p.typeface = painter.typeface(style.font)
        p.textSize = style.sizeDp
        p.color = color
        p.textAlign = Paint.Align.LEFT

        var cursorY = y
        var line = StringBuilder()
        for (word in text.split(' ')) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (p.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line.toString(), x, cursorY, p)
                cursorY += lineHeight
                line = StringBuilder(word)
            } else {
                line = StringBuilder(candidate)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), x, cursorY, p)
            cursorY += lineHeight
        }
        return cursorY
    }

    // ---- interaction ------------------------------------------------------

    fun hit(r: RectF, action: (() -> Unit)?, onDrag: ((Float) -> Unit)? = null) {
        hits.add(
            UiHit(
                RectF(r.left * scale, r.top * scale, r.right * scale, r.bottom * scale),
                action,
                onDrag
            )
        )
    }

    fun button(
        r: RectF,
        text: String,
        style: UiButtonStyle = UiButtonStyle.SECONDARY,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) {
        val alpha = if (enabled) 1f else 0.45f

        when (style) {
            UiButtonStyle.PRIMARY -> {
                val pulse = 0.5f + 0.5f * kotlin.math.sin(time * 3.4f)
                solid(r, DesignTokens.RADIUS_BUTTON, DesignTokens.withAlpha(DesignTokens.COLOR_COIN_GOLD, alpha))
                panelGlow(
                    r,
                    DesignTokens.RADIUS_BUTTON,
                    DesignTokens.withAlpha(DesignTokens.COLOR_COIN_GOLD_GLOW, 0.35f + pulse * 0.35f),
                    2f
                )
                solid(
                    RectF(r.left + 2f, r.top + 2f, r.right - 2f, r.centerY()),
                    DesignTokens.RADIUS_BUTTON - 2f,
                    DesignTokens.withAlpha(0xFFFFFFFF.toInt(), 0.14f * alpha)
                )
                labelIn(r, text, UiTextStyle.H2, DesignTokens.withAlpha(DesignTokens.TEXT_INVERSE, alpha))
            }

            UiButtonStyle.SECONDARY -> {
                panel(
                    r,
                    DesignTokens.RADIUS_BUTTON,
                    DesignTokens.withAlpha(DesignTokens.SURFACE_HUD_BG, 0.85f * alpha),
                    DesignTokens.withAlpha(DesignTokens.GLASS_BORDER, alpha)
                )
                labelIn(r, text, UiTextStyle.BODY_LARGE, DesignTokens.withAlpha(DesignTokens.TEXT_PRIMARY, alpha))
            }

            UiButtonStyle.GHOST -> {
                labelIn(r, text, UiTextStyle.BODY_LARGE, DesignTokens.withAlpha(DesignTokens.TEXT_MUTED, alpha))
            }

            UiButtonStyle.DANGER -> {
                solid(r, DesignTokens.RADIUS_BUTTON, DesignTokens.withAlpha(DesignTokens.COLOR_HAZARD, 0.88f * alpha))
                labelIn(r, text, UiTextStyle.BODY_LARGE, DesignTokens.withAlpha(DesignTokens.TEXT_PRIMARY, alpha))
            }
        }

        if (enabled) hit(r, onClick)
    }

    fun iconButton(
        r: RectF,
        icon: UiIcon,
        tint: Int = DesignTokens.TEXT_PRIMARY,
        background: Boolean = true,
        onClick: () -> Unit
    ) {
        if (background) {
            panel(r, DesignTokens.RADIUS_BUTTON, DesignTokens.withAlpha(DesignTokens.SURFACE_HUD_BG, 0.8f))
        }
        UiIcons.draw(canvas, painter, icon, r, tint)
        hit(r, onClick)
    }

    fun toggle(r: RectF, on: Boolean, onToggle: () -> Unit) {
        val trackWidth = 40f
        val trackHeight = 20f
        val track = RectF(
            r.right - trackWidth,
            r.centerY() - trackHeight / 2f,
            r.right,
            r.centerY() + trackHeight / 2f
        )
        solid(
            track,
            trackHeight / 2f,
            if (on) DesignTokens.COLOR_PRIMARY_AERO else DesignTokens.withAlpha(0xFFFFFFFF.toInt(), 0.16f)
        )
        val knobX = if (on) track.right - 10f else track.left + 10f
        circle(knobX, track.centerY(), 7.5f, DesignTokens.TEXT_PRIMARY)
        hit(r, onToggle)
    }

    fun slider(r: RectF, value: Float, onChange: (Float) -> Unit) {
        val v = value.coerceIn(0f, 1f)
        val track = RectF(r.left, r.centerY() - 3f, r.right, r.centerY() + 3f)
        solid(track, 3f, DesignTokens.withAlpha(0xFFFFFFFF.toInt(), 0.14f))
        solid(
            RectF(track.left, track.top, track.left + track.width() * v, track.bottom),
            3f,
            DesignTokens.COLOR_PRIMARY_GLOW
        )
        val knobX = track.left + track.width() * v
        circle(knobX, track.centerY(), 8f, DesignTokens.TEXT_PRIMARY)
        ringCircle(knobX, track.centerY(), 8f, DesignTokens.withAlpha(DesignTokens.COLOR_PRIMARY_GLOW, 0.85f), 2f)
        hit(r, null) { fraction -> onChange(fraction.coerceIn(0f, 1f)) }
    }

    fun progressBar(
        r: RectF,
        fraction: Float,
        fill: Int = DesignTokens.COLOR_PRIMARY_GLOW,
        background: Int = DesignTokens.withAlpha(0xFFFFFFFF.toInt(), 0.12f)
    ) {
        val f = fraction.coerceIn(0f, 1f)
        solid(r, r.height() / 2f, background)
        if (f > 0f) {
            solid(RectF(r.left, r.top, r.left + r.width() * f, r.bottom), r.height() / 2f, fill)
        }
    }

    fun badge(
        x: Float,
        y: Float,
        text: String,
        fill: Int = DesignTokens.COLOR_PRIMARY_AERO,
        textColor: Int = DesignTokens.TEXT_PRIMARY
    ) {
        val width = textWidth(text, UiTextStyle.BADGE) + 12f
        val r = RectF(x, y, x + width, y + 16f)
        solid(r, 8f, fill)
        labelIn(r, text, UiTextStyle.BADGE, textColor)
    }

    /** Standard screen header with back control, title and coin readout. Returns content top. */
    fun header(title: String, showCoins: Boolean = true): Float {
        val backRect = rect(DesignTokens.MARGIN_H, DesignTokens.SAFE_TOP, 36f, 40f)
        iconButton(backRect, UiIcon.BACK) { nav.back() }

        label(
            text = text.uppercase(),
            x = DesignTokens.MARGIN_H + 48f,
            y = DesignTokens.SAFE_TOP + 20f,
            style = UiTextStyle.H1,
            color = DesignTokens.TEXT_PRIMARY
        )

        if (showCoins) {
            val coinRect = rect(width - DesignTokens.MARGIN_H - 116f, DesignTokens.SAFE_TOP, 116f, 36f)
            panel(coinRect, DesignTokens.RADIUS_PILL)
            UiIcons.draw(
                canvas,
                painter,
                UiIcon.COIN,
                RectF(coinRect.left + 6f, coinRect.centerY() - 10f, coinRect.left + 26f, coinRect.centerY() + 10f),
                DesignTokens.COLOR_COIN_GOLD
            )
            label(
                UiFormat.coins(app.playerProfile.coins),
                coinRect.right - 10f,
                coinRect.centerY() + 4f,
                UiTextStyle.HUD_LABEL,
                DesignTokens.TEXT_PRIMARY,
                Paint.Align.RIGHT
            )
        }

        return DesignTokens.SAFE_TOP + 56f
    }
}
