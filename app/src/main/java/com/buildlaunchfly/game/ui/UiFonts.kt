package com.buildlaunchfly.game.ui

import android.content.Context
import android.graphics.Typeface
import com.buildlaunchfly.game.R

/**
 * Bundled typefaces from docs/DESIGN.md §2.2:
 * Rajdhani (Bold / SemiBold) for instrumentation and headings, Plus Jakarta Sans for body
 * copy. Both are OFL; licences live in docs/licenses/.
 *
 * Falling back to platform typefaces keeps the game usable if a font resource is missing.
 */
object UiFonts {

    private var initialized = false
    private var displayBold: Typeface? = null
    private var displaySemiBold: Typeface? = null
    private var bodyRegular: Typeface? = null

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        displayBold = load(context, R.font.rajdhani_bold)
        displaySemiBold = load(context, R.font.rajdhani_semibold)
        bodyRegular = load(context, R.font.plus_jakarta_sans)
    }

    fun displayBold(): Typeface = displayBold ?: Typeface.DEFAULT_BOLD

    fun displaySemiBold(): Typeface = displaySemiBold ?: Typeface.DEFAULT_BOLD

    fun body(): Typeface = bodyRegular ?: Typeface.DEFAULT

    private fun load(context: Context, id: Int): Typeface? {
        return try {
            context.resources.getFont(id)
        } catch (error: Throwable) {
            android.util.Log.w("BLF-FONT", "Font resource unavailable: $id", error)
            null
        }
    }
}
