package com.buildlaunchfly.game.gfx

/**
 * Design system tokens, transcribed from docs/DESIGN.md §2.
 *
 * Colours are ARGB ints ready for android.graphics.Paint. Sizes, spacing, radii and safe
 * areas are in dp; the UI is drawn in a 640x360dp design space that scales 1:1 to the
 * document's 1920x1080px canvas at density 3.
 */
object DesignTokens {

    // ---- Primary aviation palette ----------------------------------------
    val COLOR_PRIMARY_AERO: Int = 0xFF0284C7.toInt()
    val COLOR_PRIMARY_GLOW: Int = 0xFF38BDF8.toInt()
    val COLOR_PRIMARY_DEEP: Int = 0xFF03254C.toInt()

    // ---- Currency & status accents ---------------------------------------
    val COLOR_COIN_GOLD: Int = 0xFFF59E0B.toInt()
    val COLOR_COIN_GOLD_GLOW: Int = 0x66FBBF24
    val COLOR_SUCCESS: Int = 0xFF10B981.toInt()
    val COLOR_HAZARD: Int = 0xFFEF4444.toInt()

    // ---- Glassmorphism surfaces ------------------------------------------
    val SURFACE_HUD_BG: Int = 0xB80A0F1D.toInt()   // 72% #0A0F1D
    val SURFACE_MODAL_BG: Int = 0xE0050814.toInt() // 88% #050814
    val GLASS_BORDER: Int = 0x2E38BDF8             // 18% #38BDF8
    val GLASS_HIGHLIGHT: Int = 0x14FFFFFF          // 8% white

    // ---- Text ------------------------------------------------------------
    val TEXT_PRIMARY: Int = 0xFFFFFFFF.toInt()
    val TEXT_MUTED: Int = 0xB3ABB7C4.toInt()
    val TEXT_INVERSE: Int = 0xFF06121F.toInt()

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

    const val RADIUS_PANEL = 16f
    const val RADIUS_BUTTON = 12f
    const val RADIUS_PILL = 26f

    // ---- Layout ----------------------------------------------------------
    // Portrait design space, per the reference plates: 360dp wide, 640dp tall at 16:9
    // and taller on long phones (the height is taken from the viewport at runtime).
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
