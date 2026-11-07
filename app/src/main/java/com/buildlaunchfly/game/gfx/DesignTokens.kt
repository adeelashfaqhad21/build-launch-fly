package com.buildlaunchfly.game.gfx

/**
 * Placeholder for the design system. Once `docs/DESIGN.md` is supplied this object becomes
 * the single source of truth for colours, type scale, spacing and motion so that no screen
 * hard-codes a value. Values below are intentionally neutral scaffolding only.
 */
object DesignTokens {

    // Colours (linear-ish RGB, 0..1) — to be replaced from design.md.
    val accentPrimary = Vec3(0.31f, 0.76f, 0.97f)
    val accentSecondary = Vec3(0.98f, 0.72f, 0.28f)
    val surfaceDark = Vec3(0.05f, 0.08f, 0.12f)
    val surfaceLight = Vec3(0.92f, 0.95f, 0.98f)
    val textPrimary = Vec3(1f, 1f, 1f)
    val textMuted = Vec3(0.72f, 0.78f, 0.84f)

    // Spacing scale in dp.
    const val SPACE_XS = 4f
    const val SPACE_S = 8f
    const val SPACE_M = 16f
    const val SPACE_L = 24f
    const val SPACE_XL = 40f

    // Corner radii in dp.
    const val RADIUS_S = 6f
    const val RADIUS_M = 12f
    const val RADIUS_L = 20f

    // Motion durations in milliseconds.
    const val MOTION_FAST = 120
    const val MOTION_BASE = 220
    const val MOTION_SLOW = 380

    // Layout.
    const val DESIGN_ASPECT = 16f / 9f
    const val SAFE_MARGIN_DP = 20f
}
