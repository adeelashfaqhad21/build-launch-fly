package com.buildlaunchfly.game.gfx

/**
 * Design tokens transcribed from the authoritative reference plates (Ref-1 … Ref-6):
 * a bright hypercasual palette — white cards, saturated headers, gold coins, blue gems,
 * chunky outlined lettering.
 */
object DesignTokens {

    // ---- Reference palette ------------------------------------------------
    val SKY: Int = 0xFF40B9E8.toInt()
    val SAND: Int = 0xFFC5A078.toInt()
    val SAND_LIGHT: Int = 0xFFD4B183.toInt()

    /** Card / panel surface. */
    val CARD: Int = 0xFFFFF9EF.toInt()

    /** Soft navy edge and shadow used under cards and buttons. */
    val CARD_EDGE: Int = 0x33185AB7
    val CARD_SHADOW: Int = 0x40000000

    /** Ink for text on white cards. */
    val INK: Int = 0xFF1B3A5B.toInt()
    val INK_MUTED: Int = 0xFF6A7C8E.toInt()
    val WHITE: Int = 0xFFFFFFFF.toInt()

    val NAVY: Int = 0xFF185AB7.toInt()
    val BLUE: Int = 0xFF1975D1.toInt()
    val GREEN: Int = 0xFF35D52D.toInt()
    val GOLD: Int = 0xFFFFB51F.toInt()
    val YELLOW: Int = 0xFFFFD52B.toInt()
    val RED: Int = 0xFFF33135.toInt()
    val PURPLE: Int = 0xFF9A4BEA.toInt()
    val LAVENDER: Int = 0xFFE7B5F3.toInt()
    val CYAN: Int = 0xFF19A9ED.toInt()
    val ORANGE: Int = 0xFFFF9D12.toInt()
    val GEM: Int = 0xFF22B7F0.toInt()
    val PLAQUE: Int = 0xFFA58B70.toInt()
    val PLAQUE_TEXT: Int = 0xFF583B1E.toInt()

    // ---- Backwards-compatible aliases used across the screens --------------
    val COLOR_PRIMARY_AERO: Int = BLUE
    val COLOR_PRIMARY_GLOW: Int = SKY
    val COLOR_PRIMARY_DEEP: Int = NAVY
    val COLOR_COIN_GOLD: Int = GOLD
    val COLOR_COIN_GOLD_GLOW: Int = 0x66FFD52B
    val COLOR_SUCCESS: Int = GREEN
    val COLOR_HAZARD: Int = RED

    val SURFACE_HUD_BG: Int = CARD
    val SURFACE_MODAL_BG: Int = CARD
    val GLASS_BORDER: Int = CARD_EDGE
    val GLASS_HIGHLIGHT: Int = WHITE

    val TEXT_PRIMARY: Int = INK
    val TEXT_MUTED: Int = INK_MUTED
    val TEXT_INVERSE: Int = WHITE

    // ---- Typography (dp) --------------------------------------------------
    const val TYPE_DISPLAY_HERO = 36f
    const val TYPE_HEADER_1 = 24f
    const val TYPE_HEADER_2 = 18f
    const val TYPE_HUD_LABEL = 14f
    const val TYPE_BODY_LARGE = 14f
    const val TYPE_BODY = 12f
    const val TYPE_BADGE = 10f

    // ---- Spacing & radii (dp) --------------------------------------------
    const val SPACE_XS = 4f
    const val SPACE_S = 8f
    const val SPACE_M = 16f
    const val SPACE_L = 24f
    const val SPACE_XL = 40f

    const val RADIUS_PANEL = 20f
    const val RADIUS_BUTTON = 22f
    const val RADIUS_PILL = 15f

    // ---- Layout ----------------------------------------------------------
    // Portrait design space, per the reference plates: 360dp wide, 640dp tall at 16:9.
    const val DESIGN_WIDTH = 360f
    const val DESIGN_HEIGHT = 640f
    const val MARGIN_H = 20f
    const val SAFE_TOP = 20f
    const val SAFE_BOTTOM = 16f
    const val TOUCH_MIN = 48f

    // ---- Helpers ---------------------------------------------------------

    fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255f).toInt()
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    fun mix(a: Int, b: Int, t: Float): Int {
        val f = t.coerceIn(0f, 1f)
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val aa = (a ushr 24) and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        val ba = (b ushr 24) and 0xFF
        val r = (ar + (br - ar) * f).toInt()
        val g = (ag + (bg - ag) * f).toInt()
        val bl = (ab + (bb - ab) * f).toInt()
        val al = (aa + (ba - aa) * f).toInt()
        return (al shl 24) or (r shl 16) or (g shl 8) or bl
    }
}
